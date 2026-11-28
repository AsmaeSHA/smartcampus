package ma.ensakhouribga.smartcampus.signalement;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SignalementRepository extends JpaRepository<Signalement, Long> {

    /** Les signalements d'un utilisateur, les plus récents en premier. */
    @Query("select s from Signalement s join fetch s.espace where s.signalePar.id = :utilisateurId order by s.creeLe desc")
    List<Signalement> findBySignaleParIdOrderByCreeLeDesc(@Param("utilisateurId") Long utilisateurId);

    /** Tous les signalements pour l'administrateur ; chaque filtre est facultatif (null = pas de filtre). */
    @Query("""
            select s from Signalement s join fetch s.espace join fetch s.signalePar
            where (:statut is null or s.statut = :statut)
              and (:categorie is null or s.categorie = :categorie)
              and (:espaceId is null or s.espace.id = :espaceId)
            order by s.creeLe desc
            """)
    List<Signalement> rechercher(@Param("statut") StatutSignalement statut,
                                 @Param("categorie") CategorieSignalement categorie,
                                 @Param("espaceId") Long espaceId);
}
