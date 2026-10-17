package ma.ensakhouribga.smartcampus.auth;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import ma.ensakhouribga.smartcampus.user.User;
import ma.ensakhouribga.smartcampus.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Règles de connexion de SmartCampus :
 * - seul le domaine e-mail de l'école est accepté ;
 * - le mot de passe est vérifié par son hash ;
 * - un compte désactivé ne peut pas se connecter ;
 * - après 3 échecs consécutifs, l'événement est journalisé pour le suivi de sécurité.
 */
@Service
public class LoginService {

    private static final Logger log = LoggerFactory.getLogger(LoginService.class);
    static final int SECURITY_ALERT_THRESHOLD = 3;

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final String allowedDomain;
    private final Map<String, Integer> consecutiveFailures = new ConcurrentHashMap<>();

    public LoginService(UserRepository users,
                        PasswordEncoder passwordEncoder,
                        @Value("${smartcampus.allowed-domain}") String allowedDomain) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.allowedDomain = allowedDomain.toLowerCase(Locale.ROOT);
    }

    public LoginResult login(String email, String rawPassword) {
        String normalizedEmail = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);

        if (!normalizedEmail.endsWith("@" + allowedDomain)) {
            return LoginResult.DOMAIN_NOT_ALLOWED;
        }

        Optional<User> user = users.findByEmail(normalizedEmail);
        if (user.isEmpty() || !passwordEncoder.matches(rawPassword, user.get().getPasswordHash())) {
            // Même message pour « compte inconnu » et « mauvais mot de passe » : on ne révèle pas les comptes existants.
            recordFailure(normalizedEmail);
            return LoginResult.INVALID_CREDENTIALS;
        }

        if (!user.get().isActive()) {
            return LoginResult.ACCOUNT_INACTIVE;
        }

        consecutiveFailures.remove(normalizedEmail);
        return LoginResult.SUCCESS;
    }

    private void recordFailure(String email) {
        int failures = consecutiveFailures.merge(email, 1, Integer::sum);
        if (failures >= SECURITY_ALERT_THRESHOLD) {
            log.warn("Sécurité : {} échecs de connexion consécutifs pour {}", failures, email);
        }
    }
}
