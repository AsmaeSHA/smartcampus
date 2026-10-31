package ma.ensakhouribga.smartcampus.compte;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import ma.ensakhouribga.smartcampus.user.DomainesEmail;
import ma.ensakhouribga.smartcampus.user.Enseignant;
import ma.ensakhouribga.smartcampus.user.EnseignantRepository;
import ma.ensakhouribga.smartcampus.user.Etudiant;
import ma.ensakhouribga.smartcampus.user.EtudiantRepository;
import ma.ensakhouribga.smartcampus.user.Niveau;
import ma.ensakhouribga.smartcampus.user.NiveauRepository;
import ma.ensakhouribga.smartcampus.user.Role;
import ma.ensakhouribga.smartcampus.user.User;
import ma.ensakhouribga.smartcampus.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class CompteServiceTest {

    private final UserRepository users = mock(UserRepository.class);
    private final EtudiantRepository etudiants = mock(EtudiantRepository.class);
    private final EnseignantRepository enseignants = mock(EnseignantRepository.class);
    private final NiveauRepository niveaux = mock(NiveauRepository.class);
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    private final CompteService service = new CompteService(users, etudiants, enseignants, niveaux,
            encoder, new GenerateurMotDePasse(), new DomainesEmail("usms.ac.ma", "usms.ma", "ensakh.ma"));

    @BeforeEach
    void enregistrementRenvoieLeCompte() {
        when(users.saveAndFlush(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(niveaux.findById(1L)).thenReturn(Optional.of(new Niveau("GI3")));
    }

    @Test
    void creeUnEtudiantAvecMotDePasseTemporaireHache() {
        CompteCree resultat = service.creer(etudiant("  Sara.Alami@USMS.AC.ma ", "D123456789"));

        ArgumentCaptor<User> enregistre = ArgumentCaptor.forClass(User.class);
        verify(users).saveAndFlush(enregistre.capture());
        Etudiant etudiant = assertInstanceOf(Etudiant.class, enregistre.getValue());

        assertEquals("sara.alami@usms.ac.ma", etudiant.getEmail());
        assertEquals(Role.ETUDIANT, etudiant.getRole());
        assertEquals("GI3", etudiant.getNiveau().getNom());
        assertTrue(etudiant.isActive());
        assertTrue(etudiant.isChangerMotDePasse());

        String motDePasse = resultat.motDePasseTemporaire();
        assertEquals(GenerateurMotDePasse.LONGUEUR, motDePasse.length());
        assertNotEquals(motDePasse, etudiant.getPasswordHash());
        assertTrue(encoder.matches(motDePasse, etudiant.getPasswordHash()));
    }

    @Test
    void creeUnEnseignant() {
        service.creer(new CreerCompteRequest("Karim", "Bennani", "k.bennani@usms.ma", Role.ENSEIGNANT,
                null, null, null, "E1001", "PES", "Informatique", "0102"));

        ArgumentCaptor<User> enregistre = ArgumentCaptor.forClass(User.class);
        verify(users).saveAndFlush(enregistre.capture());
        Enseignant enseignant = assertInstanceOf(Enseignant.class, enregistre.getValue());
        assertEquals("E1001", enseignant.getCodeEnseignant());
        assertEquals(Role.ENSEIGNANT, enseignant.getRole());
    }

    @Test
    void creeUnAdministrateurSansChampsSupplementaires() {
        CompteCree resultat = service.creer(new CreerCompteRequest("Nadia", "Idrissi", "n.idrissi@ensakh.ma",
                Role.ADMINISTRATEUR, null, null, null, null, null, null, null));

        assertEquals(Role.ADMINISTRATEUR, resultat.compte().role());
        assertTrue(resultat.compte().changerMotDePasse());
    }

    @Test
    void refuseUnEmailHorsDomaine() {
        CompteInvalideException erreur = assertThrows(CompteInvalideException.class,
                () -> service.creer(etudiant("sara@gmail.com", "D123456789")));

        assertTrue(erreur.getMessage().contains("@usms.ac.ma"));
        verify(users, never()).saveAndFlush(any(User.class));
    }

    @Test
    void refuseUnEtudiantAvecLeDomaineDesEnseignants() {
        assertThrows(CompteInvalideException.class,
                () -> service.creer(etudiant("sara.alami@usms.ma", "D123456789")));
    }

    @Test
    void refuseUnAdministrateurAvecLeDomaineDesEtudiants() {
        assertThrows(CompteInvalideException.class,
                () -> service.creer(new CreerCompteRequest("Nadia", "Idrissi", "n.idrissi@usms.ac.ma",
                        Role.ADMINISTRATEUR, null, null, null, null, null, null, null)));
    }

    @Test
    void refuseUnEmailDejaUtilise() {
        when(users.findByEmail("sara.alami@usms.ac.ma"))
                .thenReturn(Optional.of(new User("sara.alami@usms.ac.ma", "hash", "S", "A", Role.ETUDIANT, true)));

        assertThrows(CompteExistantException.class,
                () -> service.creer(etudiant("sara.alami@usms.ac.ma", "D123456789")));
    }

    @Test
    void refuseUnCodeMassarDejaUtilise() {
        when(etudiants.existsByCodeMassar("D123456789")).thenReturn(true);

        assertThrows(CompteExistantException.class,
                () -> service.creer(etudiant("sara.alami@usms.ac.ma", "D123456789")));
    }

    @Test
    void refuseUnEtudiantSansCodeMassar() {
        CompteInvalideException erreur = assertThrows(CompteInvalideException.class,
                () -> service.creer(etudiant("sara.alami@usms.ac.ma", "  ")));

        assertTrue(erreur.getMessage().contains("code Massar"));
    }

    @Test
    void refuseUnNiveauInconnu() {
        CreerCompteRequest demande = new CreerCompteRequest("Sara", "Alami", "sara.alami@usms.ac.ma",
                Role.ETUDIANT, "D123456789", 99L, "Génie Informatique", null, null, null, "0001");

        assertThrows(CompteInvalideException.class, () -> service.creer(demande));
    }

    @Test
    void refuseUnEnseignantSansGrade() {
        CreerCompteRequest demande = new CreerCompteRequest("Karim", "Bennani", "k.bennani@usms.ma",
                Role.ENSEIGNANT, null, null, null, "E1001", null, "Informatique", "0102");

        assertThrows(CompteInvalideException.class, () -> service.creer(demande));
    }

    private static CreerCompteRequest etudiant(String email, String codeMassar) {
        return new CreerCompteRequest("Sara", "Alami", email, Role.ETUDIANT,
                codeMassar, 1L, "Génie Informatique", null, null, null, "0001");
    }
}
