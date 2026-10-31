package ma.ensakhouribga.smartcampus.user;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EnseignantRepository extends JpaRepository<Enseignant, Long> {

    boolean existsByCodeEnseignant(String codeEnseignant);

    /** Vrai si un autre enseignant que {@code id} utilise déjà ce code. */
    boolean existsByCodeEnseignantAndIdNot(String codeEnseignant, Long id);
}
