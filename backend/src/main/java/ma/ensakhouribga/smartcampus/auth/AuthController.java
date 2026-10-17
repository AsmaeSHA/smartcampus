package ma.ensakhouribga.smartcampus.auth;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final LoginService loginService;

    public AuthController(LoginService loginService) {
        this.loginService = loginService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        LoginResult result = loginService.login(request.email(), request.password());

        return switch (result) {
            case SUCCESS -> ResponseEntity.ok(new LoginResponse("Connexion réussie", "/accueil"));
            case INVALID_CREDENTIALS -> response(HttpStatus.UNAUTHORIZED,
                    "E-mail ou mot de passe incorrect");
            case DOMAIN_NOT_ALLOWED -> response(HttpStatus.FORBIDDEN,
                    "Veuillez utiliser votre e-mail institutionnel");
            case ACCOUNT_INACTIVE -> response(HttpStatus.FORBIDDEN,
                    "Ce compte est désactivé");
        };
    }

    private ResponseEntity<LoginResponse> response(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(new LoginResponse(message, null));
    }
}
