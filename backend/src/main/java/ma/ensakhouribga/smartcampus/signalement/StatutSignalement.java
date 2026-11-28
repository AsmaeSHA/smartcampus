package ma.ensakhouribga.smartcampus.signalement;

/** Cycle d'un signalement : OUVERT, puis EN_COURS, RESOLU et FERME (état final). */
public enum StatutSignalement {
    OUVERT,
    EN_COURS,
    RESOLU,
    FERME
}
