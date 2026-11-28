
package ma.ensakhouribga.smartcampus.espace;

import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EspaceRepository extends JpaRepository<Espace, Long> {

    boolean existsByCode(String code);

    boolean existsByCodeAndIdNot(String code, Long id);

    Optional<Espace> findByCode(String code);

    List<Espace> findByStatut(StatutEspace statut, Sort sort);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from Espace e where e.id = :id")
    Optional<Espace> verrouillerParId(@Param("id") Long id);
}
