package ma.ensakhouribga.smartcampus.config;

import ma.ensakhouribga.smartcampus.user.User;
import ma.ensakhouribga.smartcampus.user.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Crée un compte de test pour le développement uniquement.
 * Actif seulement avec le profil « dev » : mvn spring-boot:run -Dspring-boot.run.profiles=dev
 */
@Component
@Profile("dev")
public class DevDataLoader implements CommandLineRunner {

    static final String TEST_EMAIL = "test@usms.ma";
    static final String TEST_PASSWORD = "test1234";

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;

    public DevDataLoader(UserRepository users, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (users.findByEmail(TEST_EMAIL).isEmpty()) {
            users.save(new User(TEST_EMAIL, passwordEncoder.encode(TEST_PASSWORD), true));
        }
    }
}
