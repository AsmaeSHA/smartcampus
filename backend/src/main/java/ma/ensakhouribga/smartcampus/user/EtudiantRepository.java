package ma.ensakhouribga.smartcampus.user;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EtudiantRepository extends JpaRepository<Etudiant, Long> {

    boolean existsByCodeMassar(String codeMassar);
}
