package ma.ensakhouribga.smartcampus.signalement;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import ma.ensakhouribga.smartcampus.auth.UtilisateurCourant;
import ma.ensakhouribga.smartcampus.user.User;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/** Routes administrateur des signalements, protégées par l'AdminInterceptor (/api/admin/**). */
@RestController
@RequestMapping("/api/admin/signalements")
public class SignalementAdminController {

    private final SignalementService signalementService;
    private final UtilisateurCourant utilisateurCourant;

    public SignalementAdminController(SignalementService signalementService, UtilisateurCourant utilisateurCourant) {
        this.signalementService = signalementService;
        this.utilisateurCourant = utilisateurCourant;
    }

    /** Exemple : /api/admin/signalements?statut=OUVERT&categorie=MATERIEL&espaceId=3 (filtres facultatifs). */
    @GetMapping
    public List<SignalementResume> rechercher(@RequestParam(required = false) StatutSignalement statut,
                                              @RequestParam(required = false) CategorieSignalement categorie,
                                              @RequestParam(required = false) Long espaceId) {
        return signalementService.rechercher(statut, categorie, espaceId);
    }

    /** Changer le statut : OUVERT → EN_COURS → RESOLU → FERME (FERME possible à tout moment, puis définitif). */
    @PatchMapping("/{id}/statut")
    public SignalementDetail changerStatut(@PathVariable Long id, @Valid @RequestBody ChangerStatutRequest demande,
                                           HttpServletRequest request) {
        return signalementService.changerStatut(id, demande.statut(), administrateur(request));
    }

    @PostMapping("/{id}/commentaires")
    public ResponseEntity<SignalementDetail> commenter(@PathVariable Long id,
                                                       @Valid @RequestBody AjouterCommentaireRequest demande,
                                                       HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(signalementService.commenter(id, demande.contenu(), administrateur(request)));
    }

    /** L'AdminInterceptor a déjà vérifié la session et le rôle : l'utilisateur est forcément présent. */
    private User administrateur(HttpServletRequest request) {
        return utilisateurCourant.depuis(request).orElseThrow();
    }

    @ExceptionHandler(SignalementIntrouvableException.class)
    ResponseEntity<Map<String, String>> introuvable(SignalementIntrouvableException exception) {
        return erreur(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(TransitionSignalementException.class)
    ResponseEntity<Map<String, String>> transitionInterdite(TransitionSignalementException exception) {
        return erreur(HttpStatus.CONFLICT, exception.getMessage());
    }

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
        return erreur(HttpStatus.BAD_REQUEST, "Requête invalide : statut inconnu ou champ mal formé");
    }

    /** Statut ou catégorie inconnus dans l'URL : 400 au lieu d'une erreur serveur. */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<Map<String, String>> filtreInvalide(MethodArgumentTypeMismatchException exception) {
        return erreur(HttpStatus.BAD_REQUEST, "Filtre invalide : " + exception.getName());
    }

    private ResponseEntity<Map<String, String>> erreur(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of("message", message));
    }
}
