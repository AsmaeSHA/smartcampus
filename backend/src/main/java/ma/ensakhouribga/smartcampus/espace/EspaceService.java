package ma.ensakhouribga.smartcampus.espace;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import org.springframework.data.domain.Sort;

@Service
public class EspaceService {

    private final EspaceRepository espaces;

    public EspaceService(EspaceRepository espaces) {
        this.espaces = espaces;
    }

    @Transactional
    public EspaceResume creer(CreerEspaceRequest demande) {
        String code = demande.code().trim();
        String nom = demande.nom().trim();
        if (espaces.existsByCode(code)) {
            throw new EspaceExistantException("Un espace existe déjà avec ce code");
        }

        try {
            return EspaceResume.depuis(espaces.saveAndFlush(new Espace(code, nom, demande.typeEspace())));
        } catch (DataIntegrityViolationException exception) {
            if (contrainteCodeVioliee(exception)) {
                throw new EspaceExistantException("Un espace existe déjà avec ce code");
            }
            throw exception;
        }
    }

    @Transactional(readOnly = true)
    public List<EspaceResume> lister() {
        return espaces.findByStatut(StatutEspace.ACTIVE, Sort.by("code")).stream()
                .map(EspaceResume::depuis)
                .toList();
    }

    @Transactional
    public EspaceResume modifier(Long id, CreerEspaceRequest demande) {
        Espace espace = espaces.findById(id)
                .orElseThrow(() -> new EspaceIntrouvableException("Cet espace n'existe pas"));
        String code = demande.code().trim();
        String nom = demande.nom().trim();
        if (espaces.existsByCodeAndIdNot(code, id)) {
            throw new EspaceExistantException("Un espace existe déjà avec ce code");
        }

        espace.modifier(code, nom, demande.typeEspace());
        try {
            return EspaceResume.depuis(espaces.saveAndFlush(espace));
        } catch (DataIntegrityViolationException exception) {
            if (contrainteCodeVioliee(exception)) {
                throw new EspaceExistantException("Un espace existe déjà avec ce code");
            }
            throw exception;
        }
    }

    @Transactional
    public EspaceResume archiver(Long id) {
        Espace espace = espaces.findById(id)
                .orElseThrow(() -> new EspaceIntrouvableException("Cet espace n'existe pas"));
        if (espace.getStatut() == StatutEspace.ARCHIVE) {
            return EspaceResume.depuis(espace);
        }
        espace.archiver();
        return EspaceResume.depuis(espaces.saveAndFlush(espace));
    }

    private boolean contrainteCodeVioliee(DataIntegrityViolationException exception) {
        Throwable cause = exception;
        while (cause != null) {
            String message = cause.getMessage();
            if (message != null && message.contains("uk_espaces_code")) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }
}
