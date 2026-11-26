package ma.ensakhouribga.smartcampus.signalement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;
import ma.ensakhouribga.smartcampus.espace.Espace;
import ma.ensakhouribga.smartcampus.espace.EspaceRepository;
import ma.ensakhouribga.smartcampus.espace.TypeEspace;
import ma.ensakhouribga.smartcampus.evenement.SignalementCreeEvent;
import ma.ensakhouribga.smartcampus.user.Enseignant;
import ma.ensakhouribga.smartcampus.user.Role;
import ma.ensakhouribga.smartcampus.user.User;
import ma.ensakhouribga.smartcampus.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.EventListener;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:smartcampus-signalement-test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
class SignalementIntegrationTest {

    private static final String PASSWORD = "motdepasse-test";
    private static final String ADMIN = "admin.signalement@ensakh.ma";
    private static final String ETUDIANT = "etudiant.signalement@usms.ac.ma";
    private static final String ENSEIGNANT = "enseignant.signalement@usms.ma";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private UserRepository users;

    @Autowired
    private EspaceRepository espaces;

    @Autowired
    private SignalementRepository signalements;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EvenementsRecus evenementsRecus;

    private Espace salle;

    @TestConfiguration
    static class Config {
        @Bean
        EvenementsRecus evenementsRecus() {
            return new EvenementsRecus();
        }
    }

    /** Garde les événements publiés pour vérifier que l'administrateur pourra être notifié. */
    static class EvenementsRecus {
        final List<SignalementCreeEvent> recus = new ArrayList<>();

        @EventListener
        public void recevoir(SignalementCreeEvent evenement) {
            recus.add(evenement);
        }
    }

    @BeforeEach
    void preparer() {
        String hash = passwordEncoder.encode(PASSWORD);
        if (users.findByEmail(ADMIN).isEmpty()) {
            users.save(new User(ADMIN, hash, "Admin", "Signalement", Role.ADMINISTRATEUR, true));
        }
        if (users.findByEmail(ETUDIANT).isEmpty()) {
            users.save(new User(ETUDIANT, hash, "Etudiant", "Signalement", Role.ETUDIANT, true));
        }
        if (users.findByEmail(ENSEIGNANT).isEmpty()) {
            users.save(new Enseignant(ENSEIGNANT, hash, "Enseignant", "Signalement", true,
                    "E2001", "0202", "PES", "Informatique"));
        }
        salle = espaces.findByCode("SIG1").orElseGet(() ->
                espaces.save(new Espace("SIG1", "Salle TD signalement", TypeEspace.SALLE_COURS)));
        evenementsRecus.recus.clear();
    }

    @Test
    void etudiantSignaleUnProblemeOuvert() throws Exception {
        mvc.perform(post("/api/signalements").session(connecter(ETUDIANT))
                        .contentType("application/json")
                        .content(corps(salle.getId(), "MATERIEL", "Projecteur en panne", "Le projecteur ne s'allume plus")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.statut").value("OUVERT"))
                .andExpect(jsonPath("$.categorie").value("MATERIEL"))
                .andExpect(jsonPath("$.espaceNom").value("Salle TD signalement"))
                .andExpect(jsonPath("$.titre").value("Projecteur en panne"));

        assertThat(evenementsRecus.recus).hasSize(1);
        assertThat(evenementsRecus.recus.get(0).espaceNom()).isEqualTo("Salle TD signalement");
    }

    @Test
    void enseignantPeutAussiSignaler() throws Exception {
        mvc.perform(post("/api/signalements").session(connecter(ENSEIGNANT))
                        .contentType("application/json")
                        .content(corps(salle.getId(), "SECURITE", "Prise arrachée", "Une prise pend au mur")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.categorie").value("SECURITE"));
    }

    @Test
    void administrateurNePeutPasSignaler() throws Exception {
        long avant = signalements.count();
        mvc.perform(post("/api/signalements").session(connecter(ADMIN))
                        .contentType("application/json")
                        .content(corps(salle.getId(), "AUTRE", "Test", "Test")))
                .andExpect(status().isForbidden());
        assertThat(signalements.count()).isEqualTo(avant);
    }

    @Test
    void sansConnexionLaDemandeEstRefusee() throws Exception {
        mvc.perform(post("/api/signalements")
                        .contentType("application/json")
                        .content(corps(salle.getId(), "AUTRE", "Test", "Test")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void titreVideRenvoieUneErreurSurLeChamp() throws Exception {
        mvc.perform(post("/api/signalements").session(connecter(ETUDIANT))
                        .contentType("application/json")
                        .content(corps(salle.getId(), "PROPRETE", "  ", "Table sale")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.champ").value("titre"));
    }

    @Test
    void categorieInconnueEstRefusee() throws Exception {
        mvc.perform(post("/api/signalements").session(connecter(ETUDIANT))
                        .contentType("application/json")
                        .content(corps(salle.getId(), "INCONNUE", "Titre", "Description")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void espaceInexistantRenvoie404() throws Exception {
        mvc.perform(post("/api/signalements").session(connecter(ETUDIANT))
                        .contentType("application/json")
                        .content(corps(999999L, "AUTRE", "Titre", "Description")))
                .andExpect(status().isNotFound());
    }

    private String corps(Long espaceId, String categorie, String titre, String description) {
        return "{\"espaceId\":" + espaceId + ",\"categorie\":\"" + categorie + "\",\"titre\":\"" + titre
                + "\",\"description\":\"" + description + "\"}";
    }

    private MockHttpSession connecter(String email) throws Exception {
        return (MockHttpSession) mvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\"}"))
                .andReturn().getRequest().getSession(false);
    }
}
