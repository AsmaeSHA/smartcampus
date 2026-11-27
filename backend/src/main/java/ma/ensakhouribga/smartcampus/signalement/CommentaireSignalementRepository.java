package ma.ensakhouribga.smartcampus.signalement;

import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

public interface CommentaireSignalementRepository extends JpaRepository<CommentaireSignalement, Long> {

    /** Le fil de suivi d'un signalement, du plus ancien au plus récent. */
    @Query("select c from CommentaireSignalement c join fetch c.auteur where c.signalement.id = :signalementId order by c.creeLe asc, c.id asc")
    List<CommentaireSignalement> findBySignalementIdOrderByCreeLeAsc(@Param("signalementId") Long signalementId);
}
