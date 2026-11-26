package ma.ensakhouribga.smartcampus.signalement;

import java.util.List;
import ma.ensakhouribga.smartcampus.espace.Espace;
import ma.ensakhouribga.smartcampus.espace.EspaceIntrouvableException;
import ma.ensakhouribga.smartcampus.espace.EspaceRepository;
import ma.ensakhouribga.smartcampus.espace.StatutEspace;
import ma.ensakhouribga.smartcampus.evenement.SignalementCreeEvent;
import ma.ensakhouribga.smartcampus.evenement.SignalementStatutChangeEvent;
import ma.ensakhouribga.smartcampus.user.Role;
import ma.ensakhouribga.smartcampus.user.User;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SignalementService {

    private final SignalementRepository signalements;
    private final CommentaireSignalementRepository commentaires;
    private final EspaceRepository espaces;
    private final ApplicationEventPublisher evenements;

    public SignalementService(SignalementRepository signalements, CommentaireSignalementRepository commentaires,
                              EspaceRepository espaces, ApplicationEventPublisher evenements) {
        this.signalements = signalements;
        this.commentaires = commentaires;
        this.espaces = espaces;
        this.evenements = evenements;
    }

    /**
     * Un étudiant ou un enseignant signale un problème dans un espace (module D).
     * Le signalement commence au statut OUVERT, et l'administrateur sera notifié (sprint 4).
     */
    @Transactional
    public SignalementResume signaler(CreerSignalementRequest demande, User auteur) {
        if (auteur.getRole() != Role.ETUDIANT && auteur.getRole() != Role.ENSEIGNANT) {
            throw new SignalementInterditException("Seuls les étudiants et les enseignants peuvent signaler un problème");
        }
        Espace espace = espaces.findById(demande.espaceId())
                .orElseThrow(() -> new EspaceIntrouvableException("Cet espace n'existe pas"));
        if (espace.getStatut() == StatutEspace.ARCHIVE) {
            throw new EspaceIntrouvableException("Cet espace est archivé");
        }

        Signalement signalement = signalements.save(new Signalement(espace, auteur, demande.categorie(),
                demande.titre().trim(), demande.description().trim()));
        evenements.publishEvent(new SignalementCreeEvent(signalement.getId(), espace.getNom()));
        return SignalementResume.depuis(signalement);
    }

    /** Les signalements de l'utilisateur connecté, les plus récents en premier. */
    @Transactional(readOnly = true)
    public List<SignalementResume> mesSignalements(User utilisateur) {
        return signalements.findBySignaleParIdOrderByCreeLeDesc(utilisateur.getId()).stream()
                .map(SignalementResume::depuis)
                .toList();
    }

    /** Tous les signalements pour l'administrateur, avec des filtres facultatifs et combinables. */
    @Transactional(readOnly = true)
    public List<SignalementResume> rechercher(StatutSignalement statut, CategorieSignalement categorie, Long espaceId) {
        return signalements.rechercher(statut, categorie, espaceId).stream()
                .map(SignalementResume::depuis)
                .toList();
    }

    /** Détail et fil de suivi : visible par l'administrateur et par l'auteur du signalement seulement. */
    @Transactional(readOnly = true)
    public SignalementDetail detail(Long id, User utilisateur) {
        Signalement signalement = trouver(id);
        if (!utilisateur.estAdministrateur() && !signalement.estAuteur(utilisateur.getId())) {
            throw new SignalementInterditException("Ce signalement ne vous appartient pas");
        }
        return detailDe(signalement);
    }

    /** L'administrateur change le statut ; l'auteur sera notifié au sprint 4 (EN_COURS ou RESOLU). */
    @Transactional
    public SignalementDetail changerStatut(Long id, StatutSignalement nouveau, User administrateur) {
        Signalement signalement = trouver(id);
        signalement.changerStatut(nouveau, administrateur);
        signalements.saveAndFlush(signalement);
        evenements.publishEvent(new SignalementStatutChangeEvent(signalement.getId(),
                signalement.getSignalePar().getId(), signalement.getEspace().getNom(), nouveau.name()));
        return detailDe(signalement);
    }

    /** L'administrateur ajoute un commentaire de suivi, sauf sur un signalement fermé. */
    @Transactional
    public SignalementDetail commenter(Long id, String contenu, User administrateur) {
        Signalement signalement = trouver(id);
        if (signalement.estFerme()) {
            throw new TransitionSignalementException("Ce signalement est fermé : on ne peut plus le commenter");
        }
        commentaires.save(new CommentaireSignalement(signalement, administrateur, contenu.trim()));
        return detailDe(signalement);
    }

    private Signalement trouver(Long id) {
        return signalements.findById(id).orElseThrow(SignalementIntrouvableException::new);
    }

    private SignalementDetail detailDe(Signalement signalement) {
        User traitePar = signalement.getTraitePar();
        return new SignalementDetail(
                SignalementResume.depuis(signalement),
                traitePar == null ? null : traitePar.getPrenom() + " " + traitePar.getNom(),
                signalement.statutsPossibles(),
                commentaires.findBySignalementIdOrderByCreeLeAsc(signalement.getId()).stream()
                        .map(CommentaireResume::depuis)
                        .toList());
    }
}
