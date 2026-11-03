package ma.ensakhouribga.smartcampus.espace;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;

@Entity
@Table(name = "espaces", uniqueConstraints = @UniqueConstraint(name = "uk_espaces_code", columnNames = "code"))
public class Espace {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String code;

    @Column(nullable = false)
    private String nom;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_espace", nullable = false, length = 30)
    private TypeEspace typeEspace;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatutEspace statut;

    @Column(name = "cree_le", nullable = false, updatable = false)
    private LocalDateTime creeLe;

    @Column(name = "modifie_le", nullable = false)
    private LocalDateTime modifieLe;

    protected Espace() {
    }

    public Espace(String code, String nom, TypeEspace typeEspace) {
        this.code = code;
        this.nom = nom;
        this.typeEspace = typeEspace;
        this.statut = StatutEspace.ACTIVE;
    }

    public void modifier(String code, String nom, TypeEspace typeEspace) {
        this.code = code;
        this.nom = nom;
        this.typeEspace = typeEspace;
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

    public String getCode() {
        return code;
    }

    public String getNom() {
        return nom;
    }

    public TypeEspace getTypeEspace() {
        return typeEspace;
    }

    public StatutEspace getStatut() {
        return statut;
    }

    public LocalDateTime getCreeLe() {
        return creeLe;
    }

    public LocalDateTime getModifieLe() {
        return modifieLe;
    }
}
