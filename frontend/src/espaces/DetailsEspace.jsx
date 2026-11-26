import { useEffect, useRef, useState } from 'react';
import './fiche-espace.css';

// Fiche de salle (SC-15) : code, nom, type, état, dates, et accès aux disponibilités (SC-87).
// Même habillage que la page d'administration. Chaque type d'espace a sa couleur et son icône.

const LIBELLES_TYPE = {
  SALLE_COURS: 'Salle de cours',
  SALLE_REUNION: 'Salle de réunion',
  AMPHITHEATRE: 'Amphithéâtre',
  BIBLIOTHEQUE: 'Bibliothèque',
  TERRAIN: 'Terrain',
  BUVETTE: 'Buvette',
};

const LIBELLES_STATUT = {
  ACTIVE: 'Active',
  EN_MAINTENANCE: 'En maintenance',
  ARCHIVEE: 'Archivée',
};

// Couleur principale et teinte claire de chaque type.
const COULEURS = {
  SALLE_COURS: ['#0f8a5f', '#dff5e9'],
  SALLE_REUNION: ['#2563b8', '#e1ecfb'],
  AMPHITHEATRE: ['#7a4bc2', '#efe6fa'],
  BIBLIOTHEQUE: ['#b7791f', '#fbf0d7'],
  TERRAIN: ['#d2571f', '#fde8dc'],
  BUVETTE: ['#c2415f', '#fbe4ea'],
};

function Icone({ children, taille = 20 }) {
  return (
    <svg viewBox="0 0 24 24" width={taille} height={taille} fill="none" stroke="currentColor"
      strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
      {children}
    </svg>
  );
}

const ICONES_TYPE = {
  SALLE_COURS: <><rect x="3" y="4" width="18" height="12" rx="2" /><path d="M8 21l4-5 4 5M7 9h6M7 12h10" /></>,
  SALLE_REUNION: <><circle cx="9" cy="8" r="3" /><circle cx="17" cy="9" r="2.5" /><path d="M3 20c0-3.3 2.7-6 6-6s6 2.7 6 6M15 14.3c3 0 6 1.5 6 5.7" /></>,
  AMPHITHEATRE: <><path d="M3 21h18M5 21V10l7-6 7 6v11M9 21v-5a3 3 0 016 0v5" /></>,
  BIBLIOTHEQUE: <><path d="M4 19V5a2 2 0 012-2h13v16H6a2 2 0 00-2 2 2 2 0 002 2h13M8 7h7" /></>,
  TERRAIN: <><rect x="3" y="5" width="18" height="14" rx="2" /><path d="M12 5v14" /><circle cx="12" cy="12" r="2.6" /></>,
  BUVETTE: <><path d="M5 9h12v5a5 5 0 01-5 5h-2a5 5 0 01-5-5zM17 11h2a2 2 0 010 4h-2M8 3v3M12 3v3" /></>,
};

const ICONE_DEFAUT = <path d="M4 21V8l8-5 8 5v13M9 21v-6h6v6M4 21h16" />;
const ICONE_CODE = <path d="M5 9h14M5 15h14M10 4L8 20M16 4l-2 16" />;
const ICONE_ETAT = <><circle cx="12" cy="12" r="9" /><path d="M8 12.5l2.7 2.7L16 9.8" /></>;
const ICONE_DATE = <><rect x="4" y="5" width="16" height="15" rx="2" /><path d="M4 10h16M9 3v4M15 3v4" /></>;

function formaterDate(valeur) {
  const date = new Date(valeur);
  if (!valeur || Number.isNaN(date.getTime())) return '—';
  return date.toLocaleDateString('fr-FR', { day: 'numeric', month: 'long', year: 'numeric' });
}

function ilYa(valeur) {
  const date = new Date(valeur);
  if (!valeur || Number.isNaN(date.getTime())) return '';
  const jours = Math.round((date.getTime() - Date.now()) / 86400000);
  const rtf = new Intl.RelativeTimeFormat('fr', { numeric: 'auto' });
  if (Math.abs(jours) >= 365) return rtf.format(Math.round(jours / 365), 'year');
  if (Math.abs(jours) >= 30) return rtf.format(Math.round(jours / 30), 'month');
  return rtf.format(jours, 'day');
}

