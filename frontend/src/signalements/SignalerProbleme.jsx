import { useState } from 'react';
import './signalements.css';

// Formulaire « Signaler un problème » affiché dans la fiche d'un espace.
// Envoie POST /api/signalements ; le serveur vérifie le rôle et les champs (R13).

const CATEGORIES = [
  { valeur: 'MATERIEL', libelle: 'Matériel' },
  { valeur: 'PROPRETE', libelle: 'Propreté' },
  { valeur: 'SECURITE', libelle: 'Sécurité' },
  { valeur: 'AUTRE', libelle: 'Autre' },
];

export default function SignalerProbleme({ espace, onFermer }) {
  const [categorie, setCategorie] = useState('MATERIEL');
  const [titre, setTitre] = useState('');
  const [description, setDescription] = useState('');
  const [erreurs, setErreurs] = useState({});
  const [erreurGenerale, setErreurGenerale] = useState('');
  const [envoi, setEnvoi] = useState(false);
  const [envoye, setEnvoye] = useState(false);

  function verifier() {
    const nouvelles = {};
    if (!titre.trim()) nouvelles.titre = 'Le titre est obligatoire';
    if (!description.trim()) nouvelles.description = 'La description est obligatoire';
    setErreurs(nouvelles);
    return Object.keys(nouvelles).length === 0;
  }

  async function envoyer(evenement) {
    evenement.preventDefault();
    setErreurGenerale('');
    if (!verifier()) return;

    setEnvoi(true);
    try {
      const res = await fetch('/api/signalements', {
        method: 'POST',
        credentials: 'include',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ espaceId: espace.id, categorie, titre, description }),
      });
      if (res.ok) {
        setEnvoye(true);
        return;
      }
      const data = await res.json().catch(() => ({}));
      if (res.status === 400 && data.champ) {
        setErreurs({ [data.champ]: data.message });
      } else {
        setErreurGenerale(data.message || 'Le signalement n’a pas pu être envoyé.');
      }
    } catch (e) {
      setErreurGenerale('Le serveur ne répond pas. Vérifiez que le backend est démarré.');
    } finally {
      setEnvoi(false);
    }
  }

  if (envoye) {
    return (
      <section className="card signalement-carte" aria-live="polite">
        <div className="succes">
          <p>Merci, votre signalement a bien été envoyé à l’administration.</p>
        </div>
        <button type="button" className="action" onClick={onFermer}>Fermer</button>
      </section>
    );
  }

  return (
    <section className="card signalement-carte" aria-labelledby="titre-signalement">
      <h2 id="titre-signalement">Signaler un problème · {espace.nom}</h2>
      <form onSubmit={envoyer} noValidate>
        <div className="field">
          <label htmlFor="signalement-categorie">Catégorie</label>
          <select id="signalement-categorie" value={categorie} onChange={(e) => setCategorie(e.target.value)}>
            {CATEGORIES.map((c) => <option key={c.valeur} value={c.valeur}>{c.libelle}</option>)}
          </select>
        </div>

        <div className="field">
          <label htmlFor="signalement-titre">Titre</label>
          <input id="signalement-titre" type="text" maxLength={150} value={titre}
            onChange={(e) => setTitre(e.target.value)} placeholder="Ex. : projecteur en panne"
            aria-invalid={Boolean(erreurs.titre)} aria-describedby={erreurs.titre ? 'erreur-titre' : undefined} />
          {erreurs.titre && <p id="erreur-titre" className="champ-erreur">{erreurs.titre}</p>}
        </div>

        <div className="field">
          <label htmlFor="signalement-description">Description</label>
          <textarea id="signalement-description" rows={4} maxLength={2000} value={description}
            onChange={(e) => setDescription(e.target.value)} placeholder="Décrivez le problème"
            aria-invalid={Boolean(erreurs.description)}
            aria-describedby={erreurs.description ? 'erreur-description' : undefined} />
          {erreurs.description && <p id="erreur-description" className="champ-erreur">{erreurs.description}</p>}
        </div>

        {erreurGenerale && <p role="alert" className="error"><span>{erreurGenerale}</span></p>}

        <div className="signalement-actions">
          <button type="button" className="action" onClick={onFermer}>Annuler</button>
          <button type="submit" className="submit" disabled={envoi}>
            {envoi ? 'Envoi…' : 'Envoyer le signalement'}
          </button>
        </div>
      </form>
    </section>
  );
}
