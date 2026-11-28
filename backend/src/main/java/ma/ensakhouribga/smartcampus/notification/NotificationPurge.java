package ma.ensakhouribga.smartcampus.notification;

import java.time.LocalDateTime;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Tâche quotidienne de purge des notifications (SC-141, règle R14). */
@Component
public class NotificationPurge {

    private final NotificationService notificationService;

    public NotificationPurge(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    /** Tous les jours à 3h, heure du Maroc. */
    @Scheduled(cron = "0 0 3 * * *", zone = "Africa/Casablanca")
    public void purger() {
        notificationService.purgerLuesAnciennes(LocalDateTime.now());
    }
}