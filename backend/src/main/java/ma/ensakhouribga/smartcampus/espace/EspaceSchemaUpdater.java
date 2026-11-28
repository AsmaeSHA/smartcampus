package ma.ensakhouribga.smartcampus.espace;

import jakarta.annotation.PostConstruct;
import java.sql.Statement;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
class EspaceSchemaUpdater {

    private final JdbcTemplate jdbc;

    EspaceSchemaUpdater(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @PostConstruct
    void mettreAJourContrainteStatut() {
        jdbc.execute((ConnectionCallback<Void>) connection -> {
            if (!"PostgreSQL".equals(connection.getMetaData().getDatabaseProductName())) {
                return null;
            }
            try (Statement statement = connection.createStatement()) {
                statement.execute("""
                        ALTER TABLE espaces DROP CONSTRAINT IF EXISTS espaces_statut_check
                        """);
                statement.execute("""
                        ALTER TABLE espaces ADD CONSTRAINT espaces_statut_check
                        CHECK (statut IN ('ACTIVE', 'ARCHIVE'))
                        """);
            }
            return null;
        });
    }
}
