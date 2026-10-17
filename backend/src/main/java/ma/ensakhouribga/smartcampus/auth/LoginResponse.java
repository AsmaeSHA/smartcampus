package ma.ensakhouribga.smartcampus.auth;

/** Réponse renvoyée au front : un message lisible et la page vers laquelle rediriger. */
public record LoginResponse(String message, String redirect) {
}
