import React from 'react';
import { Navigate, Outlet, Link, useNavigate } from 'react-router-dom';
import { useAuthStore } from '../store/useAuthStore';
import { useNotificationStore } from '../store/useNotificationStore';
import { authService } from '../services/authService';
import { LogOut, Activity, LayoutDashboard, Shield } from 'lucide-react';

export const MainLayout = () => {
  const { user, isAuthenticated, isAdmin, clearAuth } = useAuthStore();
  const showToast = useNotificationStore(state => state.showToast);
  const navigate = useNavigate();

  // Protect route
  if (!isAuthenticated()) {
    return <Navigate to="/login" replace />;
  }

  const handleLogout = async () => {
    try {
      await authService.logout();
    } catch (e) {
      console.error('Logout error UI', e);
    } finally {
      clearAuth();
      showToast('Logged out successfully', 'success');
      navigate('/login');
    }
  };

  return (
    <div className="flex flex-col min-h-screen">
      {/* Navbar */}
      <nav className="sticky top-0 z-50 bg-[#0f1115cc] backdrop-blur-md border-b border-white/10 px-6 py-4">
        <div className="max-w-7xl mx-auto flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="w-8 h-8 rounded-lg bg-gradient-to-br from-cyan-400 to-purple-600 box-glow flex items-center justify-center text-black">
              <Activity size={18} />
            </div>
            <Link to={isAdmin() ? '/admin' : '/dashboard'} className="font-bold text-lg tracking-tight hover:text-cyan-400 transition-colors">
              SessionTrack
            </Link>
          </div>

          <div className="flex items-center gap-6">
            <div className="hidden md:flex items-center gap-4 text-sm font-medium">
              <Link 
                to={isAdmin() ? '/admin' : '/dashboard'} 
                className="text-gray-300 hover:text-white flex items-center gap-2 transition-colors"
              >
                {isAdmin() ? <Shield size={16} className="text-purple-400" /> : <LayoutDashboard size={16} className="text-cyan-400" />}
                {isAdmin() ? 'Admin Panel' : 'Dashboard'}
              </Link>
            </div>
            
            <div className="flex items-center gap-4 border-l border-white/10 pl-6">
              <div className="text-right hidden sm:block">
                <p className="text-sm font-semibold">{user?.username}</p>
                <p className="text-xs text-gray-500">{user?.role}</p>
              </div>
              <button 
                onClick={handleLogout}
                className="p-2 text-gray-400 hover:text-red-400 hover:bg-white/5 rounded-lg transition-all"
                title="Logout"
              >
                <LogOut size={20} />
              </button>
            </div>
          </div>
        </div>
      </nav>

      {/* Main Content */}
      <main className="flex-1 w-full max-w-7xl mx-auto p-4 sm:p-6 lg:p-8 animate-in fade-in duration-500">
        <Outlet />
      </main>
    </div>
  );
};

// Global Toast Component
export const ToastContainer = () => {
    const toast = useNotificationStore(state => state.toast);
    const clearToast = useNotificationStore(state => state.clearToast);

    if (!toast) return null;

    const isError = toast.type === 'error';

    return (
        <div className="fixed bottom-6 right-6 z-[100] animate-in slide-in-from-right-8 fade-in duration-300">
            <div className={`
                flex items-centerjustify-between min-w-[300px] p-4 rounded-xl shadow-2xl border
                ${isError 
                    ? 'bg-red-950/80 border-red-500/50 text-red-100' 
                    : 'bg-green-950/80 border-green-500/50 text-green-100'
                } backdrop-blur-md
            `}>
                <div className="flex items-center gap-3">
                    {isError ? "⚠️" : "✓"} 
                    <span className="font-medium">{toast.message}</span>
                </div>
                <button onClick={clearToast} className="ml-4 opacity-70 hover:opacity-100">✕</button>
            </div>
        </div>
    );
};
