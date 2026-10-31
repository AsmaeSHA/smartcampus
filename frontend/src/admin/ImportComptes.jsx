import { useState } from 'react';

// Import de comptes depuis un fichier CSV (SC-53).
// Envoie le fichier à POST /api/admin/comptes/import. Les lignes valides sont créées,
// les autres sont listées avec la raison du refus.

const ENTETE = [
  'role', 'prenom', 'nom', 'email', 'numero', 'codeMassar', 'niveau', 'filiere',
  'codeEnseignant', 'grade', 'specialite',
];

const EXEMPLES = [
  ['etudiant', 'Sara', 'Alami', 'sara.alami@usms.ac.ma', '0001', 'D123456789', 'GI3', 'Génie Informatique', '', '', ''],
  ['enseignant', 'Karim', 'Bennani', 'karim.bennani@usms.ma', '0102', '', '', '', 'E1001', 'PES', 'Informatique'],
  ['administrateur', 'Nadia', 'Idrissi', 'nadia.idrissi@ensakh.ma', '', '', '', '', '', '', ''],
];

// Fichier CSV lisible par Excel en français : séparateur « ; » et BOM UTF-8 pour les accents.
function telecharger(nomFichier, lignes) {
  const texte = lignes
    .map((ligne) => ligne.map((v) => `"${String(v ?? '').replace(/"/g, '""')}"`).join(';'))
    .join('\r\n');
  const blob = new Blob(['﻿', texte], { type: 'text/csv;charset=utf-8' });
  const lien = document.createElement('a');
  lien.href = URL.createObjectURL(blob);
  lien.download = nomFichier;
  lien.click();
  URL.revokeObjectURL(lien.href);
}

export default function ImportComptes({ onImporte }) {
  const [fichier, setFichier] = useState(null);
  const [envoi, setEnvoi] = useState(false);
  const [erreur, setErreur] = useState('');
  const [resultat, setResultat] = useState(null);

  async function importer(event) {
    event.preventDefault();
    setErreur('');
    if (!fichier) {
      setErreur('Choisissez un fichier CSV.');
      return;
    }
    if (!fichier.name.toLowerCase().endsWith('.csv')) {
      setErreur('Le fichier doit être au format .csv. Dans Excel : Fichier › Enregistrer sous › CSV.');
      return;
    }

    setEnvoi(true);
    try {
      const donnees = new FormData();
      donnees.append('fichier', fichier);
      const response = await fetch('/api/admin/comptes/import', { method: 'POST', body: donnees });
      const data = await response.json().catch(() => ({}));
      if (!response.ok) {
        setErreur(data.message || 'Import impossible. Vérifiez le fichier.');
        return;
      }
      setResultat(data);
      setFichier(null);
      event.target.reset();
      if (data.crees.length > 0) {
        onImporte();
      }
    } catch (err) {
      setErreur('Le serveur ne répond pas. Vérifiez que le backend est démarré.');
    } finally {
      setEnvoi(false);
    }
  }

  function telechargerMotsDePasse() {
    telecharger('mots-de-passe-temporaires.csv', [
      ['email', 'prenom', 'nom', 'role', 'motDePasseTemporaire'],
      ...resultat.crees.map((c) => [c.email, c.prenom, c.nom, c.role, c.motDePasseTemporaire]),
    ]);
  }

  return (
    <section className="card import-comptes" aria-labelledby="titre-import">
      <h2 id="titre-import">Importer depuis un CSV</h2>
      <p className="muted">
        Une ligne par compte. Les lignes valides sont créées, les autres sont signalées.{' '}
        <button type="button" className="lien" onClick={() => telecharger('modele-comptes.csv', [ENTETE, ...EXEMPLES])}>
          Télécharger le modèle
        </button>
      </p>

      <form className="import-form" onSubmit={importer}>
        <label htmlFor="fichier-csv" className="sr-only">Fichier CSV</label>
        <input
          id="fichier-csv"
          type="file"
          accept=".csv,text/csv"
          onChange={(e) => setFichier(e.target.files[0] || null)}
        />
        <button type="submit" className="action" disabled={envoi}>
          {envoi ? 'Import en cours…' : 'Importer'}
        </button>
      </form>

      {erreur && (
        <p role="alert" className="error">
          <span>{erreur}</span>
        </p>
      )}

      {resultat && (
        <div className="import-resultat" role="status">
          <p>
            <strong>{resultat.crees.length}</strong> compte(s) créé(s) et{' '}
            <strong>{resultat.erreurs.length}</strong> ligne(s) refusée(s), sur {resultat.total}.
          </p>

          {resultat.crees.length > 0 && (
            <div className="succes">
              <p>
                Téléchargez les mots de passe temporaires maintenant : ils ne seront plus affichés.
              </p>
              <button type="button" className="logout" onClick={telechargerMotsDePasse}>
                Télécharger les mots de passe (CSV)
              </button>
            </div>
          )}

          {resultat.erreurs.length > 0 && (
            <div className="table-scroll">
              <table className="table-erreurs">
                <thead>
                  <tr>
                    <th scope="col">Ligne</th>
                    <th scope="col">E-mail</th>
                    <th scope="col">Raison</th>
                  </tr>
                </thead>
                <tbody>
                  {resultat.erreurs.map((e) => (
                    <tr key={e.ligne}>
                      <td>{e.ligne}</td>
                      <td className="email">{e.email || '—'}</td>
                      <td>{e.message}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      )}
    </section>
  );
}
