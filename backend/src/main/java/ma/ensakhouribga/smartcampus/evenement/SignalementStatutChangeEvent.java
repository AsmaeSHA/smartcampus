package ma.ensakhouribga.smartcampus.evenement;

/** Publié par Asmae à chaque changement de statut d'un signalement. statut : "EN_COURS", "RESOLU" ou "FERME". */
public record SignalementStatutChangeEvent(
        Long signalementId,
        Long signaleurId,
        String espaceNom,
        String statut
) {}