import CampusMap from './CampusMap.jsx';
import LoginForm from './LoginForm.jsx';

export default function App() {
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
        <LoginForm />
      </main>
    </div>
  );
}
