package ma.ensakhouribga.smartcampus.signalement;

import java.util.List;

/** Détail d'un signalement : ses informations, l'administrateur qui le traite et le fil des commentaires. */
public record SignalementDetail(
        SignalementResume signalement,
        String traitePar,
        List<StatutSignalement> statutsPossibles,
        List<CommentaireResume> commentaires
) {}
