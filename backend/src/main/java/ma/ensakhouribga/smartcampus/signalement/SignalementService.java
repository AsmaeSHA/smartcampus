package ma.ensakhouribga.smartcampus.signalement;

import ma.ensakhouribga.smartcampus.espace.Espace;
import ma.ensakhouribga.smartcampus.espace.EspaceIntrouvableException;
import ma.ensakhouribga.smartcampus.espace.EspaceRepository;
import ma.ensakhouribga.smartcampus.evenement.SignalementCreeEvent;
import ma.ensakhouribga.smartcampus.user.Role;
import ma.ensakhouribga.smartcampus.user.User;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SignalementService {

    private final SignalementRepository signalements;
    private final EspaceRepository espaces;
    private final ApplicationEventPublisher evenements;

    public SignalementService(SignalementRepository signalements, EspaceRepository espaces,
                              ApplicationEventPublisher evenements) {
        this.signalements = signalements;
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
        // À ajouter quand SC-18 sera mergé : refuser un espace archivé.

        Signalement signalement = signalements.save(new Signalement(espace, auteur, demande.categorie(),
                demande.titre().trim(), demande.description().trim()));
        evenements.publishEvent(new SignalementCreeEvent(signalement.getId(), espace.getNom()));
        return SignalementResume.depuis(signalement);
    }
}
