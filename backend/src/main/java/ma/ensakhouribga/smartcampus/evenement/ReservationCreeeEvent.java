package ma.ensakhouribga.smartcampus.evenement;

import java.time.LocalDateTime;

/** Publié par Ikram (SC-21) quand une demande de réservation est créée. */
public record ReservationCreeeEvent(
        Long reservationId,
        String espaceNom,
        LocalDateTime debut
) {}