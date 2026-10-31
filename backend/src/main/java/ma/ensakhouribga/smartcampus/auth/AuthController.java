package ma.ensakhouribga.smartcampus.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final LoginService loginService;
    private final UtilisateurCourant utilisateurCourant;

    public AuthController(LoginService loginService, UtilisateurCourant utilisateurCourant) {
        this.loginService = loginService;
        this.utilisateurCourant = utilisateurCourant;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request,
                                               HttpServletRequest httpRequest) {
        LoginResult result = loginService.login(request.email(), request.password());

        return switch (result) {
            case SUCCESS -> {
                ouvrirSession(httpRequest, request.email());
                yield ResponseEntity.ok(new LoginResponse("Connexion réussie", "/accueil"));
            }
            case INVALID_CREDENTIALS -> response(HttpStatus.UNAUTHORIZED,
                    "E-mail ou mot de passe incorrect");
            case DOMAIN_NOT_ALLOWED -> response(HttpStatus.FORBIDDEN,
                    "Veuillez utiliser votre e-mail institutionnel");
            case ACCOUNT_INACTIVE -> response(HttpStatus.FORBIDDEN,
                    "Ce compte est désactivé");
            case TEMPORARILY_LOCKED -> response(HttpStatus.TOO_MANY_REQUESTS,
                    "Trop de tentatives. Réessayez dans quelques minutes.");
        };
    }

    /** Ferme la session de l'utilisateur. Sans session, la réponse est identique. */
    @PostMapping("/logout")
    public ResponseEntity<LoginResponse> logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return ResponseEntity.ok(new LoginResponse("Déconnexion réussie", "/connexion"));
    }

    /**
     * Indique l'utilisateur connecté et son rôle, pour que le front adapte l'affichage.
     * Renvoie 401 si la session est absente ou fermée, ou si le compte a été désactivé entre-temps.
     */
    @GetMapping("/me")
    public ResponseEntity<UtilisateurConnecte> moi(HttpServletRequest request) {
        return utilisateurCourant.depuis(request)
                .map(user -> ResponseEntity.ok(UtilisateurConnecte.depuis(user)))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }

    /**
     * Crée une nouvelle session après une connexion réussie.
     * L'ancienne session est fermée pour éviter qu'un identifiant de session volé reste valide.
     */
    private void ouvrirSession(HttpServletRequest request, String email) {
        HttpSession ancienne = request.getSession(false);
        if (ancienne != null) {
            ancienne.invalidate();
        }
        HttpSession session = request.getSession(true);
        session.setAttribute(UtilisateurCourant.SESSION_EMAIL, email.trim().toLowerCase(Locale.ROOT));
    }

    private ResponseEntity<LoginResponse> response(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(new LoginResponse(message, null));
    }
}
