package ma.ensakhouribga.smartcampus.auth;

/** Corps de la requête POST /api/auth/login. */
public record LoginRequest(String email, String password) {
}
