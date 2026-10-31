package ma.ensakhouribga.smartcampus.compte;

/** Aucun compte ne correspond à l'identifiant demandé (réponse 404). */
public class CompteIntrouvableException extends RuntimeException {

    public CompteIntrouvableException(String message) {
        super(message);
    }
}
