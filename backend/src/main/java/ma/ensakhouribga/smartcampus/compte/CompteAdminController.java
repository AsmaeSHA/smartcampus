package ma.ensakhouribga.smartcampus.compte;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import ma.ensakhouribga.smartcampus.auth.UtilisateurCourant;
import ma.ensakhouribga.smartcampus.user.User;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Routes d'administration des comptes. Toutes les routes /api/admin/** sont
 * réservées à l'administrateur par l'AdminInterceptor (R13).
 */
@RestController
@RequestMapping("/api/admin")
public class CompteAdminController {

    private final CompteService compteService;
    private final ImportComptesService importComptesService;
    private final UtilisateurCourant utilisateurCourant;

    public CompteAdminController(CompteService compteService, ImportComptesService importComptesService,
                                 UtilisateurCourant utilisateurCourant) {
        this.compteService = compteService;
        this.importComptesService = importComptesService;
        this.utilisateurCourant = utilisateurCourant;
    }

    /** SC-27 : crée un compte et renvoie, une seule fois, son mot de passe temporaire. */
    @PostMapping("/comptes")
    public ResponseEntity<CompteCree> creer(@Valid @RequestBody CreerCompteRequest demande) {
        return ResponseEntity.status(HttpStatus.CREATED).body(compteService.creer(demande));
    }

    /**
     * SC-53 : importe des comptes depuis un fichier CSV (champ de formulaire « fichier »).
     * Les lignes valides sont créées, les autres sont renvoyées avec la raison du refus.
     */
    @PostMapping("/comptes/import")
    public ImportResultat importer(@RequestParam("fichier") MultipartFile fichier) throws java.io.IOException {
        String nom = fichier.getOriginalFilename();
        if (nom == null || !nom.toLowerCase(java.util.Locale.ROOT).endsWith(".csv")) {
            throw new CompteInvalideException("Le fichier doit être au format .csv");
        }
        return importComptesService.importer(fichier.getBytes());
    }

    /** SC-64 : détail d'un compte, pour pré-remplir le formulaire de modification. */
    @GetMapping("/comptes/{id}")
    public CompteDetail detail(@PathVariable Long id) {
        return compteService.detail(id);
    }

    /** SC-64 : modifie les informations et, si besoin, le rôle d'un compte. */
    @PutMapping("/comptes/{id}")
    public CompteResume modifier(@PathVariable Long id, @Valid @RequestBody CreerCompteRequest demande,
                                 HttpServletRequest request) {
        return compteService.modifier(id, demande, idAdministrateur(request));
    }

    /** SC-28 : désactive un compte, sans supprimer ses données. */
    @PatchMapping("/comptes/{id}/desactiver")
    public CompteResume desactiver(@PathVariable Long id, HttpServletRequest request) {
        return compteService.changerActivation(id, false, idAdministrateur(request));
    }

    /** SC-28 : réactive un compte désactivé. */
    @PatchMapping("/comptes/{id}/reactiver")
    public CompteResume reactiver(@PathVariable Long id, HttpServletRequest request) {
        return compteService.changerActivation(id, true, idAdministrateur(request));
    }

    /** SC-72/73/74 : réinitialise le mot de passe et exige son remplacement à la prochaine connexion. */
    @PostMapping("/comptes/{id}/reinitialiser-mot-de-passe")
    public CompteCree reinitialiserMotDePasse(@PathVariable Long id) {
        return compteService.reinitialiserMotDePasse(id);
    }

    @GetMapping("/comptes")
    public List<CompteResume> lister() {
        return compteService.lister();
    }

    @GetMapping("/niveaux")
    public List<NiveauResume> niveaux() {
        return compteService.listerNiveaux();
    }

    @ExceptionHandler(CompteInvalideException.class)
    ResponseEntity<Map<String, String>> compteInvalide(CompteInvalideException e) {
        return erreur(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(CompteIntrouvableException.class)
    ResponseEntity<Map<String, String>> compteIntrouvable(CompteIntrouvableException e) {
        return erreur(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(CompteExistantException.class)
    ResponseEntity<Map<String, String>> compteExistant(CompteExistantException e) {
        return erreur(HttpStatus.CONFLICT, e.getMessage());
    }

    /** Champ commun manquant ou mal formé (@NotBlank, @Email…) : on renvoie le premier message. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<Map<String, String>> champInvalide(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .findFirst()
                .orElse("Données invalides");
        return erreur(HttpStatus.BAD_REQUEST, message);
    }

    /** JSON illisible ou rôle inconnu. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<Map<String, String>> requeteIllisible() {
        return erreur(HttpStatus.BAD_REQUEST, "Requête invalide : vérifiez les champs et le rôle");
    }

    /** L'AdminInterceptor garantit qu'un administrateur est connecté sur ces routes. */
    private Long idAdministrateur(HttpServletRequest request) {
        return utilisateurCourant.depuis(request).map(User::getId).orElseThrow();
    }

    private ResponseEntity<Map<String, String>> erreur(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of("message", message));
    }
}
