package ma.ensakhouribga.smartcampus.espace;

import java.util.Map;
import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import ma.ensakhouribga.smartcampus.auth.UtilisateurCourant;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.validation.FieldError;
import org.springframework.http.converter.HttpMessageNotReadableException;
@RestController
@RequestMapping("/api/espaces")
public class EspaceController {

    private final EspaceService espaceService;

    private final UtilisateurCourant utilisateurCourant;

    public EspaceController(EspaceService espaceService, UtilisateurCourant utilisateurCourant) {
        this.espaceService = espaceService;
        this.utilisateurCourant = utilisateurCourant;
    }

    @PostMapping
    public ResponseEntity<EspaceResume> creer(@Valid @RequestBody CreerEspaceRequest demande) {
        return ResponseEntity.status(HttpStatus.CREATED).body(espaceService.creer(demande));
    }

    @GetMapping
    public ResponseEntity<List<EspaceResume>> lister(HttpServletRequest request) {
        if (utilisateurCourant.profilDepuis(request).isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(espaceService.lister());
    }

    @GetMapping("/{id}")
    public ResponseEntity<EspaceResume> details(@PathVariable Long id, HttpServletRequest request) {
        // On vérifie que l'utilisateur est connecté
        if (utilisateurCourant.profilDepuis(request).isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(espaceService.trouverParId(id));
    }

    @PutMapping("/{id}")
    public EspaceResume modifier(@PathVariable Long id, @Valid @RequestBody CreerEspaceRequest demande) {
        return espaceService.modifier(id, demande);
    }

    @ExceptionHandler(EspaceExistantException.class)
    ResponseEntity<Map<String, String>> espaceExistant(EspaceExistantException exception) {
        return erreur(HttpStatus.CONFLICT, exception.getMessage());
    }

    @ExceptionHandler(EspaceIntrouvableException.class)
    ResponseEntity<Map<String, String>> espaceIntrouvable(EspaceIntrouvableException exception) {
        return erreur(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<Map<String, String>> champInvalide(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .findFirst()
                .orElse("Données invalides");
        return erreur(HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<Map<String, String>> requeteIllisible() {
        return erreur(HttpStatus.BAD_REQUEST, "Requête invalide : vérifiez les champs et le type");
    }

    private ResponseEntity<Map<String, String>> erreur(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of("message", message));
    }
}
