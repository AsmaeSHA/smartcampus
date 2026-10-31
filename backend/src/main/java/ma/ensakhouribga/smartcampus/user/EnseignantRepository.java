package ma.ensakhouribga.smartcampus.user;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EnseignantRepository extends JpaRepository<Enseignant, Long> {

    boolean existsByCodeEnseignant(String codeEnseignant);
}
