package ma.ensakhouribga.smartcampus.espace;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ma.ensakhouribga.smartcampus.user.Role;
import ma.ensakhouribga.smartcampus.user.Enseignant;
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
import java.time.LocalDateTime;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:smartcampus-espace-test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
class EspaceAdminIntegrationTest {

    private static final String PASSWORD = "motdepasse-test";
    private static final String ADMIN = "admin.espace@ensakh.ma";
    private static final String ETUDIANT = "etudiant.espace@usms.ac.ma";
    private static final String ENSEIGNANT = "enseignant.espace@usms.ma";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private UserRepository users;

    @Autowired
    private EspaceRepository espaces;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void preparer() {
        String hash = passwordEncoder.encode(PASSWORD);
        if (users.findByEmail(ADMIN).isEmpty()) {
            users.save(new User(ADMIN, hash, "Admin", "Espace", Role.ADMINISTRATEUR, true));
        }
        if (users.findByEmail(ETUDIANT).isEmpty()) {
            users.save(new User(ETUDIANT, hash, "Etudiant", "Espace", Role.ETUDIANT, true));
        }
        if (users.findByEmail(ENSEIGNANT).isEmpty()) {
            users.save(new Enseignant(ENSEIGNANT, hash, "Enseignant", "Espace", true,
                    "E1001", "0101", "PES", "Informatique"));
        }
    }

