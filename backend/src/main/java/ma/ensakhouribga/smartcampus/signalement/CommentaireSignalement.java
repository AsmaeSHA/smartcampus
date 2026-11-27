package ma.ensakhouribga.smartcampus.signalement;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import ma.ensakhouribga.smartcampus.user.User;

/** Commentaire de suivi d'un signalement. Il n'existe pas sans son signalement (composition). */
@Entity
@Table(name = "signalement_commentaires")
public class CommentaireSignalement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "signalement_id", nullable = false)
    private Signalement signalement;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "auteur_id", nullable = false)
    private User auteur;

    @Column(nullable = false, length = 2000)
    private String contenu;

    @Column(name = "cree_le", nullable = false, updatable = false)
    private LocalDateTime creeLe;

    protected CommentaireSignalement() {
        // requis par JPA
    }

    public CommentaireSignalement(Signalement signalement, User auteur, String contenu) {
        this.signalement = signalement;
        this.auteur = auteur;
        this.contenu = contenu;
    }

    @PrePersist
    void avantCreation() {
        if (creeLe == null) {
            creeLe = LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public Signalement getSignalement() {
        return signalement;
    }

    public User getAuteur() {
        return auteur;
    }

    public String getContenu() {
        return contenu;
    }

    public LocalDateTime getCreeLe() {
        return creeLe;
    }
}
