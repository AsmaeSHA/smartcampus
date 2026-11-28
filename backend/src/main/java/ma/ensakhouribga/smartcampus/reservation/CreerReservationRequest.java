package ma.ensakhouribga.smartcampus.reservation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

public record CreerReservationRequest(
        @NotNull(message = "Choisissez un espace") Long espaceId,
        @NotNull(message = "Indiquez le début de la réservation") LocalDateTime debut,
        @NotNull(message = "Indiquez la fin de la réservation") LocalDateTime fin,
        @NotBlank(message = "La justification est obligatoire")
        @Size(max = 1000, message = "La justification ne peut pas dépasser 1000 caractères") String justification
) {}
