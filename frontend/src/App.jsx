import { useEffect, useState } from 'react';
import CampusMap from './CampusMap.jsx';
import LoginForm from './LoginForm.jsx';
import LogoutButton from './LogoutButton.jsx';

export default function App() {
  // undefined : vérification en cours ; null : non connecté ; objet : utilisateur connecté.
  const [utilisateur, setUtilisateur] = useState(undefined);

  useEffect(() => {
    fetch('/api/auth/me')
      .then((response) => (response.ok ? response.json() : null))
      .then((data) => setUtilisateur(data))
      .catch(() => setUtilisateur(null));
  }, []);

  return (
    <div className="shell">
      <aside className="campus">
        <p className="brand">SmartCampus</p>
        <div className="campus-intro">
          <h1>Tout le campus, dans une seule connexion.</h1>
          <CampusMap />
        </div>
      </aside>

      <main className="panel">
        {utilisateur === undefined && <p>Chargement…</p>}
        {utilisateur === null && <LoginForm />}
        {utilisateur && (
          <section className="session">
            <h2>Bienvenue</h2>
            <p>Connecté avec {utilisateur.email}</p>
            <LogoutButton onLogout={() => setUtilisateur(null)} />
          </section>
        )}
      </main>
    </div>
  );
}
