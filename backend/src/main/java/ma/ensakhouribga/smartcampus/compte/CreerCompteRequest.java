package ma.ensakhouribga.smartcampus.compte;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import ma.ensakhouribga.smartcampus.user.Role;

/**
 * Corps de POST /api/admin/comptes (SC-27) et de PUT /api/admin/comptes/{id} (SC-64).
 * Les champs communs sont vérifiés ici ; les champs propres au rôle
 * (étudiant ou enseignant) sont vérifiés par {@link CompteService}.
 */
public record CreerCompteRequest(
        @NotBlank(message = "Le prénom est obligatoire") String prenom,
        @NotBlank(message = "Le nom est obligatoire") String nom,
        @NotBlank(message = "L'e-mail est obligatoire")
        @Email(message = "L'e-mail n'est pas valide") String email,
        @NotNull(message = "Le rôle est obligatoire") Role role,
        // Étudiant
        String codeMassar,
        Long niveauId,
        String filiere,
        // Enseignant
        String codeEnseignant,
        String grade,
        String specialite,
        // Étudiant et enseignant
        String numero) {
}
