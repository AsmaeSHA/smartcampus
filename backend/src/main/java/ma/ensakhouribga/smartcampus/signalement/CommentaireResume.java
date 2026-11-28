package ma.ensakhouribga.smartcampus.signalement;

import java.time.LocalDateTime;

public record CommentaireResume(Long id, String auteur, String contenu, LocalDateTime creeLe) {

    public static CommentaireResume depuis(CommentaireSignalement commentaire) {
        return new CommentaireResume(
                commentaire.getId(),
                commentaire.getAuteur().getPrenom() + " " + commentaire.getAuteur().getNom(),
                commentaire.getContenu(),
                commentaire.getCreeLe());
    }
}
