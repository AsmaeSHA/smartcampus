package ma.ensakhouribga.smartcampus.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/**
 * Utilisateur de SmartCampus.
 * Un administrateur est un simple User ; un étudiant et un enseignant ont leurs propres
 * informations dans les classes {@link Etudiant} et {@link Enseignant} (une table par classe).
 * Le mot de passe n'est jamais stocké en clair : seul son hash BCrypt est conservé.
 */
@Entity
@Table(name = "users")
@Inheritance(strategy = InheritanceType.JOINED)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    private String prenom;

    @Column(nullable = false)
    private String nom;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    @Column(nullable = false)
    private boolean active;

    /** Vrai tant que l'utilisateur doit remplacer un mot de passe donné par l'administrateur. */
    @Column(name = "changer_mot_de_passe", nullable = false)
    private boolean changerMotDePasse;

    @Column(name = "cree_le", nullable = false, updatable = false)
    private LocalDateTime creeLe;

    protected User() {
        // requis par JPA
    }

    public User(String email, String passwordHash, String prenom, String nom, Role role, boolean active) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.prenom = prenom;
        this.nom = nom;
        this.role = role;
        this.active = active;
    }

    @PrePersist
    void avantCreation() {
        if (creeLe == null) {
            creeLe = LocalDateTime.now();
        }
    }

    /** Modifie les informations communes du compte (SC-64). */
    public void modifierInformations(String email, String prenom, String nom) {
        this.email = email;
        this.prenom = prenom;
        this.nom = nom;
    }

    /** Désactive le compte (SC-28) : il ne peut plus se connecter, ses données sont conservées. */
    public void desactiver() {
        this.active = false;
    }

    /** Réactive un compte désactivé (SC-28). */
    public void reactiver() {
        this.active = true;
    }

    /** Remplace le hash du mot de passe sans exposer de secret en clair à l'entité. */
    public void changerMotDePasse(String passwordHash) {
        this.passwordHash = passwordHash;
        this.changerMotDePasse = false;
    }

    /** Définit un hash provisoire et impose son remplacement à la prochaine connexion. */
    public void reinitialiserMotDePasse(String passwordHash) {
        this.passwordHash = passwordHash;
        this.changerMotDePasse = true;
    }

    public boolean estAdministrateur() {
        return role == Role.ADMINISTRATEUR;
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getPrenom() {
        return prenom;
    }

    public String getNom() {
        return nom;
    }

    public Role getRole() {
        return role;
    }

    public boolean isActive() {
        return active;
    }

    public boolean isChangerMotDePasse() {
        return changerMotDePasse;
    }

    public void setChangerMotDePasse(boolean changerMotDePasse) {
        this.changerMotDePasse = changerMotDePasse;
    }

    public LocalDateTime getCreeLe() {
        return creeLe;
    }
}
