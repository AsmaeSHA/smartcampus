package ma.ensakhouribga.smartcampus.reservation;

import java.time.LocalDateTime;

public record ReservationResume(Long id, Long espaceId, String espaceCode, String espaceNom,
                                LocalDateTime debut, LocalDateTime fin, String justification,
                                StatutReservation statut, String raisonAnnulation,
                                LocalDateTime creeLe) {
    static ReservationResume depuis(Reservation reservation) {
        return new ReservationResume(reservation.getId(), reservation.getEspace().getId(),
                reservation.getEspace().getCode(), reservation.getEspace().getNom(),
                reservation.getDebut(), reservation.getFin(), reservation.getJustification(),
                reservation.getStatut(), reservation.getRaisonAnnulation(), reservation.getCreeLe());
    }
}
