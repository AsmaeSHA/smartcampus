import { useEffect, useState } from 'react';
import { BadgeStatut, LIBELLES_CATEGORIE, LIBELLES_STATUT, formaterDateHeure } from './libelles.jsx';
import './signalements.css';

// Détail et suivi d'un signalement.
// L'auteur voit le suivi ; l'administrateur (admin=true) change aussi le statut et ajoute des commentaires.

const LIBELLES_ACTION = {
  EN_COURS: 'Prendre en charge',
  RESOLU: 'Marquer comme résolu',
  FERME: 'Fermer le signalement',
};

export default function DetailSignalement({ id, admin = false, onRetour, onModifie }) {
  const [detail, setDetail] = useState(null);
  const [erreur, setErreur] = useState('');
  const [commentaire, setCommentaire] = useState('');
  const [envoi, setEnvoi] = useState(false);

  useEffect(() => {
    let actif = true;
    fetch(`/api/signalements/${id}`, { credentials: 'include' })
      .then(async (res) => {
        const data = await res.json().catch(() => ({}));
        if (!res.ok) throw new Error(data.message || 'Impossible de charger ce signalement.');
        return data;
      })
      .then((data) => actif && setDetail(data))
      .catch((e) => actif && setErreur(e.message));
    return () => {
      actif = false;
    };
  }, [id]);

  async function envoyer(url, methode, corps) {
    setEnvoi(true);
    setErreur('');
    try {
      const res = await fetch(url, {
        method: methode,
        credentials: 'include',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(corps),
      });
      const data = await res.json().catch(() => ({}));
      if (!res.ok) {
        setErreur(data.message || 'L’action n’a pas pu être enregistrée.');
        return false;
      }
      setDetail(data);
      onModifie?.();
      return true;
    } catch (e) {
      setErreur('Le serveur ne répond pas. Vérifiez que le backend est démarré.');
      return false;
    } finally {
      setEnvoi(false);
    }
  }

  function changerStatut(statut) {
    if (statut === 'FERME' && !window.confirm('Fermer ce signalement ? Il ne pourra plus être modifié.')) return;
    envoyer(`/api/admin/signalements/${id}/statut`, 'PATCH', { statut });
  }

  async function commenter(evenement) {
    evenement.preventDefault();
    if (!commentaire.trim()) {
      setErreur('Le commentaire est vide.');
      return;
    }
    const ok = await envoyer(`/api/admin/signalements/${id}/commentaires`, 'POST', { contenu: commentaire });
    if (ok) setCommentaire('');
  }

  const retour = (
    <button type="button" className="action" onClick={onRetour}>← Retour à la liste</button>
  );

  if (!detail) {
    return (
      <section className="card liste-signalements">
        {retour}
        {!erreur && <p className="muted">Chargement…</p>}
        {erreur && <p role="alert" className="error"><span>{erreur}</span></p>}
      </section>
    );
  }

  const { signalement: s, traitePar, statutsPossibles, commentaires } = detail;
  const ferme = s.statut === 'FERME';

  return (
    <section className="card liste-signalements detail-signalement" aria-labelledby="titre-detail-signalement">
      {retour}

      <div className="detail-entete">
        <div>
          <p className="muted">{s.espaceNom} · {LIBELLES_CATEGORIE[s.categorie] || s.categorie}</p>
          <h2 id="titre-detail-signalement">{s.titre}</h2>
        </div>
        <BadgeStatut statut={s.statut} />
      </div>

      <p className="detail-description">{s.description}</p>

      <dl className="detail-infos">
        <div><dt>Signalé par</dt><dd>{s.auteur}</dd></div>
        <div><dt>Le</dt><dd>{formaterDateHeure(s.creeLe)}</dd></div>
        <div><dt>Traité par</dt><dd>{traitePar || 'Pas encore pris en charge'}</dd></div>
        {ferme && <div><dt>Fermé le</dt><dd>{formaterDateHeure(s.fermeLe)}</dd></div>}
      </dl>

      {admin && statutsPossibles.length > 0 && (
        <div className="detail-actions" role="group" aria-label="Changer le statut">
          {statutsPossibles.map((statut) => (
            <button key={statut} type="button" disabled={envoi}
              className={statut === 'FERME' ? 'action' : 'submit'}
              onClick={() => changerStatut(statut)}>
              {LIBELLES_ACTION[statut] || LIBELLES_STATUT[statut]}
            </button>
          ))}
        </div>
      )}

      {erreur && <p role="alert" className="error"><span>{erreur}</span></p>}

      <h3>Suivi</h3>
      {commentaires.length === 0 && <p className="muted">Aucun commentaire pour le moment.</p>}
      <ol className="fil-commentaires">
        {commentaires.map((c) => (
          <li key={c.id}>
            <p className="fil-meta"><strong>{c.auteur}</strong> · {formaterDateHeure(c.creeLe)}</p>
            <p>{c.contenu}</p>
          </li>
        ))}
      </ol>

      {admin && !ferme && (
        <form onSubmit={commenter} className="field">
          <label htmlFor="nouveau-commentaire">Ajouter un commentaire</label>
          <textarea id="nouveau-commentaire" rows={3} maxLength={2000} value={commentaire}
            onChange={(e) => setCommentaire(e.target.value)} placeholder="Ex. : technicien prévu demain matin" />
          <div className="signalement-actions">
            <button type="submit" className="submit" disabled={envoi}>Publier le commentaire</button>
          </div>
        </form>
      )}
    </section>
  );
}
