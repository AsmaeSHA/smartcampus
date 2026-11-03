package ma.ensakhouribga.smartcampus.espace;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreerEspaceRequest(
        @NotBlank(message = "Le code est obligatoire") String code,
        @NotBlank(message = "Le nom est obligatoire") String nom,
        @NotNull(message = "Le type est obligatoire") TypeEspace typeEspace) {
}
