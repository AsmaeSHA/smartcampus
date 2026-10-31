package ma.ensakhouribga.smartcampus.compte;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;

class LecteurCsvTest {

    @Test
    void litUnFichierAvecPointVirguleEtIgnoreLesLignesVides() {
        List<LecteurCsv.Ligne> lignes = LecteurCsv.lire("role;prenom;nom\r\n\r\netudiant;Sara;Alami\r\n");

        assertEquals(2, lignes.size());
        assertEquals(List.of("etudiant", "Sara", "Alami"), lignes.get(1).valeurs());
        assertEquals(3, lignes.get(1).numero());
    }

    @Test
    void detecteLaVirguleCommeSeparateur() {
        List<LecteurCsv.Ligne> lignes = LecteurCsv.lire("role,prenom,nom\nenseignant,Karim,Bennani");

        assertEquals(List.of("enseignant", "Karim", "Bennani"), lignes.get(1).valeurs());
    }

    @Test
    void gereLesGuillemetsEtLesSeparateursDansUnChamp() {
        List<LecteurCsv.Ligne> lignes = LecteurCsv.lire("a;b\n\"Génie; Informatique\";\"il dit \"\"oui\"\"\"");

        assertEquals(List.of("Génie; Informatique", "il dit \"oui\""), lignes.get(1).valeurs());
    }

    @Test
    void decodeUtf8AvecBom() {
        byte[] contenu = "﻿prénom".getBytes(StandardCharsets.UTF_8);

        assertEquals("prénom", LecteurCsv.decoder(contenu));
    }

    @Test
    void decodeUnFichierExcelWindows() {
        byte[] contenu = "Génie".getBytes(Charset.forName("windows-1252"));

        assertEquals("Génie", LecteurCsv.decoder(contenu));
    }
}
