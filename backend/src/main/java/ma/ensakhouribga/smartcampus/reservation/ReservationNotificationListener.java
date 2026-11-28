package ma.ensakhouribga.smartcampus.reservation;

import java.time.format.DateTimeFormatter;
import ma.ensakhouribga.smartcampus.evenement.ReservationCreeeEvent;
import ma.ensakhouribga.smartcampus.notification.NotificationService;
import ma.ensakhouribga.smartcampus.notification.TypeObjet;
import ma.ensakhouribga.smartcampus.user.Role;
import ma.ensakhouribga.smartcampus.user.UserRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ReservationNotificationListener {
    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy 'à' HH:mm");
    private final UserRepository users;
    private final NotificationService notifications;

    public ReservationNotificationListener(UserRepository users, NotificationService notifications) {
        this.users = users;
        this.notifications = notifications;
    }

    /** SC-109 : envoie une notification à chaque administrateur après le commit de la demande. */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void reservationCreee(ReservationCreeeEvent event) {
        String message = "Nouvelle demande de réservation : " + event.espaceNom() + " le "
                + event.debut().format(FORMAT) + ".";
        users.findByRole(Role.ADMINISTRATEUR).forEach(admin ->
                notifications.notifier(admin.getId(), message, TypeObjet.RESERVATION, event.reservationId()));
    }
}
