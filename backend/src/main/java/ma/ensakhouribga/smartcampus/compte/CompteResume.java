package ma.ensakhouribga.smartcampus.compte;

import java.time.LocalDateTime;
import ma.ensakhouribga.smartcampus.user.Role;
import ma.ensakhouribga.smartcampus.user.User;

/** Vue d'un compte renvoyée à l'administrateur. Ne contient jamais le mot de passe ni son hash. */
public record CompteResume(Long id, String email, String prenom, String nom, Role role,
                           boolean actif, boolean changerMotDePasse, LocalDateTime creeLe) {

    static CompteResume depuis(User user) {
        return new CompteResume(user.getId(), user.getEmail(), user.getPrenom(), user.getNom(),
                user.getRole(), user.isActive(), user.isChangerMotDePasse(), user.getCreeLe());
    }
}
