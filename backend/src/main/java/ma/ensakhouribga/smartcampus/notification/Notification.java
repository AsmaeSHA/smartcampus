package ma.ensakhouribga.smartcampus.notification;

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
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import ma.ensakhouribga.smartcampus.user.User;

/** Notification envoyée à un utilisateur (module NOTIF). */
@Entity
@Table(name = "notifications")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "utilisateur_id", nullable = false)
    private User utilisateur;

    @Column(nullable = false, length = 500)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_objet", nullable = false, length = 20)
    private TypeObjet typeObjet;

    @Column(name = "objet_id")
    private Long objetId;

    @Column(nullable = false)
    private boolean lue;

    @Column(name = "cree_le", nullable = false, updatable = false)
    private LocalDateTime creeLe;

    protected Notification() {
        // requis par JPA
    }

    public Notification(User utilisateur, String message, TypeObjet typeObjet, Long objetId) {
        this.utilisateur = utilisateur;
        this.message = message;
        this.typeObjet = typeObjet;
        this.objetId = objetId;
        this.lue = false;
    }

    @PrePersist
    void avantCreation() {
        if (creeLe == null) {
            creeLe = LocalDateTime.now();
        }
    }

    public void marquerLue() {
        this.lue = true;
    }

    /** Vrai si la notification appartient à cet utilisateur. */
    public boolean estDestinataire(Long utilisateurId) {
        return utilisateur.getId().equals(utilisateurId);
    }

    public Long getId() { return id; }
    public User getUtilisateur() { return utilisateur; }
    public String getMessage() { return message; }
    public TypeObjet getTypeObjet() { return typeObjet; }
    public Long getObjetId() { return objetId; }
    public boolean isLue() { return lue; }
    public LocalDateTime getCreeLe() { return creeLe; }
}