import { useEffect, useRef, useState } from 'react';
import { BadgeStatut, LIBELLES_CATEGORIE, formaterDateHeure } from './libelles.jsx';
import DetailSignalement from './DetailSignalement.jsx';
import './signalements.css';

// « Mes signalements » : les signalements de l'étudiant ou de l'enseignant connecté, avec leur statut.

export default function MesSignalements({ onNonConnecte }) {
  const [signalements, setSignalements] = useState([]);
  const [chargement, setChargement] = useState(true);
  const [erreur, setErreur] = useState('');
  const [selection, setSelection] = useState(null);

  const nonConnecteRef = useRef(onNonConnecte);
  nonConnecteRef.current = onNonConnecte;

  useEffect(() => {
    let actif = true;
    async function charger() {
      try {
        const res = await fetch('/api/signalements/mine', { credentials: 'include' });
        if (res.status === 401) {
          nonConnecteRef.current?.();
          return;
        }
        if (!res.ok) {
          if (actif) setErreur('Impossible de charger vos signalements.');
          return;
        }
        const data = await res.json();
        if (actif) setSignalements(data);
      } catch (e) {
        if (actif) setErreur('Le serveur ne répond pas. Vérifiez que le backend est démarré.');
      } finally {
        if (actif) setChargement(false);
      }
    }
    charger();
    return () => {
      actif = false;
    };
  }, []);

  if (selection) {
    return <DetailSignalement id={selection} onRetour={() => setSelection(null)} />;
  }

  return (
    <section className="card liste-signalements" aria-labelledby="titre-mes-signalements">
      <h2 id="titre-mes-signalements">Mes signalements</h2>
      {chargement && <p className="muted">Chargement…</p>}
      {erreur && <p role="alert" className="error"><span>{erreur}</span></p>}
      {!chargement && !erreur && signalements.length === 0 && (
        <p className="muted">Vous n’avez encore signalé aucun problème. Vous pouvez le faire depuis la fiche d’un espace.</p>
      )}
      {signalements.length > 0 && (
        <div className="table-scroll">
          <table>
            <thead>
              <tr>
                <th scope="col">Date</th>
                <th scope="col">Espace</th>
                <th scope="col">Catégorie</th>
                <th scope="col">Titre</th>
                <th scope="col">Statut</th>
                <th scope="col"><span className="sr-only">Actions</span></th>
              </tr>
            </thead>
            <tbody>
              {signalements.map((s) => (
                <tr key={s.id}>
                  <td>{formaterDateHeure(s.creeLe)}</td>
                  <td>{s.espaceNom}</td>
                  <td>{LIBELLES_CATEGORIE[s.categorie] || s.categorie}</td>
                  <td>{s.titre}</td>
                  <td><BadgeStatut statut={s.statut} /></td>
                  <td>
                    <button type="button" className="action" onClick={() => setSelection(s.id)}>Voir le suivi</button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </section>
  );
}
