import { useEffect, useState } from 'react';


const LIBELLES_TYPE = {
  SALLE_COURS: 'Salle de cours',
  SALLE_REUNION: 'Salle de réunion',
  AMPHITHEATRE: 'Amphithéâtre',
  BIBLIOTHEQUE: 'Bibliothèque',
  TERRAIN: 'Terrain',
  BUVETTE: 'Buvette',
};

export default function DetailsEspace({ id }) {
  const [espace, setEspace] = useState(null);
  const [chargement, setChargement] = useState(true);
  const [erreur, setErreur] = useState('');

//  useEffect (() => {
//     fetch(`/api/espaces/${id}`, { credentials: 'include' })
//       .then((res) => {
//         if (res.status === 401) throw new Error('Non connecté');
//         if (!res.ok) throw new Error('Introuvable');
//         return res.json();
//       })
//       .then((data) => {
//         setEspace(data);
//         setChargement(false);
//       })
//       .catch((err) => {
//         setErreur(err.message === 'Non connecté' ? 'Veuillez vous connecter.' : 'Impossible de charger cet espace.');
//         setChargement(false);
//       });
//   }, [id]);

useEffect(() => {
  let actif = true;

  async function chargerDetails() {
    setChargement(true);
    setErreur('');

    try {
      const res = await fetch(`/api/espaces/${id}`, {
        credentials: 'include',
      });

      if (res.status === 401) {
        throw new Error('Non connecté');
      }

      if (!res.ok) {
        throw new Error('Introuvable');
      }

      const data = await res.json();

      if (actif) {
        setEspace(data);
      }
    } catch (err) {
      if (actif) {
        setErreur(
          err.message === 'Non connecté'
            ? 'Veuillez vous connecter.'
            : 'Impossible de charger cet espace.'
        );
      }
    } finally {
      if (actif) {
        setChargement(false);
      }
    }
  }

  chargerDetails();

  return () => {
    actif = false;
  };
}, [id]);

  function retourListe() {
    window.history.pushState({}, '', '/espaces');
    window.dispatchEvent(new PopStateEvent('popstate'));
  }

  if (chargement) return <p className="vide">Chargement des détails…</p>;
  if (erreur) return <div className="vide"><p role="alert" className="error">{erreur}</p></div>;
  if (!espace) return null;

  const actif = espace.statut === 'ACTIVE';

  return (
    <div style={{ padding: '2rem', maxWidth: '750px', margin: '0 auto' }}>
      <button 
        type="button"
        className="bouton-secondaire" 
        onClick={retourListe}
        style={{ marginBottom: '1.5rem' }}
      >
        ← Retour à la liste
      </button>

      <div className="liste-comptes" style={{ padding: '2rem' }}>
        <h1 style={{ marginTop: 0, color: 'var(--vert-foret)', fontFamily: 'var(--serif)' }}>
          Fiche de l'espace
        </h1>
        
        <div style={{ display: 'grid', gap: '1rem', marginTop: '2rem', fontSize: '1.05rem' }}>
          <p><strong>Code :</strong> <span className="code-espace">{espace.code}</span></p>
          <p><strong>Nom :</strong> <span className="nom-espace">{espace.nom}</span></p>
          <p><strong>Type :</strong> <span className="type-espace">{LIBELLES_TYPE[espace.typeEspace] || espace.typeEspace}</span></p>
          <p>
            <strong>État :</strong>{' '}
            <span className={actif ? 'etat etat-actif' : 'etat etat-inactif'}>
              <span className="point" aria-hidden="true" />
              {actif ? 'Active' : 'En maintenance'}
            </span>
          </p>
        </div>

        {/* Sous-tâche SC-87 : lien vers les disponibilités */}
        <div style={{ marginTop: '3rem', borderTop: '1px solid var(--trait-doux)', paddingTop: '1.5rem' }}>
          <button 
            type="button"
            className="bouton-menthe" 
            onClick={() => alert("La gestion des disponibilités sera disponible dans le Sprint Réservations !")}
          >
            Voir les disponibilités
          </button>
        </div>
      </div>
    </div>
  );
}