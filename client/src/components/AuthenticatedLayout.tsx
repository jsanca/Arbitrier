import React, { useState, useEffect } from 'react';
import { Link, useNavigate, useLocation, Outlet } from 'react-router-dom';
import type { User, Company } from '../models/types';
import { services } from '../services/mockServices';

export const AuthenticatedLayout: React.FC = () => {
  const [user, setUser] = useState<User | null>(null);
  const [company, setCompany] = useState<Company | null>(null);
  const [isSidebarOpen, setIsSidebarOpen] = useState(false);
  const navigate = useNavigate();
  const location = useLocation();

  useEffect(() => {
    const loadProfile = async () => {
      const currentUser = await services.auth.getCurrentUser();
      if (!currentUser) {
        navigate('/login');
        return;
      }
      setUser(currentUser);

      const currentCompany = await services.company.getCompany(currentUser.companyId);
      setCompany(currentCompany);
    };

    loadProfile();
  }, [navigate]);

  const handleSignOut = async () => {
    await services.auth.logout();
    navigate('/login');
  };

  const navItems = [
    { path: '/dashboard', label: 'Dashboard', icon: 'dashboard' },
    { path: '/orders', label: 'Orders', icon: 'list_alt' },
    { path: '/company', label: 'Company Profile', icon: 'domain' },
    { path: '/profile', label: 'My Settings', icon: 'person' }
  ];

  if (!user || !company) return null;

  return (
    <div className="min-h-screen flex bg-surface">
      {/* Sidebar for Desktop */}
      <aside className="hidden md:flex flex-col w-64 bg-primary text-on-primary border-r border-outline/20">
        {/* Brand */}
        <div className="h-16 flex items-center gap-2 px-6 border-b border-outline/20">
          <span className="material-symbols-outlined text-[28px] text-primary-fixed" style={{ fontVariationSettings: "'FILL' 1" }}>
            security
          </span>
          <span className="font-bold text-lg tracking-tight text-white">Arbitrier</span>
          <span className="text-[10px] bg-primary-container text-on-primary-container px-2 py-0.5 rounded font-mono uppercase">
            B2B
          </span>
        </div>

        {/* Company Area */}
        <div className="px-6 py-4 border-b border-outline/10 bg-primary-container/20">
          <p className="text-[10px] text-primary-fixed font-semibold uppercase tracking-wider">Company</p>
          <p className="text-sm font-semibold truncate text-white">{company.name}</p>
          <div className="flex items-center gap-1.5 mt-1">
            <span className="w-1.5 h-1.5 rounded-full bg-emerald-400"></span>
            <span className="text-xs text-primary-fixed-dim">Active Account</span>
          </div>
        </div>

        {/* Navigation Links */}
        <nav className="flex-1 px-4 py-4 space-y-1.5">
          {navItems.map(item => {
            const isActive = location.pathname === item.path || (item.path !== '/dashboard' && location.pathname.startsWith(item.path));
            return (
              <Link
                key={item.path}
                to={item.path}
                className={`flex items-center gap-3 px-4 py-2.5 rounded-lg text-sm font-medium transition-all ${
                  isActive
                    ? 'bg-primary-fixed text-on-primary-fixed'
                    : 'text-primary-fixed-dim hover:bg-primary-container/30 hover:text-white'
                }`}
              >
                <span className="material-symbols-outlined text-[20px]">{item.icon}</span>
                <span>{item.label}</span>
              </Link>
            );
          })}
        </nav>

        {/* User profile / Logout */}
        <div className="p-4 border-t border-outline/20 bg-primary-container/10">
          <div className="flex items-center gap-3 mb-3">
            <div className="w-9 h-9 rounded-full bg-secondary-fixed flex items-center justify-center text-on-secondary-fixed font-bold text-sm">
              {user.name.charAt(0)}
            </div>
            <div className="flex-1 min-w-0">
              <p className="text-xs font-semibold text-white truncate">{user.name}</p>
              <p className="text-[10px] text-primary-fixed-dim truncate">{user.role}</p>
            </div>
          </div>
          <button
            onClick={handleSignOut}
            className="flex items-center justify-center gap-2 w-full py-2 bg-primary-container hover:bg-primary-container/80 text-on-primary-container rounded-lg text-xs font-semibold transition-all"
          >
            <span className="material-symbols-outlined text-[16px]">logout</span>
            <span>Sign Out</span>
          </button>
        </div>
      </aside>

      {/* Mobile Drawer Navigation Backdrop */}
      {isSidebarOpen && (
        <div
          className="fixed inset-0 z-40 bg-black/40 md:hidden"
          onClick={() => setIsSidebarOpen(false)}
        />
      )}

      {/* Mobile Drawer Sidebar */}
      <aside
        className={`fixed top-0 bottom-0 left-0 z-50 w-64 bg-primary text-on-primary flex flex-col transition-transform duration-300 md:hidden ${
          isSidebarOpen ? 'translate-x-0' : '-translate-x-full'
        }`}
      >
        <div className="h-16 flex items-center justify-between px-6 border-b border-outline/20">
          <div className="flex items-center gap-2">
            <span className="material-symbols-outlined text-[28px] text-primary-fixed" style={{ fontVariationSettings: "'FILL' 1" }}>
              security
            </span>
            <span className="font-bold text-lg tracking-tight text-white">Arbitrier</span>
          </div>
          <button
            onClick={() => setIsSidebarOpen(false)}
            className="text-primary-fixed-dim hover:text-white"
          >
            <span className="material-symbols-outlined">close</span>
          </button>
        </div>

        <div className="px-6 py-4 border-b border-outline/10 bg-primary-container/20">
          <p className="text-[10px] text-primary-fixed font-semibold uppercase tracking-wider">Company</p>
          <p className="text-sm font-semibold truncate text-white">{company.name}</p>
        </div>

        <nav className="flex-1 px-4 py-4 space-y-1.5">
          {navItems.map(item => {
            const isActive = location.pathname === item.path || (item.path !== '/dashboard' && location.pathname.startsWith(item.path));
            return (
              <Link
                key={item.path}
                to={item.path}
                onClick={() => setIsSidebarOpen(false)}
                className={`flex items-center gap-3 px-4 py-2.5 rounded-lg text-sm font-medium transition-all ${
                  isActive
                    ? 'bg-primary-fixed text-on-primary-fixed'
                    : 'text-primary-fixed-dim hover:bg-primary-container/30 hover:text-white'
                }`}
              >
                <span className="material-symbols-outlined text-[20px]">{item.icon}</span>
                <span>{item.label}</span>
              </Link>
            );
          })}
        </nav>

        <div className="p-4 border-t border-outline/20">
          <div className="flex items-center gap-3 mb-3">
            <div className="w-9 h-9 rounded-full bg-secondary-fixed flex items-center justify-center text-on-secondary-fixed font-bold text-sm">
              {user.name.charAt(0)}
            </div>
            <div className="flex-1 min-w-0">
              <p className="text-xs font-semibold text-white truncate">{user.name}</p>
              <p className="text-[10px] text-primary-fixed-dim truncate">{user.role}</p>
            </div>
          </div>
          <button
            onClick={handleSignOut}
            className="flex items-center justify-center gap-2 w-full py-2 bg-primary-container hover:bg-primary-container/80 text-on-primary-container rounded-lg text-xs font-semibold transition-all"
          >
            <span className="material-symbols-outlined text-[16px]">logout</span>
            <span>Sign Out</span>
          </button>
        </div>
      </aside>

      {/* Main Content Area */}
      <div className="flex-1 flex flex-col min-w-0 overflow-y-auto">
        {/* Top Navbar */}
        <header className="h-16 bg-surface-container-lowest border-b border-outline-variant flex items-center justify-between px-6 sticky top-0 z-30 shadow-sm">
          <div className="flex items-center gap-4">
            <button
              onClick={() => setIsSidebarOpen(true)}
              className="text-on-surface-variant hover:text-primary md:hidden focus:outline-none"
              aria-label="Open navigation menu"
            >
              <span className="material-symbols-outlined text-[28px]">menu</span>
            </button>
            <div className="hidden sm:block">
              <p className="text-[10px] font-semibold text-secondary uppercase tracking-wider">Welcome back</p>
              <h2 className="text-sm font-semibold text-on-surface flex items-center gap-1.5">
                {user.name} <span className="text-xs text-on-surface-variant font-normal">({user.role})</span>
              </h2>
            </div>
          </div>

          <div className="flex items-center gap-4">
            {/* Quick Action: New Order */}
            <Link
              to="/orders/new"
              className="h-10 px-4 bg-primary text-on-primary hover:bg-primary-container rounded-lg flex items-center gap-2 text-sm font-semibold transition-all shadow-sm"
            >
              <span className="material-symbols-outlined text-[18px]">add_shopping_cart</span>
              <span className="hidden sm:inline">New Order</span>
            </Link>
          </div>
        </header>

        {/* Dynamic Outlet Page content */}
        <main className="flex-1 p-6 md:p-8 max-w-container-max w-full mx-auto">
          <Outlet />
        </main>
      </div>
    </div>
  );
};
