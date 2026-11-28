package ma.ensakhouribga.smartcampus.reservation;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import ma.ensakhouribga.smartcampus.auth.UtilisateurCourant;
import ma.ensakhouribga.smartcampus.espace.EspaceIntrouvableException;
import ma.ensakhouribga.smartcampus.user.User;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reservations")
public class ReservationController {
    private final ReservationService reservationService;
    private final UtilisateurCourant utilisateurCourant;

    public ReservationController(ReservationService reservationService, UtilisateurCourant utilisateurCourant) {
        this.reservationService = reservationService;
        this.utilisateurCourant = utilisateurCourant;
    }

    @PostMapping
    public ResponseEntity<?> creer(@Valid @RequestBody CreerReservationRequest demande, HttpServletRequest request) {
        User demandeur = utilisateurCourant.depuis(request).orElse(null);
        if (demandeur == null) return erreur(HttpStatus.UNAUTHORIZED, "Veuillez vous connecter");
        return ResponseEntity.status(HttpStatus.CREATED).body(reservationService.creer(demande, demandeur));
    }

    @GetMapping("/mes")
    public ResponseEntity<List<ReservationResume>> mesReservations(HttpServletRequest request) {
        User demandeur = utilisateurCourant.depuis(request).orElse(null);
        if (demandeur == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        return ResponseEntity.ok(reservationService.mesReservations(demandeur));
    }

    @PostMapping("/{id}/annuler")
    public ResponseEntity<?> annuler(@PathVariable Long id, @Valid @RequestBody AnnulerReservationRequest demande,
                                     HttpServletRequest request) {
        User demandeur = utilisateurCourant.depuis(request).orElse(null);
        if (demandeur == null) return erreur(HttpStatus.UNAUTHORIZED, "Veuillez vous connecter");
        return ResponseEntity.ok(reservationService.annuler(id, demande.raison(), demandeur));
    }

    @ExceptionHandler(ReservationIntrouvableException.class)
    ResponseEntity<Map<String, String>> introuvable(ReservationIntrouvableException exception) {
        return erreur(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(EspaceIntrouvableException.class)
    ResponseEntity<Map<String, String>> espaceIntrouvable(EspaceIntrouvableException exception) {
        return erreur(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(ReservationInvalideException.class)
    ResponseEntity<Map<String, String>> invalide(RuntimeException exception) {
        return erreur(HttpStatus.CONFLICT, exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<Map<String, String>> validation(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream().map(FieldError::getDefaultMessage)
                .findFirst().orElse("Données invalides");
        return erreur(HttpStatus.BAD_REQUEST, message);
    }

    private ResponseEntity<Map<String, String>> erreur(HttpStatus statut, String message) {
        return ResponseEntity.status(statut).body(Map.of("message", message));
    }
}
