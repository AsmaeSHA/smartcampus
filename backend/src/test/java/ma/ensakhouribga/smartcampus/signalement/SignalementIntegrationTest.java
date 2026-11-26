package ma.ensakhouribga.smartcampus.signalement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

import java.util.ArrayList;
import java.util.List;
import ma.ensakhouribga.smartcampus.espace.Espace;
import ma.ensakhouribga.smartcampus.espace.EspaceRepository;
import ma.ensakhouribga.smartcampus.espace.TypeEspace;
import ma.ensakhouribga.smartcampus.evenement.SignalementCreeEvent;
import ma.ensakhouribga.smartcampus.evenement.SignalementStatutChangeEvent;
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
        final List<SignalementStatutChangeEvent> statuts = new ArrayList<>();

        @EventListener
        public void recevoir(SignalementCreeEvent evenement) {
            recus.add(evenement);
        }

        @EventListener
        public void recevoirStatut(SignalementStatutChangeEvent evenement) {
            statuts.add(evenement);
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
        evenementsRecus.statuts.clear();
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

    @Test
    void chacunNeVoitQueSesSignalements() throws Exception {
        User etudiant = users.findByEmail(ETUDIANT).orElseThrow();
        User enseignant = users.findByEmail(ENSEIGNANT).orElseThrow();
        Espace espace = espaces.save(new Espace("SIG-MINE", "Salle mes signalements", TypeEspace.SALLE_COURS));
        signalements.save(new Signalement(espace, etudiant, CategorieSignalement.PROPRETE, "Sol sale", "Sol sale"));
        signalements.save(new Signalement(espace, enseignant, CategorieSignalement.MATERIEL, "Tableau", "Tableau cassé"));

        mvc.perform(get("/api/signalements/mine").session(connecter(ENSEIGNANT)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].auteur", everyItem(is("Enseignant Signalement"))))
                .andExpect(jsonPath("$[?(@.titre == 'Tableau')]", hasSize(1)))
                .andExpect(jsonPath("$[?(@.titre == 'Sol sale')]", hasSize(0)));
    }

    @Test
    void mesSignalementsSansConnexionRenvoie401() throws Exception {
        mvc.perform(get("/api/signalements/mine")).andExpect(status().isUnauthorized());
    }

    @Test
    void administrateurFiltreParStatutCategorieEtEspace() throws Exception {
        User etudiant = users.findByEmail(ETUDIANT).orElseThrow();
        Espace espace = espaces.save(new Espace("SIG-FILTRE", "Salle filtre", TypeEspace.SALLE_REUNION));
        signalements.save(new Signalement(espace, etudiant, CategorieSignalement.MATERIEL, "Chaise", "Chaise cassée"));
        signalements.save(new Signalement(espace, etudiant, CategorieSignalement.SECURITE, "Fil", "Fil dénudé"));

        mvc.perform(get("/api/admin/signalements").param("espaceId", espace.getId().toString())
                        .session(connecter(ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));

        mvc.perform(get("/api/admin/signalements")
                        .param("espaceId", espace.getId().toString())
                        .param("statut", "OUVERT")
                        .param("categorie", "SECURITE")
                        .session(connecter(ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].titre").value("Fil"))
                .andExpect(jsonPath("$[0].auteur").value("Etudiant Signalement"));

        mvc.perform(get("/api/admin/signalements")
                        .param("espaceId", espace.getId().toString())
                        .param("statut", "FERME")
                        .session(connecter(ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void etudiantNAccedePasALaListeAdmin() throws Exception {
        mvc.perform(get("/api/admin/signalements").session(connecter(ETUDIANT)))
                .andExpect(status().isForbidden());
    }

    @Test
    void filtreInconnuRenvoie400() throws Exception {
        mvc.perform(get("/api/admin/signalements").param("statut", "PERDU").session(connecter(ADMIN)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void espaceArchiveNePeutPasEtreSignale() throws Exception {
        Espace archive = new Espace("SIG-ARCH", "Salle archivée", TypeEspace.SALLE_COURS);
        archive.archiver();
        archive = espaces.save(archive);
        mvc.perform(post("/api/signalements").session(connecter(ETUDIANT))
                        .contentType("application/json")
                        .content(corps(archive.getId(), "AUTRE", "Titre", "Description")))
                .andExpect(status().isNotFound());
    }

    @Test
    void administrateurTraiteUnSignalementJusquALaFermeture() throws Exception {
        Signalement signalement = nouveauSignalement("SIG-TRAITE");
        String url = "/api/admin/signalements/" + signalement.getId();
        MockHttpSession admin = connecter(ADMIN);

        mvc.perform(patch(url + "/statut").session(admin).contentType("application/json")
                        .content("{\"statut\":\"EN_COURS\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.signalement.statut").value("EN_COURS"))
                .andExpect(jsonPath("$.traitePar").value("Admin Signalement"));

        mvc.perform(post(url + "/commentaires").session(admin).contentType("application/json")
                        .content("{\"contenu\":\"Technicien prévu demain\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.commentaires[0].contenu").value("Technicien prévu demain"))
                .andExpect(jsonPath("$.commentaires[0].auteur").value("Admin Signalement"));

        mvc.perform(patch(url + "/statut").session(admin).contentType("application/json")
                        .content("{\"statut\":\"RESOLU\"}"))
                .andExpect(status().isOk());
        mvc.perform(patch(url + "/statut").session(admin).contentType("application/json")
                        .content("{\"statut\":\"FERME\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.signalement.statut").value("FERME"))
                .andExpect(jsonPath("$.signalement.fermeLe").isNotEmpty())
                .andExpect(jsonPath("$.statutsPossibles", hasSize(0)));

        assertThat(evenementsRecus.statuts).extracting(SignalementStatutChangeEvent::statut)
                .containsExactly("EN_COURS", "RESOLU", "FERME");
    }

    @Test
    void unSignalementFermeNeChangePlus() throws Exception {
        Signalement signalement = nouveauSignalement("SIG-FERME");
        String url = "/api/admin/signalements/" + signalement.getId();
        MockHttpSession admin = connecter(ADMIN);

        mvc.perform(patch(url + "/statut").session(admin).contentType("application/json")
                        .content("{\"statut\":\"FERME\"}"))
                .andExpect(status().isOk());
        mvc.perform(patch(url + "/statut").session(admin).contentType("application/json")
                        .content("{\"statut\":\"EN_COURS\"}"))
                .andExpect(status().isConflict());
        mvc.perform(post(url + "/commentaires").session(admin).contentType("application/json")
                        .content("{\"contenu\":\"Trop tard\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void onNeSautePasDEtape() throws Exception {
        Signalement signalement = nouveauSignalement("SIG-SAUT");
        mvc.perform(patch("/api/admin/signalements/" + signalement.getId() + "/statut").session(connecter(ADMIN))
                        .contentType("application/json").content("{\"statut\":\"RESOLU\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void lAuteurVoitLeSuiviMaisPasLesAutres() throws Exception {
        Signalement signalement = nouveauSignalement("SIG-SUIVI");
        mvc.perform(post("/api/admin/signalements/" + signalement.getId() + "/commentaires")
                        .session(connecter(ADMIN)).contentType("application/json")
                        .content("{\"contenu\":\"Pris en charge\"}"))
                .andExpect(status().isCreated());

        mvc.perform(get("/api/signalements/" + signalement.getId()).session(connecter(ETUDIANT)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.signalement.titre").value("Lampe"))
                .andExpect(jsonPath("$.commentaires", hasSize(1)));

        mvc.perform(get("/api/signalements/" + signalement.getId()).session(connecter(ENSEIGNANT)))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/signalements/999999").session(connecter(ETUDIANT)))
                .andExpect(status().isNotFound());
    }

    @Test
    void etudiantNePeutPasTraiter() throws Exception {
        Signalement signalement = nouveauSignalement("SIG-INTERDIT");
        mvc.perform(patch("/api/admin/signalements/" + signalement.getId() + "/statut").session(connecter(ETUDIANT))
                        .contentType("application/json").content("{\"statut\":\"EN_COURS\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void commentaireVideRenvoie400() throws Exception {
        Signalement signalement = nouveauSignalement("SIG-VIDE");
        mvc.perform(post("/api/admin/signalements/" + signalement.getId() + "/commentaires")
                        .session(connecter(ADMIN)).contentType("application/json")
                        .content("{\"contenu\":\"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.champ").value("contenu"));
    }

    /** Un signalement OUVERT créé par l'étudiant, dans un espace propre au test. */
    private Signalement nouveauSignalement(String codeEspace) {
        User etudiant = users.findByEmail(ETUDIANT).orElseThrow();
        Espace espace = espaces.save(new Espace(codeEspace, "Salle " + codeEspace, TypeEspace.SALLE_COURS));
        return signalements.save(new Signalement(espace, etudiant, CategorieSignalement.MATERIEL, "Lampe", "Lampe grillée"));
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
