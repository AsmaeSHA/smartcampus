import { useState } from 'react';

// Formulaire de création d'un compte par l'administrateur (SC-27).
// Envoie POST /api/admin/comptes ; le serveur renvoie le compte et son mot de passe temporaire.

const ROLES = [
  { valeur: 'ETUDIANT', libelle: 'Étudiant' },
  { valeur: 'ENSEIGNANT', libelle: 'Enseignant' },
  { valeur: 'ADMINISTRATEUR', libelle: 'Administrateur' },
];

// Domaine e-mail attendu pour chaque rôle (R1). Le serveur fait la vraie vérification.
const DOMAINES = {
  ETUDIANT: 'usms.ac.ma',
  ENSEIGNANT: 'usms.ma',
  ADMINISTRATEUR: 'ensakh.ma',
};

const VIDE = {
  prenom: '',
  nom: '',
  email: '',
  numero: '',
  codeMassar: '',
  niveauId: '',
  filiere: '',
  codeEnseignant: '',
  grade: '',
  specialite: '',
};

export default function CreerCompteForm({ niveaux, onCree }) {
  const [role, setRole] = useState('ETUDIANT');
  const [champs, setChamps] = useState(VIDE);
  const [erreur, setErreur] = useState('');
  const [envoi, setEnvoi] = useState(false);

  function modifier(nom) {
    return (e) => setChamps((c) => ({ ...c, [nom]: e.target.value }));
  }

  function construireDemande() {
    const demande = {
      prenom: champs.prenom,
      nom: champs.nom,
      email: champs.email,
      role,
    };
    if (role === 'ETUDIANT') {
      Object.assign(demande, {
        codeMassar: champs.codeMassar,
        niveauId: champs.niveauId ? Number(champs.niveauId) : null,
        filiere: champs.filiere,
        numero: champs.numero,
      });
    }
    if (role === 'ENSEIGNANT') {
      Object.assign(demande, {
        codeEnseignant: champs.codeEnseignant,
        grade: champs.grade,
        specialite: champs.specialite,
        numero: champs.numero,
      });
    }
    return demande;
  }

  async function envoyer(event) {
    event.preventDefault();
    setErreur('');

    if (!champs.prenom.trim() || !champs.nom.trim() || !champs.email.trim()) {
      setErreur('Renseignez le prénom, le nom et l’e-mail.');
      return;
    }

    setEnvoi(true);
    try {
      const response = await fetch('/api/admin/comptes', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(construireDemande()),
      });
      const data = await response.json().catch(() => ({}));

      if (response.ok) {
        setChamps(VIDE);
        onCree(data);
        return;
      }
      setErreur(data.message || 'Création impossible. Réessayez plus tard.');
    } catch (err) {
      setErreur('Le serveur ne répond pas. Vérifiez que le backend est démarré.');
    } finally {
      setEnvoi(false);
    }
  }

  return (
    <form className="card compte-form" onSubmit={envoyer} noValidate>
      <h2>Nouveau compte</h2>

      <fieldset className="segmented">
        <legend>Rôle</legend>
        {ROLES.map((r) => (
          <label key={r.valeur} className={role === r.valeur ? 'actif' : ''}>
            <input
              type="radio"
              name="role"
              value={r.valeur}
              checked={role === r.valeur}
              onChange={() => setRole(r.valeur)}
            />
            {r.libelle}
          </label>
        ))}
      </fieldset>

      <div className="grille-2">
        <Champ id="prenom" label="Prénom" value={champs.prenom} onChange={modifier('prenom')} />
        <Champ id="nom" label="Nom" value={champs.nom} onChange={modifier('nom')} />
      </div>
      <Champ
        id="email"
        label="E-mail institutionnel"
        type="email"
        placeholder={`prenom.nom@${DOMAINES[role]}`}
        value={champs.email}
        onChange={modifier('email')}
      />

      {role === 'ETUDIANT' && (
        <>
          <div className="grille-2">
            <Champ id="codeMassar" label="Code Massar" value={champs.codeMassar} onChange={modifier('codeMassar')} />
            <Champ id="numero" label="Numéro" value={champs.numero} onChange={modifier('numero')} />
          </div>
          <div className="grille-2">
            <Champ id="filiere" label="Filière" value={champs.filiere} onChange={modifier('filiere')} />
            <div className="field">
              <label htmlFor="niveauId">Niveau</label>
              <select id="niveauId" value={champs.niveauId} onChange={modifier('niveauId')}>
                <option value="">Choisir…</option>
                {niveaux.map((n) => (
                  <option key={n.id} value={n.id}>{n.nom}</option>
                ))}
              </select>
            </div>
          </div>
        </>
      )}

      {role === 'ENSEIGNANT' && (
        <>
          <div className="grille-2">
            <Champ
              id="codeEnseignant"
              label="Code enseignant"
              value={champs.codeEnseignant}
              onChange={modifier('codeEnseignant')}
            />
            <Champ id="numero" label="Numéro" value={champs.numero} onChange={modifier('numero')} />
          </div>
          <div className="grille-2">
            <Champ id="grade" label="Grade" value={champs.grade} onChange={modifier('grade')} />
            <Champ id="specialite" label="Spécialité" value={champs.specialite} onChange={modifier('specialite')} />
          </div>
        </>
      )}

      {erreur && (
        <p role="alert" className="error">
          <span>{erreur}</span>
        </p>
      )}

      <button type="submit" className="submit" disabled={envoi}>
        {envoi ? 'Création en cours…' : 'Créer le compte'}
      </button>
    </form>
  );
}

function Champ({ id, label, type = 'text', ...props }) {
  return (
    <div className="field">
      <label htmlFor={id}>{label}</label>
      <input id={id} type={type} autoComplete="off" {...props} />
    </div>
  );
}
