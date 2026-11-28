// Libellés et couleurs partagés par les pages de signalements.

export const LIBELLES_CATEGORIE = {
  MATERIEL: 'Matériel',
  PROPRETE: 'Propreté',
  SECURITE: 'Sécurité',
  AUTRE: 'Autre',
};

export const LIBELLES_STATUT = {
  OUVERT: 'Ouvert',
  EN_COURS: 'En cours',
  RESOLU: 'Résolu',
  FERME: 'Fermé',
};

export function formaterDateHeure(valeur) {
  const date = new Date(valeur);
  if (!valeur || Number.isNaN(date.getTime())) return '—';
  return date.toLocaleString('fr-FR', { day: 'numeric', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit' });
}

export function BadgeStatut({ statut }) {
  return (
    <span className={`statut-signalement statut-${(statut || '').toLowerCase()}`}>
      {LIBELLES_STATUT[statut] || statut}
    </span>
  );
}
