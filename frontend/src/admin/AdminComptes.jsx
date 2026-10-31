import { useCallback, useEffect, useMemo, useState } from 'react';
import CreerCompteForm from './CreerCompteForm.jsx';
import ImportComptes from './ImportComptes.jsx';
import ListeComptes from './ListeComptes.jsx';
import Panneau from './Panneau.jsx';
import './admin.css';

// Page « Gestion des comptes » de l'administrateur (SC-27, SC-28, SC-64, SC-53).
// La liste occupe toute la largeur ; création, modification et import s'ouvrent dans un panneau latéral.

const TUILES = [
  { role: 'TOUS', libelle: 'Tous les comptes' },
  { role: 'ETUDIANT', libelle: 'Étudiants' },
  { role: 'ENSEIGNANT', libelle: 'Enseignants' },
  { role: 'ADMINISTRATEUR', libelle: 'Administrateurs' },
];

export default function AdminComptes({ emailConnecte }) {
  const [comptes, setComptes] = useState([]);
  const [niveaux, setNiveaux] = useState([]);
  const [chargement, setChargement] = useState(true);
  const [erreur, setErreur] = useState('');
  const [message, setMessage] = useState('');
  const [dernierCree, setDernierCree] = useState(null);
  const [copie, setCopie] = useState(false);
  const [filtreRole, setFiltreRole] = useState('TOUS');
  // null, « creer », « modifier » ou « importer »
  const [panneau, setPanneau] = useState(null);
  const [enModification, setEnModification] = useState(null);

  const charger = useCallback(async () => {
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
  }, []);

  useEffect(() => {
    charger();
  }, [charger]);

  const compteurs = useMemo(() => {
    const parRole = { TOUS: comptes.length, ETUDIANT: 0, ENSEIGNANT: 0, ADMINISTRATEUR: 0 };
    comptes.forEach((c) => {
      parRole[c.role] += 1;
    });
    return parRole;
  }, [comptes]);

  const desactives = comptes.filter((c) => !c.actif).length;

  const fermerPanneau = useCallback(() => {
    setPanneau(null);
    setEnModification(null);
  }, []);

  function ouvrirCreation() {
    setEnModification(null);
    setPanneau('creer');
  }

  function compteCree(resultat) {
    setPanneau(null);
    setMessage('');
    setCopie(false);
    setDernierCree(resultat);
    charger();
  }

  // SC-64 : charge le détail du compte puis ouvre le formulaire pré-rempli.
  async function ouvrirModification(compte) {
    try {
      const response = await fetch(`/api/admin/comptes/${compte.id}`);
      const data = await response.json().catch(() => ({}));
      if (!response.ok) {
        setErreur(data.message || 'Impossible de charger ce compte.');
        return;
      }
      setErreur('');
      setEnModification(data);
      setPanneau('modifier');
    } catch (err) {
      setErreur('Le serveur ne répond pas. Vérifiez que le backend est démarré.');
    }
  }

  function compteModifie(resultat) {
    fermerPanneau();
    setDernierCree(null);
    setMessage(`Le compte de ${resultat.prenom} ${resultat.nom} a été modifié.`);
    setComptes((liste) => liste.map((c) => (c.id === resultat.id ? resultat : c)));
  }

  // SC-28 : désactive ou réactive un compte, après confirmation.
  async function changerActivation(compte) {
    const action = compte.actif ? 'desactiver' : 'reactiver';
    const question = compte.actif
      ? `Désactiver le compte de ${compte.prenom} ${compte.nom} ? Il ne pourra plus se connecter, mais ses données seront conservées.`
      : `Réactiver le compte de ${compte.prenom} ${compte.nom} ?`;
    if (!window.confirm(question)) {
      return;
    }

    try {
      const response = await fetch(`/api/admin/comptes/${compte.id}/${action}`, { method: 'PATCH' });
      const data = await response.json().catch(() => ({}));
      if (!response.ok) {
        setErreur(data.message || 'Action impossible. Réessayez plus tard.');
        return;
      }
      setErreur('');
      setDernierCree(null);
      setMessage(`Le compte de ${data.prenom} ${data.nom} a été ${data.actif ? 'réactivé' : 'désactivé'}.`);
      setComptes((liste) => liste.map((c) => (c.id === data.id ? data : c)));
    } catch (err) {
      setErreur('Le serveur ne répond pas. Vérifiez que le backend est démarré.');
    }
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
      <section className="bandeau">
        <div className="bandeau-contenu">
          <div>
            <p className="surtitre">Administration</p>
            <h1>Gestion des comptes</h1>
            <p className="bandeau-texte">
              {compteurs.TOUS} compte{compteurs.TOUS > 1 ? 's' : ''}
              {desactives > 0 && ` · ${desactives} désactivé${desactives > 1 ? 's' : ''}`}
            </p>
          </div>
          <div className="bandeau-actions">
            <button type="button" className="bouton-clair" onClick={() => setPanneau('importer')}>
              <svg viewBox="0 0 20 20" width="18" height="18" aria-hidden="true">
                <path d="M10 13V3M6 7l4-4 4 4M4 13v3h12v-3" fill="none" stroke="currentColor" strokeWidth="1.7"
                  strokeLinecap="round" strokeLinejoin="round" />
              </svg>
              Importer un CSV
            </button>
            <button type="button" className="bouton-menthe" onClick={ouvrirCreation}>
              <svg viewBox="0 0 20 20" width="18" height="18" aria-hidden="true">
                <path d="M10 4v12M4 10h12" stroke="currentColor" strokeWidth="1.9" strokeLinecap="round" />
              </svg>
              Nouveau compte
            </button>
          </div>
        </div>
      </section>

      <div className="admin-contenu">
        <div className="tuiles" role="group" aria-label="Filtrer par rôle">
          {TUILES.map((t) => (
            <button
              key={t.role}
              type="button"
              className={filtreRole === t.role ? 'tuile active' : 'tuile'}
              aria-pressed={filtreRole === t.role}
              onClick={() => setFiltreRole(t.role)}
            >
              <span className="tuile-nombre">{compteurs[t.role]}</span>
              <span className="tuile-libelle">{t.libelle}</span>
            </button>
          ))}
        </div>

        {erreur && (
          <p role="alert" className="alerte alerte-erreur">
            {erreur}
          </p>
        )}

        {message && (
          <p role="status" className="alerte alerte-succes">
            {message}
            <button type="button" className="alerte-fermer" onClick={() => setMessage('')} aria-label="Fermer le message">
              ×
            </button>
          </p>
        )}

        {dernierCree && (
          <div role="status" className="alerte alerte-succes alerte-mot-de-passe">
            <p>
              Compte créé pour <strong>{dernierCree.compte.email}</strong>. Transmettez ce mot de passe
              temporaire à l’utilisateur : il ne sera plus affiché.
            </p>
            <div className="mot-de-passe">
              <code>{dernierCree.motDePasseTemporaire}</code>
              <button type="button" className="bouton-secondaire" onClick={copier}>
                {copie ? 'Copié ✓' : 'Copier'}
              </button>
              <button type="button" className="alerte-fermer" onClick={() => setDernierCree(null)}
                aria-label="Fermer le message">
                ×
              </button>
            </div>
          </div>
        )}

        <ListeComptes
          comptes={comptes}
          chargement={chargement}
          filtreRole={filtreRole}
          emailConnecte={emailConnecte}
          onModifier={ouvrirModification}
          onChangerActivation={changerActivation}
          onEffacerRole={() => setFiltreRole('TOUS')}
        />
      </div>

      {(panneau === 'creer' || panneau === 'modifier') && (
        <Panneau
          titre={panneau === 'modifier' ? 'Modifier le compte' : 'Nouveau compte'}
          sousTitre={
            panneau === 'modifier'
              ? `${enModification.prenom} ${enModification.nom}`
              : 'Un mot de passe temporaire sera généré.'
          }
          onFermer={fermerPanneau}
        >
          <CreerCompteForm
            key={enModification ? enModification.id : 'nouveau'}
            niveaux={niveaux}
            compte={enModification}
            onCree={compteCree}
            onModifie={compteModifie}
            onAnnuler={fermerPanneau}
          />
        </Panneau>
      )}

      {panneau === 'importer' && (
        <Panneau titre="Importer depuis un CSV" sousTitre="Créez plusieurs comptes en une seule fois." onFermer={fermerPanneau}>
          <ImportComptes onImporte={charger} />
        </Panneau>
      )}
    </div>
  );
}
