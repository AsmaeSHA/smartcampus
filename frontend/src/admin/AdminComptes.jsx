import { useEffect, useState } from 'react';
import CreerCompteForm from './CreerCompteForm.jsx';
import ListeComptes from './ListeComptes.jsx';

// Page « Gestion des comptes » de l'administrateur (SC-27).
export default function AdminComptes() {
  const [comptes, setComptes] = useState([]);
  const [niveaux, setNiveaux] = useState([]);
  const [chargement, setChargement] = useState(true);
  const [erreur, setErreur] = useState('');
  const [dernierCree, setDernierCree] = useState(null);
  const [copie, setCopie] = useState(false);

  async function charger() {
    setChargement(true);
    try {
      const [rComptes, rNiveaux] = await Promise.all([
        fetch('/api/admin/comptes'),
        fetch('/api/admin/niveaux'),
      ]);
      if (!rComptes.ok || !rNiveaux.ok) {
        setErreur('Impossible de charger les comptes. Votre session a peut-être expiré.');
        return;
      }
      setComptes(await rComptes.json());
      setNiveaux(await rNiveaux.json());
      setErreur('');
    } catch (err) {
      setErreur('Le serveur ne répond pas. Vérifiez que le backend est démarré.');
    } finally {
      setChargement(false);
    }
  }

  useEffect(() => {
    charger();
  }, []);

  function compteCree(resultat) {
    setDernierCree(resultat);
    setCopie(false);
    charger();
  }

  async function copier() {
    try {
      await navigator.clipboard.writeText(dernierCree.motDePasseTemporaire);
      setCopie(true);
    } catch (err) {
      setCopie(false);
    }
  }

  return (
    <div className="admin-comptes">
      <header className="admin-titre">
        <h1>Gestion des comptes</h1>
        <p>Créez les comptes des étudiants, des enseignants et des administrateurs.</p>
      </header>

      {erreur && (
        <p role="alert" className="error">
          <span>{erreur}</span>
        </p>
      )}

      {dernierCree && (
        <div className="succes" role="status">
          <p>
            Compte créé pour <strong>{dernierCree.compte.email}</strong>. Transmettez ce mot de passe
            temporaire à l’utilisateur : il ne sera plus affiché.
          </p>
          <div className="mot-de-passe">
            <code>{dernierCree.motDePasseTemporaire}</code>
            <button type="button" className="logout" onClick={copier}>
              {copie ? 'Copié' : 'Copier'}
            </button>
          </div>
        </div>
      )}

      <div className="admin-grille">
        <CreerCompteForm niveaux={niveaux} onCree={compteCree} />
        <ListeComptes comptes={comptes} chargement={chargement} />
      </div>
    </div>
  );
}
