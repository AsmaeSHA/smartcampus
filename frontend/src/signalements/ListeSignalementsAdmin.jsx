import { useEffect, useState } from 'react';
import { BadgeStatut, LIBELLES_CATEGORIE, LIBELLES_STATUT, formaterDateHeure } from './libelles.jsx';
import './signalements.css';

// Onglet admin « Signalements » : tous les signalements, filtrables par statut, catégorie et espace.

export default function ListeSignalementsAdmin() {
  const [statut, setStatut] = useState('');
  const [categorie, setCategorie] = useState('');
  const [espaceId, setEspaceId] = useState('');
  const [espaces, setEspaces] = useState([]);
  const [signalements, setSignalements] = useState([]);
  const [chargement, setChargement] = useState(true);
  const [erreur, setErreur] = useState('');

  useEffect(() => {
    fetch('/api/espaces', { credentials: 'include' })
      .then((res) => (res.ok ? res.json() : []))
      .then(setEspaces)
      .catch(() => setEspaces([]));
  }, []);

  useEffect(() => {
    let actif = true;
    const params = new URLSearchParams();
    if (statut) params.set('statut', statut);
    if (categorie) params.set('categorie', categorie);
    if (espaceId) params.set('espaceId', espaceId);

    setChargement(true);
    setErreur('');
    fetch(`/api/admin/signalements?${params}`, { credentials: 'include' })
      .then(async (res) => {
        if (!res.ok) throw new Error('Impossible de charger les signalements.');
        return res.json();
      })
      .then((data) => actif && setSignalements(data))
      .catch((e) => actif && setErreur(e.message || 'Le serveur ne répond pas.'))
      .finally(() => actif && setChargement(false));
    return () => {
      actif = false;
    };
  }, [statut, categorie, espaceId]);

  const filtreActif = statut || categorie || espaceId;

  return (
    <section className="card liste-signalements" aria-labelledby="titre-signalements-admin">
      <h2 id="titre-signalements-admin">Signalements</h2>

      <div className="filtres-signalements" role="group" aria-label="Filtrer les signalements">
        <div className="field">
          <label htmlFor="filtre-statut">Statut</label>
          <select id="filtre-statut" value={statut} onChange={(e) => setStatut(e.target.value)}>
            <option value="">Tous</option>
            {Object.entries(LIBELLES_STATUT).map(([valeur, libelle]) => (
              <option key={valeur} value={valeur}>{libelle}</option>
            ))}
          </select>
        </div>
        <div className="field">
          <label htmlFor="filtre-categorie">Catégorie</label>
          <select id="filtre-categorie" value={categorie} onChange={(e) => setCategorie(e.target.value)}>
            <option value="">Toutes</option>
            {Object.entries(LIBELLES_CATEGORIE).map(([valeur, libelle]) => (
              <option key={valeur} value={valeur}>{libelle}</option>
            ))}
          </select>
        </div>
        <div className="field">
          <label htmlFor="filtre-espace">Espace</label>
          <select id="filtre-espace" value={espaceId} onChange={(e) => setEspaceId(e.target.value)}>
            <option value="">Tous</option>
            {espaces.map((e) => <option key={e.id} value={e.id}>{e.nom} ({e.code})</option>)}
          </select>
        </div>
        {filtreActif && (
          <button type="button" className="action" onClick={() => { setStatut(''); setCategorie(''); setEspaceId(''); }}>
            Effacer les filtres
          </button>
        )}
      </div>

      {erreur && <p role="alert" className="error"><span>{erreur}</span></p>}
      {chargement && <p className="muted">Chargement…</p>}
      {!chargement && !erreur && (
        <p className="compteur">{signalements.length} signalement{signalements.length > 1 ? 's' : ''}</p>
      )}
      {!chargement && !erreur && signalements.length === 0 && (
        <p className="muted">{filtreActif ? 'Aucun signalement ne correspond à ces filtres.' : 'Aucun signalement pour le moment.'}</p>
      )}

      {!chargement && signalements.length > 0 && (
        <div className="table-scroll">
          <table>
            <thead>
              <tr>
                <th scope="col">Date</th>
                <th scope="col">Espace</th>
                <th scope="col">Catégorie</th>
                <th scope="col">Titre</th>
                <th scope="col">Signalé par</th>
                <th scope="col">Statut</th>
              </tr>
            </thead>
            <tbody>
              {signalements.map((s) => (
                <tr key={s.id}>
                  <td>{formaterDateHeure(s.creeLe)}</td>
                  <td>{s.espaceNom}</td>
                  <td>{LIBELLES_CATEGORIE[s.categorie] || s.categorie}</td>
                  <td>
                    <strong>{s.titre}</strong>
                    <span className="description-courte">{s.description}</span>
                  </td>
                  <td>{s.auteur}</td>
                  <td><BadgeStatut statut={s.statut} /></td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </section>
  );
}
