import { useLayoutEffect, useState } from 'react';

// Ligne orange qui relie la carte du campus aux champs du formulaire.
// Elle se place en fonction de la position réelle des éléments, et se recalcule au redimensionnement.
// Elle n'apparaît que sur écran large.

const DESKTOP_QUERY = '(min-width: 861px)';

function toPath(points) {
  return points
    .map((p, i) => `${i === 0 ? 'M' : 'L'}${p.x.toFixed(1)} ${p.y.toFixed(1)}`)
    .join(' ');
}

export default function RouteLine() {
  const [geo, setGeo] = useState(null);

  useLayoutEffect(() => {
    const desktop = window.matchMedia(DESKTOP_QUERY);
    const reduceMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;

    function measure() {
      if (!desktop.matches) {
        setGeo(null);
        return;
      }

      const start = document.getElementById('route-start');
      const end = document.getElementById('route-end');
      const form = document.querySelector('.login-form');
      const fields = [
        document.getElementById('email'),
        document.getElementById('password'),
        document.querySelector('.submit'),
      ];
      if (!start || !end || !form || fields.some((el) => !el)) return;

      const sx = window.scrollX;
      const sy = window.scrollY;
      const center = (el) => {
        const r = el.getBoundingClientRect();
        return { x: r.left + r.width / 2 + sx, y: r.top + r.height / 2 + sy };
      };

      // Colonne verticale à gauche du formulaire
      const gutterX = form.getBoundingClientRect().left + sx - 28;

      const p0 = center(start);
      const p1 = center(end);
      const stops = fields.map((el) => ({ x: gutterX, y: center(el).y }));

      const points = [p0, { x: p1.x, y: p0.y }, p1, { x: gutterX, y: p1.y }, ...stops];

      setGeo({
        width: document.documentElement.scrollWidth,
        height: document.documentElement.scrollHeight,
        d: toPath(points),
        stops,
        reduceMotion,
      });
    }

    measure();
    window.addEventListener('resize', measure);
    document.fonts?.ready.then(measure);
    return () => window.removeEventListener('resize', measure);
  }, []);

  if (!geo) return null;

  return (
    <svg
      className="route-overlay"
      width={geo.width}
      height={geo.height}
      viewBox={`0 0 ${geo.width} ${geo.height}`}
      aria-hidden="true"
      focusable="false"
    >
      {/* Trace fixe */}
      <path d={geo.d} fill="none" stroke="#E0653A" strokeOpacity="0.3" strokeWidth="2" />

      {/* Trace qui circule */}
      {!geo.reduceMotion && (
        <path
          className="route-flow"
          d={geo.d}
          fill="none"
          stroke="#E0653A"
          strokeWidth="3"
          strokeLinecap="round"
          strokeDasharray="2 14"
        />
      )}

      {/* Arrêts sur les champs */}
      {geo.stops.map((p, i) => (
        <circle key={i} cx={p.x} cy={p.y} r="4.5" fill="#EEF1EA" stroke="#E0653A" strokeWidth="2" />
      ))}

      {/* Point qui parcourt le trajet */}
      {!geo.reduceMotion && (
        <circle r="5" fill="#E0653A">
          <animateMotion dur="9s" repeatCount="indefinite" path={geo.d} />
        </circle>
      )}
    </svg>
  );
}
