import { useEffect, useState } from 'react';
import { Pill, AlertCircle, TrendingUp, Clock } from 'lucide-react';
import { useAuth } from '../lib/auth';
import { supabase } from '../lib/supabase';
import type { Medicine, DoseLog } from '../lib/supabase';
import { Card } from '../components/Card';

type DashboardStats = {
  totalMedicines: number;
  todayDoses: number;
  takenToday: number;
  adherenceRate: number;
  lowStockCount: number;
};

export function DashboardPage() {
  const { user } = useAuth();
  const [stats, setStats] = useState<DashboardStats>({
    totalMedicines: 0,
    todayDoses: 0,
    takenToday: 0,
    adherenceRate: 0,
    lowStockCount: 0,
  });
  const [upcomingDoses, setUpcomingDoses] = useState<DoseLog[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (user) {
      fetchDashboardData();
    }
  }, [user]);

  async function fetchDashboardData() {
    try {
      const today = new Date();
      today.setHours(0, 0, 0, 0);
      const tomorrow = new Date(today);
      tomorrow.setDate(tomorrow.getDate() + 1);

      // Fetch medicines
      const { data: medicines } = await supabase
        .from('medicines')
        .select('*')
        .eq('user_id', user!.id)
        .eq('is_active', true);

      const activeMedicines = (medicines as Medicine[]) || [];

      // Fetch today's dose logs
      const { data: doseLogs } = await supabase
        .from('dose_logs')
        .select('*')
        .eq('user_id', user!.id)
        .gte('scheduled_at', today.toISOString())
        .lt('scheduled_at', tomorrow.toISOString())
        .order('scheduled_at', { ascending: true });

      const todayDoses = (doseLogs as DoseLog[]) || [];
      const takenToday = todayDoses.filter((d) => d.status === 'taken').length;
      const adherenceRate = todayDoses.length > 0 ? Math.round((takenToday / todayDoses.length) * 100) : 100;

      // Low stock count
      const lowStockCount = activeMedicines.filter(
        (m) => m.pill_count <= m.refill_threshold
      ).length;

      // Upcoming doses (pending only)
      const upcoming = todayDoses
        .filter((d) => d.status === 'pending')
        .slice(0, 5);

      setStats({
        totalMedicines: activeMedicines.length,
        todayDoses: todayDoses.length,
        takenToday,
        adherenceRate,
        lowStockCount,
      });
      setUpcomingDoses(upcoming);
    } catch (error) {
      console.error('Error fetching dashboard data:', error);
    } finally {
      setLoading(false);
    }
  }

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-[400px]">
        <p className="text-gray-500">Loading...</p>
      </div>
    );
  }

  return (
    <div className="space-y-8">
      {/* Welcome Header */}
      <div>
        <h1 className="text-2xl font-bold text-gray-900">
          Welcome back, {user?.name}!
        </h1>
        <p className="text-gray-600 mt-1">
          Here's your medication overview for today.
        </p>
      </div>

      {/* Stats Grid */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <StatCard
          icon={Pill}
          label="Active Medicines"
          value={stats.totalMedicines}
          color="teal"
        />
        <StatCard
          icon={Clock}
          label="Doses Today"
          value={stats.todayDoses}
          color="indigo"
        />
        <StatCard
          icon={TrendingUp}
          label="Adherence"
          value={`${stats.adherenceRate}%`}
          color="teal"
        />
        <StatCard
          icon={AlertCircle}
          label="Low Stock"
          value={stats.lowStockCount}
          color={stats.lowStockCount > 0 ? 'red' : 'teal'}
        />
      </div>

      {/* Upcoming Doses */}
      <Card>
        <div className="flex items-center gap-2 mb-4">
          <Clock className="w-5 h-5 text-teal-500" />
          <h2 className="text-lg font-semibold text-gray-900">Upcoming Doses</h2>
        </div>
        {upcomingDoses.length === 0 ? (
          <p className="text-gray-500 text-center py-8">
            No upcoming doses for today.
          </p>
        ) : (
          <div className="space-y-3">
            {upcomingDoses.map((dose) => {
              const time = new Date(dose.scheduled_at).toLocaleTimeString([], {
                hour: '2-digit',
                minute: '2-digit',
              });
              return (
                <div
                  key={dose.id}
                  className="flex items-center justify-between p-3 bg-white rounded-lg border border-gray-200"
                >
                  <div className="flex items-center gap-3">
                    <div className="w-10 h-10 rounded-full bg-teal-100 flex items-center justify-center">
                      <Pill className="w-5 h-5 text-teal-600" />
                    </div>
                    <div>
                      <p className="font-medium text-gray-900">
                        {dose.medicine_id}
                      </p>
                      <p className="text-sm text-gray-500">{time}</p>
                    </div>
                  </div>
                  <span className="px-2 py-1 text-xs font-medium bg-amber-100 text-amber-700 rounded-full">
                    Pending
                  </span>
                </div>
              );
            })}
          </div>
        )}
      </Card>
    </div>
  );
}

type StatCardProps = {
  icon: React.ComponentType<{ className?: string }>;
  label: string;
  value: string | number;
  color: 'teal' | 'indigo' | 'red' | 'amber';
};

function StatCard({ icon: Icon, label, value, color }: StatCardProps) {
  const colorClasses = {
    teal: 'bg-teal-100 text-teal-600',
    indigo: 'bg-indigo-100 text-indigo-600',
    red: 'bg-red-100 text-red-600',
    amber: 'bg-amber-100 text-amber-700',
  };

  return (
    <Card>
      <div className="flex items-start gap-4">
        <div className={`w-12 h-12 rounded-xl flex items-center justify-center ${colorClasses[color]}`}>
          <Icon className="w-6 h-6" />
        </div>
        <div>
          <p className="text-sm text-gray-500">{label}</p>
          <p className="text-2xl font-bold text-gray-900 mt-1">{value}</p>
        </div>
      </div>
    </Card>
  );
}
