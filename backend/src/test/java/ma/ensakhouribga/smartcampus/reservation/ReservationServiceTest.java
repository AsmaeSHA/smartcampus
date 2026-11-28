package ma.ensakhouribga.smartcampus.reservation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.time.DayOfWeek;
import java.util.List;
import java.util.Optional;
import ma.ensakhouribga.smartcampus.espace.Espace;
import ma.ensakhouribga.smartcampus.espace.EspaceRepository;
import ma.ensakhouribga.smartcampus.espace.StatutEspace;
import ma.ensakhouribga.smartcampus.espace.TypeEspace;
import ma.ensakhouribga.smartcampus.evenement.ReservationCreeeEvent;
import ma.ensakhouribga.smartcampus.user.Role;
import ma.ensakhouribga.smartcampus.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

class ReservationServiceTest {
    private final ReservationRepository reservations = mock(ReservationRepository.class);
    private final EspaceRepository espaces = mock(EspaceRepository.class);
    private final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
    private final ReservationService service = new ReservationService(reservations, espaces, events);
    private final Espace espace = new Espace("S-101", "Salle 101", TypeEspace.SALLE_COURS);
    private final User etudiant = new User("etudiant@usms.ac.ma", "hash", "Sam", "Test", Role.ETUDIANT, true);

    @BeforeEach
    void preparer() {
        when(espaces.verrouillerParId(1L)).thenReturn(Optional.of(espace));
        when(reservations.existeChevauchement(any(), any(), any(), any())).thenReturn(false);
        when(reservations.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void refuseLeDimanche() {
        LocalDate dimanche = prochainDimanche(10).toLocalDate();
        LocalDateTime debut = dimanche.atTime(10, 0);
        assertThrows(ReservationInvalideException.class,
                () -> service.creer(demande(debut, debut.plusHours(1), "Cours de soutien"), etudiant));
        verify(reservations, never()).saveAndFlush(any());
        verify(events, never()).publishEvent(any(ReservationCreeeEvent.class));
    }

    @Test
    void refuseUneJustificationVide() {
        LocalDateTime debut = prochainJourOuvre(10);
        assertThrows(ReservationInvalideException.class,
                () -> service.creer(demande(debut, debut.plusHours(1), "  "), etudiant));
        verify(reservations, never()).saveAndFlush(any());
    }

    @Test
    void refuseUnDelaiInferieurA24Heures() {
        LocalDateTime debut = LocalDateTime.now().plusHours(23);
        assertThrows(ReservationInvalideException.class,
                () -> service.creer(demande(debut, debut.plusHours(1), "Motif"), etudiant));
    }

    @Test
    void refuseDesHorairesHorsOuverture() {
        LocalDate lundi = prochainJourOuvre(7).toLocalDate();
        LocalDateTime debut = lundi.atTime(7, 0);
        assertThrows(ReservationInvalideException.class,
                () -> service.creer(demande(debut, lundi.atTime(8, 0), "Motif"), etudiant));
    }

    @Test
    void refuseUneFinAvantLeDebutEtUnChangementDeJour() {
        LocalDateTime debut = prochainJourOuvre(10);
        assertThrows(ReservationInvalideException.class,
                () -> service.creer(demande(debut, debut, "Motif"), etudiant));
        assertThrows(ReservationInvalideException.class,
                () -> service.creer(demande(debut, debut.plusDays(1), "Motif"), etudiant));
    }

    @Test
    void refuseUnEspaceInactifEtUnCreneauDejaPris() {
        Espace archive = new Espace("OLD", "Archive", TypeEspace.SALLE_COURS);
        archive.archiver();
        when(espaces.verrouillerParId(1L)).thenReturn(Optional.of(archive));
        LocalDateTime debut = prochainJourOuvre(10);
        assertThrows(ReservationInvalideException.class,
                () -> service.creer(demande(debut, debut.plusHours(1), "Motif"), etudiant));

        when(espaces.verrouillerParId(1L)).thenReturn(Optional.of(espace));
        when(reservations.existeChevauchement(any(), any(), any(), any())).thenReturn(true);
        assertThrows(ReservationInvalideException.class,
                () -> service.creer(demande(debut, debut.plusHours(1), "Motif"), etudiant));
    }

    @Test
    void annuleAvecRaisonAvantLaLimiteEtRefuseApresLaLimite() {
        LocalDateTime dateFuture = LocalDateTime.now().plusHours(48).withMinute(0).withSecond(0).withNano(0);
        Reservation reservation = new Reservation(espace, etudiant, dateFuture, dateFuture.plusHours(1), "Motif");
        when(reservations.findByIdAndDemandeurId(7L, etudiant.getId())).thenReturn(Optional.of(reservation));
        when(reservations.save(reservation)).thenReturn(reservation);

        ReservationResume annulee = service.annuler(7L, "Changement de planning", etudiant);
        assertEquals(StatutReservation.ANNULEE, annulee.statut());
        assertEquals("Changement de planning", annulee.raisonAnnulation());

        LocalDateTime proche = LocalDateTime.now().plusHours(23);
        Reservation tropProche = new Reservation(espace, etudiant, proche, proche.plusHours(1), "Motif");
        when(reservations.findByIdAndDemandeurId(8L, etudiant.getId())).thenReturn(Optional.of(tropProche));
        assertThrows(ReservationInvalideException.class, () -> service.annuler(8L, "Empêchement", etudiant));
    }

    @Test
    void refuseUneAnnulationSansRaison() {
        LocalDateTime debut = LocalDateTime.now().plusHours(48);
        Reservation reservation = new Reservation(espace, etudiant, debut, debut.plusHours(1), "Motif");
        when(reservations.findByIdAndDemandeurId(9L, etudiant.getId())).thenReturn(Optional.of(reservation));
        assertThrows(ReservationInvalideException.class, () -> service.annuler(9L, " ", etudiant));
        assertTrue(reservation.getRaisonAnnulation() == null);
    }

    private CreerReservationRequest demande(LocalDateTime debut, LocalDateTime fin, String justification) {
        return new CreerReservationRequest(1L, debut, fin, justification);
    }

    private LocalDateTime prochainDimanche(int heure) {
        LocalDate dimanche = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.SUNDAY)).plusWeeks(1);
        return dimanche.atTime(LocalTime.of(heure, 0));
    }

    private LocalDateTime prochainJourOuvre(int heure) {
        LocalDate lundi = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY)).plusWeeks(1);
        return lundi.atTime(LocalTime.of(heure, 0));
    }
}
