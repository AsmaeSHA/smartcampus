package ma.ensakhouribga.smartcampus.compte;

import java.util.List;
import java.util.Locale;
import ma.ensakhouribga.smartcampus.user.DomainesEmail;
import ma.ensakhouribga.smartcampus.user.Enseignant;
import ma.ensakhouribga.smartcampus.user.EnseignantRepository;
import ma.ensakhouribga.smartcampus.user.Etudiant;
import ma.ensakhouribga.smartcampus.user.EtudiantRepository;
import ma.ensakhouribga.smartcampus.user.Niveau;
import ma.ensakhouribga.smartcampus.user.NiveauRepository;
import ma.ensakhouribga.smartcampus.user.Role;
import ma.ensakhouribga.smartcampus.user.User;
import ma.ensakhouribga.smartcampus.user.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Gestion des comptes par l'administrateur (SC-27 : création).
 * Règles appliquées :
 * - l'e-mail doit être unique et utiliser le domaine du rôle (R1) :
 *   étudiant @usms.ac.ma, enseignant @usms.ma, administrateur @ensakh.ma ;
 * - les champs propres au rôle sont obligatoires (code Massar, filière… pour un étudiant ;
 *   code enseignant, grade… pour un enseignant) ;
 * - le mot de passe est généré par le serveur, stocké haché (BCrypt) et doit être changé
 *   à la première connexion.
 */
@Service
public class CompteService {

    private final UserRepository users;
    private final EtudiantRepository etudiants;
    private final EnseignantRepository enseignants;
    private final NiveauRepository niveaux;
    private final PasswordEncoder passwordEncoder;
    private final GenerateurMotDePasse generateur;
    private final DomainesEmail domaines;

    public CompteService(UserRepository users,
                         EtudiantRepository etudiants,
                         EnseignantRepository enseignants,
                         NiveauRepository niveaux,
                         PasswordEncoder passwordEncoder,
                         GenerateurMotDePasse generateur,
                         DomainesEmail domaines) {
        this.users = users;
        this.etudiants = etudiants;
        this.enseignants = enseignants;
        this.niveaux = niveaux;
        this.passwordEncoder = passwordEncoder;
        this.generateur = generateur;
        this.domaines = domaines;
    }

    @Transactional
    public CompteCree creer(CreerCompteRequest demande) {
        String email = demande.email().trim().toLowerCase(Locale.ROOT);
        if (!domaines.correspond(email, demande.role())) {
            throw new CompteInvalideException("Pour ce rôle, l'e-mail doit se terminer par @"
                    + domaines.pour(demande.role()));
        }
        if (users.findByEmail(email).isPresent()) {
            throw new CompteExistantException("Un compte existe déjà avec cet e-mail");
        }

        String motDePasse = generateur.generer();
        String hash = passwordEncoder.encode(motDePasse);
        String prenom = demande.prenom().trim();
        String nom = demande.nom().trim();

        User compte = switch (demande.role()) {
            case ETUDIANT -> creerEtudiant(demande, email, hash, prenom, nom);
            case ENSEIGNANT -> creerEnseignant(demande, email, hash, prenom, nom);
            case ADMINISTRATEUR -> new User(email, hash, prenom, nom, Role.ADMINISTRATEUR, true);
        };
        compte.setChangerMotDePasse(true);

        User enregistre;
        try {
            enregistre = users.saveAndFlush(compte);
        } catch (DataIntegrityViolationException e) {
            // Deux créations simultanées avec le même e-mail ou le même code : la base tranche.
            throw new CompteExistantException("Un compte existe déjà avec cet e-mail ou ce code");
        }
        return new CompteCree(CompteResume.depuis(enregistre), motDePasse);
    }

    @Transactional(readOnly = true)
    public List<CompteResume> lister() {
        return users.findAll(Sort.by("nom", "prenom")).stream()
                .map(CompteResume::depuis)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<NiveauResume> listerNiveaux() {
        return niveaux.findAll(Sort.by("nom")).stream()
                .map(NiveauResume::depuis)
                .toList();
    }

    private Etudiant creerEtudiant(CreerCompteRequest demande, String email, String hash,
                                   String prenom, String nom) {
        String codeMassar = requis(demande.codeMassar(), "Le code Massar");
        String numero = requis(demande.numero(), "Le numéro");
        String filiere = requis(demande.filiere(), "La filière");
        if (demande.niveauId() == null) {
            throw new CompteInvalideException("Le niveau est obligatoire pour un étudiant");
        }
        Niveau niveau = niveaux.findById(demande.niveauId())
                .orElseThrow(() -> new CompteInvalideException("Ce niveau n'existe pas"));
        if (etudiants.existsByCodeMassar(codeMassar)) {
            throw new CompteExistantException("Un étudiant existe déjà avec ce code Massar");
        }
        return new Etudiant(email, hash, prenom, nom, true, codeMassar, numero, filiere, niveau);
    }

    private Enseignant creerEnseignant(CreerCompteRequest demande, String email, String hash,
                                       String prenom, String nom) {
        String codeEnseignant = requis(demande.codeEnseignant(), "Le code enseignant");
        String numero = requis(demande.numero(), "Le numéro");
        String grade = requis(demande.grade(), "Le grade");
        String specialite = requis(demande.specialite(), "La spécialité");
        if (enseignants.existsByCodeEnseignant(codeEnseignant)) {
            throw new CompteExistantException("Un enseignant existe déjà avec ce code");
        }
        return new Enseignant(email, hash, prenom, nom, true, codeEnseignant, numero, grade, specialite);
    }

    private static String requis(String valeur, String champ) {
        if (valeur == null || valeur.isBlank()) {
            throw new CompteInvalideException(champ + " est obligatoire pour ce rôle");
        }
        return valeur.trim();
    }
}
