package ma.ensakhouribga.smartcampus.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.Optional;
import ma.ensakhouribga.smartcampus.user.User;
import ma.ensakhouribga.smartcampus.user.UserRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Retrouve l'utilisateur connecté à partir de la session.
 * Le compte est relu en base à chaque requête (règle R13) : un compte désactivé
 * ou dont le rôle a changé est pris en compte immédiatement, sans attendre une reconnexion.
 */
@Component
public class UtilisateurCourant {

    /** Attribut de session qui garde l'e-mail de l'utilisateur connecté. */
    public static final String SESSION_EMAIL = "utilisateurEmail";

    private final UserRepository users;

    public UtilisateurCourant(UserRepository users) {
        this.users = users;
    }

    /** Utilisateur connecté et actif, ou vide si la session est absente, inconnue ou désactivée. */
    public Optional<User> depuis(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        Object email = session == null ? null : session.getAttribute(SESSION_EMAIL);
        if (email == null) {
            return Optional.empty();
        }
        return users.findByEmail(email.toString()).filter(User::isActive);
    }

    /** Charge et transforme le profil pendant la transaction, y compris le niveau étudiant. */
    @Transactional(readOnly = true)
    public Optional<UtilisateurConnecte> profilDepuis(HttpServletRequest request) {
        return depuis(request).map(UtilisateurConnecte::depuis);
    }
}
