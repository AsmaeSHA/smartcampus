import { useEffect, useState } from 'react';
import CampusMap from './CampusMap.jsx';
import LoginForm from './LoginForm.jsx';
import LogoutButton from './LogoutButton.jsx';
import AdminComptes from './admin/AdminComptes.jsx';

export default function App() {
  // undefined : vérification en cours ; null : non connecté ; objet : utilisateur connecté.
  const [utilisateur, setUtilisateur] = useState(undefined);

  useEffect(() => {
    fetch('/api/auth/me')
      .then((response) => (response.ok ? response.json() : null))
      .then((data) => setUtilisateur(data))
      .catch(() => setUtilisateur(null));
  }, []);

  // L'administrateur arrive sur la gestion des comptes (SC-27).
  // Le rôle sert seulement à l'affichage : les droits sont vérifiés par le serveur (R13).
  if (utilisateur && utilisateur.role === 'ADMINISTRATEUR') {
    return (
      <div className="admin-shell">
        <header className="admin-top">
          <p className="brand">SmartCampus</p>
          <div className="admin-user">
            <span>{utilisateur.prenom} {utilisateur.nom} · Administrateur</span>
            <LogoutButton onLogout={() => setUtilisateur(null)} />
          </div>
        </header>
        <main className="admin-main">
          <AdminComptes emailConnecte={utilisateur.email} />
        </main>
      </div>
    );
  }

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
            <h2>Bienvenue{utilisateur.prenom ? `, ${utilisateur.prenom}` : ''}</h2>
            <p>Connecté avec {utilisateur.email}</p>
            <LogoutButton onLogout={() => setUtilisateur(null)} />
          </section>
        )}
      </main>
    </div>
  );
}
