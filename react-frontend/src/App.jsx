import React, { lazy, Suspense } from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import AuthLayout from './layouts/AuthLayout';
import { MainLayout, ToastContainer } from './layouts/MainLayout';
import Login from './pages/Login';
import Register from './pages/Register';
import { Loader2 } from 'lucide-react';

// Lazy load main views for code splitting
const UserDashboard = lazy(() => import('./pages/UserDashboard'));
const AdminAnalytics = lazy(() => import('./pages/AdminAnalytics'));

const AppLoader = () => (
  <div className="flex h-screen w-full items-center justify-center bg-[#0f1115]">
    <Loader2 className="animate-spin text-cyan-400" size={48} />
  </div>
);

function App() {
  return (
    <>
      <BrowserRouter>
        <Routes>
          {/* Auth Routes */}
          <Route element={<AuthLayout />}>
            <Route path="/login" element={<Login />} />
            <Route path="/register" element={<Register />} />
          </Route>

          {/* Protected Dashboard Routes */}
          <Route element={<MainLayout />}>
            <Route 
              path="/dashboard" 
              element={
                <Suspense fallback={<AppLoader />}>
                  <UserDashboard />
                </Suspense>
              } 
            />
            <Route 
              path="/admin" 
              element={
                <Suspense fallback={<AppLoader />}>
                  <AdminAnalytics />
                </Suspense>
              } 
            />
          </Route>

          {/* Fallback standard redirect to login state */}
          <Route path="/" element={<Navigate to="/login" replace />} />
          <Route path="*" element={<Navigate to="/login" replace />} />
        </Routes>
      </BrowserRouter>
      <ToastContainer />
    </>
  );
}

export default App;
