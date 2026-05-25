import { useEffect, useState } from 'react';
import { Pill, Plus, CreditCard as Edit2, Trash2, CircleAlert as AlertCircle, Camera } from 'lucide-react';
import { useAuth } from '../lib/auth';
import { supabase } from '../lib/supabase';
import type { Medicine } from '../lib/supabase';
import { Button, Card } from '../components/Card';
import { CameraScanner } from '../components/CameraScanner';

export function MedicinesPage() {
  const { user } = useAuth();
  const [medicines, setMedicines] = useState<Medicine[]>([]);
  const [loading, setLoading] = useState(true);
  const [showModal, setShowModal] = useState(false);
  const [editingMedicine, setEditingMedicine] = useState<Medicine | null>(null);
  const [formData, setFormData] = useState({
    name: '',
    dosage: '',
    form: 'tablet',
    pill_count: 0,
    refill_threshold: 10,
  });
  const [showScanner, setShowScanner] = useState(false);
  const [hasCamera, setHasCamera] = useState<boolean | null>(null);

  // Check camera availability on mount
  useEffect(() => {
    async function checkCamera() {
      try {
        const devices = await navigator.mediaDevices.enumerateDevices();
        const hasVideoDevice = devices.some(device => device.kind === 'videoinput');
        setHasCamera(hasVideoDevice);
      } catch {
        setHasCamera(false);
      }
    }
    checkCamera();
  }, []);

  useEffect(() => {
    if (user) {
      fetchMedicines();
    }
  }, [user]);

  async function fetchMedicines() {
    try {
      const { data } = await supabase
        .from('medicines')
        .select('*')
        .eq('user_id', user!.id)
        .order('created_at', { ascending: false });
      setMedicines((data as Medicine[]) || []);
    } catch (error) {
      console.error('Error fetching medicines:', error);
    } finally {
      setLoading(false);
    }
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!user) return;

    try {
      if (editingMedicine) {
        await supabase
          .from('medicines')
          .update({
            name: formData.name,
            dosage: formData.dosage,
            form: formData.form,
            pill_count: formData.pill_count,
            refill_threshold: formData.refill_threshold,
          })
          .eq('id', editingMedicine.id);
      } else {
        await supabase.from('medicines').insert({
          user_id: user.id,
          name: formData.name,
          dosage: formData.dosage,
          form: formData.form,
          pill_count: formData.pill_count,
          refill_threshold: formData.refill_threshold,
          is_active: true,
        });
      }
      setShowModal(false);
      setEditingMedicine(null);
      setFormData({
        name: '',
        dosage: '',
        form: 'tablet',
        pill_count: 0,
        refill_threshold: 10,
      });
      fetchMedicines();
    } catch (error) {
      console.error('Error saving medicine:', error);
    }
  }

  async function handleDelete(id: string) {
    if (!confirm('Are you sure you want to delete this medicine?')) return;
    try {
      await supabase.from('medicines').delete().eq('id', id);
      fetchMedicines();
    } catch (error) {
      console.error('Error deleting medicine:', error);
    }
  }

  function openEditModal(medicine: Medicine) {
    setEditingMedicine(medicine);
    setFormData({
      name: medicine.name,
      dosage: medicine.dosage,
      form: medicine.form,
      pill_count: medicine.pill_count,
      refill_threshold: medicine.refill_threshold,
    });
    setShowModal(true);
  }

  function handleScanCapture(name: string, dosage: string) {
    setFormData(prev => ({
      ...prev,
      name: name || prev.name,
      dosage: dosage || prev.dosage,
    }));
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
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Medicines</h1>
          <p className="text-gray-600 mt-1">
            Manage your medication inventory.
          </p>
        </div>
        <Button onClick={() => setShowModal(true)}>
          <Plus className="w-4 h-4" />
          Add Medicine
        </Button>
      </div>

      {/* Medicines List */}
      {medicines.length === 0 ? (
        <Card className="text-center py-12">
          <Pill className="w-12 h-12 text-gray-300 mx-auto mb-4" />
          <h3 className="text-lg font-medium text-gray-900 mb-1">
            No medicines yet
          </h3>
          <p className="text-gray-500 mb-4">
            Add your first medicine to start tracking.
          </p>
          <Button onClick={() => setShowModal(true)}>
            <Plus className="w-4 h-4" />
            Add Medicine
          </Button>
        </Card>
      ) : (
        <div className="grid gap-4">
          {medicines.map((medicine) => {
            const isLowStock = medicine.pill_count <= medicine.refill_threshold;
            return (
              <Card key={medicine.id}>
                <div className="flex items-start justify-between gap-4">
                  <div className="flex items-start gap-4">
                    <div className={`w-12 h-12 rounded-xl flex items-center justify-center ${
                      isLowStock ? 'bg-red-100' : 'bg-teal-100'
                    }`}>
                      <Pill className={`w-6 h-6 ${isLowStock ? 'text-red-600' : 'text-teal-600'}`} />
                    </div>
                    <div>
                      <h3 className="font-semibold text-gray-900">
                        {medicine.name}
                      </h3>
                      <p className="text-sm text-gray-600">
                        {medicine.dosage} - {medicine.form}
                      </p>
                      <div className="flex items-center gap-2 mt-2">
                        <span className="text-sm text-gray-500">
                          {medicine.pill_count} pills remaining
                        </span>
                        {isLowStock && (
                          <span className="inline-flex items-center gap-1 px-2 py-0.5 text-xs font-medium bg-red-100 text-red-700 rounded-full">
                            <AlertCircle className="w-3 h-3" />
                            Low Stock
                          </span>
                        )}
                      </div>
                    </div>
                  </div>
                  <div className="flex items-center gap-2">
                    <button
                      onClick={() => openEditModal(medicine)}
                      className="p-2 text-gray-400 hover:text-gray-600 hover:bg-gray-200 rounded-lg transition-colors"
                    >
                      <Edit2 className="w-4 h-4" />
                    </button>
                    <button
                      onClick={() => handleDelete(medicine.id)}
                      className="p-2 text-gray-400 hover:text-red-600 hover:bg-red-50 rounded-lg transition-colors"
                    >
                      <Trash2 className="w-4 h-4" />
                    </button>
                  </div>
                </div>
              </Card>
            );
          })}
        </div>
      )}

      {/* Modal */}
      {showModal && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
          <div className="bg-white rounded-xl p-6 w-full max-w-md">
            <h2 className="text-xl font-semibold text-gray-900 mb-4">
              {editingMedicine ? 'Edit Medicine' : 'Add Medicine'}
            </h2>

            {/* Scan button - only show when adding new medicine and camera available */}
            {!editingMedicine && hasCamera && (
              <button
                type="button"
                onClick={() => setShowScanner(true)}
                className="w-full mb-4 px-4 py-3 bg-teal-50 text-teal-700 rounded-lg hover:bg-teal-100 transition-colors flex items-center justify-center gap-2 font-medium"
              >
                <Camera className="w-5 h-5" />
                Scan Medicine Strip
              </button>
            )}

            {/* Camera not available message */}
            {!editingMedicine && hasCamera === false && (
              <div className="w-full mb-4 px-4 py-3 bg-gray-100 text-gray-500 rounded-lg text-center text-sm">
                Camera not available on this device.
              </div>
            )}

            <form onSubmit={handleSubmit} className="space-y-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Medicine Name
                </label>
                <input
                  type="text"
                  value={formData.name}
                  onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                  className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-teal-500"
                  required
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Dosage
                </label>
                <input
                  type="text"
                  value={formData.dosage}
                  onChange={(e) => setFormData({ ...formData, dosage: e.target.value })}
                  placeholder="e.g., 500mg"
                  className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-teal-500"
                  required
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Form
                </label>
                <select
                  value={formData.form}
                  onChange={(e) => setFormData({ ...formData, form: e.target.value })}
                  className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-teal-500"
                >
                  <option value="tablet">Tablet</option>
                  <option value="capsule">Capsule</option>
                  <option value="liquid">Liquid</option>
                  <option value="injection">Injection</option>
                  <option value="patch">Patch</option>
                  <option value="other">Other</option>
                </select>
              </div>
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    Pill Count
                  </label>
                  <input
                    type="number"
                    value={formData.pill_count}
                    onChange={(e) => setFormData({ ...formData, pill_count: parseInt(e.target.value) || 0 })}
                    className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-teal-500"
                    min="0"
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    Refill Threshold
                  </label>
                  <input
                    type="number"
                    value={formData.refill_threshold}
                    onChange={(e) => setFormData({ ...formData, refill_threshold: parseInt(e.target.value) || 10 })}
                    className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-teal-500"
                    min="0"
                  />
                </div>
              </div>
              <div className="flex gap-3 pt-4">
                <Button
                  type="button"
                  variant="ghost"
                  onClick={() => {
                    setShowModal(false);
                    setEditingMedicine(null);
                  }}
                  className="flex-1"
                >
                  Cancel
                </Button>
                <Button type="submit" className="flex-1">
                  {editingMedicine ? 'Save Changes' : 'Add Medicine'}
                </Button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Camera Scanner Modal */}
      <CameraScanner
        isOpen={showScanner}
        onClose={() => setShowScanner(false)}
        onCapture={handleScanCapture}
      />
    </div>
  );
}
