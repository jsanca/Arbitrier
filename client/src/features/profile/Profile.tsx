import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import type { User } from '../../models/types';
import { services } from '../../services/mockServices';
import { LoadingState } from '../../components/LoadingState';

export const Profile: React.FC = () => {
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(true);
  const navigate = useNavigate();

  // Settings states
  const [emailAlerts, setEmailAlerts] = useState(true);
  const [creditAlerts, setCreditAlerts] = useState(true);
  const [backorderAlerts, setBackorderAlerts] = useState(true);
  const [language, setLanguage] = useState('en');

  useEffect(() => {
    const loadUserProfile = async () => {
      try {
        const currentUser = await services.auth.getCurrentUser();
        if (!currentUser) {
          navigate('/login');
          return;
        }
        setUser(currentUser);
      } catch (err) {
        console.error('Failed to load profile settings', err);
      } finally {
        setLoading(false);
      }
    };

    loadUserProfile();
  }, [navigate]);

  const handleSignOut = async () => {
    await services.auth.logout();
    navigate('/login');
  };

  const handleSaveSettings = () => {
    alert('Settings preferences updated successfully (Mock)!');
  };

  if (loading) return <LoadingState message="Opening settings profile..." />;
  if (!user) return null;

  return (
    <div className="max-w-2xl mx-auto space-y-8" data-testid="profile-page">
      <div>
        <h1 className="text-2xl font-bold font-headline-lg text-on-surface">My Settings</h1>
        <p className="text-sm text-on-surface-variant mt-1">
          Manage your personal buyer profile settings and notification rules.
        </p>
      </div>

      {/* Profile Card */}
      <div className="bg-surface-container-lowest border border-outline-variant rounded-xl p-6 shadow-sm flex flex-col sm:flex-row items-center gap-6">
        <div className="w-16 h-16 rounded-full bg-primary-fixed text-on-primary-fixed flex items-center justify-center text-xl font-bold">
          {user.name.charAt(0)}
        </div>
        <div className="flex-1 text-center sm:text-left min-w-0">
          <h2 className="text-lg font-bold text-on-surface">{user.name}</h2>
          <p className="text-xs text-on-surface-variant mt-0.5">{user.email}</p>
          <div className="mt-2 flex flex-wrap gap-2 justify-center sm:justify-start">
            <span className="text-[10px] bg-primary-container text-on-primary-container px-2 py-0.5 rounded font-mono uppercase font-semibold">
              {user.role}
            </span>
            <span className="text-[10px] bg-surface-container-high text-secondary px-2 py-0.5 rounded font-mono uppercase font-semibold">
              ID: {user.id}
            </span>
          </div>
        </div>
        <button
          onClick={handleSignOut}
          className="h-10 px-4 border border-error hover:bg-error/10 text-error font-semibold rounded-lg text-xs transition-all flex items-center justify-center gap-1.5"
        >
          <span className="material-symbols-outlined text-[16px]">logout</span>
          <span>Sign Out</span>
        </button>
      </div>

      {/* Notification and language configurations */}
      <div className="bg-surface-container-lowest border border-outline-variant rounded-xl p-6 shadow-sm space-y-6">
        <h3 className="text-sm font-bold font-headline-md border-b border-outline-variant pb-2">Procurement Alerts</h3>

        <div className="space-y-4">
          <div className="flex items-start gap-3">
            <input
              id="email-alerts"
              type="checkbox"
              checked={emailAlerts}
              onChange={e => setEmailAlerts(e.target.checked)}
              className="w-4.5 h-4.5 rounded border-outline-variant text-primary focus:ring-primary mt-0.5 cursor-pointer"
            />
            <label htmlFor="email-alerts" className="text-xs text-on-surface-variant cursor-pointer select-none">
              <span className="block font-semibold text-on-surface text-sm">Email order confirmations</span>
              Receive instant email receipts for all submitted and finalized bulk orders.
            </label>
          </div>

          <div className="flex items-start gap-3">
            <input
              id="credit-alerts"
              type="checkbox"
              checked={creditAlerts}
              onChange={e => setCreditAlerts(e.target.checked)}
              className="w-4.5 h-4.5 rounded border-outline-variant text-primary focus:ring-primary mt-0.5 cursor-pointer"
            />
            <label htmlFor="credit-alerts" className="text-xs text-on-surface-variant cursor-pointer select-none">
              <span className="block font-semibold text-on-surface text-sm">Credit utilization warnings</span>
              Get notified when corporate credit usage exceeds 80% of limit thresholds.
            </label>
          </div>

          <div className="flex items-start gap-3">
            <input
              id="backorder-alerts"
              type="checkbox"
              checked={backorderAlerts}
              onChange={e => setBackorderAlerts(e.target.checked)}
              className="w-4.5 h-4.5 rounded border-outline-variant text-primary focus:ring-primary mt-0.5 cursor-pointer"
            />
            <label htmlFor="backorder-alerts" className="text-xs text-on-surface-variant cursor-pointer select-none">
              <span className="block font-semibold text-on-surface text-sm">Stock allocation warnings</span>
              Receive alert notifications immediately if orders encounter backorder or inventory discrepancies.
            </label>
          </div>
        </div>

        {/* Language selector placeholder */}
        <div className="border-t border-outline-variant/30 pt-6">
          <label className="block text-xs font-semibold text-on-surface-variant uppercase tracking-wider mb-2">
            Portal Display Language
          </label>
          <select
            value={language}
            onChange={e => setLanguage(e.target.value)}
            className="w-48 h-10 px-3 border border-outline-variant focus:border-primary focus:ring-1 focus:ring-primary rounded-xl text-xs bg-surface-container-low outline-none"
          >
            <option value="en">English (US)</option>
            <option value="es">Español</option>
            <option value="fr">Français</option>
          </select>
        </div>

        {/* Save button */}
        <div className="border-t border-outline-variant/30 pt-6 flex justify-end">
          <button
            onClick={handleSaveSettings}
            className="h-10 px-6 bg-primary text-on-primary hover:opacity-95 font-semibold rounded-xl text-xs transition-all shadow-sm"
          >
            Save Preferences
          </button>
        </div>
      </div>
    </div>
  );
};
