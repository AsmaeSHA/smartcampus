package ma.ensakhouribga.smartcampus.espace;

import java.time.LocalDateTime;

public record EspaceResume(Long id, String code, String nom, TypeEspace typeEspace,
                           StatutEspace statut, LocalDateTime creeLe, LocalDateTime modifieLe) {

    static EspaceResume depuis(Espace espace) {
        return new EspaceResume(espace.getId(), espace.getCode(), espace.getNom(), espace.getTypeEspace(),
                espace.getStatut(), espace.getCreeLe(), espace.getModifieLe());
    }
}
