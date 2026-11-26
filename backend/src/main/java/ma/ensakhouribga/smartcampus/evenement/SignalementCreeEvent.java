package ma.ensakhouribga.smartcampus.evenement;

/** Publié par Asmae quand un signalement est créé. */
public record SignalementCreeEvent(
        Long signalementId,
        String espaceNom
) {}