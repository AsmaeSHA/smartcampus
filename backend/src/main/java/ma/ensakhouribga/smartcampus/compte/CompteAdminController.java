package ma.ensakhouribga.smartcampus.compte;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Routes d'administration des comptes. Toutes les routes /api/admin/** sont
 * réservées à l'administrateur par l'AdminInterceptor (R13).
 */
@RestController
@RequestMapping("/api/admin")
public class CompteAdminController {

    private final CompteService compteService;

    public CompteAdminController(CompteService compteService) {
        this.compteService = compteService;
    }

    /** SC-27 : crée un compte et renvoie, une seule fois, son mot de passe temporaire. */
    @PostMapping("/comptes")
    public ResponseEntity<CompteCree> creer(@Valid @RequestBody CreerCompteRequest demande) {
        return ResponseEntity.status(HttpStatus.CREATED).body(compteService.creer(demande));
    }

    @GetMapping("/comptes")
    public List<CompteResume> lister() {
        return compteService.lister();
    }

    @GetMapping("/niveaux")
    public List<NiveauResume> niveaux() {
        return compteService.listerNiveaux();
    }

    @ExceptionHandler(CompteInvalideException.class)
    ResponseEntity<Map<String, String>> compteInvalide(CompteInvalideException e) {
        return erreur(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(CompteExistantException.class)
    ResponseEntity<Map<String, String>> compteExistant(CompteExistantException e) {
        return erreur(HttpStatus.CONFLICT, e.getMessage());
    }

    /** Champ commun manquant ou mal formé (@NotBlank, @Email…) : on renvoie le premier message. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<Map<String, String>> champInvalide(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .findFirst()
                .orElse("Données invalides");
        return erreur(HttpStatus.BAD_REQUEST, message);
    }

    /** JSON illisible ou rôle inconnu. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<Map<String, String>> requeteIllisible() {
        return erreur(HttpStatus.BAD_REQUEST, "Requête invalide : vérifiez les champs et le rôle");
    }

    private ResponseEntity<Map<String, String>> erreur(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of("message", message));
    }
}
