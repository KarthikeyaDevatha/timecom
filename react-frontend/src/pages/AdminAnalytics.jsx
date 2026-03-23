import React, { useState, useEffect, useCallback, useMemo } from 'react';
import { adminService } from '../services/adminService';
import { useNotificationStore } from '../store/useNotificationStore';
import { 
  BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, 
  PieChart, Pie, Cell, Legend
} from 'recharts';
import { Users, Activity, Zap, Server, Trash2, RefreshCw } from 'lucide-react';
import { formatDistanceToNow } from 'date-fns';

const COLORS = ['#00e5ff', '#7e57c2', '#00e676', '#ff5252', '#ffd740'];

const AdminAnalytics = () => {
  const showToast = useNotificationStore(state => state.showToast);
  
  const [analytics, setAnalytics] = useState(null);
  const [sessions, setSessions] = useState([]);
  const [loading, setLoading] = useState(true);

  const fetchData = useCallback(async () => {
    setLoading(true);
    try {
      const [analyticsData, sessionsData] = await Promise.all([
        adminService.getAnalyticsSummary(),
        adminService.getAllPlatformSessions()
      ]);
      setAnalytics(analyticsData);
      setSessions(sessionsData || []);
    } catch (err) {
      if (err.message !== 'Unauthorized') {
        showToast('Failed to load initial analytics', 'error');
      }
    } finally {
      setLoading(false);
    }
  }, [showToast]);

  useEffect(() => {
    fetchData();
  }, [fetchData]);

  const handleTerminateSession = async (sessionId) => {
    if (!window.confirm(`Force terminate global session #${sessionId}?`)) return;
    try {
      await adminService.terminateAnySession(sessionId);
      showToast(`Session #${sessionId} terminated`, 'success');
      fetchData(); // Refresh all
    } catch (err) {
      showToast(err.message, 'error');
    }
  };

  // Prepare chart data
  const activityChartData = useMemo(() => {
    if (!analytics?.activityBreakdownToday) return [];
    return Object.entries(analytics.activityBreakdownToday).map(([name, count]) => ({
      name: name.replace('_', ' '),
      count
    }));
  }, [analytics]);

  const deviceChartData = useMemo(() => {
    if (!analytics?.sessionsByDeviceType) return [];
    return Object.entries(analytics.sessionsByDeviceType).map(([name, value]) => ({
      name, value
    }));
  }, [analytics]);

  return (
    <div className="space-y-8">
      {/* Header */}
      <div className="flex justify-between items-end">
        <div>
          <h1 className="text-3xl font-bold tracking-tight text-purple-400">System Analytics</h1>
          <p className="text-gray-400 mt-1">Platform-wide real-time session monitoring</p>
        </div>
        <button 
          onClick={fetchData} disabled={loading}
          className="btn-outline px-4 py-2 border border-white/20 hover:bg-white/10 rounded-lg flex items-center gap-2 transition-colors disabled:opacity-50"
        >
          <RefreshCw size={16} className={loading ? "animate-spin text-cyan-400" : ""} /> 
          Refresh Data
        </button>
      </div>

      {/* KPI Row */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
        <KpiCard title="Active Sessions" value={analytics?.totalActiveSessions ?? '-'} icon={<Server />} color="cyan" />
        <KpiCard title="Unique Users" value={analytics?.totalActiveUsers ?? '-'} icon={<Users />} color="purple" />
        <KpiCard title="Events (Today)" value={analytics?.totalActivitiesToday ?? '-'} icon={<Activity />} color="green" />
        <KpiCard title="Events (Week)" value={analytics?.totalActivitiesThisWeek ?? '-'} icon={<Zap />} color="yellow" />
      </div>

      {/* Charts Row */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <div className="glass-panel p-6 h-96 flex flex-col">
          <h3 className="text-lg font-semibold mb-6 flex items-center gap-2">
            Activity Breakdown <span className="text-xs font-normal text-gray-500">(Today)</span>
          </h3>
          <div className="flex-1 min-h-0">
            {activityChartData.length > 0 ? (
              <ResponsiveContainer width="100%" height="100%">
                <BarChart data={activityChartData} margin={{ top: 0, right: 0, left: -20, bottom: 0 }}>
                  <CartesianGrid strokeDasharray="3 3" stroke="#333" vertical={false} />
                  <XAxis dataKey="name" stroke="#888" tick={{fill: '#888', fontSize: 12}} />
                  <YAxis stroke="#888" tick={{fill: '#888', fontSize: 12}} allowDecimals={false} />
                  <Tooltip 
                    cursor={{fill: 'rgba(255,255,255,0.05)'}}
                    contentStyle={{ backgroundColor: '#1a1d24', border: '1px solid #333', borderRadius: '8px' }}
                  />
                  <Bar dataKey="count" fill="var(--color-primary)" radius={[4, 4, 0, 0]} />
                </BarChart>
              </ResponsiveContainer>
            ) : (
                <div className="h-full flex items-center justify-center text-gray-500">No activity data for today</div>
            )}
          </div>
        </div>

        <div className="glass-panel p-6 h-96 flex flex-col">
          <h3 className="text-lg font-semibold mb-6">Device Distribution</h3>
          <div className="flex-1 min-h-0">
            {deviceChartData.length > 0 ? (
               <ResponsiveContainer width="100%" height="100%">
                <PieChart>
                  <Pie
                    data={deviceChartData}
                    cx="50%" cy="45%"
                    innerRadius={60} outerRadius={90}
                    paddingAngle={5} dataKey="value"
                  >
                    {deviceChartData.map((entry, index) => (
                      <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                    ))}
                  </Pie>
                  <Tooltip contentStyle={{ backgroundColor: '#1a1d24', border: '1px solid #333', borderRadius: '8px' }}/>
                  <Legend verticalAlign="bottom" height={36}/>
                </PieChart>
              </ResponsiveContainer>
            ) : (
                <div className="h-full flex items-center justify-center text-gray-500">No session data available</div>
            )}
          </div>
        </div>
      </div>

      {/* Global Sessions Table */}
      <div className="glass-panel overflow-hidden">
        <div className="p-5 border-b border-white/10 bg-black/20">
          <h3 className="font-semibold text-lg flex items-center gap-2">
            Global Active Sessions <span className="bg-cyan-500/10 text-cyan-400 border border-cyan-500/20 px-2 py-0.5 rounded-full text-xs font-medium">{sessions.length} Live</span>
          </h3>
        </div>
        
        <div className="overflow-x-auto">
          <table className="w-full text-left text-sm whitespace-nowrap">
            <thead className="bg-white/5 text-gray-400 uppercase tracking-wider text-xs">
              <tr>
                <th className="px-6 py-4 font-medium">Session ID</th>
                <th className="px-6 py-4 font-medium">User</th>
                <th className="px-6 py-4 font-medium">IP / Device</th>
                <th className="px-6 py-4 font-medium">Started</th>
                <th className="px-6 py-4 font-medium">Last Activity</th>
                <th className="px-6 py-4 font-medium text-right">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-white/5">
              {loading && sessions.length === 0 ? (
                 <tr><td colSpan={6} className="px-6 py-8 text-center text-gray-500">Loading data...</td></tr>
              ) : sessions.length === 0 ? (
                 <tr><td colSpan={6} className="px-6 py-8 text-center text-gray-500">No active sessions found across the platform.</td></tr>
              ) : (
                sessions.map(session => (
                  <tr key={session.id} className="hover:bg-white/5 transition-colors group">
                    <td className="px-6 py-4">
                        <span className="font-mono text-gray-400">#{session.id}</span>
                    </td>
                    <td className="px-6 py-4 font-medium text-white">{session.username}</td>
                    <td className="px-6 py-4">
                        <div className="flex items-center gap-2">
                          <span className={`w-1.5 h-1.5 rounded-full ${session.active ? 'bg-green-400 box-glow' : 'bg-gray-600'}`}></span>
                          {session.ipAddress} <span className="text-gray-500 text-xs">({session.deviceType})</span>
                        </div>
                    </td>
                    <td className="px-6 py-4 text-gray-400 text-xs">
                         {new Date(session.createdAt).toLocaleString()}
                    </td>
                    <td className="px-6 py-4 text-gray-400 text-xs">
                        {session.lastActivityAt ? formatDistanceToNow(new Date(session.lastActivityAt), { addSuffix: true }) : 'Never'}
                    </td>
                    <td className="px-6 py-4 text-right">
                        <button 
                          onClick={() => handleTerminateSession(session.id)}
                          className="text-red-400 hover:text-red-300 hover:bg-red-500/10 p-2 rounded transition-colors opacity-0 group-hover:opacity-100 focus:opacity-100"
                          title="Force Terminate"
                        >
                          <Trash2 size={16} />
                        </button>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
};

// KPI Card Widget
const KpiCard = ({ title, value, icon, color }) => {
    const colorClasses = {
        cyan: 'text-cyan-400 border-t-cyan-400 bg-cyan-950/20',
        purple: 'text-purple-400 border-t-purple-400 bg-purple-950/20',
        green: 'text-green-400 border-t-green-400 bg-green-950/20',
        yellow: 'text-yellow-400 border-t-yellow-400 bg-yellow-950/20',
    };

    const sel = colorClasses[color] || colorClasses.cyan;

    return (
        <div className={`glass-card p-5 border-t-2 flex flex-col gap-3 ${sel}`}>
            <div className="flex justify-between items-start opacity-70">
                <span className="text-sm font-medium uppercase tracking-wider text-gray-300">{title}</span>
                {icon}
            </div>
            <div className="text-4xl font-bold tracking-tight text-white drop-shadow-lg">
                {value}
            </div>
        </div>
    );
};

export default AdminAnalytics;
