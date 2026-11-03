import { useState, useEffect } from 'react';

const LIBELLES_TYPES = {
  SALLE_COURS: 'Salle de cours',
  SALLE_REUNION: 'Salle de réunion',
  AMPHITHEATRE: 'Amphithéâtre',
  BIBLIOTHEQUE: 'Bibliothèque',
  TERRAIN: 'Terrain',
  BUVETTE: 'Buvette',
};

export default function ListeEspaces() {
  const [espaces, setEspaces] = useState([]);
  const [chargement, setChargement] = useState(true);
  const [erreur, setErreur] = useState('');
  const [filtreType, setFiltreType] = useState('TOUS');

  useEffect(() => {
    fetch('/api/espaces', { credentials: 'include' })
      .then((res) => {
        if (!res.ok) throw new Error('Erreur de chargement');
        return res.json();
      })
      .then((data) => {
        setEspaces(data);
        setChargement(false);
      })
      .catch(() => {
        setErreur('Le serveur ne répond pas. Vérifiez que le backend est démarré.');
        setChargement(false);
      });
  }, []);

  // Filtre (SC-14)
  const espacesFiltres = espaces.filter((espace) => {
    if (filtreType === 'TOUS') return true;
    return espace.typeEspace === filtreType;
  });

  if (chargement) return <p className="vide">Chargement des espaces…</p>;

  return (
    <div className="liste-comptes" style={{ padding: '2rem' }}>
      <div className="barre-outils" style={{ marginBottom: '20px', display: 'flex', gap: '15px', alignItems: 'center' }}>
        <h2 style={{ margin: 0 }}>Liste des espaces</h2>
        
        <select 
          className="filtre-etat" 
          value={filtreType} 
          onChange={(e) => setFiltreType(e.target.value)}
        >
          <option value="TOUS">Tous les types</option>
          {Object.entries(LIBELLES_TYPES).map(([valeur, libelle]) => (
            <option key={valeur} value={valeur}>{libelle}</option>
          ))}
        </select>
      </div>

      {erreur && <p className="alerte alerte-erreur">{erreur}</p>}

      <table className="table-comptes">
        <thead>
          <tr>
            <th scope="col">Code</th>
            <th scope="col">Nom</th>
            <th scope="col">Type</th>
            <th scope="col">État</th>
          </tr>
        </thead>
        <tbody>
          {espacesFiltres.map((espace) => (
            <tr key={espace.id}>
              <td><strong>{espace.code}</strong></td>
              <td>{espace.nom}</td>
              <td>
                <span className="role role-etudiant">
                  {LIBELLES_TYPES[espace.typeEspace] || espace.typeEspace}
                </span>
              </td>
              <td>
                <span className={espace.archive ? 'etat etat-inactif' : 'etat etat-actif'}>
                  <span className="point" aria-hidden="true" />
                  {espace.archive ? 'Archivé' : 'Disponible'}
                </span>
              </td>
            </tr>
          ))}
          {espacesFiltres.length === 0 && (
            <tr>
              <td colSpan="4" style={{ textAlign: 'center', padding: '20px' }}>
                Aucun espace ne correspond à ce filtre.
              </td>
            </tr>
          )}
        </tbody>
      </table>
    </div>
  );
}