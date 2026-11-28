package ma.ensakhouribga.smartcampus.signalement;

public class SignalementIntrouvableException extends RuntimeException {

    public SignalementIntrouvableException() {
        super("Ce signalement n'existe pas");
    }
}
