package ma.ensakhouribga.smartcampus.signalement;

import java.time.LocalDateTime;

/** Vue d'un signalement renvoyée par l'API. */
public record SignalementResume(
        Long id,
        Long espaceId,
        String espaceNom,
        CategorieSignalement categorie,
        String titre,
        String description,
        StatutSignalement statut,
        LocalDateTime creeLe,
        LocalDateTime modifieLe,
        LocalDateTime fermeLe
) {
    public static SignalementResume depuis(Signalement signalement) {
        return new SignalementResume(
                signalement.getId(),
                signalement.getEspace().getId(),
                signalement.getEspace().getNom(),
                signalement.getCategorie(),
                signalement.getTitre(),
                signalement.getDescription(),
                signalement.getStatut(),
                signalement.getCreeLe(),
                signalement.getModifieLe(),
                signalement.getFermeLe());
    }
}
