package ma.ensakhouribga.smartcampus.compte;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

/** Tests d'intégration de SC-53 : import de comptes depuis un fichier CSV. */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:smartcampus-import-test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
class ImportComptesIntegrationTest {

    private static final String PASSWORD = "motdepasse-test";
    private static final String ADMIN = "admin.import@ensakh.ma";
    private static final String ENTETE =
            "role;prenom;nom;email;numero;codeMassar;niveau;filiere;codeEnseignant;grade;specialite\n";

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

    @BeforeEach
    void preparer() {
        niveaux.findByNom("GI3").orElseGet(() -> niveaux.save(new Niveau("GI3")));
        if (users.findByEmail(ADMIN).isEmpty()) {
            users.save(new User(ADMIN, passwordEncoder.encode(PASSWORD), "Admin", "Import",
                    Role.ADMINISTRATEUR, true));
        }
    }

    @Test
    void importeLesLignesValidesEtSignaleLesAutres() throws Exception {
        String csv = ENTETE
                + "etudiant;Sara;Alami;sara.import@usms.ac.ma;0001;D300000001;gi3;Génie Informatique;;;\n"
                + "enseignant;Karim;Bennani;karim.import@usms.ma;0102;;;;E3001;PES;Informatique\n"
                + "etudiant;Sans;Massar;sans.massar@usms.ac.ma;0002;;GI3;GI;;;\n"
                + "etudiant;Mauvais;Domaine;mauvais@usms.ma;0003;D300000003;GI3;GI;;;\n";

        String reponse = mvc.perform(multipart("/api/admin/comptes/import").file(fichier(csv))
                        .session(connecter(ADMIN, PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(4))
                .andExpect(jsonPath("$.crees.length()").value(2))
                .andExpect(jsonPath("$.erreurs.length()").value(2))
                .andExpect(jsonPath("$.erreurs[0].ligne").value(4))
                .andExpect(jsonPath("$.erreurs[0].message", containsString("Massar")))
                .andExpect(jsonPath("$.erreurs[1].ligne").value(5))
                .andExpect(jsonPath("$.erreurs[1].message", containsString("@usms.ac.ma")))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        // Le compte importé se connecte avec son mot de passe temporaire.
        JsonNode sara = json.readTree(reponse).get("crees").get(0);
        MockHttpSession session = connecter("sara.import@usms.ac.ma", sara.get("motDePasseTemporaire").asText());
        mvc.perform(get("/api/auth/me").session(session))
                .andExpect(jsonPath("$.role").value("ETUDIANT"))
                .andExpect(jsonPath("$.changerMotDePasse").value(true));
    }

    @Test
    void unEmailEnDoubleDansLeFichierNEstCreeQuUneFois() throws Exception {
        String csv = ENTETE
                + "enseignant;Nadia;Un;double.import@usms.ma;01;;;;E3101;PA;Maths\n"
                + "enseignant;Nadia;Deux;double.import@usms.ma;02;;;;E3102;PA;Maths\n";

        mvc.perform(multipart("/api/admin/comptes/import").file(fichier(csv)).session(connecter(ADMIN, PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.crees.length()").value(1))
                .andExpect(jsonPath("$.erreurs[0].ligne").value(3));
    }

    @Test
    void unNiveauInconnuEstSignale() throws Exception {
        String csv = ENTETE + "etudiant;Ali;Niveau;ali.niveau@usms.ac.ma;01;D300000010;GI9;GI;;;\n";

        mvc.perform(multipart("/api/admin/comptes/import").file(fichier(csv)).session(connecter(ADMIN, PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.erreurs[0].message", containsString("GI9")));
    }

    @Test
    void uneColonneObligatoireManquanteRefuseLeFichier() throws Exception {
        String csv = "role;prenom;email\nenseignant;Karim;k@usms.ma\n";

        mvc.perform(multipart("/api/admin/comptes/import").file(fichier(csv)).session(connecter(ADMIN, PASSWORD)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("nom")));
    }

    @Test
    void unFichierQuiNEstPasUnCsvEstRefuse() throws Exception {
        MockMultipartFile excel = new MockMultipartFile("fichier", "comptes.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", new byte[] {1, 2, 3});

        mvc.perform(multipart("/api/admin/comptes/import").file(excel).session(connecter(ADMIN, PASSWORD)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString(".csv")));
    }

    @Test
    void unFichierSansCompteEstRefuse() throws Exception {
        mvc.perform(multipart("/api/admin/comptes/import").file(fichier(ENTETE))
                        .session(connecter(ADMIN, PASSWORD)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unNonAdministrateurNePeutPasImporter() throws Exception {
        String prof = "prof.import@usms.ma";
        if (users.findByEmail(prof).isEmpty()) {
            users.save(new User(prof, passwordEncoder.encode(PASSWORD), "Prof", "Import", Role.ENSEIGNANT, true));
        }

        mvc.perform(multipart("/api/admin/comptes/import").file(fichier(ENTETE + "x;y;z;w\n"))
                        .session(connecter(prof, PASSWORD)))
                .andExpect(status().isForbidden());
    }

    private static MockMultipartFile fichier(String contenu) {
        return new MockMultipartFile("fichier", "comptes.csv", "text/csv", contenu.getBytes(StandardCharsets.UTF_8));
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
