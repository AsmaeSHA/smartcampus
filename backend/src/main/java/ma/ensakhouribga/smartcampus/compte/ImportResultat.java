package ma.ensakhouribga.smartcampus.compte;

import java.util.List;
import ma.ensakhouribga.smartcampus.user.Role;

/**
 * Résultat d'un import CSV (SC-53) : les comptes créés, avec leur mot de passe temporaire
 * (renvoyé une seule fois), et les lignes refusées avec la raison.
 */
public record ImportResultat(int total, List<CompteImporte> crees, List<LigneRefusee> erreurs) {

    public record CompteImporte(int ligne, String email, String prenom, String nom, Role role,
                                String motDePasseTemporaire) {
    }

    public record LigneRefusee(int ligne, String email, String message) {
    }
}
