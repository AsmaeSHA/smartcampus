import { useEffect, useState } from 'react';

const LIBELLES = { EN_ATTENTE: 'En attente', CONFIRMEE: 'Confirmée', REFUSEE: 'Refusée', ANNULEE: 'Annulée' };

async function lireReponse(response) {
  const texte = await response.text();
  if (!texte) return null;
  try { return JSON.parse(texte); } catch { return null; }
}

function afficherDate(texte) {
  return new Date(texte).toLocaleString('fr-FR', { dateStyle: 'long', timeStyle: 'short' });
}

export default function MesReservations({ onNonConnecte, onRetour }) {
  const [reservations, setReservations] = useState([]);
  const [chargement, setChargement] = useState(true);
  const [erreur, setErreur] = useState('');
  const [annulationId, setAnnulationId] = useState(null);
  const [raison, setRaison] = useState('');
  const [envoi, setEnvoi] = useState(false);

  async function charger() {
    setChargement(true);
    try {
      const response = await fetch('/api/reservations/mes', { credentials: 'include' });
      if (response.status === 401) { onNonConnecte?.(); return; }
      const data = await lireReponse(response);
      if (!response.ok) {
        if (response.status === 404) {
          setErreur("L’API des réservations est absente du backend actif (404). Arrêtez puis redémarrez le backend depuis ce dépôt.");
          return;
        }
        setErreur(data?.message || `Impossible de charger vos réservations (HTTP ${response.status}).`);
        return;
      }
      if (!Array.isArray(data)) {
        setErreur('Le serveur a renvoyé une réponse inattendue pour vos réservations. Vérifiez le terminal backend.');
        return;
      }
      setReservations(data);
      setErreur('');
    } catch {
      setErreur('Le serveur ne répond pas. Vérifiez que le backend est démarré.');
    } finally { setChargement(false); }
  }

  useEffect(() => { charger(); }, []);

  async function annuler(id) {
    if (!raison.trim()) { setErreur('La raison de l’annulation est obligatoire.'); return; }
    setEnvoi(true);
    try {
      const response = await fetch(`/api/reservations/${id}/annuler`, {
        method: 'POST', credentials: 'include', headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ raison }),
      });
      const data = await lireReponse(response);
      if (!response.ok) { setErreur(data?.message || `Impossible d’annuler cette réservation (HTTP ${response.status}).`); return; }
      setReservations(items => items.map(item => item.id === id ? data : item));
      setAnnulationId(null); setRaison(''); setErreur('');
    } catch { setErreur('Le serveur ne répond pas. Vérifiez que le backend est démarré.'); }
    finally { setEnvoi(false); }
  }

  return (
    <section className="reservations-page">
      <div className="reservations-entete">
        <div><p className="eyebrow">VOTRE ESPACE</p><h1>Mes réservations</h1><p>Consultez vos demandes et annulez-les au moins 24 h avant le début.</p></div>
        <button type="button" className="bouton-secondaire" onClick={onRetour}>Retour à l’accueil</button>
      </div>
      {erreur && <p className="error" role="alert">{erreur}</p>}
      {chargement && <p className="muted">Chargement des réservations…</p>}
      {!chargement && reservations.length === 0 && <p className="role-card">Vous n’avez pas encore de demande de réservation.</p>}
      <div className="reservations-liste">
        {reservations.map(reservation => {
          const annulable = ['EN_ATTENTE', 'CONFIRMEE'].includes(reservation.statut)
            && new Date(reservation.debut).getTime() - Date.now() >= 24 * 60 * 60_000;
          return (
            <article className="role-card reservation-item" key={reservation.id}>
              <div className="reservation-item-entete"><div><p className="eyebrow">{reservation.espaceCode}</p><h2>{reservation.espaceNom}</h2></div><span className={`reservation-statut statut-${reservation.statut.toLowerCase()}`}>{LIBELLES[reservation.statut] || reservation.statut}</span></div>
              <p><strong>Du</strong> {afficherDate(reservation.debut)} <strong>au</strong> {afficherDate(reservation.fin)}</p>
              <p><strong>Justification :</strong> {reservation.justification}</p>
              {reservation.raisonAnnulation && <p><strong>Raison d’annulation :</strong> {reservation.raisonAnnulation}</p>}
              {annulable && annulationId !== reservation.id && <button type="button" className="action danger" onClick={() => { setAnnulationId(reservation.id); setRaison(''); setErreur(''); }}>Annuler la réservation</button>}
              {annulationId === reservation.id && (
                <div className="reservation-annulation"><label>Raison de l’annulation<textarea rows={3} maxLength={500} value={raison} onChange={e => setRaison(e.target.value)} required /></label><div><button type="button" className="action" onClick={() => setAnnulationId(null)}>Retour</button><button type="button" className="action danger" disabled={envoi} onClick={() => annuler(reservation.id)}>{envoi ? 'Annulation…' : 'Confirmer l’annulation'}</button></div></div>
              )}
            </article>
          );
        })}
      </div>
    </section>
  );
}
