package ma.ensakhouribga.smartcampus.compte;

/**
 * Réponse de la création d'un compte.
 * Le mot de passe temporaire n'est renvoyé qu'ici, une seule fois : il n'est stocké que haché.
 */
public record CompteCree(CompteResume compte, String motDePasseTemporaire) {
}
