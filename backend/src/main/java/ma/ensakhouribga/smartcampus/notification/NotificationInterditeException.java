package ma.ensakhouribga.smartcampus.notification;

public class NotificationInterditeException extends RuntimeException {
    public NotificationInterditeException() {
        super("Cette notification ne vous appartient pas");
    }
}