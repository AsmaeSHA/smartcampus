package ma.ensakhouribga.smartcampus.evenement;

import java.time.LocalDateTime;

/** Publié par Asmae (annulation admin) et Chaymae (blocage) quand une réservation est annulée. */
public record ReservationAnnuleeEvent(
        Long reservationId,
        Long demandeurId,
        String espaceNom,
        LocalDateTime debut,
        boolean parAdministrateur
) {}