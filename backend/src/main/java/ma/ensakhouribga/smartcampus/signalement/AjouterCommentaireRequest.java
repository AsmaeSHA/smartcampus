package ma.ensakhouribga.smartcampus.signalement;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AjouterCommentaireRequest(
        @NotBlank(message = "Le commentaire est vide")
        @Size(max = 2000, message = "Le commentaire ne doit pas dépasser 2000 caractères") String contenu
) {}
