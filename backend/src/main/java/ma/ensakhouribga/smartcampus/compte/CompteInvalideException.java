package ma.ensakhouribga.smartcampus.compte;

/** Données de compte refusées par une règle métier (réponse 400). */
public class CompteInvalideException extends RuntimeException {

    public CompteInvalideException(String message) {
        super(message);
    }
}
