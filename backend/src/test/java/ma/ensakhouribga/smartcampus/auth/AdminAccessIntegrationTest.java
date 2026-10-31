package ma.ensakhouribga.smartcampus.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;
import ma.ensakhouribga.smartcampus.user.Enseignant;
import ma.ensakhouribga.smartcampus.user.Etudiant;
import ma.ensakhouribga.smartcampus.user.Niveau;
import ma.ensakhouribga.smartcampus.user.NiveauRepository;
import ma.ensakhouribga.smartcampus.user.Role;
import ma.ensakhouribga.smartcampus.user.User;
import ma.ensakhouribga.smartcampus.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Vérifie que les routes /api/admin/** sont réservées à l'administrateur (règle R13).
 * Une route factice, définie seulement pour ce test (classe @TestConfiguration imbriquée,
 * chargée automatiquement par Spring), simule une future route admin.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:smartcampus-admin-test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
class AdminAccessIntegrationTest {

    private static final String PASSWORD = "motdepasse-test";
    private static final String ADMIN = "admin.test@ensakh.ma";
    private static final String ETUDIANT = "etudiant.test@usms.ac.ma";
    private static final String ENSEIGNANT = "enseignant.test@usms.ma";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private UserRepository users;

    @Autowired
    private NiveauRepository niveaux;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void creerComptes() {
        String hash = passwordEncoder.encode(PASSWORD);
        Niveau gi3 = niveaux.findByNom("GI3").orElseGet(() -> niveaux.save(new Niveau("GI3")));
        if (users.findByEmail(ADMIN).isEmpty()) {
            users.save(new User(ADMIN, hash, "Admin", "Test", Role.ADMINISTRATEUR, true));
        }
        if (users.findByEmail(ETUDIANT).isEmpty()) {
            users.save(new Etudiant(ETUDIANT, hash, "Etudiant", "Test", true,
                    "D111111111", "0002", "Génie Informatique", gi3));
        }
        if (users.findByEmail(ENSEIGNANT).isEmpty()) {
            users.save(new Enseignant(ENSEIGNANT, hash, "Enseignant", "Test", true,
                    "E0001", "0003", "PES", "Informatique"));
        }
    }

    @Test
    void routeAdminSansSessionRenvoie401() throws Exception {
        mvc.perform(get("/api/admin/test-acces"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void routeAdminRefuseeAUnEtudiant() throws Exception {
        mvc.perform(get("/api/admin/test-acces").session(connecter(ETUDIANT)))
                .andExpect(status().isForbidden());
    }

    @Test
    void routeAdminRefuseeAUnEnseignant() throws Exception {
        mvc.perform(get("/api/admin/test-acces").session(connecter(ENSEIGNANT)))
                .andExpect(status().isForbidden());
    }

    @Test
    void routeAdminAutoriseeAlAdministrateur() throws Exception {
        mvc.perform(get("/api/admin/test-acces").session(connecter(ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.acces").value("ok"));
    }

    @Test
    void meRenvoieLeRoleDeLEtudiant() throws Exception {
        mvc.perform(get("/api/auth/me").session(connecter(ETUDIANT)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ETUDIANT"))
                .andExpect(jsonPath("$.prenom").value("Etudiant"));
    }

    private MockHttpSession connecter(String email) throws Exception {
        return (MockHttpSession) mvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andReturn()
                .getRequest()
                .getSession(false);
    }

    /** Route factice protégée par l'AdminInterceptor, utilisée uniquement dans ce test. */
    @TestConfiguration(proxyBeanMethods = false)
    @RestController
    static class RouteAdminDeTest {

        @GetMapping("/api/admin/test-acces")
        Map<String, String> acces() {
            return Map.of("acces", "ok");
        }
    }
}
