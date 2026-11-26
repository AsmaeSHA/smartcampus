package ma.ensakhouribga.smartcampus.signalement;

import jakarta.validation.constraints.NotNull;

public record ChangerStatutRequest(@NotNull(message = "Choisissez le nouveau statut") StatutSignalement statut) {}
