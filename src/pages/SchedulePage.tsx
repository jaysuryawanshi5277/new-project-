import { useEffect, useState } from 'react';
import { Calendar, Check, X, Clock } from 'lucide-react';
import { useAuth } from '../lib/auth';
import { supabase } from '../lib/supabase';
import type { DoseLog, Medicine } from '../lib/supabase';
import { Card } from '../components/Card';

export function SchedulePage() {
  const { user } = useAuth();
  const [doseLogs, setDoseLogs] = useState<DoseLog[]>([]);
  const [medicines, setMedicines] = useState<Medicine[]>([]);
  const [loading, setLoading] = useState(true);
  const [selectedDate, setSelectedDate] = useState(new Date());

  useEffect(() => {
    if (user) {
      fetchData();
    }
  }, [user, selectedDate]);

  async function fetchData() {
    try {
      // Fetch medicines for reference
      const { data: medsData } = await supabase
        .from('medicines')
        .select('*')
        .eq('user_id', user!.id);
      setMedicines((medsData as Medicine[]) || []);

      // Fetch dose logs for selected date
      const startOfDay = new Date(selectedDate);
      startOfDay.setHours(0, 0, 0, 0);
      const endOfDay = new Date(selectedDate);
      endOfDay.setHours(23, 59, 59, 999);

      const { data: logsData } = await supabase
        .from('dose_logs')
        .select('*')
        .eq('user_id', user!.id)
        .gte('scheduled_at', startOfDay.toISOString())
        .lte('scheduled_at', endOfDay.toISOString())
        .order('scheduled_at', { ascending: true });

      setDoseLogs((logsData as DoseLog[]) || []);
    } catch (error) {
      console.error('Error fetching schedule data:', error);
    } finally {
      setLoading(false);
    }
  }

  async function markDose(logId: string, status: 'taken' | 'skipped') {
    try {
      const now = new Date().toISOString();
      await supabase
        .from('dose_logs')
        .update({
          status,
          taken_at: status === 'taken' ? now : null,
        })
        .eq('id', logId);
      fetchData();
    } catch (error) {
      console.error('Error updating dose:', error);
    }
  }

  function getMedicineName(medicineId: string) {
    const medicine = medicines.find((m) => m.id === medicineId);
    return medicine?.name || 'Unknown Medicine';
  }

  function getStatusStyles(status: string) {
    switch (status) {
      case 'taken':
        return 'bg-teal-100 text-teal-700';
      case 'skipped':
        return 'bg-gray-100 text-gray-600';
      case 'missed':
        return 'bg-red-100 text-red-700';
      default:
        return 'bg-amber-100 text-amber-700';
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
    <div className="space-y-6">
      {/* Header */}
      <div>
        <h1 className="text-2xl font-bold text-gray-900">Schedule</h1>
        <p className="text-gray-600 mt-1">
          Track and manage your dose schedule.
        </p>
      </div>

      {/* Date Selector */}
      <Card>
        <div className="flex items-center gap-4">
          <Calendar className="w-5 h-5 text-teal-500" />
          <input
            type="date"
            value={selectedDate.toISOString().split('T')[0]}
            onChange={(e) => setSelectedDate(new Date(e.target.value))}
            className="px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-teal-500"
          />
        </div>
      </Card>

      {/* Dose List */}
      {doseLogs.length === 0 ? (
        <Card className="text-center py-12">
          <Clock className="w-12 h-12 text-gray-300 mx-auto mb-4" />
          <h3 className="text-lg font-medium text-gray-900 mb-1">
            No doses scheduled
          </h3>
          <p className="text-gray-500">
            No doses are scheduled for this day.
          </p>
        </Card>
      ) : (
        <div className="space-y-4">
          {doseLogs.map((log) => {
            const time = new Date(log.scheduled_at).toLocaleTimeString([], {
              hour: '2-digit',
              minute: '2-digit',
            });
            const statusStyles = getStatusStyles(log.status);

            return (
              <Card key={log.id}>
                <div className="flex items-center justify-between gap-4">
                  <div className="flex items-center gap-4">
                    <div className={`w-12 h-12 rounded-xl flex items-center justify-center ${
                      log.status === 'taken' ? 'bg-teal-100' :
                      log.status === 'missed' ? 'bg-red-100' :
                      log.status === 'skipped' ? 'bg-gray-100' : 'bg-amber-100'
                    }`}>
                      <Clock className={`w-6 h-6 ${
                        log.status === 'taken' ? 'text-teal-600' :
                        log.status === 'missed' ? 'text-red-600' :
                        log.status === 'skipped' ? 'text-gray-600' : 'text-amber-600'
                      }`} />
                    </div>
                    <div>
                      <h3 className="font-semibold text-gray-900">
                        {getMedicineName(log.medicine_id)}
                      </h3>
                      <p className="text-sm text-gray-600">{time}</p>
                    </div>
                  </div>

                  <div className="flex items-center gap-3">
                    <span className={`px-3 py-1 text-xs font-medium rounded-full ${statusStyles}`}>
                      {log.status.charAt(0).toUpperCase() + log.status.slice(1)}
                    </span>
                    {log.status === 'pending' && (
                      <div className="flex gap-2">
                        <button
                          onClick={() => markDose(log.id, 'taken')}
                          className="p-2 text-teal-600 hover:bg-teal-50 rounded-lg transition-colors"
                          title="Mark as taken"
                        >
                          <Check className="w-5 h-5" />
                        </button>
                        <button
                          onClick={() => markDose(log.id, 'skipped')}
                          className="p-2 text-gray-400 hover:bg-gray-100 rounded-lg transition-colors"
                          title="Skip dose"
                        >
                          <X className="w-5 h-5" />
                        </button>
                      </div>
                    )}
                  </div>
                </div>
              </Card>
            );
          })}
        </div>
      )}
    </div>
  );
}
