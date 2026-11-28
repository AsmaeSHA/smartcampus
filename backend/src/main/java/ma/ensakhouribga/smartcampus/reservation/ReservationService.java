package ma.ensakhouribga.smartcampus.reservation;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.DayOfWeek;
import java.util.List;
import ma.ensakhouribga.smartcampus.espace.Espace;
import ma.ensakhouribga.smartcampus.espace.EspaceIntrouvableException;
import ma.ensakhouribga.smartcampus.espace.EspaceRepository;
import ma.ensakhouribga.smartcampus.espace.StatutEspace;
import ma.ensakhouribga.smartcampus.evenement.ReservationAnnuleeEvent;
import ma.ensakhouribga.smartcampus.evenement.ReservationCreeeEvent;
import ma.ensakhouribga.smartcampus.user.Role;
import ma.ensakhouribga.smartcampus.user.User;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReservationService {
    private static final LocalTime OUVERTURE = LocalTime.of(8, 0);
    private static final LocalTime FERMETURE = LocalTime.of(20, 0);
    private static final List<StatutReservation> STATUTS_OCCUPANTS =
            List.of(StatutReservation.EN_ATTENTE, StatutReservation.CONFIRMEE);

    private final ReservationRepository reservations;
    private final EspaceRepository espaces;
    private final ApplicationEventPublisher evenements;

    public ReservationService(ReservationRepository reservations, EspaceRepository espaces,
                              ApplicationEventPublisher evenements) {
        this.reservations = reservations;
        this.espaces = espaces;
        this.evenements = evenements;
    }

    /** SC-21/192 : demande d'un étudiant ou enseignant, conservée à l'état EN_ATTENTE. */
    @Transactional
    public ReservationResume creer(CreerReservationRequest demande, User demandeur) {
        if (demandeur.getRole() != Role.ETUDIANT && demandeur.getRole() != Role.ENSEIGNANT) {
            throw new ReservationInvalideException("Seuls les étudiants et les enseignants peuvent réserver un espace");
        }
        verifierCreneau(demande.debut(), demande.fin());
        String justification = demande.justification().trim();
        if (justification.isEmpty()) throw new ReservationInvalideException("La justification est obligatoire");

        // Le verrou sérialise les demandes concurrentes qui visent le même espace.
        Espace espace = espaces.verrouillerParId(demande.espaceId())
                .orElseThrow(() -> new EspaceIntrouvableException("Cet espace n'existe pas"));
        if (espace.getStatut() != StatutEspace.ACTIVE) {
            throw new ReservationInvalideException("Cet espace n'est pas disponible à la réservation");
        }
        if (reservations.existeChevauchement(espace.getId(), STATUTS_OCCUPANTS, demande.debut(), demande.fin())) {
            throw new ReservationInvalideException("Cet espace est déjà réservé sur ce créneau");
        }

        Reservation reservation = reservations.saveAndFlush(new Reservation(espace, demandeur,
                demande.debut(), demande.fin(), justification));
        evenements.publishEvent(new ReservationCreeeEvent(reservation.getId(), espace.getNom(), reservation.getDebut()));
        return ReservationResume.depuis(reservation);
    }

    @Transactional(readOnly = true)
    public List<ReservationResume> mesReservations(User demandeur) {
        return reservations.findByDemandeurIdOrderByCreeLeDesc(demandeur.getId()).stream()
                .map(ReservationResume::depuis).toList();
    }

    /** SC-24/119/121/123 : raison obligatoire et annulation au moins 24 heures avant le début. */
    @Transactional
    public ReservationResume annuler(Long reservationId, String raison, User demandeur) {
        Reservation reservation = reservations.findByIdAndDemandeurId(reservationId, demandeur.getId())
                .orElseThrow(ReservationIntrouvableException::new);
        if (reservation.getStatut() != StatutReservation.EN_ATTENTE
                && reservation.getStatut() != StatutReservation.CONFIRMEE) {
            throw new ReservationInvalideException("Cette réservation ne peut plus être annulée");
        }
        if (LocalDateTime.now().isAfter(reservation.getDebut().minusHours(24))) {
            throw new ReservationInvalideException("L'annulation doit être effectuée au moins 24 heures avant le début");
        }
        String motif = raison == null ? "" : raison.trim();
        if (motif.isEmpty()) throw new ReservationInvalideException("La raison de l'annulation est obligatoire");

        reservation.annuler(motif);
        Reservation enregistree = reservations.save(reservation);
        evenements.publishEvent(new ReservationAnnuleeEvent(enregistree.getId(), demandeur.getId(),
                enregistree.getEspace().getNom(), enregistree.getDebut(), false));
        return ReservationResume.depuis(enregistree);
    }

    private void verifierCreneau(LocalDateTime debut, LocalDateTime fin) {
        if (debut.isBefore(LocalDateTime.now().plusHours(24))) {
            throw new ReservationInvalideException("La réservation doit être demandée au moins 24 heures à l'avance");
        }
        if (!fin.isAfter(debut)) throw new ReservationInvalideException("La fin doit être postérieure au début");
        if (!debut.toLocalDate().equals(fin.toLocalDate())) {
            throw new ReservationInvalideException("Une réservation doit commencer et se terminer le même jour");
        }
        if (debut.getDayOfWeek() == DayOfWeek.SUNDAY) {
            throw new ReservationInvalideException("Les réservations ne sont pas autorisées le dimanche");
        }
        if (debut.toLocalTime().isBefore(OUVERTURE) || fin.toLocalTime().isAfter(FERMETURE)) {
            throw new ReservationInvalideException("Les réservations sont possibles entre 08:00 et 20:00");
        }
    }
}
