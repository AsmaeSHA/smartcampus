package ma.ensakhouribga.smartcampus.signalement;

import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/** Routes administrateur des signalements, protégées par l'AdminInterceptor (/api/admin/**). */
@RestController
@RequestMapping("/api/admin/signalements")
public class SignalementAdminController {

    private final SignalementService signalementService;

    public SignalementAdminController(SignalementService signalementService) {
        this.signalementService = signalementService;
    }

    /** Exemple : /api/admin/signalements?statut=OUVERT&categorie=MATERIEL&espaceId=3 (filtres facultatifs). */
    @GetMapping
    public List<SignalementResume> rechercher(@RequestParam(required = false) StatutSignalement statut,
                                              @RequestParam(required = false) CategorieSignalement categorie,
                                              @RequestParam(required = false) Long espaceId) {
        return signalementService.rechercher(statut, categorie, espaceId);
    }

    /** Statut ou catégorie inconnus dans l'URL : 400 au lieu d'une erreur serveur. */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<Map<String, String>> filtreInvalide(MethodArgumentTypeMismatchException exception) {
        return ResponseEntity.badRequest().body(Map.of("message", "Filtre invalide : " + exception.getName()));
    }
}
