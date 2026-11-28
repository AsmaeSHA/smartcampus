package ma.ensakhouribga.smartcampus.notification;

public class NotificationIntrouvableException extends RuntimeException {
    public NotificationIntrouvableException() {
        super("Notification introuvable");
    }
}