import { useEffect, useRef, useState } from 'react';
import './cloche.css';

// Cloche des notifications (SC-140) : nombre de non lues, liste au clic,
// et une notification cliquée est marquée comme lue.
export default function Cloche() {
  const [nombre, setNombre] = useState(0);
  const [ouverte, setOuverte] = useState(false);
  const [notifications, setNotifications] = useState([]);
  const [chargement, setChargement] = useState(false);
  const [erreur, setErreur] = useState('');
  const zone = useRef(null);

  async function chargerNombre() {
    try {
      const res = await fetch('/api/notifications/non-lues/nombre');
      if (res.ok) {
        const data = await res.json();
        setNombre(data.nombre);
      }
    } catch {
      // le compteur sera rechargé au prochain essai
    }
  }

  async function chargerListe() {
    setChargement(true);
    setErreur('');
    try {
      const res = await fetch('/api/notifications');
      if (!res.ok) throw new Error();
      setNotifications(await res.json());
    } catch {
      setErreur('Impossible de charger les notifications.');
    } finally {
      setChargement(false);
    }
  }

  async function lire(notification) {
    if (notification.lue) return;
    const res = await fetch(`/api/notifications/${notification.id}/lue`, { method: 'PATCH' });
    if (res.ok) {
      setNotifications((liste) => liste.map((n) => (n.id === notification.id ? { ...n, lue: true } : n)));
      setNombre((n) => Math.max(0, n - 1));
    }
  }

  function basculer() {
    const prochain = !ouverte;
    setOuverte(prochain);
    if (prochain) chargerListe();
  }

  // Compteur au chargement, puis toutes les 30 secondes.
  useEffect(() => {
    chargerNombre();
    const minuteur = setInterval(chargerNombre, 30000);
    return () => clearInterval(minuteur);
  }, []);

  // Fermer la liste quand on clique ailleurs.
  useEffect(() => {
    if (!ouverte) return undefined;
    function clicDehors(e) {
      if (zone.current && !zone.current.contains(e.target)) setOuverte(false);
    }
    document.addEventListener('mousedown', clicDehors);
    return () => document.removeEventListener('mousedown', clicDehors);
  }, [ouverte]);

  return (
    <div className="cloche" ref={zone}>
      <button
        type="button"
        className="cloche-bouton"
        onClick={basculer}
        aria-label={`Notifications, ${nombre} non lue${nombre > 1 ? 's' : ''}`}
        aria-expanded={ouverte}
      >
        <svg viewBox="0 0 24 24" width="20" height="20" aria-hidden="true">
          <path
            fill="currentColor"
            d="M12 22a2.5 2.5 0 0 0 2.45-2h-4.9A2.5 2.5 0 0 0 12 22Zm7-6V11a7 7 0 0 0-5.5-6.84V3.5a1.5 1.5 0 0 0-3 0v.66A7 7 0 0 0 5 11v5l-2 2v1h18v-1l-2-2Z"
          />
        </svg>
        {nombre > 0 && <span className="cloche-badge">{nombre > 99 ? '99+' : nombre}</span>}
      </button>

      {ouverte && (
        <div className="cloche-panneau" role="dialog" aria-label="Notifications">
          <p className="cloche-titre">Notifications</p>
          {chargement && <p className="cloche-vide">Chargement…</p>}
          {erreur && <p className="cloche-vide">{erreur}</p>}
          {!chargement && !erreur && notifications.length === 0 && (
            <p className="cloche-vide">Aucune notification.</p>
          )}
          {!chargement && !erreur && notifications.length > 0 && (
            <ul className="cloche-liste">
              {notifications.map((n) => (
                <li key={n.id}>
                  <button
                    type="button"
                    className={n.lue ? 'cloche-item' : 'cloche-item non-lue'}
                    onClick={() => lire(n)}
                  >
                    <span className="cloche-message">{n.message}</span>
                    <span className="cloche-date">{new Date(n.creeLe).toLocaleString('fr-FR')}</span>
                  </button>
                </li>
              ))}
            </ul>
          )}
        </div>
      )}
    </div>
  );
}