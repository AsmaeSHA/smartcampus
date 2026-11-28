package ma.ensakhouribga.smartcampus.notification;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import ma.ensakhouribga.smartcampus.user.Role;
import ma.ensakhouribga.smartcampus.user.User;
import ma.ensakhouribga.smartcampus.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:smartcampus-purge-test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class NotificationPurgeTest {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private NotificationRepository notifications;

    @Autowired
    private UserRepository users;

    @Autowired
    private JdbcTemplate jdbc;

    private User user;

    @BeforeEach
    void preparer() {
        notifications.deleteAll();
        user = users.findByEmail("purge@usms.ac.ma").orElseGet(() ->
                users.save(new User("purge@usms.ac.ma", "hash", "Purge", "Test", Role.ETUDIANT, true)));
    }

    @Test
    void uneNotificationLueDe31JoursEstSupprimee() {
        Notification n = creer(31, true);

        notificationService.purgerLuesAnciennes(LocalDateTime.now());

        assertThat(notifications.findById(n.getId())).isEmpty();
    }

    @Test
    void uneNotificationNonLueDe31JoursEstGardee() {
        Notification n = creer(31, false);

        notificationService.purgerLuesAnciennes(LocalDateTime.now());

        assertThat(notifications.findById(n.getId())).isPresent();
    }

    @Test
    void uneNotificationLueRecenteEstGardee() {
        Notification n = creer(10, true);

        notificationService.purgerLuesAnciennes(LocalDateTime.now());

        assertThat(notifications.findById(n.getId())).isPresent();
    }

    /** Crée une notification puis change sa date de création directement en base. */
    private Notification creer(int joursAvant, boolean lue) {
        Notification n = notificationService.notifier(user.getId(), "Test purge", TypeObjet.RESERVATION, 1L);
        jdbc.update("update notifications set cree_le = ?, lue = ? where id = ?",
                LocalDateTime.now().minusDays(joursAvant), lue, n.getId());
        return n;
    }
}