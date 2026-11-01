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
import org.springframework.web.bind.annotation.PutMapping;

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
            case PASSWORD_CHANGE_REQUIRED -> {
                ouvrirSession(httpRequest, request.email());
                yield ResponseEntity.ok(new LoginResponse("Veuillez changer votre mot de passe", "/mot-de-passe"));
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
        return utilisateurCourant.profilDepuis(request)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }

    /** SC-69/70 : change le mot de passe de l'utilisateur connecté après vérification de l'ancien. */
    @PutMapping("/password")
    public ResponseEntity<LoginResponse> changerMotDePasse(@RequestBody ChangerMotDePasseRequest demande,
                                                            HttpServletRequest request) {
        return utilisateurCourant.depuis(request)
                .map(user -> {
                    if (demande == null || demande.nouveauMotDePasse() == null) {
                        return response(HttpStatus.BAD_REQUEST, "Le nouveau mot de passe est obligatoire");
                    }
                    try {
                        loginService.changerMotDePasse(user, demande.ancienMotDePasse(), demande.nouveauMotDePasse());
                    } catch (IllegalArgumentException e) {
                        return response(HttpStatus.BAD_REQUEST, e.getMessage());
                    } catch (SecurityException e) {
                        return response(HttpStatus.UNAUTHORIZED, e.getMessage());
                    }
                    return ResponseEntity.ok(new LoginResponse("Mot de passe modifié", "/accueil"));
                })
                .orElseGet(() -> response(HttpStatus.UNAUTHORIZED, "Veuillez vous connecter"));
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
