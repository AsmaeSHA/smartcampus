package ma.ensakhouribga.smartcampus.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** Compte enseignant : informations communes de {@link User} plus les champs propres à l'enseignant. */
@Entity
@Table(name = "enseignants")
public class Enseignant extends User {

    @Column(name = "code_enseignant", nullable = false, unique = true)
    private String codeEnseignant;

    @Column(nullable = false)
    private String numero;

    @Column(nullable = false)
    private String grade;

    @Column(nullable = false)
    private String specialite;

    protected Enseignant() {
        // requis par JPA
    }

    public Enseignant(String email, String passwordHash, String prenom, String nom, boolean active,
                      String codeEnseignant, String numero, String grade, String specialite) {
        super(email, passwordHash, prenom, nom, Role.ENSEIGNANT, active);
        this.codeEnseignant = codeEnseignant;
        this.numero = numero;
        this.grade = grade;
        this.specialite = specialite;
    }

    /** Modifie les informations propres à l'enseignant (SC-64). */
    public void modifier(String codeEnseignant, String numero, String grade, String specialite) {
        this.codeEnseignant = codeEnseignant;
        this.numero = numero;
        this.grade = grade;
        this.specialite = specialite;
    }

    public String getCodeEnseignant() {
        return codeEnseignant;
    }

    public String getNumero() {
        return numero;
    }

    public String getGrade() {
        return grade;
    }

    public String getSpecialite() {
        return specialite;
    }
}
