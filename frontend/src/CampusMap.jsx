// Carte décorative du campus. Un trajet orange circule dans les rues.
// Passe par l'Amphi A, la Bibliothèque, Administration, Laboratoires et Restaurant, avec une branche vers chaque bâtiment.
const LOOP = 'M70 84 V140 H210 V170 V140 H335 V110 V140 H280 V290 H335 V320 V290 H75 V330 V290 H120 V140 H70 V84';

export default function CampusMap() {
  const reduceMotion =
    typeof window !== 'undefined' &&
    window.matchMedia('(prefers-reduced-motion: reduce)').matches;

  return (
    <svg className="campus-map" viewBox="0 0 400 420" aria-hidden="true" focusable="false">
      {/* Rues */}
      <g stroke="#1F4441" strokeWidth="14" strokeLinecap="butt">
        <line x1="0" y1="140" x2="400" y2="140" />
        <line x1="0" y1="290" x2="400" y2="290" />
        <line x1="120" y1="0" x2="120" y2="420" />
        <line x1="280" y1="0" x2="280" y2="420" />
      </g>

      {/* Bâtiments */}
      <g fill="#2A5550">
        <rect x="40" y="40" width="60" height="44" />
        <rect x="170" y="170" width="80" height="50" />
        <rect x="300" y="50" width="70" height="60" />
        <rect x="40" y="330" width="70" height="44" />
        <rect x="300" y="320" width="70" height="50" />
      </g>

      {/* Libellés */}
      <g fill="#9FB8B2" fontFamily="IBM Plex Sans, sans-serif" fontSize="10" textAnchor="middle">
        <text x="70" y="66">Amphi A</text>
        <text x="210" y="199">Bibliothèque</text>
        <text x="335" y="84">Administration</text>
        <text x="75" y="356">Restaurant</text>
        <text x="335" y="349">Laboratoires</text>
      </g>

      {/* Boucle de fond */}
      <path d={LOOP} fill="none" stroke="#E0653A" strokeOpacity="0.3" strokeWidth="2" />

      {/* Trait qui circule : le décalage des tirets est animé en SMIL */}
      {!reduceMotion && (
        <path
          d={LOOP}
          fill="none"
          stroke="#E0653A"
          strokeWidth="3"
          strokeLinecap="round"
          strokeDasharray="2 12"
        >
          <animate attributeName="stroke-dashoffset" from="0" to="-14" dur="1s" repeatCount="indefinite" />
        </path>
      )}

      {/* Point qui parcourt la boucle */}
      {!reduceMotion && (
        <circle r="5" fill="#E0653A">
          <animateMotion dur="12s" repeatCount="indefinite" path={LOOP} />
        </circle>
      )}
    </svg>
  );
}
