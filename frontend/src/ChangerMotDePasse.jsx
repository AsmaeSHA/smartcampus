import { useState } from 'react';

export default function ChangerMotDePasse({ onChange, obligatoire = false }) {
  const [ancienMotDePasse, setAncienMotDePasse] = useState('');
  const [nouveauMotDePasse, setNouveauMotDePasse] = useState('');
  const [confirmation, setConfirmation] = useState('');
  const [erreur, setErreur] = useState('');
  const [chargement, setChargement] = useState(false);

  async function soumettre(event) {
    event.preventDefault();
    setErreur('');
    if (nouveauMotDePasse.length < 12 || nouveauMotDePasse.length > 128) {
      setErreur('Le nouveau mot de passe doit contenir entre 12 et 128 caractères.');
      return;
    }
    if (nouveauMotDePasse !== confirmation) {
      setErreur('La confirmation ne correspond pas au nouveau mot de passe.');
      return;
    }
    setChargement(true);
    try {
      const response = await fetch('/api/auth/password', {
        method: 'PUT', headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ ancienMotDePasse, nouveauMotDePasse }),
      });
      const data = await response.json().catch(() => ({}));
      if (!response.ok) { setErreur(data.message || 'Impossible de modifier le mot de passe.'); return; }
      onChange?.();
    } catch {
      setErreur('Le serveur ne répond pas. Vérifiez que le backend est démarré.');
    } finally { setChargement(false); }
  }

  return (
    <section className="card password-card">
      <h2>{obligatoire ? 'Changez votre mot de passe provisoire' : 'Changer mon mot de passe'}</h2>
      <form className="compte-form" onSubmit={soumettre}>
        <div className="field"><label htmlFor="ancien-mot-de-passe">Mot de passe actuel</label><input id="ancien-mot-de-passe" type="password" autoComplete="current-password" value={ancienMotDePasse} onChange={e => setAncienMotDePasse(e.target.value)} required /></div>
        <div className="field"><label htmlFor="nouveau-mot-de-passe">Nouveau mot de passe</label><input id="nouveau-mot-de-passe" type="password" autoComplete="new-password" minLength="12" maxLength="128" value={nouveauMotDePasse} onChange={e => setNouveauMotDePasse(e.target.value)} required /><span className="muted">12 à 128 caractères.</span></div>
        <div className="field"><label htmlFor="confirmation-mot-de-passe">Confirmer le nouveau mot de passe</label><input id="confirmation-mot-de-passe" type="password" autoComplete="new-password" value={confirmation} onChange={e => setConfirmation(e.target.value)} required /></div>
        {erreur && <p className="error" role="alert">{erreur}</p>}
        <button className="submit" type="submit" disabled={chargement}>{chargement ? 'Modification…' : 'Modifier le mot de passe'}</button>
      </form>
    </section>
  );
}
