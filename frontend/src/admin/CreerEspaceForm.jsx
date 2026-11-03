import { useState } from 'react';

const TYPES = [
  ['SALLE_COURS', 'Salle de cours'],
  ['SALLE_REUNION', 'Salle de réunion'],
  ['AMPHITHEATRE', 'Amphithéâtre'],
  ['BIBLIOTHEQUE', 'Bibliothèque'],
  ['TERRAIN', 'Terrain'],
  ['BUVETTE', 'Buvette'],
];

export default function CreerEspaceForm({ onCree }) {
  const [champs, setChamps] = useState({ code: '', nom: '', typeEspace: '' });
  const [erreur, setErreur] = useState('');
  const [envoi, setEnvoi] = useState(false);

  function modifier(cle) {
    return (event) => setChamps((valeurs) => ({ ...valeurs, [cle]: event.target.value }));
  }

  async function envoyer(event) {
    event.preventDefault();
    setErreur('');
    if (!champs.code.trim() || !champs.nom.trim() || !champs.typeEspace) {
      setErreur('Renseignez le code, le nom et le type de l’espace.');
      return;
    }

    setEnvoi(true);
    try {
      const response = await fetch('/api/espaces', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          code: champs.code.trim(),
          nom: champs.nom.trim(),
          typeEspace: champs.typeEspace,
        }),
      });
      const data = await response.json().catch(() => ({}));
      if (!response.ok) {
        setErreur(data.message || 'Création impossible. Réessayez plus tard.');
        return;
      }
      setChamps({ code: '', nom: '', typeEspace: '' });
      onCree(data);
    } catch (error) {
      setErreur('Le serveur ne répond pas. Vérifiez que le backend est démarré.');
    } finally {
      setEnvoi(false);
    }
  }

  return (
    <form className="compte-form" onSubmit={envoyer} noValidate>
      <div className="field">
        <label htmlFor="espace-code">Code</label>
        <input id="espace-code" value={champs.code} onChange={modifier('code')} autoComplete="off" />
      </div>
      <div className="field">
        <label htmlFor="espace-nom">Nom</label>
        <input id="espace-nom" value={champs.nom} onChange={modifier('nom')} autoComplete="off" />
      </div>
      <div className="field">
        <label htmlFor="espace-type">Type d’espace</label>
        <select id="espace-type" value={champs.typeEspace} onChange={modifier('typeEspace')}>
          <option value="">Choisir…</option>
          {TYPES.map(([valeur, libelle]) => <option key={valeur} value={valeur}>{libelle}</option>)}
        </select>
      </div>
      {erreur && <p role="alert" className="error"><span>{erreur}</span></p>}
      <div className="form-actions">
        <button type="submit" className="submit" disabled={envoi}>
          {envoi ? 'Création en cours…' : 'Créer l’espace'}
        </button>
      </div>
    </form>
  );
}
