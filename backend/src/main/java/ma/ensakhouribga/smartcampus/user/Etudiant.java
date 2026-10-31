package ma.ensakhouribga.smartcampus.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** Compte étudiant : informations communes de {@link User} plus les champs propres à l'étudiant. */
@Entity
@Table(name = "etudiants")
public class Etudiant extends User {

    @Column(name = "code_massar", nullable = false, unique = true)
    private String codeMassar;

    @Column(nullable = false)
    private String numero;

    @Column(nullable = false)
    private String filiere;

    @ManyToOne(optional = false)
    @JoinColumn(name = "niveau_id", nullable = false)
    private Niveau niveau;

    protected Etudiant() {
        // requis par JPA
    }

    public Etudiant(String email, String passwordHash, String prenom, String nom, boolean active,
                    String codeMassar, String numero, String filiere, Niveau niveau) {
        super(email, passwordHash, prenom, nom, Role.ETUDIANT, active);
        this.codeMassar = codeMassar;
        this.numero = numero;
        this.filiere = filiere;
        this.niveau = niveau;
    }

    public String getCodeMassar() {
        return codeMassar;
    }

    public String getNumero() {
        return numero;
    }

    public String getFiliere() {
        return filiere;
    }

    public Niveau getNiveau() {
        return niveau;
    }
}
