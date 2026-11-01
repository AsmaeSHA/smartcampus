import CampusMap from './CampusMap.jsx';
import LogoutButton from './LogoutButton.jsx';

function Info({ label, value }) {
  return <div className="profil-info"><span>{label}</span><strong>{value || 'Non renseigné'}</strong></div>;
}

export default function AccueilEtudiant({ utilisateur, onChangerMotDePasse }) {
  return (
    <div className="role-shell espace-etudiant">
      <header className="role-topbar">
        <a className="brand" href="/accueil">SmartCampus</a>
        <div className="role-user"><span>{utilisateur.prenom} {utilisateur.nom}</span><LogoutButton onLogout={() => window.location.assign('/connexion')} /></div>
      </header>
      <main className="role-main">
        <section className="role-hero">
          <div><p className="eyebrow">ESPACE ÉTUDIANT</p><h1>Bienvenue, {utilisateur.prenom}</h1><p>Retrouvez ici votre profil étudiant et les repères de votre campus.</p></div>
          <div className="role-hero-map"><CampusMap /></div>
        </section>
        <section className="role-content-grid">
          <article className="role-card profil-card">
            <div className="role-card-heading"><div><p className="eyebrow">VOTRE PROFIL</p><h2>Informations étudiantes</h2></div><span className="role-mark">É</span></div>
            <div className="profil-grid">
              <Info label="Filière" value={utilisateur.filiere} />
              <Info label="Niveau" value={utilisateur.niveau} />
              <Info label="Numéro étudiant" value={utilisateur.numero} />
              <Info label="Code Massar" value={utilisateur.codeMassar} />
            </div>
            <div className="profil-email"><span>Adresse institutionnelle</span><strong>{utilisateur.email}</strong></div>
          </article>
          <aside className="role-card account-card">
            <p className="eyebrow">COMPTE</p><h2>Votre sécurité</h2>
            <p>Gardez votre mot de passe confidentiel et choisissez un mot de passe difficile à deviner.</p>
            <button className="role-link-button" type="button" onClick={onChangerMotDePasse}>Changer mon mot de passe <span aria-hidden="true">→</span></button>
          </aside>
        </section>
        <footer className="role-footer">SmartCampus <span>·</span> Votre campus, au même endroit.</footer>
      </main>
    </div>
  );
}
