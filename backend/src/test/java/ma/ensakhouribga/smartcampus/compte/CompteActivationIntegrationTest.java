package ma.ensakhouribga.smartcampus.compte;

import static org.hamcrest.Matchers.containsString;
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
import org.springframework.test.web.servlet.ResultActions;

/** Tests d'intégration de SC-28 : désactiver et réactiver un compte. */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:smartcampus-activation-test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
class CompteActivationIntegrationTest {

    private static final String PASSWORD = "motdepasse-test";
    private static final String ADMIN = "admin.activation@ensakh.ma";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private UserRepository users;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void creerAdmin() {
        creerSiAbsent(ADMIN, Role.ADMINISTRATEUR);
    }

    @Test
    void unCompteDesactiveNePeutPlusSeConnecter() throws Exception {
        User prof = creerSiAbsent("prof.desactive@usms.ma", Role.ENSEIGNANT);

        mvc.perform(patch("/api/admin/comptes/{id}/desactiver", prof.getId()).session(connecter(ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.actif").value(false));

        login("prof.desactive@usms.ma").andExpect(status().isForbidden());
        // Les données sont conservées : le compte existe toujours.
        mvc.perform(get("/api/admin/comptes").session(connecter(ADMIN)))
                .andExpect(jsonPath("$[?(@.email == 'prof.desactive@usms.ma')].actif").value(false));
    }

    @Test
    void unUtilisateurConnectePerdLAccesDesSaDesactivation() throws Exception {
        User prof = creerSiAbsent("prof.connecte@usms.ma", Role.ENSEIGNANT);
        MockHttpSession sessionProf = connecter("prof.connecte@usms.ma");
        mvc.perform(get("/api/auth/me").session(sessionProf)).andExpect(status().isOk());

        mvc.perform(patch("/api/admin/comptes/{id}/desactiver", prof.getId()).session(connecter(ADMIN)))
                .andExpect(status().isOk());

        mvc.perform(get("/api/auth/me").session(sessionProf)).andExpect(status().isUnauthorized());
    }

    @Test
    void unCompteReactivePeutDeNouveauSeConnecter() throws Exception {
        User prof = creerSiAbsent("prof.reactive@usms.ma", Role.ENSEIGNANT);
        MockHttpSession admin = connecter(ADMIN);

        mvc.perform(patch("/api/admin/comptes/{id}/desactiver", prof.getId()).session(admin))
                .andExpect(status().isOk());
        mvc.perform(patch("/api/admin/comptes/{id}/reactiver", prof.getId()).session(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.actif").value(true));

        login("prof.reactive@usms.ma").andExpect(status().isOk());
    }

    @Test
    void lAdministrateurNePeutPasDesactiverSonPropreCompte() throws Exception {
        Long idAdmin = users.findByEmail(ADMIN).orElseThrow().getId();

        mvc.perform(patch("/api/admin/comptes/{id}/desactiver", idAdmin).session(connecter(ADMIN)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("propre compte")));
    }

    @Test
    void unCompteInconnuRenvoie404() throws Exception {
        mvc.perform(patch("/api/admin/comptes/{id}/desactiver", 999999).session(connecter(ADMIN)))
                .andExpect(status().isNotFound());
    }

    @Test
    void unEnseignantNePeutPasDesactiverDeCompte() throws Exception {
        creerSiAbsent("prof.curieux@usms.ma", Role.ENSEIGNANT);
        Long idAdmin = users.findByEmail(ADMIN).orElseThrow().getId();

        mvc.perform(patch("/api/admin/comptes/{id}/desactiver", idAdmin).session(connecter("prof.curieux@usms.ma")))
                .andExpect(status().isForbidden());
    }

    /**
     * Un enseignant « simple » (sans la table enseignants) suffit ici : SC-28 ne touche que
     * les informations communes du compte.
     */
    private User creerSiAbsent(String email, Role role) {
        return users.findByEmail(email).orElseGet(() -> users.save(
                new User(email, passwordEncoder.encode(PASSWORD), "Prenom", "Nom", role, true)));
    }

    private ResultActions login(String email) throws Exception {
        return mvc.perform(post("/api/auth/login")
                .contentType("application/json")
                .content("{\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\"}"));
    }

    private MockHttpSession connecter(String email) throws Exception {
        return (MockHttpSession) login(email)
                .andExpect(status().isOk())
                .andReturn()
                .getRequest()
                .getSession(false);
    }
}
