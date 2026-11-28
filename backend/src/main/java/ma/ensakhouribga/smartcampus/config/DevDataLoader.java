package ma.ensakhouribga.smartcampus.config;

import ma.ensakhouribga.smartcampus.user.Etudiant;
import ma.ensakhouribga.smartcampus.user.Niveau;
import ma.ensakhouribga.smartcampus.user.NiveauRepository;
import ma.ensakhouribga.smartcampus.user.Role;
import ma.ensakhouribga.smartcampus.user.User;
import ma.ensakhouribga.smartcampus.user.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import ma.ensakhouribga.smartcampus.notification.NotificationRepository;
import ma.ensakhouribga.smartcampus.notification.NotificationService;
import ma.ensakhouribga.smartcampus.notification.TypeObjet;
import ma.ensakhouribga.smartcampus.espace.Espace;
import ma.ensakhouribga.smartcampus.espace.EspaceRepository;
import ma.ensakhouribga.smartcampus.espace.TypeEspace;
import ma.ensakhouribga.smartcampus.user.Enseignant;

/**
 * Crée des comptes de test (données fictives) pour le développement uniquement.
 * Actif seulement avec le profil « dev » : mvn spring-boot:run -Dspring-boot.run.profiles=dev
 */
@Component
@Profile("dev")
public class DevDataLoader implements CommandLineRunner {

    static final String TEST_EMAIL = "test@usms.ac.ma";
    static final String TEST_PASSWORD = "test1234";
    static final String ADMIN_EMAIL = "admin@ensakh.ma";
    static final String ADMIN_PASSWORD = "admin1234";
    /** Niveaux de l'ENSA Khouribga : cycle préparatoire puis filières d'ingénieur (années 1 à 3). */
    static final String[] NIVEAUX = {
        "CP1", "CP2",
        "GI1", "GI2", "GI3",
        "IID1", "IID2", "IID3",
        "MGSI1", "MGSI2", "MGSI3",
        "GE1", "GE2", "GE3",
        "GP1", "GP2", "GP3"
    };

    private final UserRepository users;
    private final NiveauRepository niveaux;
    private final PasswordEncoder passwordEncoder;
    private final NotificationRepository notifications;
    private final NotificationService notificationService;

    private final EspaceRepository espaces;

    public DevDataLoader(UserRepository users, NiveauRepository niveaux, PasswordEncoder passwordEncoder,
                    NotificationRepository notifications, NotificationService notificationService,
                    EspaceRepository espaces)   {
        this.users = users;
        this.niveaux = niveaux;
        this.passwordEncoder = passwordEncoder;
        this.notifications = notifications;
        this.notificationService = notificationService;
        this.espaces = espaces;
    }

    @Override
    public void run(String... args) {
        // Niveaux proposés dans le formulaire de création d'un étudiant.
        for (String nom : NIVEAUX) {
            niveaux.findByNom(nom).orElseGet(() -> niveaux.save(new Niveau(nom)));
        }
        Niveau gi3 = niveaux.findByNom("GI3").orElseThrow();

        if (users.findByEmail(TEST_EMAIL).isEmpty()) {
            users.save(new Etudiant(TEST_EMAIL, passwordEncoder.encode(TEST_PASSWORD),
                    "Etudiant", "Test", true,
                    "D000000000", "0001", "Génie Informatique", gi3));
        }
        if (users.findByEmail(ADMIN_EMAIL).isEmpty()) {
            users.save(new User(ADMIN_EMAIL, passwordEncoder.encode(ADMIN_PASSWORD),
                    "Admin", "Test", Role.ADMINISTRATEUR, true));
        }

                // Comptes de test supplémentaires (SC-194) : 3 étudiants et 2 enseignants au total.
        Niveau gi2 = niveaux.findByNom("GI2").orElseThrow();
        creerEtudiant("sara.etudiant@usms.ac.ma", "Sara", "Alaoui", "D111111111", "0002", "Génie Informatique", gi3);
        creerEtudiant("ahmed.etudiant@usms.ac.ma", "ahmed", "Bennani", "D222222222", "0003", "Génie Informatique", gi2);
        creerEnseignant("enseignant@usms.ma", "Karim", "Idrissi", "ENS001", "1001", "PES", "Informatique");
        creerEnseignant("nadia.enseignant@usms.ma", "Nadia", "Tazi", "ENS002", "1002", "PH", "Mathématiques");

        // Un espace de chaque type (SC-194).
        creerEspace("AMPHI-A", "Amphi A", TypeEspace.AMPHITHEATRE);
        creerEspace("SC-101", "Salle de cours 101", TypeEspace.SALLE_COURS);
        creerEspace("SC-102", "Salle de cours 102", TypeEspace.SALLE_COURS);
        creerEspace("REU-1", "Salle de réunion 1", TypeEspace.SALLE_REUNION);
        creerEspace("BIB-1", "Bibliothèque centrale", TypeEspace.BIBLIOTHEQUE);
        creerEspace("TER-1", "Terrain de sport", TypeEspace.TERRAIN);
        creerEspace("BUV-1", "Buvette", TypeEspace.BUVETTE);
        // TODO SC-19 : passer SC-102 en MAINTENANCE quand le statut existera.

        // Quelques notifications de test (SC-140), créées une seule fois.
        for (String email : new String[] {TEST_EMAIL, ADMIN_EMAIL, "enseignant@usms.ma"}) {
            User user = users.findByEmail(email).orElseThrow();
            if (!notifications.existsByUtilisateurId(user.getId())) {
                notificationService.notifier(user.getId(),
                        "Bienvenue sur SmartCampus : vos notifications apparaîtront ici.", TypeObjet.RESERVATION, null);
                notificationService.notifier(user.getId(),
                        "Exemple : votre demande pour l'Amphi A a été validée.", TypeObjet.RESERVATION, 1L);
            }
        }
    }
        private void creerEtudiant(String email, String prenom, String nom, String codeMassar,
                               String numero, String filiere, Niveau niveau) {
        if (users.findByEmail(email).isEmpty()) {
            users.save(new Etudiant(email, passwordEncoder.encode(TEST_PASSWORD), prenom, nom, true,
                    codeMassar, numero, filiere, niveau));
        }
    }

    private void creerEnseignant(String email, String prenom, String nom, String codeEnseignant,
                                 String numero, String grade, String specialite) {
        if (users.findByEmail(email).isEmpty()) {
            users.save(new Enseignant(email, passwordEncoder.encode(TEST_PASSWORD), prenom, nom, true,
                    codeEnseignant, numero, grade, specialite));
        }
    }

    private void creerEspace(String code, String nom, TypeEspace type) {
        if (!espaces.existsByCode(code)) {
            espaces.save(new Espace(code, nom, type));
        }
    }
}
