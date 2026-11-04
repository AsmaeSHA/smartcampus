import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import './css/espaces.css';

const PAR_PAGE = 12;

export const LIBELLES_TYPE = {
  SALLE_COURS: 'Salle de cours',
  SALLE_REUNION: 'Salle de réunion',
  AMPHITHEATRE: 'Amphithéâtre',
  BIBLIOTHEQUE: 'Bibliothèque',
  TERRAIN: 'Terrain',
  BUVETTE: 'Buvette',
};

const LIBELLES_STATUT = {
  ACTIVE: 'Active',
  ARCHIVEE: 'Archivée',
};


function normaliser(texte) {
  return (texte || '')
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .toLowerCase();
}

function libelleStatut(statut) {
  return LIBELLES_STATUT[statut] || statut || 'Non renseigné';
}

export default function ListeEspaces({ onNonConnecte, rechargement = 0 }) {
  const [espaces, setEspaces] = useState([]);
  const [chargement, setChargement] = useState(true);
  const [erreur, setErreur] = useState('');
  const [recherche, setRecherche] = useState('');
  
  const [type, setType] = useState('tous');
  const [page, setPage] = useState(1);

  const nonConnecteRef = useRef(onNonConnecte);
  nonConnecteRef.current = onNonConnecte;

  const charger = useCallback(async () => {
    setChargement(true);
    setErreur('');

    try {
      const response = await fetch('/api/espaces', {
        credentials: 'include',
      });

      if (response.status === 401) {
        nonConnecteRef.current?.();
        return;
      }

      if (!response.ok) {
        const data = await response.json().catch(() => ({}));
        setErreur(data.message || 'Impossible de charger les espaces.');
        return;
      }

      const data = await response.json();
      setEspaces(Array.isArray(data) ? data : []);
    } catch (error) {
      setErreur('Le serveur ne répond pas. Vérifiez que le backend est démarré.');
    } finally {
      setChargement(false);
    }
  }, []);

  useEffect(() => {
    charger();
  }, [rechargement]);

  useEffect(() => {
    setPage(1);
  }, [recherche, type]);

  const filtres = useMemo(() => {
    const terme = normaliser(recherche.trim());

    return espaces.filter((espace) => {
      if (espace.statut === 'ARCHIVEE') return false;
      if (type !== 'tous' && espace.typeEspace !== type) return false;
      if (!terme) return true;
      return normaliser(`${espace.code || ''} ${espace.nom || ''}`).includes(terme);
    });
  }, [espaces, recherche, type]);

  const pages = Math.max(1, Math.ceil(filtres.length / PAR_PAGE));
  const pageCourante = Math.min(page, pages);
  const debut = (pageCourante - 1) * PAR_PAGE;
  const visibles = filtres.slice(debut, debut + PAR_PAGE);

  const filtresActifs = recherche.trim() !== '' || type !== 'tous';

  function effacerFiltres() {
    setRecherche('');
    setType('tous');
  }


function voirDetails(id) {
  if (!id) {
    alert("Erreur : l'identifiant de cet espace est introuvable.");
    return;
  }


  window.history.pushState({}, '', `/espaces/${id}`);
  window.dispatchEvent(new PopStateEvent('popstate'));
}

  return (
    <section className="liste-comptes liste-espaces" aria-label="Liste des espaces">
      <div className="barre-outils">
        <div className="recherche">
          <svg viewBox="0 0 20 20" width="18" height="18" aria-hidden="true">
            <circle cx="9" cy="9" r="5.8" fill="none" stroke="currentColor" strokeWidth="1.7" />
            <path d="M13.3 13.3L17 17" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" />
          </svg> 
          <label htmlFor="recherche-espace" className="sr-only">
            Rechercher un espace
          </label>
          <input
            id="recherche-espace"
            type="search"
            placeholder="Rechercher par code ou nom"
            value={recherche}
            onChange={(e) => setRecherche(e.target.value)}
          />
        </div>

        <label htmlFor="filtre-type" className="sr-only">
          Filtrer par type
        </label>
        <select
          id="filtre-type"
          className="filtre-etat"
          value={type}
          onChange={(e) => setType(e.target.value)}
        >
          <option value="tous">Tous les types</option>
          {Object.entries(LIBELLES_TYPE).map(([valeur, libelle]) => (
            <option key={valeur} value={valeur}>
              {libelle}
            </option>
          ))}
        </select>

        <p className="resultats" aria-live="polite">
          {filtres.length} espace{filtres.length > 1 ? 's' : ''}
        </p>
      </div>

      {chargement && espaces.length === 0 && (
        <p className="vide">Chargement des espaces…</p>
      )}

      {erreur && (
        <div className="vide">
          <p role="alert" className="error">{erreur}</p>
          <button type="button" className="bouton-secondaire" onClick={charger}>
            Réessayer
          </button>
        </div>
      )}

      {!chargement && !erreur && filtres.length === 0 && (
        <div className="vide">
          <p>
            {espaces.length === 0
              ? 'Aucun espace pour l’instant.'
              : 'Aucun espace ne correspond à votre recherche.'}
          </p>
          {filtresActifs && (
            <button type="button" className="bouton-secondaire" onClick={effacerFiltres}>
              Effacer les filtres
            </button>
          )}
        </div>
      )}

      {visibles.length > 0 && (
        <div className="table-responsive">
          <table className="table-comptes table-espaces">
            <thead>
              <tr>
                <th scope="col" className="col-code">Code</th>
                <th scope="col" className="col-nom">Nom</th>
                <th scope="col" className="col-type">Type</th>
                <th scope="col" className="col-statut">État</th>
                {/* ⭐ NOUVELLE COLONNE ⭐ */}
                <th scope="col" style={{ textAlign: 'right' }}>Actions</th>
              </tr>
            </thead>
            <tbody>
              {visibles.map((espace) => {
                const actif = espace.statut === 'ACTIVE';
                return (
                  <tr key={espace.id ?? espace.code}>
                    <td>
                      <span className="code-espace">{espace.code}</span>
                    </td>
                    <td>
                      <span className="nom-espace">{espace.nom}</span>
                    </td>
                    <td>
                      <span className="type-espace">
                        {LIBELLES_TYPE[espace.typeEspace] || espace.typeEspace || 'Non renseigné'}
                      </span>
                    </td>
                    <td>
                      <span className={actif ? 'etat etat-actif' : 'etat etat-inactif'}>
                        <span className="point" aria-hidden="true" />
                        {libelleStatut(espace.statut)}
                      </span>
                    </td>
                    {/* BOUTON DÉTAILS */}
                    <td style={{ textAlign: 'right' }}>
                      <button 
                        type="button" 
                        className="bouton-ligne" 
                        onClick={() => voirDetails(espace.id)} 
                      >
                        Voir la fiche
                      </button>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      )}

      {filtres.length > PAR_PAGE && (
        <nav className="pagination" aria-label="Pagination de la liste des espaces">
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
            <span>Page {pageCourante} / {pages}</span>
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