import { useEffect, useRef } from 'react';

// Panneau latéral (tiroir) pour le formulaire de compte et l'import CSV.
// Accessibilité : rôle « dialog », fermeture par Échap ou par le fond, focus gardé dans le panneau.
export default function Panneau({ titre, sousTitre, onFermer, children }) {
  const panneau = useRef(null);
  // Garde la dernière version de onFermer sans relancer l'effet (sinon le focus sauterait à chaque rendu).
  const fermer = useRef(onFermer);
  fermer.current = onFermer;

  useEffect(() => {
    const precedent = document.activeElement;
    const premierChamp = panneau.current?.querySelector('input:not([type="radio"]), select, button');
    premierChamp?.focus();
    document.body.style.overflow = 'hidden';

    function clavier(event) {
      if (event.key === 'Escape') {
        fermer.current();
        return;
      }
      if (event.key !== 'Tab' || !panneau.current) {
        return;
      }
      // Garde le focus dans le panneau pendant la navigation au clavier.
      const focusables = panneau.current.querySelectorAll(
        'button:not([disabled]), input:not([disabled]), select:not([disabled]), [href]'
      );
      if (focusables.length === 0) {
        return;
      }
      const premier = focusables[0];
      const dernier = focusables[focusables.length - 1];
      if (event.shiftKey && document.activeElement === premier) {
        event.preventDefault();
        dernier.focus();
      } else if (!event.shiftKey && document.activeElement === dernier) {
        event.preventDefault();
        premier.focus();
      }
    }

    document.addEventListener('keydown', clavier);
    return () => {
      document.removeEventListener('keydown', clavier);
      document.body.style.overflow = '';
      precedent?.focus?.();
    };
  }, []);

  return (
    <div className="panneau-fond" onMouseDown={(e) => e.target === e.currentTarget && onFermer()}>
      <aside
        ref={panneau}
        className="panneau"
        role="dialog"
        aria-modal="true"
        aria-labelledby="panneau-titre"
      >
        <header className="panneau-entete">
          <div>
            <h2 id="panneau-titre">{titre}</h2>
            {sousTitre && <p>{sousTitre}</p>}
          </div>
          <button type="button" className="panneau-fermer" onClick={onFermer} aria-label="Fermer le panneau">
            <svg viewBox="0 0 20 20" width="18" height="18" aria-hidden="true">
              <path d="M5 5l10 10M15 5L5 15" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" />
            </svg>
          </button>
        </header>
        <div className="panneau-corps">{children}</div>
      </aside>
    </div>
  );
}
