import { useCallback, useEffect, useMemo, useState } from 'react';
import CreerCompteForm from './CreerCompteForm.jsx';
import CreerEspaceForm from './CreerEspaceForm.jsx';
import ModifierEspaceForm from './ModifierEspaceForm.jsx';
import ListeEspaces from './ListeEspaces.jsx';
import ImportComptes from './ImportComptes.jsx';
import ListeComptes from './ListeComptes.jsx';
import Panneau from './Panneau.jsx';
import './admin.css';

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
  const [dernierReinitialise, setDernierReinitialise] = useState(null);
  const [copie, setCopie] = useState(false);
  const [filtreRole, setFiltreRole] = useState('TOUS');
  const [panneau, setPanneau] = useState(null);
  const [enModification, setEnModification] = useState(null);
  const [espaceEnModification, setEspaceEnModification] = useState(null);
  const [actualisationEspaces, setActualisationEspaces] = useState(0);

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
    } catch (error) {
      setErreur('Le serveur ne répond pas. Vérifiez que le backend est démarré.');
    } finally {
      setChargement(false);
    }
  }, []);

  useEffect(() => { charger(); }, [charger]);

  const compteurs = useMemo(() => {
    const resultat = { TOUS: comptes.length, ETUDIANT: 0, ENSEIGNANT: 0, ADMINISTRATEUR: 0 };
    comptes.forEach((compte) => { resultat[compte.role] += 1; });
    return resultat;
  }, [comptes]);

  function fermerPanneau() {
    setPanneau(null);
    setEnModification(null);
    setEspaceEnModification(null);
  }

  function compteCree(resultat) {
    fermerPanneau();
    setDernierCree(resultat);
    setCopie(false);
    charger();
  }

  function espaceCree(resultat) {
    fermerPanneau();
    setMessage(`L’espace ${resultat.code} a été créé et est actif.`);
    setActualisationEspaces((valeur) => valeur + 1);
  }

  function espaceModifie(resultat) {
    fermerPanneau();
    setMessage(`L’espace ${resultat.code} a été modifié.`);
    setActualisationEspaces((valeur) => valeur + 1);
  }

  function ouvrirModificationEspace(espace) {
    setEspaceEnModification(espace);
    setPanneau('modifier-espace');
  }

  async function ouvrirModification(compte) {
    try {
      const response = await fetch(`/api/admin/comptes/${compte.id}`);
      const data = await response.json().catch(() => ({}));
      if (!response.ok) {
        setErreur(data.message || 'Impossible de charger ce compte.');
        return;
      }

      setEnModification(data);
      setPanneau('modifier');
    } catch (error) {
      setErreur('Le serveur ne répond pas. Vérifiez que le backend est démarré.');
    }
  }

  function compteModifie(resultat) {
    fermerPanneau();
    setMessage(`Le compte de ${resultat.prenom} ${resultat.nom} a été modifié.`);
    setComptes((liste) => liste.map((compte) => compte.id === resultat.id ? resultat : compte));
  }

  async function changerActivation(compte) {
    const action = compte.actif ? 'desactiver' : 'reactiver';
    if (!window.confirm(compte.actif
      ? `Désactiver le compte de ${compte.prenom} ${compte.nom} ?`
      : `Réactiver le compte de ${compte.prenom} ${compte.nom} ?`)) return;
    try {
      const response = await fetch(`/api/admin/comptes/${compte.id}/${action}`, { method: 'PATCH' });
      const data = await response.json().catch(() => ({}));
      if (!response.ok) {
        setErreur(data.message || 'Action impossible. Réessayez plus tard.');
        return;
      }
      setComptes((liste) => liste.map((element) => element.id === data.id ? data : element));
      setMessage(`Le compte de ${data.prenom} ${data.nom} a été ${data.actif ? 'réactivé' : 'désactivé'}.`);
    } catch (error) {
      setErreur('Le serveur ne répond pas. Vérifiez que le backend est démarré.');
    }
  }

  async function reinitialiserMotDePasse(compte) {
    if (!window.confirm(`Générer un mot de passe provisoire pour ${compte.prenom} ${compte.nom} ?`)) return;
    try {
      const response = await fetch(`/api/admin/comptes/${compte.id}/reinitialiser-mot-de-passe`, { method: 'POST' });
      const data = await response.json().catch(() => ({}));
      if (!response.ok) {
        setErreur(data.message || 'Réinitialisation impossible.');
        return;
      }
      setDernierReinitialise(data);
      setCopie(false);
      setComptes((liste) => liste.map((element) => element.id === data.compte.id ? data.compte : element));
    } catch (error) {
      setErreur('Le serveur ne répond pas. Vérifiez que le backend est démarré.');
    }
  }

  async function copier(valeur) {
    try {
      await navigator.clipboard.writeText(valeur);
      setCopie(true);
    } catch (error) {
      setCopie(false);
    }
  }

  return (
    <div className="admin-comptes">
      <section className="bandeau">
        <div className="bandeau-contenu">
          <div>
            <p className="surtitre">Administration</p>
            <h1>Gestion des comptes et des espaces</h1>
            <p className="bandeau-texte">{compteurs.TOUS} compte{compteurs.TOUS > 1 ? 's' : ''}</p>
          </div>
          <div className="bandeau-actions">
            <button type="button" className="bouton-clair" onClick={() => setPanneau('importer')}>Importer un CSV</button>
            <button type="button" className="bouton-clair" onClick={() => setPanneau('espace')}>Créer un espace</button>
            <button type="button" className="bouton-menthe" onClick={() => setPanneau('creer')}>Nouveau compte</button>
          </div>
        </div>
      </section>

      <div className="admin-contenu">
        <div className="tuiles" role="group" aria-label="Filtrer par rôle">
          {TUILES.map((tuile) => (
            <button key={tuile.role} type="button" className={filtreRole === tuile.role ? 'tuile active' : 'tuile'}
              aria-pressed={filtreRole === tuile.role} onClick={() => setFiltreRole(tuile.role)}>
              <span className="tuile-nombre">{compteurs[tuile.role]}</span>
              <span className="tuile-libelle">{tuile.libelle}</span>
            </button>
          ))}
        </div>

        {erreur && <p role="alert" className="alerte alerte-erreur">{erreur}</p>}
        {message && <p role="status" className="alerte alerte-succes">{message}
          <button type="button" className="alerte-fermer" onClick={() => setMessage('')} aria-label="Fermer">×</button>
        </p>}
        {dernierCree && (
          <div role="status" className="alerte alerte-succes alerte-mot-de-passe">
            <p>Compte créé pour <strong>{dernierCree.compte.email}</strong>. Transmettez ce mot de passe temporaire.</p>
            <code>{dernierCree.motDePasseTemporaire}</code>
            <button type="button" className="bouton-secondaire" onClick={() => copier(dernierCree.motDePasseTemporaire)}>{copie ? 'Copié ✓' : 'Copier'}</button>
          </div>
        )}
        {dernierReinitialise && (
          <div role="status" className="alerte alerte-succes">
            Mot de passe provisoire : <code>{dernierReinitialise.motDePasseTemporaire}</code>
            <button type="button" className="bouton-secondaire" onClick={() => copier(dernierReinitialise.motDePasseTemporaire)}>{copie ? 'Copié ✓' : 'Copier'}</button>
          </div>
        )}
        <ListeComptes comptes={comptes} chargement={chargement} filtreRole={filtreRole}
          emailConnecte={emailConnecte} onModifier={ouvrirModification}
          onChangerActivation={changerActivation} onReinitialiserMotDePasse={reinitialiserMotDePasse}
          onEffacerRole={() => setFiltreRole('TOUS')} />
        <ListeEspaces onModifier={ouvrirModificationEspace} actualisation={actualisationEspaces} />
      </div>

      {panneau && (
        <Panneau
          titre={panneau === 'espace' ? 'Créer un espace' : panneau === 'modifier-espace' ? 'Modifier l’espace' : panneau === 'modifier' ? 'Modifier le compte' : panneau === 'importer' ? 'Importer depuis un CSV' : 'Nouveau compte'}
          sousTitre={panneau === 'espace' ? 'L’espace sera créé avec le statut actif.' : panneau === 'modifier-espace' ? espaceEnModification?.code : ''}
          onFermer={fermerPanneau}
        >
          {panneau === 'espace' && <CreerEspaceForm onCree={espaceCree} />}
          {panneau === 'modifier-espace' && <ModifierEspaceForm espace={espaceEnModification} onModifie={espaceModifie} onAnnuler={fermerPanneau} />}
          {panneau === 'importer' && <ImportComptes onImporte={charger} />}
          {(panneau === 'creer' || panneau === 'modifier') && (
            <CreerCompteForm niveaux={niveaux} compte={enModification} onCree={compteCree}
              onModifie={compteModifie} onAnnuler={fermerPanneau} />
          )}
        </Panneau>
      )}
    </div>
  );
}
