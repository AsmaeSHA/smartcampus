package ma.ensakhouribga.smartcampus.auth;

/** Résultat d'une tentative de connexion. */
public enum LoginResult {
    SUCCESS,
    INVALID_CREDENTIALS,
    DOMAIN_NOT_ALLOWED,
    ACCOUNT_INACTIVE,
    TEMPORARILY_LOCKED
}
