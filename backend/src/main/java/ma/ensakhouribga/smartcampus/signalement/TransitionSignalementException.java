package ma.ensakhouribga.smartcampus.signalement;

/** Changement de statut interdit, par exemple sur un signalement FERME (409). */
public class TransitionSignalementException extends RuntimeException {

    public TransitionSignalementException(String message) {
        super(message);
    }
}
