[1mdiff --git a/frontend/src/admin/AdminComptes.jsx b/frontend/src/admin/AdminComptes.jsx[m
[1mindex ade7308..83cd008 100644[m
[1m--- a/frontend/src/admin/AdminComptes.jsx[m
[1m+++ b/frontend/src/admin/AdminComptes.jsx[m
[36m@@ -295,3 +295,300 @@[m [mexport default function AdminComptes({ emailConnecte }) {[m
     </div>[m
   );[m
 }[m
[32m+[m[32m// import { useCallback, useEffect, useMemo, useState } from 'react';[m
[32m+[m[32m// import CreerCompteForm from './CreerCompteForm.jsx';[m
[32m+[m[32m// import ImportComptes from './ImportComptes.jsx';[m
[32m+[m[32m// import ListeComptes from './ListeComptes.jsx';[m
[32m+[m[32m// import Panneau from './Panneau.jsx';[m
[32m+[m[32m// import './admin.css';[m
[32m+[m
[32m+[m[32m// // Page « Gestion des comptes » de l'administrateur (SC-27, SC-28, SC-64, SC-53).[m
[32m+[m[32m// // La liste occupe toute la largeur ; création, modification et import s'ouvrent dans un panneau latéral.[m
[32m+[m
[32m+[m[32m// const TUILES = [[m
[32m+[m[32m//   { role: 'TOUS', libelle: 'Tous les comptes' },[m
[32m+[m[32m//   { role: 'ETUDIANT', libelle: 'Étudiants' },[m
[32m+[m[32m//   { role: 'ENSEIGNANT', libelle: 'Enseignants' },[m
[32m+[m[32m//   { role: 'ADMINISTRATEUR', libelle: 'Administrateurs' },[m
[32m+[m[32m// ];[m
[32m+[m
[32m+[m[32m// export default function AdminComptes({ emailConnecte }) {[m
[32m+[m[32m//   const [comptes, setComptes] = useState([]);[m
[32m+[m[32m//   const [niveaux, setNiveaux] = useState([]);[m
[32m+[m[32m//   const [chargement, setChargement] = useState(true);[m
[32m+[m[32m//   const [erreur, setErreur] = useState('');[m
[32m+[m[32m//   const [message, setMessage] = useState('');[m
[32m+[m[32m//   const [dernierCree, setDernierCree] = useState(null);[m
[32m+[m[32m//   const [copie, setCopie] = useState(false);[m
[32m+[m[32m//   const [filtreRole, setFiltreRole] = useState('TOUS');[m
[32m+[m[32m//   // null, « creer », « modifier » ou « importer »[m
[32m+[m[32m//   const [panneau, setPanneau] = useState(null);[m
[32m+[m[32m//   const [enModification, setEnModification] = useState(null);[m
[32m+[m[32m//   const [message, setMessage] = useState('');[m
[32m+[m[32m//   const [dernierReinitialise, setDernierReinitialise] = useState(null);[m
[32m+[m
[32m+[m[32m//   const charger = useCallback(async () => {[m
[32m+[m[32m//     setChargement(true);[m
[32m+[m[32m//     try {[m
[32m+[m[32m//       const [rComptes, rNiveaux] = await Promise.all([[m
[32m+[m[32m//         fetch('/api/admin/comptes'),[m
[32m+[m[32m//         fetch('/api/admin/niveaux'),[m
[32m+[m[32m//       ]);[m
[32m+[m[32m//       if (!rComptes.ok || !rNiveaux.ok) {[m
[32m+[m[32m//         setErreur('Impossible de charger les comptes. Votre session a peut-être expiré.');[m
[32m+[m[32m//         return;[m
[32m+[m[32m//       }[m
[32m+[m[32m//       setComptes(await rComptes.json());[m
[32m+[m[32m//       setNiveaux(await rNiveaux.json());[m
[32m+[m[32m//       setErreur('');[m
[32m+[m[32m//     } catch (err) {[m
[32m+[m[32m//       setErreur('Le serveur ne répond pas. Vérifiez que le backend est démarré.');[m
[32m+[m[32m//     } finally {[m
[32m+[m[32m//       setChargement(false);[m
[32m+[m[32m//     }[m
[32m+[m[32m//   }, []);[m
[32m+[m
[32m+[m[32m//   useEffect(() => {[m
[32m+[m[32m//     charger();[m
[32m+[m[32m//   }, [charger]);[m
[32m+[m
[32m+[m[32m//   const compteurs = useMemo(() => {[m
[32m+[m[32m//     const parRole = { TOUS: comptes.length, ETUDIANT: 0, ENSEIGNANT: 0, ADMINISTRATEUR: 0 };[m
[32m+[m[32m//     comptes.forEach((c) => {[m
[32m+[m[32m//       parRole[c.role] += 1;[m
[32m+[m[32m//     });[m
[32m+[m[32m//     return parRole;[m
[32m+[m[32m//   }, [comptes]);[m
[32m+[m
[32m+[m[32m//   const desactives = comptes.filter((c) => !c.actif).length;[m
[32m+[m
[32m+[m[32m//   const fermerPanneau = useCallback(() => {[m
[32m+[m[32m//     setPanneau(null);[m
[32m+[m[32m//     setEnModification(null);[m
[32m+[m[32m//   }, []);[m
[32m+[m
[32m+[m[32m//   function ouvrirCreation() {[m
[32m+[m[32m//     setEnModification(null);[m
[32m+[m[32m//     setPanneau('creer');[m
[32m+[m[32m//   }[m
[32m+[m
[32m+[m[32m//   function compteCree(resultat) {[m
[32m+[m[32m//     setPanneau(null);[m
[32m+[m[32m//     setMessage('');[m
[32m+[m[32m//     setCopie(false);[m
[32m+[m[32m//     setDernierCree(resultat);[m
[32m+[m[32m//     charger();[m
[32m+[m[32m//   }[m
[32m+[m
[32m+[m[32m//   // SC-64 : charge le détail du compte puis ouvre le formulaire pré-rempli.[m
[32m+[m[32m//   async function ouvrirModification(compte) {[m
[32m+[m[32m//     try {[m
[32m+[m[32m//       const response = await fetch(`/api/admin/comptes/${compte.id}`);[m
[32m+[m[32m//       const data = await response.json().catch(() => ({}));[m
[32m+[m[32m//       if (!response.ok) {[m
[32m+[m[32m//         setErreur(data.message || 'Impossible de charger ce compte.');[m
[32m+[m[32m//         return;[m
[32m+[m[32m//       }[m
[32m+[m[32m//       setErreur('');[m
[32m+[m[32m//       setEnModification(data);[m
[32m+[m[32m//       setPanneau('modifier');[m
[32m+[m[32m//     } catch (err) {[m
[32m+[m[32m//       setErreur('Le serveur ne répond pas. Vérifiez que le backend est démarré.');[m
[32m+[m[32m//     }[m
[32m+[m[32m//   }[m
[32m+[m
[32m+[m[32m//   function compteModifie(resultat) {[m
[32m+[m[32m//     fermerPanneau();[m
[32m+[m[32m//     setDernierCree(null);[m
[32m+[m[32m//     setMessage(`Le compte de ${resultat.prenom} ${resultat.nom} a été modifié.`);[m
[32m+[m[32m//     setComptes((liste) => liste.map((c) => (c.id === resultat.id ? resultat : c)));[m
[32m+[m[32m//   }[m
[32m+[m
[32m+[m[32m//   // SC-28 : désactive ou réactive un compte, après confirmation.[m
[32m+[m[32m//   async function changerActivation(compte) {[m
[32m+[m[32m//     const action = compte.actif ? 'desactiver' : 'reactiver';[m
[32m+[m[32m//     const question = compte.actif[m
[32m+[m[32m//       ? `Désactiver le compte de ${compte.prenom} ${compte.nom} ? Il ne pourra plus se connecter, mais ses données seront conservées.`[m
[32m+[m[32m//       : `Réactiver le compte de ${compte.prenom} ${compte.nom} ?`;[m
[32m+[m[32m//     if (!window.confirm(question)) {[m
[32m+[m[32m//       return;[m
[32m+[m[32m//     }[m
[32m+[m
[32m+[m[32m//     try {[m
[32m+[m[32m//       const response = await fetch(`/api/admin/comptes/${compte.id}/${action}`, { method: 'PATCH' });[m
[32m+[m[32m//       const data = await response.json().catch(() => ({}));[m
[32m+[m[32m//       if (!response.ok) {[m
[32m+[m[32m//         setErreur(data.message || 'Action impossible. Réessayez plus tard.');[m
[32m+[m[32m//         return;[m
[32m+[m[32m//       }[m
[32m+[m[32m//       setErreur('');[m
[32m+[m[32m//       setDernierCree(null);[m
[32m+[m[32m//       setMessage(`Le compte de ${data.prenom} ${data.nom} a été ${data.actif ? 'réactivé' : 'désactivé'}.`);[m
[32m+[m[32m//       setComptes((liste) => liste.map((c) => (c.id === data.id ? data : c)));[m
[32m+[m[32m//     } catch (err) {[m
[32m+[m[32m//       setErreur('Le serveur ne répond pas. Vérifiez que le backend est démarré.');[m
[32m+[m[32m//     }[m
[32m+[m[32m//   }[m
[32m+[m
[32m+[m[32m//   async function reinitialiserMotDePasse(compte) {[m
[32m+[m[32m//     if (!window.confirm(`Générer un mot de passe provisoire pour ${compte.prenom} ${compte.nom} ? Son mot de passe actuel cessera de fonctionner.`)) return;[m
[32m+[m[32m//     try {[m
[32m+[m[32m//       const response = await fetch(`/api/admin/comptes/${compte.id}/reinitialiser-mot-de-passe`, { method: 'POST' });[m
[32m+[m[32m//       const data = await response.json().catch(() => ({}));[m
[32m+[m[32m//       if (!response.ok) { setErreur(data.message || 'Réinitialisation impossible.'); return; }[m
[32m+[m[32m//       setErreur(''); setDernierReinitialise(data); setCopie(false);[m
[32m+[m[32m//       setComptes(liste => liste.map(c => c.id === data.compte.id ? data.compte : c));[m
[32m+[m[32m//     } catch { setErreur('Le serveur ne répond pas. Vérifiez que le backend est démarré.'); }[m
[32m+[m[32m//   }[m
[32m+[m
[32m+[m[32m//   async function copier() {[m
[32m+[m[32m//     try {[m
[32m+[m[32m//       await navigator.clipboard.writeText(dernierCree.motDePasseTemporaire);[m
[32m+[m[32m//       setCopie(true);[m
[32m+[m[32m//     } catch (err) {[m
[32m+[m[32m//       setCopie(false);[m
[32m+[m[32m//     }[m
[32m+[m[32m//   }[m
[32m+[m
[32m+[m[32m//   return ([m
[32m+[m[32m//     <div className="admin-comptes">[m
[32m+[m[32m//       <section className="bandeau">[m
[32m+[m[32m//         <div className="bandeau-contenu">[m
[32m+[m[32m//           <div>[m
[32m+[m[32m//             <p className="surtitre">Administration</p>[m
[32m+[m[32m//             <h1>Gestion des comptes</h1>[m
[32m+[m[32m//             <p className="bandeau-texte">[m
[32m+[m[32m//               {compteurs.TOUS} compte{compteurs.TOUS > 1 ? 's' : ''}[m
[32m+[m[32m//               {desactives > 0 && ` · ${desactives} désactivé${desactives > 1 ? 's' : ''}`}[m
[32m+[m[32m//             </p>[m
[32m+[m[32m//           </div>[m
[32m+[m[32m//           <div className="bandeau-actions">[m
[32m+[m[32m//             <button type="button" className="bouton-clair" onClick={() => setPanneau('importer')}>[m
[32m+[m[32m//               <svg viewBox="0 0 20 20" width="18" height="18" aria-hidden="true">[m
[32m+[m[32m//                 <path d="M10 13V3M6 7l4-4 4 4M4 13v3h12v-3" fill="none" stroke="currentColor" strokeWidth="1.7"[m
[32m+[m[32m//                   strokeLinecap="round" strokeLinejoin="round" />[m
[32m+[m[32m//               </svg>[m
[32m+[m[32m//               Importer un CSV[m
[32m+[m[32m//             </button>[m
[32m+[m[32m//             <button type="button" className="bouton-menthe" onClick={ouvrirCreation}>[m
[32m+[m[32m//               <svg viewBox="0 0 20 20" width="18" height="18" aria-hidden="true">[m
[32m+[m[32m//                 <path d="M10 4v12M4 10h12" stroke="currentColor" strokeWidth="1.9" strokeLinecap="round" />[m
[32m+[m[32m//               </svg>[m
[32m+[m[32m//               Nouveau compte[m
[32m+[m[32m//             </button>[m
[32m+[m[32m//           </div>[m
[32m+[m[32m//         </div>[m
[32m+[m[32m//       </section>[m
[32m+[m
[32m+[m[32m//       <div className="admin-contenu">[m
[32m+[m[32m//         <div className="tuiles" role="group" aria-label="Filtrer par rôle">[m
[32m+[m[32m//           {TUILES.map((t) => ([m
[32m+[m[32m//             <button[m
[32m+[m[32m//               key={t.role}[m
[32m+[m[32m//               type="button"[m
[32m+[m[32m//               className={filtreRole === t.role ? 'tuile active' : 'tuile'}[m
[32m+[m[32m//               aria-pressed={filtreRole === t.role}[m
[32m+[m[32m//               onClick={() => setFiltreRole(t.role)}[m
[32m+[m[32m//             >[m
[32m+[m[32m//               <span className="tuile-nombre">{compteurs[t.role]}</span>[m
[32m+[m[32m//               <span className="tuile-libelle">{t.libelle}</span>[m
[32m+[m[32m//             </button>[m
[32m+[m[32m//           ))}[m
[32m+[m[32m//         </div>[m
[32m+[m
[32m+[m[32m//       {dernierReinitialise && ([m
[32m+[m[32m//         <div className="succes" role="status">[m
[32m+[m[32m//           <p>Mot de passe provisoire généré pour <strong>{dernierReinitialise.compte.email}</strong>. Il ne sera plus affiché après fermeture de cet écran.</p>[m
[32m+[m[32m//           <div className="mot-de-passe"><code>{dernierReinitialise.motDePasseTemporaire}</code><button type="button" className="logout" onClick={async () => { try { await navigator.clipboard.writeText(dernierReinitialise.motDePasseTemporaire); setCopie(true); } catch { setCopie(false); } }}>{copie ? 'Copié' : 'Copier'}</button></div>[m
[32m+[m[32m//         </div>[m
[32m+[m[32m//       )}[m
[32m+[m
[32m+[m[32m//       {dernierCree && ([m
[32m+[m[32m//         <div className="succes" role="status">[m
[32m+[m[32m//           <p>[m
[32m+[m[32m//             Compte créé pour <strong>{dernierCree.compte.email}</strong>. Transmettez ce mot de passe[m
[32m+[m[32m//             temporaire à l’utilisateur : il ne sera plus affiché.[m
[32m+[m[32m//         {erreur && ([m
[32m+[m[32m//           <p role="alert" className="alerte alerte-erreur">[m
[32m+[m[32m//             {erreur}[m
[32m+[m[32m//           </p>[m
[32m+[m[32m//         )}[m
[32m+[m
[32m+[m[32m//         {message && ([m
[32m+[m[32m//           <p role="status" className="alerte alerte-succes">[m
[32m+[m[32m//             {message}[m
[32m+[m[32m//             <button type="button" className="alerte-fermer" onClick={() => setMessage('')} aria-label="Fermer le message">[m
[32m+[m[32m//               ×[m
[32m+[m[32m//             </button>[m
[32m+[m[32m//           </p>[m
[32m+[m[32m//         )}[m
[32m+[m
[32m+[m[32m//         {dernierCree && ([m
[32m+[m[32m//           <div role="status" className="alerte alerte-succes alerte-mot-de-passe">[m
[32m+[m[32m//             <p>[m
[32m+[m[32m//               Compte créé pour <strong>{dernierCree.compte.email}</strong>. Transmettez ce mot de passe[m
[32m+[m[32m//               temporaire à l’utilisateur : il ne sera plus affiché.[m
[32m+[m[32m//             </p>[m
[32m+[m[32m//             <div className="mot-de-passe">[m
[32m+[m[32m//               <code>{dernierCree.motDePasseTemporaire}</code>[m
[32m+[m[32m//               <button type="button" className="bouton-secondaire" onClick={copier}>[m
[32m+[m[32m//                 {copie ? 'Copié ✓' : 'Copier'}[m
[32m+[m[32m//               </button>[m
[32m+[m[32m//               <button type="button" className="alerte-fermer" onClick={() => setDernierCree(null)}[m
[32m+[m[32m//                 aria-label="Fermer le message">[m
[32m+[m[32m//                 ×[m
[32m+[m[32m//               </button>[m
[32m+[m[32m//             </div>[m
[32m+[m[32m//           </div>[m
[32m+[m[32m//         )}[m
[32m+[m
[32m+[m[32m//         <ListeComptes[m
[32m+[m[32m//           comptes={comptes}[m
[32m+[m[32m//           chargement={chargement}[m
[32m+[m[32m//           filtreRole={filtreRole}[m
[32m+[m[32m//           emailConnecte={emailConnecte}[m
[32m+[m[32m//           onModifier={ouvrirModification}[m
[32m+[m[32m//           onChangerActivation={changerActivation}[m
[32m+[m[32m//           onEffacerRole={() => setFiltreRole('TOUS')}[m
[32m+[m[32m//         />[m
[32m+[m[32m//       </div>[m
[32m+[m
[32m+[m[32m//       {(panneau === 'creer' || panneau === 'modifier') && ([m
[32m+[m[32m//         <Panneau[m
[32m+[m[32m//           titre={panneau === 'modifier' ? 'Modifier le compte' : 'Nouveau compte'}[m
[32m+[m[32m//           sousTitre={[m
[32m+[m[32m//             panneau === 'modifier'[m
[32m+[m[32m//               ? `${enModification.prenom} ${enModification.nom}`[m
[32m+[m[32m//               : 'Un mot de passe temporaire sera généré.'[m
[32m+[m[32m//           }[m
[32m+[m[32m//           onFermer={fermerPanneau}[m
[32m+[m[32m//         >[m
[32m+[m[32m//           <CreerCompteForm[m
[32m+[m[32m//             key={enModification ? enModification.id : 'nouveau'}[m
[32m+[m[32m//             niveaux={niveaux}[m
[32m+[m[32m//             compte={enModification}[m
[32m+[m[32m//             onCree={compteCree}[m
[32m+[m[32m//             onModifie={compteModifie}[m
[32m+[m[32m//             onAnnuler={fermerPanneau}[m
[32m+[m[32m//           />[m
[32m+[m[32m//