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

/**
 * Crée des comptes de test (données fictives) pour le développement uniquement.
 * Actif seulement avec le profil « dev » : mvn spring-boot:run -Dspring-boot.run.profiles=dev
 */
@Component
@Profile("dev")
public class DevDataLoader implements CommandLineRunner {

    static final String TEST_EMAIL = "test@usms.ma";
    static final String TEST_PASSWORD = "test1234";
    static final String ADMIN_EMAIL = "admin@usms.ma";
    static final String ADMIN_PASSWORD = "admin1234";

    private final UserRepository users;
    private final NiveauRepository niveaux;
    private final PasswordEncoder passwordEncoder;

    public DevDataLoader(UserRepository users, NiveauRepository niveaux, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.niveaux = niveaux;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        Niveau gi3 = niveaux.findByNom("GI3").orElseGet(() -> niveaux.save(new Niveau("GI3")));

        if (users.findByEmail(TEST_EMAIL).isEmpty()) {
            users.save(new Etudiant(TEST_EMAIL, passwordEncoder.encode(TEST_PASSWORD),
                    "Etudiant", "Test", true,
                    "D000000000", "0001", "Génie Informatique", gi3));
        }
        if (users.findByEmail(ADMIN_EMAIL).isEmpty()) {
            users.save(new User(ADMIN_EMAIL, passwordEncoder.encode(ADMIN_PASSWORD),
                    "Admin", "Test", Role.ADMINISTRATEUR, true));
        }
    }
}
