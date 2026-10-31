package ma.ensakhouribga.smartcampus.compte;

import ma.ensakhouribga.smartcampus.user.Niveau;

/** Niveau proposé dans le formulaire de création d'un étudiant. */
public record NiveauResume(Long id, String nom) {

    static NiveauResume depuis(Niveau niveau) {
        return new NiveauResume(niveau.getId(), niveau.getNom());
    }
}
