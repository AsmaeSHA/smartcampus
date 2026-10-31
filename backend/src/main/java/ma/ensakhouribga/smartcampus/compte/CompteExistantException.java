package ma.ensakhouribga.smartcampus.compte;

/** Un compte utilise déjà cet e-mail ou ce code (réponse 409). */
public class CompteExistantException extends RuntimeException {

    public CompteExistantException(String message) {
        super(message);
    }
}
