package ma.ensakhouribga.smartcampus.notification;

import java.util.List;
import ma.ensakhouribga.smartcampus.user.User;
import ma.ensakhouribga.smartcampus.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {

    private final NotificationRepository notifications;
    private final UserRepository users;

    public NotificationService(NotificationRepository notifications, UserRepository users) {
        this.notifications = notifications;
        this.users = users;
    }

    /** Crée une notification pour un utilisateur. Utilisé par les écouteurs du sprint 4. */
    @Transactional
    public Notification notifier(Long utilisateurId, String message, TypeObjet typeObjet, Long objetId) {
        User utilisateur = users.getReferenceById(utilisateurId);
        return notifications.save(new Notification(utilisateur, message, typeObjet, objetId));
    }

    /** Les notifications de l'utilisateur, les plus récentes en premier. */
    @Transactional(readOnly = true)
    public List<NotificationResume> lister(Long utilisateurId) {
        return notifications.findByUtilisateurIdOrderByCreeLeDesc(utilisateurId).stream()
                .map(NotificationResume::depuis)
                .toList();
    }

    @Transactional(readOnly = true)
    public long nombreNonLues(Long utilisateurId) {
        return notifications.countByUtilisateurIdAndLueFalse(utilisateurId);
    }

    /** Seul le destinataire peut marquer sa notification comme lue (sinon 403). */
    @Transactional
    public NotificationResume marquerLue(Long notificationId, Long utilisateurId) {
        Notification notification = notifications.findById(notificationId)
                .orElseThrow(NotificationIntrouvableException::new);
        if (!notification.estDestinataire(utilisateurId)) {
            throw new NotificationInterditeException();
        }
        notification.marquerLue();
        return NotificationResume.depuis(notification);
    }
}