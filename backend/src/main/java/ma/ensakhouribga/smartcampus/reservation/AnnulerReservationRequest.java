package ma.ensakhouribga.smartcampus.reservation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AnnulerReservationRequest(
        @NotBlank(message = "La raison de l'annulation est obligatoire")
        @Size(max = 500, message = "La raison ne peut pas dépasser 500 caractères") String raison
) {}
