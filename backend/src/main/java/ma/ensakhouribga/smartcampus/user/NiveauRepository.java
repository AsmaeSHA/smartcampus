package ma.ensakhouribga.smartcampus.user;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NiveauRepository extends JpaRepository<Niveau, Long> {

    Optional<Niveau> findByNom(String nom);
}
