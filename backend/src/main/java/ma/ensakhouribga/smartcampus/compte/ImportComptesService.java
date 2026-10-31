package ma.ensakhouribga.smartcampus.compte;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import ma.ensakhouribga.smartcampus.compte.ImportResultat.CompteImporte;
import ma.ensakhouribga.smartcampus.compte.ImportResultat.LigneRefusee;
import ma.ensakhouribga.smartcampus.user.Niveau;
import ma.ensakhouribga.smartcampus.user.NiveauRepository;
import ma.ensakhouribga.smartcampus.user.Role;
import org.springframework.stereotype.Service;

/**
 * Import de comptes depuis un fichier CSV (SC-53).
 *
 * Chaque ligne est créée avec les mêmes règles que la création un par un (SC-27), dans sa propre
 * transaction : une ligne invalide est signalée sans empêcher la création des autres.
 * Ce service n'est volontairement pas @Transactional.
 *
 * Colonnes attendues (l'ordre est libre, les majuscules et accents sont ignorés) :
 * role ; prenom ; nom ; email ; numero ; codeMassar ; niveau ; filiere ; codeEnseignant ; grade ; specialite
 */
@Service
public class ImportComptesService {

    static final int MAX_LIGNES = 500;
    static final List<String> COLONNES_OBLIGATOIRES = List.of("role", "prenom", "nom", "email");

    private final CompteService compteService;
    private final NiveauRepository niveaux;

    public ImportComptesService(CompteService compteService, NiveauRepository niveaux) {
        this.compteService = compteService;
        this.niveaux = niveaux;
    }

    public ImportResultat importer(byte[] contenu) {
        if (contenu == null || contenu.length == 0) {
            throw new CompteInvalideException("Le fichier est vide");
        }
        List<LecteurCsv.Ligne> lignes = LecteurCsv.lire(LecteurCsv.decoder(contenu));
        if (lignes.size() < 2) {
            throw new CompteInvalideException("Le fichier ne contient aucun compte après la ligne d'en-tête");
        }
        if (lignes.size() - 1 > MAX_LIGNES) {
            throw new CompteInvalideException("Le fichier dépasse " + MAX_LIGNES + " comptes : découpez-le");
        }

        Map<String, Integer> colonnes = lireEntete(lignes.get(0).valeurs());
        List<CompteImporte> crees = new ArrayList<>();
        List<LigneRefusee> erreurs = new ArrayList<>();

        for (LecteurCsv.Ligne ligne : lignes.subList(1, lignes.size())) {
            String email = valeur(ligne, colonnes, "email");
            try {
                CreerCompteRequest demande = versDemande(ligne, colonnes);
                CompteCree cree = compteService.creer(demande);
                CompteResume c = cree.compte();
                crees.add(new CompteImporte(ligne.numero(), c.email(), c.prenom(), c.nom(), c.role(),
                        cree.motDePasseTemporaire()));
            } catch (CompteInvalideException | CompteExistantException e) {
                erreurs.add(new LigneRefusee(ligne.numero(), email, e.getMessage()));
            }
        }
        return new ImportResultat(lignes.size() - 1, crees, erreurs);
    }

    /** Associe chaque nom de colonne normalisé à sa position. */
    private Map<String, Integer> lireEntete(List<String> entete) {
        Map<String, Integer> colonnes = new HashMap<>();
        for (int i = 0; i < entete.size(); i++) {
            colonnes.put(normaliser(entete.get(i)), i);
        }
        List<String> manquantes = COLONNES_OBLIGATOIRES.stream()
                .filter(c -> !colonnes.containsKey(normaliser(c)))
                .toList();
        if (!manquantes.isEmpty()) {
            throw new CompteInvalideException("Colonnes manquantes dans l'en-tête : " + String.join(", ", manquantes));
        }
        return colonnes;
    }

    private CreerCompteRequest versDemande(LecteurCsv.Ligne ligne, Map<String, Integer> colonnes) {
        String prenom = valeur(ligne, colonnes, "prenom");
        String nom = valeur(ligne, colonnes, "nom");
        String email = valeur(ligne, colonnes, "email");
        if (prenom == null || nom == null || email == null) {
            throw new CompteInvalideException("Le prénom, le nom et l'e-mail sont obligatoires");
        }
        if (email.indexOf('@') <= 0) {
            throw new CompteInvalideException("L'e-mail n'est pas valide");
        }
        Role role = lireRole(valeur(ligne, colonnes, "role"));

        Long niveauId = null;
        String niveau = valeur(ligne, colonnes, "niveau");
        if (role == Role.ETUDIANT && niveau != null) {
            niveauId = niveaux.findByNom(niveau.toUpperCase(Locale.ROOT))
                    .map(Niveau::getId)
                    .orElseThrow(() -> new CompteInvalideException("Niveau inconnu : " + niveau));
        }

        return new CreerCompteRequest(prenom, nom, email, role,
                valeur(ligne, colonnes, "codeMassar"), niveauId, valeur(ligne, colonnes, "filiere"),
                valeur(ligne, colonnes, "codeEnseignant"), valeur(ligne, colonnes, "grade"),
                valeur(ligne, colonnes, "specialite"), valeur(ligne, colonnes, "numero"));
    }

    private static Role lireRole(String valeur) {
        if (valeur == null) {
            throw new CompteInvalideException("Le rôle est obligatoire");
        }
        return switch (normaliser(valeur)) {
            case "etudiant" -> Role.ETUDIANT;
            case "enseignant" -> Role.ENSEIGNANT;
            case "administrateur", "admin" -> Role.ADMINISTRATEUR;
            default -> throw new CompteInvalideException(
                    "Rôle inconnu : " + valeur + " (attendu : etudiant, enseignant ou administrateur)");
        };
    }

    /** Valeur d'une colonne, ou null si la colonne est absente ou la cellule vide. */
    private static String valeur(LecteurCsv.Ligne ligne, Map<String, Integer> colonnes, String colonne) {
        Integer index = colonnes.get(normaliser(colonne));
        if (index == null || index >= ligne.valeurs().size()) {
            return null;
        }
        String v = ligne.valeurs().get(index).trim();
        return v.isEmpty() ? null : v;
    }

    /** « Code Massar », « codeMassar » et « code_massar » deviennent tous « codemassar ». */
    static String normaliser(String texte) {
        String sansAccents = Normalizer.normalize(texte, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return sansAccents.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }
}
