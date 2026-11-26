package ma.ensakhouribga.smartcampus.signalement;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.Map;
import java.util.Optional;
import ma.ensakhouribga.smartcampus.auth.UtilisateurCourant;
import ma.ensakhouribga.smartcampus.espace.EspaceIntrouvableException;
import ma.ensakhouribga.smartcampus.user.User;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/signalements")
public class SignalementController {

    private final SignalementService signalementService;
    private final UtilisateurCourant utilisateurCourant;

    public SignalementController(SignalementService signalementService, UtilisateurCourant utilisateurCourant) {
        this.signalementService = signalementService;
        this.utilisateurCourant = utilisateurCourant;
    }

    @PostMapping
    public ResponseEntity<SignalementResume> signaler(@Valid @RequestBody CreerSignalementRequest demande,
                                                      HttpServletRequest request) {
        Optional<User> auteur = utilisateurCourant.depuis(request);
        if (auteur.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(signalementService.signaler(demande, auteur.get()));
    }

    @ExceptionHandler(SignalementInterditException.class)
    ResponseEntity<Map<String, String>> interdit(SignalementInterditException exception) {
        return erreur(HttpStatus.FORBIDDEN, exception.getMessage());
    }

    @ExceptionHandler(EspaceIntrouvableException.class)
    ResponseEntity<Map<String, String>> espaceIntrouvable(EspaceIntrouvableException exception) {
        return erreur(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    /** Champ manquant ou invalide : on renvoie le champ et son message (400). */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<Map<String, String>> champInvalide(MethodArgumentNotValidException exception) {
        FieldError champ = exception.getBindingResult().getFieldErrors().stream().findFirst().orElse(null);
        if (champ == null) {
            return erreur(HttpStatus.BAD_REQUEST, "Données invalides");
        }
        return ResponseEntity.badRequest().body(Map.of("champ", champ.getField(), "message", champ.getDefaultMessage()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<Map<String, String>> requeteIllisible() {
        return erreur(HttpStatus.BAD_REQUEST, "Requête invalide : vérifiez la catégorie et les champs");
    }

    private ResponseEntity<Map<String, String>> erreur(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of("message", message));
    }
}
