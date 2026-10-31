package ma.ensakhouribga.smartcampus.auth;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import org.springframework.test.web.servlet.MvcResult;

/**
 * Tests d'intégration de la connexion (SC-52) : contrôleur, service, base et session.
 * Utilise une base H2 en mémoire pour ne pas dépendre de PostgreSQL pendant les tests.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:smartcampus-test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
class AuthIntegrationTest {

    private static final String EMAIL = "integration@usms.ma";
    private static final String PASSWORD = "motdepasse-test";
    private static final String LOGIN_BODY =
            "{\"email\":\"" + EMAIL + "\",\"password\":\"" + PASSWORD + "\"}";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private UserRepository users;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void creerCompte() {
        if (users.findByEmail(EMAIL).isEmpty()) {
            users.save(new User(EMAIL, passwordEncoder.encode(PASSWORD), "Integration", "Test",
                    Role.ADMINISTRATEUR, true));
        }
    }

    @Test
    void connexionAvecBonsIdentifiantsOuvreUneSession() throws Exception {
        MvcResult result = mvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content(LOGIN_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.redirect").value("/accueil"))
                .andReturn();

        MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);

        mvc.perform(get("/api/auth/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(EMAIL))
                .andExpect(jsonPath("$.role").value("ADMINISTRATEUR"));
    }

    @Test
    void motDePasseIncorrectRefuseLaConnexion() throws Exception {
        mvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content("{\"email\":\"" + EMAIL + "\",\"password\":\"mauvais\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message", containsString("incorrect")));
    }

    @Test
    void domaineExterneEstRefuseSansSession() throws Exception {
        mvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content("{\"email\":\"quelquun@gmail.com\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message", containsString("institutionnel")));
    }

    @Test
    void meSansSessionRenvoie401() throws Exception {
        mvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logoutFermeLaSession() throws Exception {
        MvcResult result = mvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content(LOGIN_BODY))
                .andExpect(status().isOk())
                .andReturn();
        MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);

        mvc.perform(post("/api/auth/logout").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.redirect").value("/connexion"));

        mvc.perform(get("/api/auth/me").session(session))
                .andExpect(status().isUnauthorized());
    }
}
