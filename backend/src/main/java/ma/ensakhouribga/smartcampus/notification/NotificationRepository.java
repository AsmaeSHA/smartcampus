package ma.ensakhouribga.smartcampus.notification;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByUtilisateurIdOrderByCreeLeDesc(Long utilisateurId);

    long countByUtilisateurIdAndLueFalse(Long utilisateurId);
    boolean existsByUtilisateurId(Long utilisateurId);
    long deleteByLueTrueAndCreeLeBefore(LocalDateTime limite);
}