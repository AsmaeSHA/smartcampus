package ma.ensakhouribga.smartcampus.user;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EtudiantRepository extends JpaRepository<Etudiant, Long> {

    boolean existsByCodeMassar(String codeMassar);

    /** Vrai si un autre étudiant que {@code id} utilise déjà ce code Massar. */
    boolean existsByCodeMassarAndIdNot(String codeMassar, Long id);
}
