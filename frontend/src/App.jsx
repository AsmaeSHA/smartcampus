import { useEffect, useState } from 'react';
import CampusMap from './CampusMap.jsx';
import LoginForm from './LoginForm.jsx';
import LogoutButton from './LogoutButton.jsx';
import AdminComptes from './admin/AdminComptes.jsx';
import ChangerMotDePasse from './ChangerMotDePasse.jsx';
import AccueilEtudiant from './AccueilEtudiant.jsx';
import AccueilEnseignant from './AccueilEnseignant.jsx';

export default function App() {
  // undefined : vérification en cours ; null : non connecté ; objet : utilisateur connecté.
  const [utilisateur, setUtilisateur] = useState(undefined);
  const [chemin, setChemin] = useState(window.location.pathname);

  useEffect(() => {
    const actualiserChemin = () => setChemin(window.location.pathname);
    window.addEventListener('popstate', actualiserChemin);
    return () => window.removeEventListener('popstate', actualiserChemin);
  }, []);

  useEffect(() => {
    fetch('/api/auth/me')
      .then((response) => (response.ok ? response.json() : null))
      .then((data) => setUtilisateur(data))
      .catch(() => setUtilisateur(null));
  }, []);

  function naviguer(vers) {
    window.history.pushState({}, '', vers);
    setChemin(vers);
  }

  if (utilisateur && utilisateur.changerMotDePasse) {
    if (chemin !== '/mot-de-passe') {
      window.history.replaceState({}, '', '/mot-de-passe');
    }
    return (
      <div className="shell">
        <aside className="campus">
          <p className="brand">SmartCampus</p>
          <div className="campus-intro"><h1>Tout le campus, dans une seule connexion.</h1><CampusMap /></div>
        </aside>
        <main className="panel">
          <ChangerMotDePasse obligatoire onChange={() => { window.history.replaceState({}, '', '/accueil'); window.location.reload(); }} />
        </main>
      </div>
    );
  }

  // L'administrateur arrive sur la gestion des comptes (SC-27).
  // Le rôle sert seulement à l'affichage : les droits sont vérifiés par le serveur (R13).
  if (utilisateur && utilisateur.role === 'ADMINISTRATEUR') {
    if (chemin !== '/accueil') {
      window.history.replaceState({}, '', '/accueil');
    }
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

  if (utilisateur && chemin === '/mot-de-passe') {
    return (
      <div className="shell">
        <aside className="campus">
          <p className="brand">SmartCampus</p>
          <div className="campus-intro"><h1>Tout le campus, dans une seule connexion.</h1><CampusMap /></div>
        </aside>
        <main className="panel">
          <ChangerMotDePasse onChange={() => { window.history.replaceState({}, '', '/accueil'); window.location.reload(); }} />
        </main>
      </div>
    );
  }

  if (utilisateur && utilisateur.role === 'ETUDIANT') {
    return <AccueilEtudiant utilisateur={utilisateur} onChangerMotDePasse={() => naviguer('/mot-de-passe')} />;
  }

  if (utilisateur && utilisateur.role === 'ENSEIGNANT') {
    return <AccueilEnseignant utilisateur={utilisateur} onChangerMotDePasse={() => naviguer('/mot-de-passe')} />;
  }

  if (utilisateur && chemin !== '/accueil') {
    window.history.replaceState({}, '', '/accueil');
  }
  if (utilisateur === null && chemin !== '/connexion') {
    window.history.replaceState({}, '', '/connexion');
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
            <button type="button" className="action" onClick={() => naviguer('/mot-de-passe')}>Changer mon mot de passe</button>
          </section>
        )}
      </main>
    </div>
  );
}
