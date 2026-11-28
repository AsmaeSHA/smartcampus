package ma.ensakhouribga.smartcampus.reservation;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import ma.ensakhouribga.smartcampus.evenement.ReservationCreeeEvent;
import ma.ensakhouribga.smartcampus.notification.NotificationService;
import ma.ensakhouribga.smartcampus.notification.TypeObjet;
import ma.ensakhouribga.smartcampus.user.Role;
import ma.ensakhouribga.smartcampus.user.User;
import ma.ensakhouribga.smartcampus.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class ReservationNotificationListenerTest {
    @Test
    void notifieTousLesAdministrateursDeLaDemande() {
        UserRepository users = mock(UserRepository.class);
        NotificationService notifications = mock(NotificationService.class);
        User admin = new User("admin@ensakh.ma", "hash", "Admin", "Test", Role.ADMINISTRATEUR, true);
        User admin2 = new User("admin2@ensakh.ma", "hash", "Admin2", "Test", Role.ADMINISTRATEUR, true);
        ReflectionTestUtils.setField(admin, "id", 1L);
        ReflectionTestUtils.setField(admin2, "id", 2L);
        when(users.findByRole(Role.ADMINISTRATEUR)).thenReturn(List.of(admin, admin2));
        ReservationNotificationListener listener = new ReservationNotificationListener(users, notifications);
        LocalDateTime debut = LocalDateTime.of(2027, 1, 10, 10, 0);

        listener.reservationCreee(new ReservationCreeeEvent(42L, "Salle 101", debut));

        verify(notifications).notifier(admin.getId(), "Nouvelle demande de réservation : Salle 101 le 10/01/2027 à 10:00.", TypeObjet.RESERVATION, 42L);
        verify(notifications).notifier(admin2.getId(), "Nouvelle demande de réservation : Salle 101 le 10/01/2027 à 10:00.", TypeObjet.RESERVATION, 42L);
    }
}
