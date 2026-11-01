package ma.ensakhouribga.smartcampus.auth;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import ma.ensakhouribga.smartcampus.user.DomainesEmail;
import ma.ensakhouribga.smartcampus.user.User;
import ma.ensakhouribga.smartcampus.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Règles de connexion de SmartCampus :
 * - seuls les domaines e-mail de l'école sont acceptés (étudiant, enseignant, administrateur) ;
 * - le mot de passe est vérifié par son hash ;
 * - un compte désactivé ne peut pas se connecter ;
 * - après 3 échecs consécutifs, le compte est verrouillé 5 minutes.
 */
@Service
public class LoginService {

    private static final Logger log = LoggerFactory.getLogger(LoginService.class);

    static final int MAX_FAILURES = 3;
    static final Duration LOCK_DURATION = Duration.ofMinutes(5);

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final DomainesEmail domaines;
    private final Clock clock;
    private final Map<String, Integer> consecutiveFailures = new ConcurrentHashMap<>();
    private final Map<String, Instant> lockedUntil = new ConcurrentHashMap<>();

    @Autowired
    public LoginService(UserRepository users,
                        PasswordEncoder passwordEncoder,
                        DomainesEmail domaines) {
        this(users, passwordEncoder, domaines, Clock.systemDefaultZone());
    }

    LoginService(UserRepository users,
                 PasswordEncoder passwordEncoder,
                 DomainesEmail domaines,
                 Clock clock) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.domaines = domaines;
        this.clock = clock;
    }

    public LoginResult login(String email, String rawPassword) {
        String normalizedEmail = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);

        if (!domaines.estInstitutionnel(normalizedEmail)) {
            return LoginResult.DOMAIN_NOT_ALLOWED;
        }

        Instant now = clock.instant();
        if (isLocked(normalizedEmail, now)) {
            return LoginResult.TEMPORARILY_LOCKED;
        }

        Optional<User> user = users.findByEmail(normalizedEmail);
        if (user.isEmpty() || !passwordEncoder.matches(rawPassword, user.get().getPasswordHash())) {
            // Même message pour « compte inconnu » et « mauvais mot de passe » : on ne révèle pas les comptes existants.
            recordFailure(normalizedEmail, now);
            return LoginResult.INVALID_CREDENTIALS;
        }

        if (!user.get().isActive()) {
            return LoginResult.ACCOUNT_INACTIVE;
        }

        consecutiveFailures.remove(normalizedEmail);
        if (user.get().isChangerMotDePasse()) {
            return LoginResult.PASSWORD_CHANGE_REQUIRED;
        }
        return LoginResult.SUCCESS;
    }

    @org.springframework.transaction.annotation.Transactional
    public void changerMotDePasse(User user, String ancien, String nouveau) {
        if (ancien == null || !passwordEncoder.matches(ancien, user.getPasswordHash())) {
            throw new SecurityException("L'ancien mot de passe est incorrect");
        }
        if (nouveau == null) {
            throw new IllegalArgumentException("Le nouveau mot de passe est obligatoire");
        }
        if (nouveau.length() < 12 || nouveau.length() > 128) {
            throw new IllegalArgumentException("Le nouveau mot de passe doit contenir entre 12 et 128 caractères");
        }
        if (passwordEncoder.matches(nouveau, user.getPasswordHash())) {
            throw new IllegalArgumentException("Le nouveau mot de passe doit être différent de l'ancien");
        }
        user.changerMotDePasse(passwordEncoder.encode(nouveau));
        users.save(user);
    }

    private boolean isLocked(String email, Instant now) {
        Instant until = lockedUntil.get(email);
        if (until == null) {
            return false;
        }
        if (now.isBefore(until)) {
            return true;
        }
        // Le verrouillage est terminé : on repart de zéro.
        lockedUntil.remove(email);
        return false;
    }

    private void recordFailure(String email, Instant now) {
        int failures = consecutiveFailures.merge(email, 1, Integer::sum);
        if (failures >= MAX_FAILURES) {
            consecutiveFailures.remove(email);
            lockedUntil.put(email, now.plus(LOCK_DURATION));
            log.warn("Sécurité : compte verrouillé {} minutes après {} échecs pour {}",
                    LOCK_DURATION.toMinutes(), MAX_FAILURES, email);
        }
    }
}
