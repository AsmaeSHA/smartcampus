package ma.ensakhouribga.smartcampus.auth;

import ma.ensakhouribga.smartcampus.user.Role;
import ma.ensakhouribga.smartcampus.user.User;

/** Réponse de GET /api/auth/me : ce que le front a besoin de savoir sur l'utilisateur connecté. */
public record UtilisateurConnecte(String email, String prenom, String nom, Role role,
                                  boolean changerMotDePasse) {

    static UtilisateurConnecte depuis(User user) {
        return new UtilisateurConnecte(user.getEmail(), user.getPrenom(), user.getNom(),
                user.getRole(), user.isChangerMotDePasse());
    }
}