    @Test
    void administrateurCreeUnEspaceActif() throws Exception {
        mvc.perform(post("/api/espaces").session(connecter(ADMIN))
                        .contentType("application/json")
                        .content("""
                                {"code":"S101","nom":"Salle de cours 101","typeEspace":"SALLE_COURS"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("S101"))
                .andExpect(jsonPath("$.nom").value("Salle de cours 101"))
                .andExpect(jsonPath("$.typeEspace").value("SALLE_COURS"))
                .andExpect(jsonPath("$.statut").value("ACTIVE"));
    }

    @Test
    void utilisateurNonAdminEstRefuse() throws Exception {
        mvc.perform(post("/api/espaces").session(connecter(ETUDIANT))
                        .contentType("application/json")
                        .content("{\"code\":\"S102\",\"nom\":\"Salle\",\"typeEspace\":\"TERRAIN\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void enseignantEstRefuse() throws Exception {
        mvc.perform(post("/api/espaces").session(connecter(ENSEIGNANT))
                        .contentType("application/json")
                        .content("{\"code\":\"S106\",\"nom\":\"Salle\",\"typeEspace\":\"TERRAIN\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void utilisateurNonAuthentifieEstRefuse() throws Exception {
        mvc.perform(post("/api/espaces")
                        .contentType("application/json")
                        .content("{\"code\":\"S103\",\"nom\":\"Salle\",\"typeEspace\":\"TERRAIN\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void champsObligatoiresEtTypeInvalideSontRefuses() throws Exception {
        mvc.perform(post("/api/espaces").session(connecter(ADMIN))
                        .contentType("application/json")
                        .content("{\"code\":\" \",\"nom\":\"Salle\",\"typeEspace\":\"TERRAIN\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("code")));

        mvc.perform(post("/api/espaces").session(connecter(ADMIN))
                        .contentType("application/json")
                        .content("{\"code\":\"S104\",\"nom\":\"Salle\",\"typeEspace\":\"INCONNU\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void codeDoitEtreUnique() throws Exception {
        String corps = "{\"code\":\"S105\",\"nom\":\"Salle\",\"typeEspace\":\"TERRAIN\"}";
        mvc.perform(post("/api/espaces").session(connecter(ADMIN))
                        .contentType("application/json").content(corps))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/espaces").session(connecter(ADMIN))
                        .contentType("application/json").content(corps))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("code")));
    }

    @Test
    void administrateurModifieNomTypeEtMetAJourDateSansChangerIdentite() throws Exception {
        Espace espace = espaces.saveAndFlush(new Espace("S200", "Ancienne salle", TypeEspace.SALLE_COURS));
        Long id = espace.getId();
        LocalDateTime creeLe = espace.getCreeLe();
        LocalDateTime modifieLe = espace.getModifieLe();

        mvc.perform(put("/api/espaces/" + id).session(connecter(ADMIN))
                        .contentType("application/json")
                        .content("{\"id\":99999,\"code\":\"S200\",\"nom\":\"Salle rénovée\","
                                + "\"typeEspace\":\"BIBLIOTHEQUE\",\"statut\":\"ACTIVE\","
                                + "\"creeLe\":\"2000-01-01T00:00:00\",\"modifieLe\":\"2000-01-01T00:00:00\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.code").value("S200"))
                .andExpect(jsonPath("$.nom").value("Salle rénovée"))
                .andExpect(jsonPath("$.typeEspace").value("BIBLIOTHEQUE"))
                .andExpect(jsonPath("$.statut").value("ACTIVE"))
                .andExpect(jsonPath("$.creeLe").isNotEmpty())
                .andExpect(jsonPath("$.modifieLe").isNotEmpty());

        Espace persiste = espaces.findById(id).orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals(id, persiste.getId());
        LocalDateTime creeLeMillis = creeLe.withNano((creeLe.getNano() / 1_000_000) * 1_000_000);
        LocalDateTime persisteCreeLeMillis = persiste.getCreeLe()
                .withNano((persiste.getCreeLe().getNano() / 1_000_000) * 1_000_000);
        org.junit.jupiter.api.Assertions.assertEquals(creeLeMillis, persisteCreeLeMillis);
        org.junit.jupiter.api.Assertions.assertTrue(persiste.getModifieLe().isAfter(modifieLe));
        org.junit.jupiter.api.Assertions.assertEquals(StatutEspace.ACTIVE, persiste.getStatut());
    }

    @Test
    void etudiantEtEnseignantNePeuventPasModifier() throws Exception {
        Espace espace = espaces.saveAndFlush(new Espace("S201", "Salle", TypeEspace.TERRAIN));
        String corps = "{\"code\":\"S201\",\"nom\":\"Autre nom\",\"typeEspace\":\"BUVETTE\"}";
        mvc.perform(put("/api/espaces/" + espace.getId()).session(connecter(ETUDIANT))
                        .contentType("application/json").content(corps))
                .andExpect(status().isForbidden());
        mvc.perform(put("/api/espaces/" + espace.getId()).session(connecter(ENSEIGNANT))
                        .contentType("application/json").content(corps))
                .andExpect(status().isForbidden());
    }

    @Test
    void modificationSansSessionRenvoie401() throws Exception {
        mvc.perform(put("/api/espaces/1")
                        .contentType("application/json")
                        .content("{\"code\":\"S202\",\"nom\":\"Salle\",\"typeEspace\":\"TERRAIN\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void espaceInexistantEtChampsInvalidesSontRefuses() throws Exception {
        mvc.perform(put("/api/espaces/99999").session(connecter(ADMIN))
                        .contentType("application/json")
                        .content("{\"code\":\"S203\",\"nom\":\"Salle\",\"typeEspace\":\"TERRAIN\"}"))
                .andExpect(status().isNotFound());

        Espace espace = espaces.saveAndFlush(new Espace("S204", "Salle", TypeEspace.TERRAIN));
        mvc.perform(put("/api/espaces/" + espace.getId()).session(connecter(ADMIN))
                        .contentType("application/json")
                        .content("{\"code\":\"S204\",\"nom\":\" \",\"typeEspace\":\"TERRAIN\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(put("/api/espaces/" + espace.getId()).session(connecter(ADMIN))
                        .contentType("application/json")
                        .content("{\"code\":\"S204\",\"nom\":\"Salle\",\"typeEspace\":\"INCONNU\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void codeDUnAutreEspaceEstRefuseMaisSonPropreCodeEstAutorise() throws Exception {
        Espace premier = espaces.saveAndFlush(new Espace("S205", "Premier", TypeEspace.TERRAIN));
        Espace second = espaces.saveAndFlush(new Espace("S206", "Second", TypeEspace.TERRAIN));

        mvc.perform(put("/api/espaces/" + second.getId()).session(connecter(ADMIN))
                        .contentType("application/json")
                        .content("{\"code\":\"S205\",\"nom\":\"Second modifié\",\"typeEspace\":\"TERRAIN\"}"))
                .andExpect(status().isConflict());
        mvc.perform(put("/api/espaces/" + premier.getId()).session(connecter(ADMIN))
                        .contentType("application/json")
                        .content("{\"code\":\"S205\",\"nom\":\"Premier modifié\",\"typeEspace\":\"TERRAIN\"}"))
                .andExpect(status().isOk());
    }

    private MockHttpSession connecter(String email) throws Exception {
        return (MockHttpSession) mvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\"}"))
                .andReturn().getRequest().getSession(false);
    }
}
