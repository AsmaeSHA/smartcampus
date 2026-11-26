package ma.ensakhouribga.smartcampus.signalement;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import ma.ensakhouribga.smartcampus.espace.Espace;
import ma.ensakhouribga.smartcampus.user.User;

/** Problème signalé dans un espace par un étudiant ou un enseignant, traité par l'administrateur. */
@Entity
@Table(name = "signalements")
public class Signalement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "espace_id", nullable = false)
    private Espace espace;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "signale_par", nullable = false)
    private User signalePar;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CategorieSignalement categorie;

    @Column(nullable = false, length = 150)
    private String titre;

    @Column(nullable = false, length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatutSignalement statut;

    /** Administrateur qui traite le signalement (vide tant qu'il n'est pas pris en charge). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "traite_par")
    private User traitePar;

    @Column(name = "cree_le", nullable = false, updatable = false)
    private LocalDateTime creeLe;

    @Column(name = "modifie_le", nullable = false)
    private LocalDateTime modifieLe;

    @Column(name = "ferme_le")
    private LocalDateTime fermeLe;

    protected Signalement() {
        // requis par JPA
    }

    public Signalement(Espace espace, User signalePar, CategorieSignalement categorie,
                       String titre, String description) {
        this.espace = espace;
        this.signalePar = signalePar;
        this.categorie = categorie;
        this.titre = titre;
        this.description = description;
        this.statut = StatutSignalement.OUVERT;
    }

    @PrePersist
    void avantCreation() {
        LocalDateTime maintenant = LocalDateTime.now();
        if (creeLe == null) {
            creeLe = maintenant;
        }
        modifieLe = maintenant;
    }

    @PreUpdate
    void avantModification() {
        modifieLe = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Espace getEspace() {
        return espace;
    }

    public User getSignalePar() {
        return signalePar;
    }

    public CategorieSignalement getCategorie() {
        return categorie;
    }

    public String getTitre() {
        return titre;
    }

    public String getDescription() {
        return description;
    }

    public StatutSignalement getStatut() {
        return statut;
    }

    public User getTraitePar() {
        return traitePar;
    }

    public LocalDateTime getCreeLe() {
        return creeLe;
    }

    public LocalDateTime getModifieLe() {
        return modifieLe;
    }

    public LocalDateTime getFermeLe() {
        return fermeLe;
    }
}
