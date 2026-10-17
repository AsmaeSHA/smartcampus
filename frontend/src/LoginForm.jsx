import { useState } from 'react';

// Formulaire de connexion (SC-4). Envoie les identifiants à POST /api/auth/login.
export default function LoginForm() {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  async function handleSubmit(event) {
    event.preventDefault();
    setError('');

    if (!email.trim() || !password) {
      setError('Renseignez votre e-mail et votre mot de passe.');
      return;
    }

    setLoading(true);
    try {
      const response = await fetch('/api/auth/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email, password }),
      });

      const data = await response.json().catch(() => ({}));

      if (response.ok) {
        window.location.href = data.redirect || '/accueil';
        return;
      }

      setError(data.message || 'Connexion impossible. Réessayez plus tard.');
    } catch (err) {
      setError('Le serveur ne répond pas. Vérifiez que le backend est démarré.');
    } finally {
      setLoading(false);
    }
  }

  return (
    <form className="login-form" onSubmit={handleSubmit} noValidate>
      <header>
        <h2>Connexion</h2>
        <p>Utilisez votre adresse e-mail institutionnelle.</p>
      </header>

      <div className="field">
        <label htmlFor="email">E-mail</label>
        <input
          id="email"
          type="email"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          placeholder="prenom.nom@ensakhouribga.ma"
          autoComplete="username"
        />
      </div>

      <div className="field">
        <label htmlFor="password">Mot de passe</label>
        <div className="password-row">
          <input
            id="password"
            type={showPassword ? 'text' : 'password'}
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            autoComplete="current-password"
          />
          <button
            type="button"
            className="toggle"
            onClick={() => setShowPassword((v) => !v)}
            aria-pressed={showPassword}
          >
            {showPassword ? 'Masquer' : 'Afficher'}
          </button>
        </div>
      </div>

      {error && (
        <p role="alert" className="error">
          <svg viewBox="0 0 20 20" aria-hidden="true" width="18" height="18">
            <circle cx="10" cy="10" r="8.5" fill="none" stroke="currentColor" strokeWidth="1.8" />
            <path d="M10 5.5v5.5M10 13.8v.4" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" />
          </svg>
          <span>{error}</span>
        </p>
      )}

      <button type="submit" className="submit" disabled={loading}>
        {loading ? 'Connexion en cours…' : 'Se connecter'}
      </button>
    </form>
  );
}
