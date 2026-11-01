package ma.ensakhouribga.smartcampus.compte;

import java.security.SecureRandom;
import org.springframework.stereotype.Component;

/**
 * Génère les mots de passe temporaires donnés aux nouveaux comptes.
 * Les caractères faciles à confondre (0/O, 1/l/I) sont exclus pour que l'administrateur
 * puisse transmettre le mot de passe sans erreur.
 */
@Component
public class GenerateurMotDePasse {

    static final int LONGUEUR = 16;
    private static final String ALPHABET =
            "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789";

    private final SecureRandom random = new SecureRandom();

    public String generer() {
        StringBuilder motDePasse = new StringBuilder(LONGUEUR);
        for (int i = 0; i < LONGUEUR; i++) {
            motDePasse.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return motDePasse.toString();
    }
}
