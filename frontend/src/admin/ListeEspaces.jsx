import { useEffect, useState } from 'react';

const LIBELLES_TYPE = {
  SALLE_COURS: 'Salle de cours',
  SALLE_REUNION: 'Salle de réunion',
  AMPHITHEATRE: 'Amphithéâtre',
  BIBLIOTHEQUE: 'Bibliothèque',
  TERRAIN: 'Terrain',
  BUVETTE: 'Buvette',
};

export default function ListeEspaces({ onModifier, actualisation }) {
  const [espaces, setEspaces] = useState([]);
  const [chargement, setChargement] = useState(true);
  const [erreur, setErreur] = useState('');

  useEffect(() => {
    let active = true;
    setChargement(true);
    fetch('/api/espaces')
      .then(async (response) => {
        const data = await response.json().catch(() => []);
        if (!response.ok) throw new Error(data.message || 'Impossible de charger les espaces.');
        return data;
      })
      .then((data) => {
        if (active) {
          setEspaces(data);
          setErreur('');
        }
      })
      .catch((error) => {
        if (active) setErreur(error.message || 'Impossible de charger les espaces.');
      })
      .finally(() => {
        if (active) setChargement(false);
      });
    return () => { active = false; };
  }, [actualisation]);

  function espaceModifie(espace) {
    setEspaces((liste) => liste.map((element) => element.id === espace.id ? espace : element));
  }

  return (
    <section className="liste-comptes" aria-label="Liste des espaces">
      <h2>Espaces</h2>
      {chargement && <p className="vide">Chargement des espaces…</p>}
      {erreur && <p role="alert" className="alerte alerte-erreur">{erreur}</p>}
      {!chargement && !erreur && espaces.length === 0 && <p className="vide">Aucun espace pour l’instant.</p>}
      {!chargement && espaces.length > 0 && (
        <div className="table-scroll">
          <table className="table-comptes">
            <thead>
              <tr><th>Code</th><th>Nom</th><th>Type</th><th>Statut</th><th><span className="sr-only">Actions</span></th></tr>
            </thead>
            <tbody>
              {espaces.map((espace) => (
                <tr key={espace.id}>
                  <td>{espace.code}</td>
                  <td>{espace.nom}</td>
                  <td>{LIBELLES_TYPE[espace.typeEspace] || espace.typeEspace}</td>
                  <td>{espace.statut}</td>
                  <td className="actions">
                    <button type="button" className="bouton-ligne" onClick={() => onModifier(espace)}>Modifier</button>
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
