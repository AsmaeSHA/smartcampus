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
 * Gestion des comptes par l'administrateur.
 * SC-27 : création.
 * - l'e-mail doit être unique et utiliser le domaine du rôle (R1) :
 *   étudiant @usms.ac.ma, enseignant @usms.ma, administrateur @ensakh.ma ;
 * - les champs propres au rôle sont obligatoires (code Massar, filière… pour un étudiant ;
 *   code enseignant, grade… pour un enseignant) ;
 * - le mot de passe est généré par le serveur, stocké haché (BCrypt) et doit être changé
 *   à la première connexion.
 * SC-28 : désactivation et réactivation, sans suppression des données ;
 * un administrateur ne peut pas désactiver son propre compte.
 * SC-64 : modification des informations et du rôle, avec les mêmes règles que la création ;
 * un administrateur ne peut pas retirer son propre rôle d'administrateur.
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
    private final ChangementRole changementRole;

    public CompteService(UserRepository users,
                         EtudiantRepository etudiants,
                         EnseignantRepository enseignants,
                         NiveauRepository niveaux,
                         PasswordEncoder passwordEncoder,
                         GenerateurMotDePasse generateur,
                         DomainesEmail domaines,
                         ChangementRole changementRole) {
        this.users = users;
        this.etudiants = etudiants;
        this.enseignants = enseignants;
        this.niveaux = niveaux;
        this.passwordEncoder = passwordEncoder;
        this.generateur = generateur;
        this.domaines = domaines;
        this.changementRole = changementRole;
    }

    // ---------- SC-27 : créer un compte ----------

    @Transactional
    public CompteCree creer(CreerCompteRequest demande) {
        String email = emailValide(demande);
        if (users.findByEmail(email).isPresent()) {
            throw new CompteExistantException("Un compte existe déjà avec cet e-mail");
        }

        String motDePasse = generateur.generer();
        String hash = passwordEncoder.encode(motDePasse);
        String prenom = demande.prenom().trim();
        String nom = demande.nom().trim();

        User compte = switch (demande.role()) {
            case ETUDIANT -> {
                ChampsEtudiant c = champsEtudiant(demande, null);
                yield new Etudiant(email, hash, prenom, nom, true, c.codeMassar(), c.numero(), c.filiere(), c.niveau());
            }
            case ENSEIGNANT -> {
                ChampsEnseignant c = champsEnseignant(demande, null);
                yield new Enseignant(email, hash, prenom, nom, true,
                        c.codeEnseignant(), c.numero(), c.grade(), c.specialite());
            }
            case ADMINISTRATEUR -> new User(email, hash, prenom, nom, Role.ADMINISTRATEUR, true);
        };
        compte.setChangerMotDePasse(true);

        return new CompteCree(CompteResume.depuis(enregistrer(compte)), motDePasse);
    }

    /** SC-72 : produit un mot de passe provisoire sécurisé, sans jamais stocker sa valeur en clair. */
    @Transactional
    public CompteCree reinitialiserMotDePasse(Long id) {
        User compte = users.findById(id).orElseThrow(() -> new CompteIntrouvableException("Compte introuvable"));
        String temporaire = generateur.generer();
        compte.reinitialiserMotDePasse(passwordEncoder.encode(temporaire));
        return new CompteCree(CompteResume.depuis(users.save(compte)), temporaire);
    }

    // ---------- SC-28 : désactiver et réactiver ----------

    /**
     * Active ou désactive un compte (SC-28). Rien n'est supprimé : seul l'état change.
     * Un compte désactivé ne peut plus se connecter (R2) et perd l'accès dès sa requête suivante.
     *
     * @param idAdministrateur id de l'administrateur qui fait la demande
     */
    @Transactional
    public CompteResume changerActivation(Long id, boolean actif, Long idAdministrateur) {
        User compte = trouver(id);
        if (!actif && compte.getId().equals(idAdministrateur)) {
            throw new CompteInvalideException("Vous ne pouvez pas désactiver votre propre compte");
        }
        if (actif) {
            compte.reactiver();
        } else {
            compte.desactiver();
        }
        return CompteResume.depuis(users.save(compte));
    }

    // ---------- SC-64 : modifier le rôle et les informations ----------

    @Transactional(readOnly = true)
    public CompteDetail detail(Long id) {
        return CompteDetail.depuis(trouver(id));
    }

    /**
     * Modifie les informations d'un compte et, si besoin, son rôle (SC-64).
     * Le mot de passe, l'état actif et l'historique ne changent pas.
     * Si l'e-mail change, l'utilisateur est déconnecté et doit se reconnecter avec le nouvel e-mail.
     *
     * @param idAdministrateur id de l'administrateur qui fait la demande
     */
    @Transactional
    public CompteResume modifier(Long id, CreerCompteRequest demande, Long idAdministrateur) {
        User compte = trouver(id);
        if (compte.getId().equals(idAdministrateur) && demande.role() != Role.ADMINISTRATEUR) {
            throw new CompteInvalideException("Vous ne pouvez pas retirer votre propre rôle d'administrateur");
        }

        String email = emailValide(demande);
        users.findByEmail(email)
                .filter(autre -> !autre.getId().equals(id))
                .ifPresent(autre -> {
                    throw new CompteExistantException("Un autre compte utilise déjà cet e-mail");
                });

        switch (demande.role()) {
            case ETUDIANT -> {
                ChampsEtudiant c = champsEtudiant(demande, id);
                if (compte instanceof Etudiant etudiant) {
                    etudiant.modifier(c.codeMassar(), c.numero(), c.filiere(), c.niveau());
                } else {
                    changementRole.enEtudiant(id, c.codeMassar(), c.numero(), c.filiere(), c.niveau().getId());
                }
            }
            case ENSEIGNANT -> {
                ChampsEnseignant c = champsEnseignant(demande, id);
                if (compte instanceof Enseignant enseignant) {
                    enseignant.modifier(c.codeEnseignant(), c.numero(), c.grade(), c.specialite());
                } else {
                    changementRole.enEnseignant(id, c.codeEnseignant(), c.numero(), c.grade(), c.specialite());
                }
            }
            case ADMINISTRATEUR -> {
                if (compte instanceof Etudiant || compte instanceof Enseignant
                        || compte.getRole() != Role.ADMINISTRATEUR) {
                    changementRole.enAdministrateur(id);
                }
            }
        }

        // Après un changement de rôle, le contexte JPA a été vidé : on relit le compte.
        User aJour = users.findById(id).orElseThrow();
        aJour.modifierInformations(email, demande.prenom().trim(), demande.nom().trim());
        return CompteResume.depuis(enregistrer(aJour));
    }

    // ---------- Listes ----------

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

    // ---------- Règles communes à la création et à la modification ----------

    private record ChampsEtudiant(String codeMassar, String numero, String filiere, Niveau niveau) {
    }

    private record ChampsEnseignant(String codeEnseignant, String numero, String grade, String specialite) {
    }

    private User trouver(Long id) {
        return users.findById(id)
                .orElseThrow(() -> new CompteIntrouvableException("Ce compte n'existe pas"));
    }

    /** E-mail normalisé, après vérification du domaine attendu pour le rôle (R1). */
    private String emailValide(CreerCompteRequest demande) {
        String email = demande.email().trim().toLowerCase(Locale.ROOT);
        if (!domaines.correspond(email, demande.role())) {
            throw new CompteInvalideException("Pour ce rôle, l'e-mail doit se terminer par @"
                    + domaines.pour(demande.role()));
        }
        return email;
    }

    /** @param idExclu compte en cours de modification (null pour une création) */
    private ChampsEtudiant champsEtudiant(CreerCompteRequest demande, Long idExclu) {
        String codeMassar = requis(demande.codeMassar(), "Le code Massar");
        String numero = requis(demande.numero(), "Le numéro");
        String filiere = requis(demande.filiere(), "La filière");
        if (demande.niveauId() == null) {
            throw new CompteInvalideException("Le niveau est obligatoire pour un étudiant");
        }
        Niveau niveau = niveaux.findById(demande.niveauId())
                .orElseThrow(() -> new CompteInvalideException("Ce niveau n'existe pas"));
        boolean codePris = idExclu == null
                ? etudiants.existsByCodeMassar(codeMassar)
                : etudiants.existsByCodeMassarAndIdNot(codeMassar, idExclu);
        if (codePris) {
            throw new CompteExistantException("Un étudiant existe déjà avec ce code Massar");
        }
        return new ChampsEtudiant(codeMassar, numero, filiere, niveau);
    }

    /** @param idExclu compte en cours de modification (null pour une création) */
    private ChampsEnseignant champsEnseignant(CreerCompteRequest demande, Long idExclu) {
        String codeEnseignant = requis(demande.codeEnseignant(), "Le code enseignant");
        String numero = requis(demande.numero(), "Le numéro");
        String grade = requis(demande.grade(), "Le grade");
        String specialite = requis(demande.specialite(), "La spécialité");
        boolean codePris = idExclu == null
                ? enseignants.existsByCodeEnseignant(codeEnseignant)
                : enseignants.existsByCodeEnseignantAndIdNot(codeEnseignant, idExclu);
        if (codePris) {
            throw new CompteExistantException("Un enseignant existe déjà avec ce code");
        }
        return new ChampsEnseignant(codeEnseignant, numero, grade, specialite);
    }

    private User enregistrer(User compte) {
        try {
            return users.saveAndFlush(compte);
        } catch (DataIntegrityViolationException e) {
            // Deux opérations simultanées avec le même e-mail ou le même code : la base tranche.
            throw new CompteExistantException("Un compte existe déjà avec cet e-mail ou ce code");
        }
    }

    private static String requis(String valeur, String champ) {
        if (valeur == null || valeur.isBlank()) {
            throw new CompteInvalideException(champ + " est obligatoire pour ce rôle");
        }
        return valeur.trim();
    }
}
