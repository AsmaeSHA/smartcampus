package ma.ensakhouribga.smartcampus.notification;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import ma.ensakhouribga.smartcampus.auth.UtilisateurCourant;
import ma.ensakhouribga.smartcampus.user.User;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;
    private final UtilisateurCourant utilisateurCourant;

    public NotificationController(NotificationService notificationService, UtilisateurCourant utilisateurCourant) {
        this.notificationService = notificationService;
        this.utilisateurCourant = utilisateurCourant;
    }

    @GetMapping
    public ResponseEntity<List<NotificationResume>> lister(HttpServletRequest request) {
        Optional<User> user = utilisateurCourant.depuis(request);
        if (user.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(notificationService.lister(user.get().getId()));
    }

    @GetMapping("/non-lues/nombre")
    public ResponseEntity<Map<String, Long>> nombreNonLues(HttpServletRequest request) {
        Optional<User> user = utilisateurCourant.depuis(request);
        if (user.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(Map.of("nombre", notificationService.nombreNonLues(user.get().getId())));
    }

    @PatchMapping("/{id}/lue")
    public ResponseEntity<NotificationResume> marquerLue(@PathVariable Long id, HttpServletRequest request) {
        Optional<User> user = utilisateurCourant.depuis(request);
        if (user.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(notificationService.marquerLue(id, user.get().getId()));
    }

    @ExceptionHandler(NotificationIntrouvableException.class)
    ResponseEntity<Map<String, String>> introuvable(NotificationIntrouvableException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", exception.getMessage()));
    }

    @ExceptionHandler(NotificationInterditeException.class)
    ResponseEntity<Map<String, String>> interdite(NotificationInterditeException exception) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", exception.getMessage()));
    }
}