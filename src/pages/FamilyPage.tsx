import { useEffect, useState } from 'react';
import { Users, Plus, X } from 'lucide-react';
import { useAuth } from '../lib/auth';
import { supabase } from '../lib/supabase';
import type { FamilyMember, User } from '../lib/supabase';
import { Button, Card } from '../components/Card';

export function FamilyPage() {
  const { user } = useAuth();
  const [familyMembers, setFamilyMembers] = useState<(FamilyMember & { member?: User })[]>([]);
  const [loading, setLoading] = useState(true);
  const [showInvite, setShowInvite] = useState(false);
  const [inviteEmail, setInviteEmail] = useState('');
  const [inviteRelationship, setInviteRelationship] = useState('');

  useEffect(() => {
    if (user) {
      fetchFamilyMembers();
    }
  }, [user]);

  async function fetchFamilyMembers() {
    try {
      const { data } = await supabase
        .from('family_members')
        .select('*, member:users!family_members_member_id_fkey(*)')
        .eq('caregiver_id', user!.id);
      setFamilyMembers((data as (FamilyMember & { member: User })[]) || []);
    } catch (error) {
      console.error('Error fetching family members:', error);
    } finally {
      setLoading(false);
    }
  }

  async function handleInvite(e: React.FormEvent) {
    e.preventDefault();
    // Note: In a real app, this would need to look up the user by email
    // and handle the invitation flow properly
    alert('Invitation feature requires user lookup. Please implement based on your auth flow.');
    setShowInvite(false);
    setInviteEmail('');
    setInviteRelationship('');
  }

  async function handleRemove(id: string) {
    if (!confirm('Are you sure you want to remove this family member?')) return;
    try {
      await supabase.from('family_members').delete().eq('id', id);
      fetchFamilyMembers();
    } catch (error) {
      console.error('Error removing family member:', error);
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
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Family</h1>
          <p className="text-gray-600 mt-1">
            Manage family members you care for.
          </p>
        </div>
        <Button onClick={() => setShowInvite(true)}>
          <Plus className="w-4 h-4" />
          Add Member
        </Button>
      </div>

      {/* Family List */}
      {familyMembers.length === 0 ? (
        <Card className="text-center py-12">
          <Users className="w-12 h-12 text-gray-300 mx-auto mb-4" />
          <h3 className="text-lg font-medium text-gray-900 mb-1">
            No family members yet
          </h3>
          <p className="text-gray-500 mb-4">
            Add family members to manage their medications.
          </p>
          <Button onClick={() => setShowInvite(true)}>
            <Plus className="w-4 h-4" />
            Add Member
          </Button>
        </Card>
      ) : (
        <div className="grid gap-4">
          {familyMembers.map((member) => (
            <Card key={member.id}>
              <div className="flex items-start justify-between gap-4">
                <div className="flex items-start gap-4">
                  <div className="w-12 h-12 rounded-xl bg-indigo-100 flex items-center justify-center">
                    <Users className="w-6 h-6 text-indigo-600" />
                  </div>
                  <div>
                    <h3 className="font-semibold text-gray-900">
                      {member.member?.name || 'Unknown'}
                    </h3>
                    <p className="text-sm text-gray-600">
                      {member.relationship}
                    </p>
                    <div className="flex items-center gap-2 mt-2">
                      <span className={`px-2 py-0.5 text-xs font-medium rounded-full ${
                        member.permission === 'full'
                          ? 'bg-teal-100 text-teal-700'
                          : member.permission === 'edit'
                          ? 'bg-indigo-100 text-indigo-600'
                          : 'bg-gray-100 text-gray-600'
                      }`}>
                        {member.permission === 'full' ? 'Full Access' :
                         member.permission === 'edit' ? 'Can Edit' : 'View Only'}
                      </span>
                      {member.notify_missed && (
                        <span className="px-2 py-0.5 text-xs font-medium bg-amber-100 text-amber-700 rounded-full">
                          Notify on Missed
                        </span>
                      )}
                    </div>
                  </div>
                </div>
                <button
                  onClick={() => handleRemove(member.id)}
                  className="p-2 text-gray-400 hover:text-red-600 hover:bg-red-50 rounded-lg transition-colors"
                >
                  <X className="w-4 h-4" />
                </button>
              </div>
            </Card>
          ))}
        </div>
      )}

      {/* Invite Modal */}
      {showInvite && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
          <div className="bg-white rounded-xl p-6 w-full max-w-md">
            <h2 className="text-xl font-semibold text-gray-900 mb-4">
              Add Family Member
            </h2>
            <form onSubmit={handleInvite} className="space-y-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Email Address
                </label>
                <input
                  type="email"
                  value={inviteEmail}
                  onChange={(e) => setInviteEmail(e.target.value)}
                  className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-teal-500"
                  required
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Relationship
                </label>
                <select
                  value={inviteRelationship}
                  onChange={(e) => setInviteRelationship(e.target.value)}
                  className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-teal-500"
                  required
                >
                  <option value="">Select...</option>
                  <option value="Parent">Parent</option>
                  <option value="Spouse">Spouse</option>
                  <option value="Child">Child</option>
                  <option value="Sibling">Sibling</option>
                  <option value="Other">Other</option>
                </select>
              </div>
              <div className="flex gap-3 pt-4">
                <Button
                  type="button"
                  variant="ghost"
                  onClick={() => {
                    setShowInvite(false);
                    setInviteEmail('');
                    setInviteRelationship('');
                  }}
                  className="flex-1"
                >
                  Cancel
                </Button>
                <Button type="submit" className="flex-1">
                  Send Invite
                </Button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
