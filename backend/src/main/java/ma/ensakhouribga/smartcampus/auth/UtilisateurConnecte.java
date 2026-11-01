package ma.ensakhouribga.smartcampus.auth;

import ma.ensakhouribga.smartcampus.user.Role;
import ma.ensakhouribga.smartcampus.user.User;
import ma.ensakhouribga.smartcampus.user.Etudiant;
import ma.ensakhouribga.smartcampus.user.Enseignant;

/** Réponse de GET /api/auth/me : ce que le front a besoin de savoir sur l'utilisateur connecté. */
public record UtilisateurConnecte(String email, String prenom, String nom, Role role,
                                  boolean changerMotDePasse, String numero, String codeMassar,
                                  String filiere, String niveau, String codeEnseignant,
                                  String grade, String specialite) {

    static UtilisateurConnecte depuis(User user) {
        if (user instanceof Etudiant etudiant) {
            return new UtilisateurConnecte(user.getEmail(), user.getPrenom(), user.getNom(), user.getRole(),
                    user.isChangerMotDePasse(), etudiant.getNumero(), etudiant.getCodeMassar(),
                    etudiant.getFiliere(), etudiant.getNiveau().getNom(), null, null, null);
        }
        if (user instanceof Enseignant enseignant) {
            return new UtilisateurConnecte(user.getEmail(), user.getPrenom(), user.getNom(), user.getRole(),
                    user.isChangerMotDePasse(), enseignant.getNumero(), null, null, null,
                    enseignant.getCodeEnseignant(), enseignant.getGrade(), enseignant.getSpecialite());
        }
        return new UtilisateurConnecte(user.getEmail(), user.getPrenom(), user.getNom(), user.getRole(),
                user.isChangerMotDePasse(), null, null, null, null, null, null, null);
    }
}
