package ma.ensakhouribga.smartcampus.compte;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Lecture simple d'un fichier CSV pour l'import des comptes (SC-53).
 * - Séparateur détecté sur la ligne d'en-tête : « ; » (Excel en français) ou « , ».
 * - Encodage : UTF-8 (avec ou sans BOM), sinon Windows-1252 (CSV enregistré par Excel sous Windows).
 * - Les champs entre guillemets peuvent contenir le séparateur et des guillemets doublés ("").
 */
final class LecteurCsv {

    private LecteurCsv() {
    }

    /** Une ligne du fichier avec son numéro réel (1 = en-tête), pour les messages d'erreur. */
    record Ligne(int numero, List<String> valeurs) {
    }

    static String decoder(byte[] contenu) {
        try {
            String texte = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(contenu))
                    .toString();
            return texte.startsWith("﻿") ? texte.substring(1) : texte;
        } catch (CharacterCodingException e) {
            return new String(contenu, Charset.forName("windows-1252"));
        }
    }

    /** Découpe le texte en lignes non vides. La première ligne renvoyée est l'en-tête. */
    static List<Ligne> lire(String texte) {
        String[] lignesBrutes = texte.split("\\r?\\n", -1);
        List<Ligne> lignes = new ArrayList<>();
        char separateur = 0;
        for (int i = 0; i < lignesBrutes.length; i++) {
            String brute = lignesBrutes[i];
            if (brute.isBlank()) {
                continue;
            }
            if (separateur == 0) {
                separateur = detecterSeparateur(brute);
            }
            lignes.add(new Ligne(i + 1, decouper(brute, separateur)));
        }
        return lignes;
    }

    private static char detecterSeparateur(String entete) {
        long pointsVirgules = entete.chars().filter(c -> c == ';').count();
        long virgules = entete.chars().filter(c -> c == ',').count();
        return pointsVirgules >= virgules ? ';' : ',';
    }

    private static List<String> decouper(String ligne, char separateur) {
        List<String> valeurs = new ArrayList<>();
        StringBuilder courant = new StringBuilder();
        boolean entreGuillemets = false;
        for (int i = 0; i < ligne.length(); i++) {
            char c = ligne.charAt(i);
            if (entreGuillemets) {
                if (c == '"' && i + 1 < ligne.length() && ligne.charAt(i + 1) == '"') {
                    courant.append('"');
                    i++;
                } else if (c == '"') {
                    entreGuillemets = false;
                } else {
                    courant.append(c);
                }
            } else if (c == '"') {
                entreGuillemets = true;
            } else if (c == separateur) {
                valeurs.add(courant.toString().trim());
                courant.setLength(0);
            } else {
                courant.append(c);
            }
        }
        valeurs.add(courant.toString().trim());
        return valeurs;
    }
}