export default function DetailsEspace({ id, onRetour, onNonConnecte }) {
  const [espace, setEspace] = useState(null);
  const [chargement, setChargement] = useState(true);
  const [erreur, setErreur] = useState('');
  const [bientot, setBientot] = useState(false);

  const nonConnecteRef = useRef(onNonConnecte);
  nonConnecteRef.current = onNonConnecte;

  useEffect(() => {
    let actif = true;

    async function chargerDetails() {
      setChargement(true);
      setErreur('');
      setBientot(false);
      try {
        const res = await fetch(`/api/espaces/${id}`, { credentials: 'include' });

        if (res.status === 401) {
          nonConnecteRef.current?.();
          return;
        }
        if (!res.ok) {
          const data = await res.json().catch(() => ({}));
          if (actif) setErreur(data.message || 'Impossible de charger cet espace.');
          return;
        }
        const data = await res.json();
        if (actif) setEspace(data);
      } catch (e) {
        if (actif) setErreur('Le serveur ne répond pas. Vérifiez que le backend est démarré.');
      } finally {
        if (actif) setChargement(false);
      }
    }

    chargerDetails();
    return () => {
      actif = false;
    };
  }, [id]);

  function retour() {
    if (onRetour) {
      onRetour();
      return;
    }
    window.history.pushState({}, '', '/espaces');
    window.dispatchEvent(new PopStateEvent('popstate'));
  }

  if (chargement || erreur || !espace) {
    return (
      <div className="fiche-page">
        <div className="fiche-simple">
          <button type="button" className="bouton-secondaire" onClick={retour}>
            ← Retour aux espaces
          </button>
          {chargement && <p className="vide">Chargement de la fiche…</p>}
          {!chargement && erreur && <p role="alert" className="error"><span>{erreur}</span></p>}
        </div>
      </div>
    );
  }

  const actif = espace.statut === 'ACTIVE';
  const libelleType = LIBELLES_TYPE[espace.typeEspace] || espace.typeEspace || 'Non renseigné';
  const libelleStatut = LIBELLES_STATUT[espace.statut] || espace.statut || 'Non renseigné';
  const [accent, accentClair] = COULEURS[espace.typeEspace] || COULEURS.SALLE_COURS;
  const iconeType = ICONES_TYPE[espace.typeEspace] || ICONE_DEFAUT;
  const classeEtat = actif ? 'etat etat-actif' : 'etat etat-inactif';

  return (
    <div className="fiche-page" style={{ '--accent': accent, '--accent-clair': accentClair }}>
      <header className="fiche-hero">
        <div className="fiche-hero-contenu">
          <div>
            <p className="fiche-eyebrow">Espaces</p>
            <h1>Fiche de salle</h1>
            <p className="fiche-sous-titre">{espace.nom} · {espace.code}</p>
          </div>
          <button type="button" className="bouton-menthe" onClick={() => setBientot(true)}>
            Voir les disponibilités
          </button>
        </div>

        <div className="fiche-onglets">
          <button type="button" className="fiche-onglet" onClick={retour}>
            ← Retour aux espaces
          </button>
          <span className="fiche-onglet fiche-onglet-actif" aria-current="page">Fiche de salle</span>
        </div>
      </header>

      <div className="fiche-contenu">
        {bientot && (
          <p className="fiche-note" role="status">
            Les disponibilités seront accessibles avec la fonctionnalité de réservation.
          </p>
        )}

        <section className="fiche-identite" aria-label="Identité de l’espace">
          <span className="fiche-badge-type"><Icone taille={28}>{iconeType}</Icone></span>
          <div className="fiche-identite-texte">
            <p className="fiche-type">{libelleType}</p>
            <h2>{espace.nom}</h2>
          </div>
          <span className={`${classeEtat} fiche-etat`}>
            <span className="point" aria-hidden="true" />
            {libelleStatut}
          </span>
        </section>

        <ul className="fiche-tuiles">
          <li className="fiche-tuile">
            <span className="fiche-icone"><Icone>{ICONE_CODE}</Icone></span>
            <span className="fiche-tuile-texte">
              <span className="fiche-libelle">Code</span>
              <span className="fiche-valeur">{espace.code}</span>
              <span className="fiche-relatif">Identifiant de l’espace</span>
            </span>
          </li>
          <li className="fiche-tuile">
            <span className="fiche-icone"><Icone>{iconeType}</Icone></span>
            <span className="fiche-tuile-texte">
              <span className="fiche-libelle">Type</span>
              <span className="fiche-valeur">{libelleType}</span>
              <span className="fiche-relatif">Catégorie de l’espace</span>
            </span>
          </li>
          <li className="fiche-tuile">
            <span className="fiche-icone"><Icone>{ICONE_ETAT}</Icone></span>
            <span className="fiche-tuile-texte">
              <span className="fiche-libelle">État</span>
              <span className="fiche-valeur">
                <span className={classeEtat}>
                  <span className="point" aria-hidden="true" />
                  {libelleStatut}
                </span>
              </span>
              <span className="fiche-relatif">Statut actuel</span>
            </span>
          </li>
          <li className="fiche-tuile">
            <span className="fiche-icone"><Icone>{ICONE_DATE}</Icone></span>
            <span className="fiche-tuile-texte">
              <span className="fiche-libelle">Modifié le</span>
              <span className="fiche-valeur">{formaterDate(espace.modifieLe)}</span>
              <span className="fiche-relatif">{ilYa(espace.modifieLe)}</span>
            </span>
          </li>
        </ul>
      </div>
    </div>
  );
}
