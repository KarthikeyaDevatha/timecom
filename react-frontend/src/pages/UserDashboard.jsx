import React, { useState, useEffect, useCallback } from 'react';
import { useAuthStore } from '../store/useAuthStore';
import { useNotificationStore } from '../store/useNotificationStore';
import { sessionService } from '../services/sessionService';
import { trackingService } from '../services/trackingService';
import { ShoppingCart, Eye, CreditCard, Settings, RefreshCw, Trash2, ShieldCheck, Clock, Monitor } from 'lucide-react';
import { formatDistanceToNow } from 'date-fns';

const UserDashboard = () => {
  const { user } = useAuthStore();
  const showToast = useNotificationStore(state => state.showToast);
  
  const [sessions, setSessions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [trackingEvent, setTrackingEvent] = useState(null);

  const fetchSessions = useCallback(async () => {
    try {
      setLoading(true);
      const data = await sessionService.getAllUserSessions();
      setSessions(data || []);
    } catch (err) {
      if (err.message !== 'Unauthorized') {
        showToast('Failed to load active sessions', 'error');
      }
    } finally {
      setLoading(false);
    }
  }, [showToast]);

  useEffect(() => {
    fetchSessions();
  }, [fetchSessions]);

  const handleTrackEvent = async (actionType, path) => {
    if (trackingEvent) return;
    setTrackingEvent(actionType);
    try {
      await trackingService.trackEvent({
        actionType,
        resourcePath: path,
        resourceId: `demo-${Math.floor(Math.random() * 1000)}`,
        metadata: { source: 'dashboard_simulator' }
      });
      showToast(`Event tracked: ${actionType.replace('_', ' ')}`, 'success');
      // Background refresh to update Last Activity
      sessionService.getAllUserSessions().then(data => setSessions(data || [])).catch(() => {});
    } catch (err) {
      showToast(err.message || 'Tracking failed', 'error');
    } finally {
      setTrackingEvent(null);
    }
  };

  const handleTerminateSession = async (sessionId) => {
    if (!window.confirm('Terminate this session? The device will be logged out.')) return;
    try {
      await sessionService.terminateSession(sessionId);
      showToast('Session terminated successfully');
      fetchSessions();
    } catch (err) {
      showToast(err.message, 'error');
    }
  };

  return (
    <div className="space-y-8">
      {/* Header */}
      <div>
        <h1 className="text-3xl font-bold tracking-tight">Dashboard</h1>
        <p className="text-gray-400 mt-1">Manage your active sessions and trigger events</p>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        
        {/* Activity Simulator */}
        <div className="glass-card flex flex-col h-full lg:col-span-1">
          <div className="p-5 border-b border-white/10 flex justify-between items-center">
            <h3 className="font-semibold text-lg flex items-center gap-2">
              <ActivityIcon /> Simulate Activity
            </h3>
            <span className="bg-green-500/10 text-green-400 border border-green-500/20 px-2 py-0.5 rounded-full text-xs font-medium">Live</span>
          </div>
          <div className="p-5 flex-1">
            <p className="text-sm text-gray-400 mb-6">Trigger tracking events for the current session to see real-time updates.</p>
            <div className="grid grid-cols-2 gap-3">
              <ActionButton 
                icon={<Eye size={20} />} label="View Product" 
                loading={trackingEvent === 'PRODUCT_VIEW'} 
                onClick={() => handleTrackEvent('PRODUCT_VIEW', '/products/featured')} 
              />
              <ActionButton 
                icon={<ShoppingCart size={20} />} label="Add to Cart" 
                loading={trackingEvent === 'ADD_TO_CART'} 
                onClick={() => handleTrackEvent('ADD_TO_CART', '/cart/add')} 
              />
              <ActionButton 
                icon={<CreditCard size={20} />} label="Checkout" 
                loading={trackingEvent === 'CHECKOUT'} 
                onClick={() => handleTrackEvent('CHECKOUT', '/checkout')} 
              />
              <ActionButton 
                icon={<Settings size={20} />} label="Settings" 
                loading={trackingEvent === 'CUSTOM'} 
                onClick={() => handleTrackEvent('CUSTOM', '/profile/settings')} 
              />
            </div>
          </div>
        </div>

        {/* Sessions Manager */}
        <div className="glass-card flex flex-col h-full lg:col-span-2">
          <div className="p-5 border-b border-white/10 flex justify-between items-center">
            <h3 className="font-semibold text-lg flex items-center gap-2">
              <ShieldCheck size={20} className="text-cyan-400" /> Active Sessions
            </h3>
            <button 
              onClick={fetchSessions} disabled={loading}
              className="p-2 text-gray-400 hover:text-cyan-400 rounded-lg transition-colors disabled:opacity-50"
            >
              <RefreshCw size={18} className={loading && !trackingEvent ? "animate-spin" : ""} />
            </button>
          </div>
          
          <div className="p-5 flex-1">
            {loading && sessions.length === 0 ? (
              <div className="space-y-3">
                {[1,2,3].map(i => <div key={i} className="h-20 w-full bg-white/5 animate-pulse rounded-xl"></div>)}
              </div>
            ) : sessions.length === 0 ? (
              <p className="text-gray-400 text-center py-8">No active sessions found.</p>
            ) : (
              <div className="space-y-4">
                {sessions.map(session => {
                  const isCurrent = session.id === user?.sessionId;
                  return (
                    <div 
                      key={session.id} 
                      className={`p-4 rounded-xl border transition-all flex flex-col sm:flex-row gap-4 items-start sm:items-center justify-between
                        ${isCurrent 
                          ? 'bg-cyan-500/5 border-cyan-500/30 box-glow' 
                          : 'bg-black/20 border-white/10 hover:border-white/20 hover:bg-black/40'}`}
                    >
                      <div className="flex items-start gap-4">
                        <div className={`p-3 rounded-lg ${isCurrent ? 'bg-cyan-500/20 text-cyan-400' : 'bg-white/10 text-gray-300'}`}>
                          <Monitor size={24} />
                        </div>
                        <div>
                          <div className="flex items-center gap-2 mb-1">
                            <div className={`w-2 h-2 rounded-full ${session.active ? 'bg-green-400 box-glow' : 'bg-gray-500'}`} />
                            <span className="font-medium">{session.ipAddress}</span>
                            <span className="text-xs text-gray-400 uppercase tracking-wide">({session.deviceType})</span>
                            {isCurrent && (
                              <span className="ml-2 text-xs font-semibold bg-cyan-500/20 text-cyan-400 px-2 py-0.5 rounded">Current</span>
                            )}
                          </div>
                          <div className="text-sm text-gray-400 flex flex-col sm:flex-row sm:gap-4">
                            <span className="flex items-center gap-1"><Clock size={14}/> Started: {new Date(session.createdAt).toLocaleString(undefined, { month:'short', day:'numeric', hour:'2-digit', minute:'2-digit'})}</span>
                            <span className="flex items-center gap-1">
                                Activity: {session.lastActivityAt ? formatDistanceToNow(new Date(session.lastActivityAt), { addSuffix: true }) : 'Never'}
                            </span>
                          </div>
                        </div>
                      </div>

                      {!isCurrent && (
                        <button 
                          onClick={() => handleTerminateSession(session.id)}
                          className="w-full sm:w-auto px-4 py-2 border border-red-500/20 text-red-400 hover:bg-red-500/10 hover:border-red-500/50 rounded-lg transition-all text-sm font-medium flex items-center justify-center gap-2"
                        >
                          <Trash2 size={16} /> Terminate
                        </button>
                      )}
                    </div>
                  );
                })}
              </div>
            )}
          </div>
        </div>

      </div>
    </div>
  );
};

// Helper Components
const ActivityIcon = () => (
  <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" className="text-purple-400">
    <polyline points="22 12 18 12 15 21 9 3 6 12 2 12"></polyline>
  </svg>
);

const ActionButton = ({ icon, label, loading, onClick }) => (
  <button 
    onClick={onClick} disabled={loading}
    className="flex flex-col items-center justify-center p-4 bg-white/5 border border-white/10 rounded-xl hover:bg-white/10 hover:border-cyan-400/50 transition-all gap-2 disabled:opacity-50 group"
  >
    <div className={`text-gray-300 group-hover:text-cyan-400 transition-colors ${loading ? 'animate-pulse' : ''}`}>
      {icon}
    </div>
    <span className="text-xs font-medium text-gray-300 group-hover:text-white transition-colors">{label}</span>
  </button>
);

export default UserDashboard;
