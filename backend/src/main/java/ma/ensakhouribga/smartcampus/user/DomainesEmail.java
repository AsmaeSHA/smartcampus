package ma.ensakhouribga.smartcampus.user;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Domaines e-mail institutionnels, un par rôle (règle R1 mise à jour) :
 * étudiant @usms.ac.ma, enseignant @usms.ma, administrateur @ensakh.ma.
 * Les valeurs viennent de application.properties (smartcampus.domaine.*).
 */
@Component
public class DomainesEmail {

    private final Map<Role, String> domaines = new EnumMap<>(Role.class);

    @Autowired
    public DomainesEmail(@Value("${smartcampus.domaine.etudiant}") String etudiant,
                         @Value("${smartcampus.domaine.enseignant}") String enseignant,
                         @Value("${smartcampus.domaine.administrateur}") String administrateur) {
        domaines.put(Role.ETUDIANT, etudiant.toLowerCase(Locale.ROOT));
        domaines.put(Role.ENSEIGNANT, enseignant.toLowerCase(Locale.ROOT));
        domaines.put(Role.ADMINISTRATEUR, administrateur.toLowerCase(Locale.ROOT));
    }

    /** Domaine attendu pour un rôle, par exemple « usms.ac.ma » pour un étudiant. */
    public String pour(Role role) {
        return domaines.get(role);
    }

    /** Vrai si l'e-mail (déjà normalisé en minuscules) appartient au domaine du rôle. */
    public boolean correspond(String email, Role role) {
        return email.endsWith("@" + pour(role));
    }

    /** Vrai si l'e-mail appartient à l'un des domaines de l'école, quel que soit le rôle. */
    public boolean estInstitutionnel(String email) {
        return domaines.values().stream().anyMatch(domaine -> email.endsWith("@" + domaine));
    }
}
