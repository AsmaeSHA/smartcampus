import { useState } from 'react';

// Bouton de déconnexion (SC-5). Ferme la session côté serveur, puis prévient le parent.
export default function LogoutButton({ onLogout }) {
  const [loading, setLoading] = useState(false);

  async function handleClick() {
    setLoading(true);
    try {
      await fetch('/api/auth/logout', { method: 'POST' });
    } finally {
      setLoading(false);
      onLogout();
    }
  }

  return (
    <button type="button" className="logout" onClick={handleClick} disabled={loading}>
      {loading ? 'Déconnexion…' : 'Se déconnecter'}
    </button>
  );
}
