import { useEffect, useMemo, useState } from 'react';

// Liste des comptes (GET /api/admin/comptes). Aucun mot de passe n'est renvoyé par le serveur.
// Recherche, filtre d'état et pagination se font côté navigateur.
// SC-28 : désactiver / réactiver. SC-64 : modifier.

const PAR_PAGE = 12;

export const LIBELLES_ROLE = {
  ETUDIANT: 'Étudiant',
  ENSEIGNANT: 'Enseignant',
  ADMINISTRATEUR: 'Administrateur',
};

// « Élève » et « eleve » donnent le même résultat de recherche.
function normaliser(texte) {
  return (texte || '').normalize('NFD').replace(/\p{M}/gu, '').toLowerCase();
}

function initiales(compte) {
  return `${compte.prenom?.[0] || ''}${compte.nom?.[0] || ''}`.toUpperCase();
}

export default function ListeComptes({
  comptes,
  chargement,
  filtreRole,
  emailConnecte,
  onModifier,
  onChangerActivation,
  onEffacerRole,
}) {
  const [recherche, setRecherche] = useState('');
  const [etat, setEtat] = useState('tous');
  const [page, setPage] = useState(1);

  useEffect(() => setPage(1), [recherche, etat, filtreRole]);

  const filtres = useMemo(() => {
    const terme = normaliser(recherche.trim());
    return comptes.filter((c) => {
      if (filtreRole !== 'TOUS' && c.role !== filtreRole) return false;
      if (etat === 'actifs' && !c.actif) return false;
      if (etat === 'desactives' && c.actif) return false;
      if (etat === 'motdepasse' && !c.changerMotDePasse) return false;
      if (!terme) return true;
      return normaliser(`${c.prenom} ${c.nom} ${c.nom} ${c.prenom} ${c.email}`).includes(terme);
    });
  }, [comptes, recherche, etat, filtreRole]);

  const pages = Math.max(1, Math.ceil(filtres.length / PAR_PAGE));
  const pageCourante = Math.min(page, pages);
  const debut = (pageCourante - 1) * PAR_PAGE;
  const visibles = filtres.slice(debut, debut + PAR_PAGE);
  const filtresActifs = recherche.trim() !== '' || etat !== 'tous' || filtreRole !== 'TOUS';

  function effacerFiltres() {
    setRecherche('');
    setEtat('tous');
    onEffacerRole();
  }

  return (
    <section className="liste-comptes" aria-label="Liste des comptes">
      <div className="barre-outils">
        <div className="recherche">
          <svg viewBox="0 0 20 20" width="18" height="18" aria-hidden="true">
            <circle cx="9" cy="9" r="5.8" fill="none" stroke="currentColor" strokeWidth="1.7" />
            <path d="M13.3 13.3L17 17" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" />
          </svg>
          <label htmlFor="recherche-compte" className="sr-only">Rechercher un compte</label>
          <input
            id="recherche-compte"
            type="search"
            placeholder="Rechercher par nom ou e-mail"
            value={recherche}
            onChange={(e) => setRecherche(e.target.value)}
          />
        </div>

        <label htmlFor="filtre-etat" className="sr-only">Filtrer par état</label>
        <select id="filtre-etat" className="filtre-etat" value={etat} onChange={(e) => setEtat(e.target.value)}>
          <option value="tous">Tous les états</option>
          <option value="actifs">Actifs</option>
          <option value="desactives">Désactivés</option>
          <option value="motdepasse">Mot de passe à changer</option>
        </select>

        <p className="resultats" aria-live="polite">
          {filtres.length} compte{filtres.length > 1 ? 's' : ''}
        </p>
      </div>

      {chargement && comptes.length === 0 && <p className="vide">Chargement des comptes…</p>}

      {!chargement && filtres.length === 0 && (
        <div className="vide">
          <p>{comptes.length === 0 ? 'Aucun compte pour l’instant.' : 'Aucun compte ne correspond à votre recherche.'}</p>
          {filtresActifs && (
            <button type="button" className="bouton-secondaire" onClick={effacerFiltres}>
              Effacer les filtres
            </button>
          )}
        </div>
      )}

      {visibles.length > 0 && (
        <table className="table-comptes">
          <thead>
            <tr>
              <th scope="col">Utilisateur</th>
              <th scope="col">Rôle</th>
              <th scope="col">État</th>
              <th scope="col"><span className="sr-only">Actions</span></th>
            </tr>
          </thead>
          <tbody>
            {visibles.map((c) => {
              const estMoi = c.email === emailConnecte;
              return (
                <tr key={c.id} className={c.actif ? '' : 'ligne-inactive'}>
                  <td className="cellule-utilisateur">
                    <div className="utilisateur">
                      <span className={`avatar avatar-${c.role.toLowerCase()}`} aria-hidden="true">
                        {initiales(c)}
                      </span>
                      <span className="identite">
                        <span className="nom">
                          {c.prenom} {c.nom}
                          {estMoi && <span className="vous">Vous</span>}
                        </span>
                        <span className="email">{c.email}</span>
                      </span>
                    </div>
                  </td>
                  <td>
                    <span className={`role role-${c.role.toLowerCase()}`}>{LIBELLES_ROLE[c.role] || c.role}</span>
                  </td>
                  <td>
                    <span className={c.actif ? 'etat etat-actif' : 'etat etat-inactif'}>
                      <span className="point" aria-hidden="true" />
                      {c.actif ? 'Actif' : 'Désactivé'}
                    </span>
                    {c.changerMotDePasse && <span className="indice">Mot de passe à changer</span>}
                  </td>
                  <td className="actions">
                    <button
                      type="button"
                      className="bouton-ligne"
                      onClick={() => onModifier(c)}
                      aria-label={`Modifier le compte de ${c.prenom} ${c.nom}`}
                    >
                      Modifier
                    </button>
                    <button type="button" className="action" onClick={() => onReinitialiserMotDePasse(c)} aria-label={`Réinitialiser le mot de passe de ${c.prenom} ${c.nom}`}>
                      Réinitialiser le mot de passe
                    </button>
                    {c.email === emailConnecte ? (
                      <span className="muted vous">Vous</span>
                    ) : (
                    {!estMoi && (
                      <button
                        type="button"
                        className={c.actif ? 'bouton-ligne danger' : 'bouton-ligne succes-texte'}
                        onClick={() => onChangerActivation(c)}
                        aria-label={`${c.actif ? 'Désactiver' : 'Réactiver'} le compte de ${c.prenom} ${c.nom}`}
                      >
                        {c.actif ? 'Désactiver' : 'Réactiver'}
                      </button>
                    )}
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      )}

      {filtres.length > PAR_PAGE && (
        <nav className="pagination" aria-label="Pagination de la liste des comptes">
          <p>
            {debut + 1}–{Math.min(debut + PAR_PAGE, filtres.length)} sur {filtres.length}
          </p>
          <div>
            <button
              type="button"
              className="bouton-secondaire"
              onClick={() => setPage(pageCourante - 1)}
              disabled={pageCourante === 1}
            >
              Précédent
            </button>
            <span>
              Page {pageCourante} / {pages}
            </span>
            <button
              type="button"
              className="bouton-secondaire"
              onClick={() => setPage(pageCourante + 1)}
              disabled={pageCourante === pages}
            >
              Suivant
            </button>
          </div>
        </nav>
      )}
    </section>
  );
}
