import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuthStore } from '../store/useAuthStore';
import { useNotificationStore } from '../store/useNotificationStore';
import { authService } from '../services/authService';
import { Loader2 } from 'lucide-react';

const Login = () => {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  
  const setAuth = useAuthStore(state => state.setAuth);
  const showToast = useNotificationStore(state => state.showToast);
  const navigate = useNavigate();

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError(null);
    try {
      const data = await authService.login({ username, password });
      setAuth(data.token, { username: data.username, role: data.role, sessionId: data.sessionId });
      showToast(`Welcome back, ${data.username}!`);
      if (data.role === 'ADMIN') {
        navigate('/admin');
      } else {
        navigate('/dashboard');
      }
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <form onSubmit={handleSubmit} className="flex flex-col gap-5 w-full">
      <div className="flex flex-col gap-1.5">
        <label className="text-sm text-gray-400">Username</label>
        <input 
          type="text" 
          value={username} onChange={(e) => setUsername(e.target.value)}
          className="bg-black/20 border border-white/10 rounded-lg p-3 text-white focus:outline-none focus:border-cyan-400 focus:ring-1 focus:ring-cyan-400/50 transition-all"
          required
        />
      </div>
      <div className="flex flex-col gap-1.5">
        <label className="text-sm text-gray-400">Password</label>
        <input 
          type="password" 
          value={password} onChange={(e) => setPassword(e.target.value)}
          className="bg-black/20 border border-white/10 rounded-lg p-3 text-white focus:outline-none focus:border-cyan-400 focus:ring-1 focus:ring-cyan-400/50 transition-all"
          required
        />
      </div>

      {error && (
        <div className="bg-red-500/10 border border-red-500/20 text-red-400 p-3 rounded-lg text-sm">
          {error}
        </div>
      )}

      <button 
        type="submit" 
        disabled={loading}
        className="mt-2 bg-gradient-to-r from-cyan-400 to-cyan-500 hover:from-cyan-300 hover:to-cyan-400 text-black font-semibold p-3 rounded-lg transition-all box-glow flex items-center justify-center disabled:opacity-70 disabled:cursor-not-allowed"
      >
        {loading ? <Loader2 className="animate-spin mr-2" size={20} /> : 'Sign In'}
      </button>

      <p className="text-center text-sm text-gray-400 mt-2">
        Don't have an account? <Link to="/register" className="text-cyan-400 hover:text-cyan-300 transition-colors">Register</Link>
      </p>
    </form>
  );
};

export default Login;
