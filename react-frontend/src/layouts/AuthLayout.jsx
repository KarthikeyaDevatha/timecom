import React from 'react';
import { Navigate, Outlet } from 'react-router-dom';
import { useAuthStore } from '../store/useAuthStore';
import { Activity } from 'lucide-react';

const AuthLayout = () => {
  const isAuthenticated = useAuthStore((state) => state.isAuthenticated());

  // redirect to dashboard if already logged in
  if (isAuthenticated) {
    const isAdmin = useAuthStore.getState().isAdmin();
    return <Navigate to={isAdmin ? '/admin' : '/dashboard'} replace />;
  }

  return (
    <div className="flex flex-col items-center justify-center min-h-screen p-4">
      <div className="w-full max-w-md p-8 glass-panel animate-in fade-in slide-in-from-bottom-4 duration-500">
        <div className="flex flex-col items-center mb-8">
          <div className="flex items-center justify-center w-12 h-12 mb-4 rounded-xl bg-gradient-to-br from-cyan-400 to-purple-600 box-glow text-white">
            <Activity size={24} />
          </div>
          <h1 className="text-2xl font-bold text-center">Welcome Back</h1>
          <p className="text-gray-400 text-sm mt-1">Secure session management & analytics</p>
        </div>
        
        <Outlet />
      </div>
    </div>
  );
};

export default AuthLayout;
