package ma.ensakhouribga.smartcampus.notification;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ma.ensakhouribga.smartcampus.user.Role;
import ma.ensakhouribga.smartcampus.user.User;
import ma.ensakhouribga.smartcampus.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:smartcampus-notification-test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
class NotificationIntegrationTest {

    private static final String PASSWORD = "motdepasse-test";
    private static final String ALICE = "alice.notif@usms.ac.ma";
    private static final String BOB = "bob.notif@usms.ac.ma";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private UserRepository users;

    @Autowired
    private NotificationRepository notifications;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User alice;
    private User bob;

    @BeforeEach
    void preparer() {
        notifications.deleteAll();
        String hash = passwordEncoder.encode(PASSWORD);
        alice = users.findByEmail(ALICE).orElseGet(() ->
                users.save(new User(ALICE, hash, "Alice", "Notif", Role.ETUDIANT, true)));
        bob = users.findByEmail(BOB).orElseGet(() ->
                users.save(new User(BOB, hash, "Bob", "Notif", Role.ETUDIANT, true)));
    }

    @Test
    void listeSeulementSesNotificationsLaPlusRecenteEnPremier() throws Exception {
        notificationService.notifier(alice.getId(), "Première", TypeObjet.RESERVATION, 1L);
        Thread.sleep(5);
        notificationService.notifier(alice.getId(), "Deuxième", TypeObjet.SIGNALEMENT, 2L);
        notificationService.notifier(bob.getId(), "Pour Bob", TypeObjet.RESERVATION, 3L);

        mvc.perform(get("/api/notifications").session(connecter(ALICE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].message").value("Deuxième"))
                .andExpect(jsonPath("$[1].message").value("Première"));
    }

    @Test
    void leCompteurBaisseQuandOnLitUneNotification() throws Exception {
        Notification n = notificationService.notifier(alice.getId(), "A lire", TypeObjet.RESERVATION, 1L);
        notificationService.notifier(alice.getId(), "Autre", TypeObjet.RESERVATION, 2L);
        MockHttpSession session = connecter(ALICE);

        mvc.perform(get("/api/notifications/non-lues/nombre").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value(2));

        mvc.perform(patch("/api/notifications/" + n.getId() + "/lue").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lue").value(true));

        mvc.perform(get("/api/notifications/non-lues/nombre").session(session))
                .andExpect(jsonPath("$.nombre").value(1));
    }

    @Test
    void unUtilisateurNePeutPasMarquerLaNotificationDUnAutre() throws Exception {
        Notification n = notificationService.notifier(alice.getId(), "Privée", TypeObjet.RESERVATION, 1L);

        mvc.perform(patch("/api/notifications/" + n.getId() + "/lue").session(connecter(BOB)))
                .andExpect(status().isForbidden());

        mvc.perform(get("/api/notifications").session(connecter(BOB)))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void notificationInexistanteRenvoie404() throws Exception {
        mvc.perform(patch("/api/notifications/999999/lue").session(connecter(ALICE)))
                .andExpect(status().isNotFound());
    }

    @Test
    void nonConnecteRenvoie401() throws Exception {
        mvc.perform(get("/api/notifications")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/notifications/non-lues/nombre")).andExpect(status().isUnauthorized());
    }

    private MockHttpSession connecter(String email) throws Exception {
        return (MockHttpSession) mvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\"}"))
                .andReturn().getRequest().getSession(false);
    }
}