package ma.ensakhouribga.smartcampus.compte;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import ma.ensakhouribga.smartcampus.user.Role;
import org.springframework.stereotype.Component;

/**
 * Change le rôle d'un compte existant (SC-64).
 *
 * Avec l'héritage JOINED, un compte étudiant est une ligne dans « users » plus une ligne dans
 * « etudiants ». JPA ne sait pas transformer un objet Etudiant en Enseignant : on modifie donc
 * directement les tables, en gardant le même id (les données liées au compte sont conservées).
 * Après l'appel, le contexte JPA est vidé : il faut relire le compte avec son repository.
 * Doit être appelé dans une transaction (celle de CompteService).
 */
@Component
public class ChangementRole {

    @PersistenceContext
    private EntityManager em;

    public void enEtudiant(Long id, String codeMassar, String numero, String filiere, Long niveauId) {
        preparer(id, Role.ETUDIANT);
        em.createNativeQuery("insert into etudiants (id, code_massar, numero, filiere, niveau_id) "
                        + "values (?1, ?2, ?3, ?4, ?5)")
                .setParameter(1, id)
                .setParameter(2, codeMassar)
                .setParameter(3, numero)
                .setParameter(4, filiere)
                .setParameter(5, niveauId)
                .executeUpdate();
        terminer();
    }

    public void enEnseignant(Long id, String codeEnseignant, String numero, String grade, String specialite) {
        preparer(id, Role.ENSEIGNANT);
        em.createNativeQuery("insert into enseignants (id, code_enseignant, numero, grade, specialite) "
                        + "values (?1, ?2, ?3, ?4, ?5)")
                .setParameter(1, id)
                .setParameter(2, codeEnseignant)
                .setParameter(3, numero)
                .setParameter(4, grade)
                .setParameter(5, specialite)
                .executeUpdate();
        terminer();
    }

    public void enAdministrateur(Long id) {
        preparer(id, Role.ADMINISTRATEUR);
        terminer();
    }

    /** Retire les informations de l'ancien rôle et enregistre le nouveau rôle. */
    private void preparer(Long id, Role nouveauRole) {
        em.flush();
        em.createNativeQuery("delete from etudiants where id = ?1").setParameter(1, id).executeUpdate();
        em.createNativeQuery("delete from enseignants where id = ?1").setParameter(1, id).executeUpdate();
        em.createNativeQuery("update users set role = ?1 where id = ?2")
                .setParameter(1, nouveauRole.name())
                .setParameter(2, id)
                .executeUpdate();
    }

    private void terminer() {
        em.flush();
        em.clear();
    }
}
