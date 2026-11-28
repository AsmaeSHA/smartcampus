package ma.ensakhouribga.smartcampus.reservation;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    @Query("select case when count(r) > 0 then true else false end from Reservation r "
            + "where r.espace.id = :espaceId and r.statut in :statuts "
            + "and r.debut < :fin and r.fin > :debut")
    boolean existeChevauchement(@Param("espaceId") Long espaceId,
                                @Param("statuts") List<StatutReservation> statuts,
                                @Param("debut") LocalDateTime debut,
                                @Param("fin") LocalDateTime fin);

    List<Reservation> findByDemandeurIdOrderByCreeLeDesc(Long demandeurId);

    Optional<Reservation> findByIdAndDemandeurId(Long id, Long demandeurId);
}
