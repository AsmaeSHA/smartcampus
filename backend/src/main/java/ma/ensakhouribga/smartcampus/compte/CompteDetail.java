package ma.ensakhouribga.smartcampus.compte;

import ma.ensakhouribga.smartcampus.user.Enseignant;
import ma.ensakhouribga.smartcampus.user.Etudiant;
import ma.ensakhouribga.smartcampus.user.Role;
import ma.ensakhouribga.smartcampus.user.User;

/**
 * Détail d'un compte pour pré-remplir le formulaire de modification (SC-64).
 * Les champs qui ne concernent pas le rôle du compte valent null. Jamais de mot de passe.
 */
public record CompteDetail(Long id, String email, String prenom, String nom, Role role, boolean actif,
                           String codeMassar, Long niveauId, String filiere,
                           String codeEnseignant, String grade, String specialite,
                           String numero) {

    static CompteDetail depuis(User user) {
        if (user instanceof Etudiant e) {
            return new CompteDetail(e.getId(), e.getEmail(), e.getPrenom(), e.getNom(), e.getRole(), e.isActive(),
                    e.getCodeMassar(), e.getNiveau().getId(), e.getFiliere(), null, null, null, e.getNumero());
        }
        if (user instanceof Enseignant e) {
            return new CompteDetail(e.getId(), e.getEmail(), e.getPrenom(), e.getNom(), e.getRole(), e.isActive(),
                    null, null, null, e.getCodeEnseignant(), e.getGrade(), e.getSpecialite(), e.getNumero());
        }
        return new CompteDetail(user.getId(), user.getEmail(), user.getPrenom(), user.getNom(), user.getRole(),
                user.isActive(), null, null, null, null, null, null, null);
    }
}
