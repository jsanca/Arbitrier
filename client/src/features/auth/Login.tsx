import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { services } from '../../services/mockServices';

export const Login: React.FC = () => {
  const [email, setEmail] = useState('brio@arbitrier.com');
  const [password, setPassword] = useState('password');
  const [showPassword, setShowPassword] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const navigate = useNavigate();

  const handleLoginSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError('');
    try {
      await services.auth.login(email, password);
      navigate('/dashboard');
    } catch (err) {
      setError('Invalid credentials');
    } finally {
      setLoading(false);
    }
  };

  const handleKeycloakLogin = async () => {
    setLoading(true);
    try {
      await services.auth.login('sso-buyer@arbitrier.com');
      navigate('/dashboard');
    } catch (err) {
      setError('SSO Authentication Failed');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen flex flex-col items-center justify-center relative overflow-hidden bg-surface py-12 px-4 sm:px-6 lg:px-8">
      {/* Background decoration */}
      <div className="absolute top-0 left-0 right-0 h-96 bg-gradient-to-b from-surface-container/40 to-transparent pointer-events-none z-0"></div>

      <main className="relative z-10 w-full max-w-[440px]">
        {/* Brand Logo */}
        <div className="flex flex-col items-center mb-8">
          <div className="flex items-center gap-2 mb-1">
            <span className="material-symbols-outlined text-primary text-[44px]" style={{ fontVariationSettings: "'FILL' 1" }}>
              security
            </span>
            <h1 className="text-3xl font-bold font-headline-xl text-primary tracking-tight">Arbitrier</h1>
          </div>
          <p className="text-[11px] font-label-md text-secondary uppercase tracking-[0.25em]">Customer Portal</p>
        </div>

        {/* Login Card */}
        <div className="bg-surface-container-lowest border border-outline-variant p-8 rounded-2xl shadow-lg">
          <div className="mb-6">
            <h2 className="text-xl font-bold font-headline-md text-on-surface mb-1">Welcome Back</h2>
            <p className="text-xs text-on-surface-variant">Access your company's order orchestration portal.</p>
          </div>

          {error && (
            <div className="mb-4 p-3 bg-error-container/30 border border-error-container text-xs text-error rounded-lg flex items-center gap-2">
              <span className="material-symbols-outlined text-[16px]">error</span>
              <span>{error}</span>
            </div>
          )}

          {/* Form */}
          <form onSubmit={handleLoginSubmit} className="space-y-4">
            <div>
              <label className="block text-xs font-semibold text-on-surface-variant uppercase tracking-wider mb-1.5" htmlFor="email">
                Email Address
              </label>
              <input
                id="email"
                type="email"
                value={email}
                onChange={e => setEmail(e.target.value)}
                placeholder="name@company.com"
                required
                className="w-full h-11 px-4 border border-outline-variant focus:border-primary focus:ring-1 focus:ring-primary rounded-xl transition-all outline-none text-sm bg-surface-container-lowest"
              />
            </div>

            <div>
              <div className="flex justify-between items-center mb-1.5">
                <label className="block text-xs font-semibold text-on-surface-variant uppercase tracking-wider" htmlFor="password">
                  Password
                </label>
                <a href="#" onClick={e => e.preventDefault()} className="text-xs font-semibold text-primary hover:underline">
                  Forgot password?
                </a>
              </div>
              <div className="relative">
                <input
                  id="password"
                  type={showPassword ? 'text' : 'password'}
                  value={password}
                  onChange={e => setPassword(e.target.value)}
                  placeholder="••••••••"
                  required
                  className="w-full h-11 pl-4 pr-10 border border-outline-variant focus:border-primary focus:ring-1 focus:ring-primary rounded-xl transition-all outline-none text-sm bg-surface-container-lowest"
                />
                <button
                  type="button"
                  onClick={() => setShowPassword(!showPassword)}
                  className="absolute right-3 top-1/2 -translate-y-1/2 text-on-surface-variant hover:text-primary transition-colors focus:outline-none"
                >
                  <span className="material-symbols-outlined text-[18px]">
                    {showPassword ? 'visibility_off' : 'visibility'}
                  </span>
                </button>
              </div>
            </div>

            <div className="flex items-center gap-2 py-1">
              <input
                id="remember"
                type="checkbox"
                defaultChecked
                className="w-4 h-4 rounded border-outline-variant text-primary focus:ring-primary cursor-pointer"
              />
              <label htmlFor="remember" className="text-xs text-on-surface-variant cursor-pointer select-none">
                Stay signed in for 30 days
              </label>
            </div>

            <button
              type="submit"
              aria-label="Sign In"
              disabled={loading}
              className="w-full h-11 bg-primary text-on-primary font-semibold rounded-xl hover:opacity-90 transition-all flex items-center justify-center gap-2 shadow-md disabled:opacity-50"
            >
              {loading ? 'Authenticating...' : 'Sign In'}
              {!loading && <span className="material-symbols-outlined text-[18px]">login</span>}
            </button>
          </form>

          {/* Divider */}
          <div className="relative my-6">
            <div className="absolute inset-0 flex items-center">
              <div className="w-full border-t border-outline-variant"></div>
            </div>
            <div className="relative flex justify-center text-xs">
              <span className="bg-surface-container-lowest px-3 text-secondary font-medium">Or continue with</span>
            </div>
          </div>

          {/* Keycloak SSO Mock */}
          <button
            type="button"
            onClick={handleKeycloakLogin}
            disabled={loading}
            className="flex items-center justify-center gap-2.5 w-full h-11 border border-outline-variant rounded-xl hover:bg-surface-container-low transition-all text-on-surface-variant text-sm font-semibold disabled:opacity-50"
          >
            <span className="material-symbols-outlined text-primary text-[20px]" style={{ fontVariationSettings: "'FILL' 1" }}>
              key
            </span>
            <span>Sign in with Keycloak SSO</span>
          </button>
        </div>

        {/* Footer */}
        <footer className="mt-8 flex flex-col items-center gap-2">
          <div className="flex gap-4 text-xs font-semibold text-secondary">
            <a href="#" onClick={e => e.preventDefault()} className="hover:text-primary transition-colors">Privacy Policy</a>
            <span>•</span>
            <a href="#" onClick={e => e.preventDefault()} className="hover:text-primary transition-colors">Terms of Service</a>
          </div>
          <p className="text-[10px] text-on-surface-variant/50">© 2026 Arbitrier B2B. All rights reserved.</p>

          {/* Neutral Placeholders for future compliance validations */}
          <div className="mt-6 text-[10px] text-center text-on-surface-variant/40 max-w-xs space-y-1">
            <p>[Prototype Mode] Security controls & uptime SLAs are verified via sandbox environment configurations.</p>
            <p>Future integrations: Keycloak Realm OIDC Adapter & SOC2 Compliance Validation.</p>
          </div>
        </footer>
      </main>
    </div>
  );
};
