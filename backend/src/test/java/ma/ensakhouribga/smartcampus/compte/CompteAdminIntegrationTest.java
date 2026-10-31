package ma.ensakhouribga.smartcampus.compte;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

/** Tests d'intégration de SC-27 : création et liste des comptes par l'administrateur. */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:smartcampus-compte-test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
class CompteAdminIntegrationTest {

    private static final String PASSWORD = "motdepasse-test";
    private static final String ADMIN = "admin.compte@ensakh.ma";
    private static final String ETUDIANT = "etudiant.compte@usms.ac.ma";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private UserRepository users;

    @Autowired
    private NiveauRepository niveaux;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper json;

    private Long niveauId;

    @BeforeEach
    void preparer() {
        Niveau gi3 = niveaux.findByNom("GI3").orElseGet(() -> niveaux.save(new Niveau("GI3")));
        niveauId = gi3.getId();
        String hash = passwordEncoder.encode(PASSWORD);
        if (users.findByEmail(ADMIN).isEmpty()) {
            users.save(new User(ADMIN, hash, "Admin", "Compte", Role.ADMINISTRATEUR, true));
        }
        if (users.findByEmail(ETUDIANT).isEmpty()) {
            users.save(new Etudiant(ETUDIANT, hash, "Etudiant", "Compte", true,
                    "D900000000", "0009", "Génie Informatique", gi3));
        }
    }

    @Test
    void lAdministrateurCreeUnEtudiantQuiPeutSeConnecter() throws Exception {
        String reponse = mvc.perform(post("/api/admin/comptes").session(connecter(ADMIN))
                        .contentType("application/json")
                        .content(etudiantJson("nouveau.etudiant@usms.ac.ma", "D100000001")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.compte.email").value("nouveau.etudiant@usms.ac.ma"))
                .andExpect(jsonPath("$.compte.role").value("ETUDIANT"))
                .andExpect(jsonPath("$.compte.changerMotDePasse").value(true))
                .andExpect(jsonPath("$.motDePasseTemporaire").isNotEmpty())
                .andReturn().getResponse().getContentAsString();

        JsonNode corps = json.readTree(reponse);
        String motDePasse = corps.get("motDePasseTemporaire").asText();

        // Le nouveau compte se connecte avec le mot de passe temporaire et doit le changer.
        MockHttpSession session = connecter("nouveau.etudiant@usms.ac.ma", motDePasse);
        mvc.perform(get("/api/auth/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ETUDIANT"))
                .andExpect(jsonPath("$.changerMotDePasse").value(true));
    }

    @Test
    void unEtudiantNePeutPasCreerDeCompte() throws Exception {
        mvc.perform(post("/api/admin/comptes").session(connecter(ETUDIANT))
                        .contentType("application/json")
                        .content(etudiantJson("interdit@usms.ac.ma", "D100000002")))
                .andExpect(status().isForbidden());
    }

    @Test
    void unEmailDejaUtiliseRenvoie409() throws Exception {
        mvc.perform(post("/api/admin/comptes").session(connecter(ADMIN))
                        .contentType("application/json")
                        .content(etudiantJson(ETUDIANT, "D100000003")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("existe")));
    }

    @Test
    void unEmailHorsDomaineRenvoie400() throws Exception {
        mvc.perform(post("/api/admin/comptes").session(connecter(ADMIN))
                        .contentType("application/json")
                        .content(etudiantJson("quelquun@gmail.com", "D100000004")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("@usms.ac.ma")));
    }

    @Test
    void unEtudiantAvecLeDomaineEnseignantRenvoie400() throws Exception {
        mvc.perform(post("/api/admin/comptes").session(connecter(ADMIN))
                        .contentType("application/json")
                        .content(etudiantJson("mauvais.domaine@usms.ma", "D100000005")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("@usms.ac.ma")));
    }

    @Test
    void unPrenomManquantRenvoie400() throws Exception {
        mvc.perform(post("/api/admin/comptes").session(connecter(ADMIN))
                        .contentType("application/json")
                        .content("{\"nom\":\"Alami\",\"email\":\"sans.prenom@ensakh.ma\",\"role\":\"ADMINISTRATEUR\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("obligatoire")));
    }

    @Test
    void laListeDesComptesNeContientPasLesMotsDePasse() throws Exception {
        mvc.perform(get("/api/admin/comptes").session(connecter(ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].email", hasItem(ADMIN)))
                .andExpect(content().string(not(containsString("passwordHash"))))
                .andExpect(content().string(not(containsString("$2a$"))));
    }

    @Test
    void laListeDesNiveauxEstDisponible() throws Exception {
        mvc.perform(get("/api/admin/niveaux").session(connecter(ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].nom", hasItem("GI3")));
    }

    private String etudiantJson(String email, String codeMassar) {
        return """
                {"prenom":"Sara","nom":"Alami","email":"%s","role":"ETUDIANT",
                 "codeMassar":"%s","niveauId":%d,"filiere":"Génie Informatique","numero":"0001"}
                """.formatted(email, codeMassar, niveauId);
    }

    private MockHttpSession connecter(String email) throws Exception {
        return connecter(email, PASSWORD);
    }

    private MockHttpSession connecter(String email, String motDePasse) throws Exception {
        return (MockHttpSession) mvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + motDePasse + "\"}"))
                .andExpect(status().isOk())
                .andReturn()
                .getRequest()
                .getSession(false);
    }
}
