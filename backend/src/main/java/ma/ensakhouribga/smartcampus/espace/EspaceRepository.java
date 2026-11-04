package ma.ensakhouribga.smartcampus.espace;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Sort;

public interface EspaceRepository extends JpaRepository<Espace, Long> {

    boolean existsByCode(String code);

    boolean existsByCodeAndIdNot(String code, Long id);

    Optional<Espace> findByCode(String code);

    List<Espace> findByStatut(StatutEspace statut, Sort sort);
}
