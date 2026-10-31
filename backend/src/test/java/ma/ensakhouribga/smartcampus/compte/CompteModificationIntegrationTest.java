package ma.ensakhouribga.smartcampus.compte;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.test.web.servlet.ResultActions;

/** Tests d'intégration de SC-64 : modifier le rôle et les informations d'un compte. */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:smartcampus-modification-test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
class CompteModificationIntegrationTest {

    private static final String PASSWORD = "motdepasse-test";
    private static final String ADMIN = "admin.modification@ensakh.ma";

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

    private Long gi2;
    private Long gi3;
    private MockHttpSession admin;

    @BeforeEach
    void preparer() throws Exception {
        gi2 = niveaux.findByNom("GI2").orElseGet(() -> niveaux.save(new Niveau("GI2"))).getId();
        gi3 = niveaux.findByNom("GI3").orElseGet(() -> niveaux.save(new Niveau("GI3"))).getId();
        if (users.findByEmail(ADMIN).isEmpty()) {
            users.save(new User(ADMIN, passwordEncoder.encode(PASSWORD), "Admin", "Modif",
                    Role.ADMINISTRATEUR, true));
        }
        admin = connecter(ADMIN, PASSWORD);
    }

    @Test
    void modifieLesInformationsDUnEtudiant() throws Exception {
        CompteCreeTest sara = creerEtudiant("sara.modif@usms.ac.ma", "D200000001");

        mvc.perform(put("/api/admin/comptes/{id}", sara.id()).session(admin)
                        .contentType("application/json")
                        .content(etudiantJson("Sara", "Alami-Idrissi", "sara.modif@usms.ac.ma", "D200000001", gi2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nom").value("Alami-Idrissi"));

        mvc.perform(get("/api/admin/comptes/{id}", sara.id()).session(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ETUDIANT"))
                .andExpect(jsonPath("$.niveauId").value(gi2))
                .andExpect(jsonPath("$.codeMassar").value("D200000001"));
    }

    @Test
    void changeUnEtudiantEnEnseignantEnGardantLeMemeCompte() throws Exception {
        CompteCreeTest compte = creerEtudiant("youssef@usms.ac.ma", "D200000002");

        mvc.perform(put("/api/admin/comptes/{id}", compte.id()).session(admin)
                        .contentType("application/json")
                        .content("""
                                {"prenom":"Youssef","nom":"Tazi","email":"youssef.tazi@usms.ma","role":"ENSEIGNANT",
                                 "codeEnseignant":"E2002","grade":"PA","specialite":"Informatique","numero":"0202"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(compte.id()))
                .andExpect(jsonPath("$.role").value("ENSEIGNANT"));

        mvc.perform(get("/api/admin/comptes/{id}", compte.id()).session(admin))
                .andExpect(jsonPath("$.codeEnseignant").value("E2002"))
                .andExpect(jsonPath("$.codeMassar").doesNotExist());

        // Même mot de passe, nouvel e-mail, nouveau rôle.
        MockHttpSession session = connecter("youssef.tazi@usms.ma", compte.motDePasse());
        mvc.perform(get("/api/auth/me").session(session))
                .andExpect(jsonPath("$.role").value("ENSEIGNANT"));
    }

    @Test
    void unEnseignantPromuAdministrateurAccedeALAdministration() throws Exception {
        CompteCreeTest compte = creerEtudiant("futur.admin@usms.ac.ma", "D200000003");

        mvc.perform(put("/api/admin/comptes/{id}", compte.id()).session(admin)
                        .contentType("application/json")
                        .content("{\"prenom\":\"Futur\",\"nom\":\"Admin\",\"email\":\"futur.admin@ensakh.ma\","
                                + "\"role\":\"ADMINISTRATEUR\"}"))
                .andExpect(status().isOk());

        mvc.perform(get("/api/admin/comptes").session(connecter("futur.admin@ensakh.ma", compte.motDePasse())))
                .andExpect(status().isOk());
    }

    @Test
    void lAdministrateurNePeutPasRetirerSonPropreRole() throws Exception {
        Long idAdmin = users.findByEmail(ADMIN).orElseThrow().getId();

        mvc.perform(put("/api/admin/comptes/{id}", idAdmin).session(admin)
                        .contentType("application/json")
                        .content(etudiantJson("Admin", "Modif", "admin.modif@usms.ac.ma", "D200000004", gi3)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("propre r")));
    }

    @Test
    void unCodeMassarDejaPrisParUnAutreRenvoie409() throws Exception {
        creerEtudiant("premier@usms.ac.ma", "D200000005");
        CompteCreeTest second = creerEtudiant("second@usms.ac.ma", "D200000006");

        mvc.perform(put("/api/admin/comptes/{id}", second.id()).session(admin)
                        .contentType("application/json")
                        .content(etudiantJson("Second", "Etudiant", "second@usms.ac.ma", "D200000005", gi3)))
                .andExpect(status().isConflict());
    }

    @Test
    void unEtudiantNePeutPasModifierDeCompte() throws Exception {
        CompteCreeTest compte = creerEtudiant("curieux@usms.ac.ma", "D200000007");

        mvc.perform(put("/api/admin/comptes/{id}", compte.id())
                        .session(connecter("curieux@usms.ac.ma", compte.motDePasse()))
                        .contentType("application/json")
                        .content(etudiantJson("Curieux", "Etudiant", "curieux@usms.ac.ma", "D200000007", gi3)))
                .andExpect(status().isForbidden());
    }

    private record CompteCreeTest(Long id, String motDePasse) {
    }

    private CompteCreeTest creerEtudiant(String email, String codeMassar) throws Exception {
        String reponse = mvc.perform(post("/api/admin/comptes").session(admin)
                        .contentType("application/json")
                        .content(etudiantJson("Etudiant", "Test", email, codeMassar, gi3)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        JsonNode corps = json.readTree(reponse);
        return new CompteCreeTest(corps.get("compte").get("id").asLong(), corps.get("motDePasseTemporaire").asText());
    }

    private String etudiantJson(String prenom, String nom, String email, String codeMassar, Long niveauId) {
        return """
                {"prenom":"%s","nom":"%s","email":"%s","role":"ETUDIANT",
                 "codeMassar":"%s","niveauId":%d,"filiere":"GI","numero":"0001"}
                """.formatted(prenom, nom, email, codeMassar, niveauId);
    }

    private ResultActions login(String email, String motDePasse) throws Exception {
        return mvc.perform(post("/api/auth/login")
                .contentType("application/json")
                .content("{\"email\":\"" + email + "\",\"password\":\"" + motDePasse + "\"}"));
    }

    private MockHttpSession connecter(String email, String motDePasse) throws Exception {
        return (MockHttpSession) login(email, motDePasse)
                .andExpect(status().isOk())
                .andReturn()
                .getRequest()
                .getSession(false);
    }
}
