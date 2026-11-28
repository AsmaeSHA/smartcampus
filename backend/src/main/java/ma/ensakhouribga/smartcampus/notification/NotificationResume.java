package ma.ensakhouribga.smartcampus.notification;

import java.time.LocalDateTime;

/** Ce que l'API renvoie au front pour une notification. */
public record NotificationResume(Long id, String message, TypeObjet typeObjet, Long objetId,
                                 boolean lue, LocalDateTime creeLe) {

    static NotificationResume depuis(Notification n) {
        return new NotificationResume(n.getId(), n.getMessage(), n.getTypeObjet(), n.getObjetId(),
                n.isLue(), n.getCreeLe());
    }
}