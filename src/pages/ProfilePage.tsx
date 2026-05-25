import { useState } from 'react';
import { User, Save, Bell, Shield, Download } from 'lucide-react';
import { useAuth } from '../lib/auth';
import { supabase } from '../lib/supabase';
import { Button, Card } from '../components/Card';

export function ProfilePage() {
  const { user } = useAuth();
  const [name, setName] = useState(user?.name || '');
  const [phone, setPhone] = useState(user?.phone || '');
  const [saving, setSaving] = useState(false);
  const [saved, setSaved] = useState(false);

  async function handleSave() {
    if (!user) return;
    setSaving(true);
    try {
      await supabase
        .from('users')
        .update({ name, phone })
        .eq('id', user.id);
      setSaved(true);
      setTimeout(() => setSaved(false), 2000);
    } catch (error) {
      console.error('Error updating profile:', error);
    } finally {
      setSaving(false);
    }
  }

  return (
    <div className="space-y-6 max-w-2xl">
      {/* Header */}
      <div>
        <h1 className="text-2xl font-bold text-gray-900">Profile</h1>
        <p className="text-gray-600 mt-1">
          Manage your account settings.
        </p>
      </div>

      {/* Profile Form */}
      <Card>
        <div className="space-y-6">
          <div className="flex items-center gap-4 mb-6">
            <div className="w-16 h-16 rounded-full bg-teal-100 flex items-center justify-center">
              <User className="w-8 h-8 text-teal-600" />
            </div>
            <div>
              <h2 className="text-lg font-semibold text-gray-900">
                {user?.name}
              </h2>
              <p className="text-sm text-gray-500">{user?.email}</p>
            </div>
          </div>

          <div className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Full Name
              </label>
              <input
                type="text"
                value={name}
                onChange={(e) => setName(e.target.value)}
                className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-teal-500"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Phone Number
              </label>
              <input
                type="tel"
                value={phone}
                onChange={(e) => setPhone(e.target.value)}
                placeholder="(555) 123-4567"
                className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-teal-500"
              />
            </div>
            <div className="pt-4">
              <Button onClick={handleSave} disabled={saving}>
                <Save className="w-4 h-4" />
                {saving ? 'Saving...' : saved ? 'Saved!' : 'Save Changes'}
              </Button>
            </div>
          </div>
        </div>
      </Card>

      {/* Notifications */}
      <Card>
        <div className="flex items-start gap-4">
          <div className="w-10 h-10 rounded-lg bg-amber-100 flex items-center justify-center">
            <Bell className="w-5 h-5 text-amber-600" />
          </div>
          <div className="flex-1">
            <h3 className="font-semibold text-gray-900">Notifications</h3>
            <p className="text-sm text-gray-600 mt-1">
              Receive reminders when doses are due and alerts for missed medications.
            </p>
          </div>
        </div>
      </Card>

      {/* Security */}
      <Card>
        <div className="flex items-start gap-4">
          <div className="w-10 h-10 rounded-lg bg-indigo-100 flex items-center justify-center">
            <Shield className="w-5 h-5 text-indigo-600" />
          </div>
          <div className="flex-1">
            <h3 className="font-semibold text-gray-900">Security</h3>
            <p className="text-sm text-gray-600 mt-1">
              Your data is protected with industry-standard encryption and secure authentication.
            </p>
          </div>
        </div>
      </Card>

      {/* Export */}
      <Card>
        <div className="flex items-start gap-4">
          <div className="w-10 h-10 rounded-lg bg-teal-100 flex items-center justify-center">
            <Download className="w-5 h-5 text-teal-600" />
          </div>
          <div className="flex-1">
            <h3 className="font-semibold text-gray-900">Export Data</h3>
            <p className="text-sm text-gray-600 mt-1 mb-4">
              Download your medication history and adherence reports.
            </p>
            <Button variant="secondary" size="sm">
              <Download className="w-4 h-4" />
              Export to PDF
            </Button>
          </div>
        </div>
      </Card>
    </div>
  );
}
