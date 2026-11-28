package ma.ensakhouribga.smartcampus.reservation;

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
import ma.ensakhouribga.smartcampus.espace.Espace;
import ma.ensakhouribga.smartcampus.user.User;

@Entity
@Table(name = "reservations")
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "espace_id", nullable = false)
    private Espace espace;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "demandeur_id", nullable = false)
    private User demandeur;

    @Column(name = "debut", nullable = false)
    private LocalDateTime debut;

    @Column(name = "fin", nullable = false)
    private LocalDateTime fin;

    @Column(nullable = false, length = 1000)
    private String justification;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatutReservation statut;

    @Column(name = "raison_annulation", length = 500)
    private String raisonAnnulation;

    @Column(name = "cree_le", nullable = false, updatable = false)
    private LocalDateTime creeLe;

    protected Reservation() {}

    public Reservation(Espace espace, User demandeur, LocalDateTime debut, LocalDateTime fin,
                       String justification) {
        this.espace = espace;
        this.demandeur = demandeur;
        this.debut = debut;
        this.fin = fin;
        this.justification = justification;
        this.statut = StatutReservation.EN_ATTENTE;
    }

    public void annuler(String raison) {
        this.statut = StatutReservation.ANNULEE;
        this.raisonAnnulation = raison;
    }

    @PrePersist
    void avantCreation() {
        if (creeLe == null) creeLe = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Espace getEspace() { return espace; }
    public User getDemandeur() { return demandeur; }
    public LocalDateTime getDebut() { return debut; }
    public LocalDateTime getFin() { return fin; }
    public String getJustification() { return justification; }
    public StatutReservation getStatut() { return statut; }
    public String getRaisonAnnulation() { return raisonAnnulation; }
    public LocalDateTime getCreeLe() { return creeLe; }
}
