package ma.ensakhouribga.smartcampus.signalement;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Données envoyées par l'étudiant ou l'enseignant pour signaler un problème. */
public record CreerSignalementRequest(
        @NotNull(message = "Choisissez l'espace concerné") Long espaceId,
        @NotNull(message = "Choisissez une catégorie") CategorieSignalement categorie,
        @NotBlank(message = "Le titre est obligatoire")
        @Size(max = 150, message = "Le titre ne doit pas dépasser 150 caractères") String titre,
        @NotBlank(message = "La description est obligatoire")
        @Size(max = 2000, message = "La description ne doit pas dépasser 2000 caractères") String description
) {}
