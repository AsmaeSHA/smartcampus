import { useState } from 'react';

function dateLocale(date) {
  const ajustee = new Date(date.getTime() - date.getTimezoneOffset() * 60_000);
  return ajustee.toISOString().slice(0, 16);
}

function datesInitiales() {
  const debut = new Date();
  debut.setDate(debut.getDate() + 2);
  if (debut.getDay() === 0) debut.setDate(debut.getDate() + 1);
  debut.setHours(9, 0, 0, 0);
  const fin = new Date(debut.getTime() + 60 * 60_000);
  return { debut: dateLocale(debut), fin: dateLocale(fin) };
}

async function lireMessageErreur(response, messageParDefaut) {
  if (response.status === 404) {
    return "L’API des réservations est absente du backend actif (404). Arrêtez puis redémarrez le backend depuis ce dépôt.";
  }
  const texte = await response.text();
  if (texte) {
    try {
      const donnees = JSON.parse(texte);
      if (donnees.message) return donnees.message;
    } catch {
      // Une page d'erreur HTML ne doit pas remplacer le message lisible par l'état HTTP.
    }
  }
  return `${messageParDefaut} (HTTP ${response.status}).`;
}

export default function DemanderReservation({ espace, onFermer, onCreee }) {
  const initiales = datesInitiales();
  const [debut, setDebut] = useState(initiales.debut);
  const [fin, setFin] = useState(initiales.fin);
  const [justification, setJustification] = useState('');
  const [erreur, setErreur] = useState('');
  const [envoi, setEnvoi] = useState(false);

  async function soumettre(event) {
    event.preventDefault();
    setErreur('');
    if (new Date(debut).getDay() === 0) {
      setErreur('Les réservations ne sont pas autorisées le dimanche.');
      return;
    }
    setEnvoi(true);
    try {
      const response = await fetch('/api/reservations', {
        method: 'POST',
        credentials: 'include',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ espaceId: espace.id, debut, fin, justification }),
      });
      if (!response.ok) {
        setErreur(await lireMessageErreur(response, 'La demande de réservation a été refusée'));
        return;
      }
      const data = await response.json();
      onCreee?.(data);
    } catch {
      setErreur('Le serveur ne répond pas. Vérifiez que le backend est démarré.');
    } finally {
      setEnvoi(false);
    }
  }

  return (
    <section className="reservation-form-card" aria-labelledby="titre-demande-reservation">
      <div className="reservation-form-heading">
        <div><p className="eyebrow">DEMANDE DE RÉSERVATION</p><h2 id="titre-demande-reservation">{espace.nom}</h2></div>
        <button type="button" className="action" onClick={onFermer}>Fermer</button>
      </div>
      <form className="reservation-form" onSubmit={soumettre}>
        <div className="reservation-dates">
          <label>Début<input type="datetime-local" value={debut} onChange={e => setDebut(e.target.value)} required /></label>
          <label>Fin<input type="datetime-local" value={fin} onChange={e => setFin(e.target.value)} required /></label>
        </div>
        <p className="reservation-aide">Réservation au moins 24 h à l’avance, du lundi au samedi, entre 08:00 et 20:00.</p>
        <label>Justification<textarea value={justification} onChange={e => setJustification(e.target.value)} maxLength={1000} rows={4} required placeholder="Expliquez le motif de votre demande." /></label>
        {erreur && <p className="error" role="alert">{erreur}</p>}
        <button className="bouton-menthe" type="submit" disabled={envoi}>{envoi ? 'Envoi…' : 'Envoyer la demande'}</button>
      </form>
    </section>
  );
}
