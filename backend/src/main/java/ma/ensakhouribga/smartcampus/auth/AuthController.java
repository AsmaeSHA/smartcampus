package ma.ensakhouribga.smartcampus.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    /** Attribut de session qui garde l'e-mail de l'utilisateur connecté. */
    static final String SESSION_EMAIL = "utilisateurEmail";

    private final LoginService loginService;

    public AuthController(LoginService loginService) {
        this.loginService = loginService;
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
        };
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
        session.setAttribute(SESSION_EMAIL, email.trim().toLowerCase(Locale.ROOT));
    }

    private ResponseEntity<LoginResponse> response(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(new LoginResponse(message, null));
    }
}
