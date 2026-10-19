package ma.ensakhouribga.smartcampus.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import ma.ensakhouribga.smartcampus.user.User;
import ma.ensakhouribga.smartcampus.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class LoginServiceTest {

    private static final String DOMAIN = "ensakhouribga.ma";
    private static final String EMAIL = "asmae@ensakhouribga.ma";
    private static final String PASSWORD = "secret123";

    private final UserRepository users = mock(UserRepository.class);
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    private final LoginService service = new LoginService(users, encoder, DOMAIN);

    @Test
    void connexionReussiteAvecBonsIdentifiants() {
        when(users.findByEmail(EMAIL))
                .thenReturn(Optional.of(new User(EMAIL, encoder.encode(PASSWORD), true)));

        assertEquals(LoginResult.SUCCESS, service.login(EMAIL, PASSWORD));
    }

    @Test
    void mauvaisMotDePasseEstRefuse() {
        when(users.findByEmail(EMAIL))
                .thenReturn(Optional.of(new User(EMAIL, encoder.encode(PASSWORD), true)));

        assertEquals(LoginResult.INVALID_CREDENTIALS, service.login(EMAIL, "mauvais"));
    }

    @Test
    void compteInconnuEstRefuseAvecLeMemeMessage() {
        when(users.findByEmail(EMAIL)).thenReturn(Optional.empty());

        assertEquals(LoginResult.INVALID_CREDENTIALS, service.login(EMAIL, PASSWORD));
    }

    @Test
    void domaineExterneEstRefuseSansInterrogerLaBase() {
        assertEquals(LoginResult.DOMAIN_NOT_ALLOWED,
                service.login("asmae@gmail.com", PASSWORD));

        verify(users, never()).findByEmail("asmae@gmail.com");
    }

    @Test
    void compteDesactiveNePeutPasSeConnecter() {
        when(users.findByEmail(EMAIL))
                .thenReturn(Optional.of(new User(EMAIL, encoder.encode(PASSWORD), false)));

        assertEquals(LoginResult.ACCOUNT_INACTIVE, service.login(EMAIL, PASSWORD));
    }

    @Test
    void emailEnMajusculesEtEspacesEstAccepte() {
        when(users.findByEmail(EMAIL))
                .thenReturn(Optional.of(new User(EMAIL, encoder.encode(PASSWORD), true)));

        assertEquals(LoginResult.SUCCESS, service.login("  ASMAE@ENSAKHOURIBGA.MA ", PASSWORD));
    }
}
