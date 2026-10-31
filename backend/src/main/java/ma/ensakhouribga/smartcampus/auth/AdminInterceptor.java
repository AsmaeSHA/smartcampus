package ma.ensakhouribga.smartcampus.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Optional;
import ma.ensakhouribga.smartcampus.user.User;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Protège toutes les routes /api/admin/** (règle R13, vérification côté serveur) :
 * 401 sans session valide, 403 si l'utilisateur connecté n'est pas administrateur.
 */
@Component
public class AdminInterceptor implements HandlerInterceptor {

    private final UtilisateurCourant utilisateurCourant;

    public AdminInterceptor(UtilisateurCourant utilisateurCourant) {
        this.utilisateurCourant = utilisateurCourant;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        Optional<User> utilisateur = utilisateurCourant.depuis(request);
        if (utilisateur.isEmpty()) {
            refuser(response, HttpStatus.UNAUTHORIZED, "Veuillez vous connecter");
            return false;
        }
        if (!utilisateur.get().estAdministrateur()) {
            refuser(response, HttpStatus.FORBIDDEN, "Accès réservé à l'administrateur");
            return false;
        }
        return true;
    }

    private void refuser(HttpServletResponse response, HttpStatus status, String message) throws Exception {
        response.setStatus(status.value());
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"message\":\"" + message + "\"}");
    }
}
