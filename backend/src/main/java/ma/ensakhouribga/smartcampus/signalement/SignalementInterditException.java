package ma.ensakhouribga.smartcampus.signalement;

/** L'utilisateur n'a pas le droit de faire cette action sur un signalement (403). */
public class SignalementInterditException extends RuntimeException {

    public SignalementInterditException(String message) {
        super(message);
    }
}
