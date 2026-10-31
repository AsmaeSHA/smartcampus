// Liste des comptes (GET /api/admin/comptes). Aucun mot de passe n'est renvoyé par le serveur.

const LIBELLES_ROLE = {
  ETUDIANT: 'Étudiant',
  ENSEIGNANT: 'Enseignant',
  ADMINISTRATEUR: 'Administrateur',
};

export default function ListeComptes({ comptes, chargement }) {
  return (
    <section className="card liste-comptes" aria-labelledby="titre-liste">
      <h2 id="titre-liste">
        Comptes <span className="compteur">{comptes.length}</span>
      </h2>

      {chargement && <p className="muted">Chargement…</p>}
      {!chargement && comptes.length === 0 && <p className="muted">Aucun compte pour l’instant.</p>}

      {comptes.length > 0 && (
        <div className="table-scroll">
          <table>
            <thead>
              <tr>
                <th scope="col">Nom</th>
                <th scope="col">E-mail</th>
                <th scope="col">Rôle</th>
                <th scope="col">État</th>
              </tr>
            </thead>
            <tbody>
              {comptes.map((c) => (
                <tr key={c.id}>
                  <td>{c.nom} {c.prenom}</td>
                  <td className="email">{c.email}</td>
                  <td>{LIBELLES_ROLE[c.role] || c.role}</td>
                  <td>
                    <span className={c.actif ? 'badge actif' : 'badge inactif'}>
                      {c.actif ? 'Actif' : 'Désactivé'}
                    </span>
                    {c.changerMotDePasse && <span className="badge attente">Mot de passe à changer</span>}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </section>
  );
}
