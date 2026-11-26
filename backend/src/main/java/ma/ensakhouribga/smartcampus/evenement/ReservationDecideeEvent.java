package ma.ensakhouribga.smartcampus.evenement;

import java.time.LocalDateTime;

/** Publié par Asmae (SC-22, SC-23) quand une demande est validée ou refusée. statut : "CONFIRMEE" ou "REFUSEE". */
public record ReservationDecideeEvent(
        Long reservationId,
        Long demandeurId,
        String espaceNom,
        LocalDateTime debut,
        String statut
) {}