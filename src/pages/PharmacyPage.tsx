import { useState } from 'react';
import { MapPin, Navigation, Phone, Clock, Search } from 'lucide-react';
import { Card } from '../components/Card';

type Pharmacy = {
  id: string;
  name: string;
  address: string;
  phone?: string;
  distance: number;
  isOpen: boolean;
};

export function PharmacyPage() {
  const [pharmacies, setPharmacies] = useState<Pharmacy[]>([]);
  const [loading, setLoading] = useState(false);
  const [searchQuery, setSearchQuery] = useState('');
  const [, setLocation] = useState<{ lat: number; lng: number } | null>(null);

  async function searchPharmacies() {
    setLoading(true);
    try {
      // In a real app, this would use the Overpass API or Google Places API
      // For demo purposes, we'll show mock data
      await new Promise((resolve) => setTimeout(resolve, 1500));

      const mockPharmacies: Pharmacy[] = [
        {
          id: '1',
          name: 'CVS Pharmacy',
          address: '123 Main St',
          phone: '(555) 123-4567',
          distance: 0.5,
          isOpen: true,
        },
        {
          id: '2',
          name: 'Walgreens',
          address: '456 Oak Ave',
          phone: '(555) 234-5678',
          distance: 1.2,
          isOpen: true,
        },
        {
          id: '3',
          name: 'Rite Aid Pharmacy',
          address: '789 Pine Rd',
          phone: '(555) 345-6789',
          distance: 2.1,
          isOpen: false,
        },
      ];

      setPharmacies(mockPharmacies);
    } catch (error) {
      console.error('Error searching pharmacies:', error);
    } finally {
      setLoading(false);
    }
  }

  function getCurrentLocation() {
    if (!navigator.geolocation) {
      alert('Geolocation is not supported by your browser.');
      return;
    }

    navigator.geolocation.getCurrentPosition(
      (position) => {
        setLocation({
          lat: position.coords.latitude,
          lng: position.coords.longitude,
        });
        searchPharmacies();
      },
      () => {
        alert('Unable to retrieve your location.');
      }
    );
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div>
        <h1 className="text-2xl font-bold text-gray-900">Find Pharmacy</h1>
        <p className="text-gray-600 mt-1">
          Locate nearby pharmacies for refills.
        </p>
      </div>

      {/* Search */}
      <Card>
        <div className="flex flex-col sm:flex-row gap-3">
          <div className="flex-1">
            <input
              type="text"
              placeholder="Enter your location..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-teal-500"
            />
          </div>
          <button
            onClick={getCurrentLocation}
            className="px-4 py-2 bg-gray-100 text-gray-700 rounded-lg hover:bg-gray-200 transition-colors flex items-center justify-center gap-2"
          >
            <Navigation className="w-4 h-4" />
            Use My Location
          </button>
          <button
            onClick={searchPharmacies}
            disabled={loading}
            className="px-4 py-2 bg-teal-500 text-white rounded-lg hover:bg-teal-600 transition-colors flex items-center justify-center gap-2 disabled:bg-teal-300"
          >
            <Search className="w-4 h-4" />
            {loading ? 'Searching...' : 'Search'}
          </button>
        </div>
      </Card>

      {/* Results */}
      {pharmacies.length > 0 ? (
        <div className="grid gap-4">
          {pharmacies.map((pharmacy) => (
            <Card key={pharmacy.id}>
              <div className="flex items-start justify-between gap-4">
                <div className="flex items-start gap-4">
                  <div className="w-12 h-12 rounded-xl bg-teal-100 flex items-center justify-center">
                    <MapPin className="w-6 h-6 text-teal-600" />
                  </div>
                  <div>
                    <h3 className="font-semibold text-gray-900">
                      {pharmacy.name}
                    </h3>
                    <p className="text-sm text-gray-600">{pharmacy.address}</p>
                    <div className="flex items-center gap-3 mt-2 text-sm">
                      <span className="text-gray-500">
                        {pharmacy.distance} mi away
                      </span>
                      <span className={`inline-flex items-center gap-1 ${
                        pharmacy.isOpen ? 'text-teal-600' : 'text-red-600'
                      }`}>
                        <Clock className="w-3 h-3" />
                        {pharmacy.isOpen ? 'Open now' : 'Closed'}
                      </span>
                    </div>
                  </div>
                </div>
                {pharmacy.phone && (
                  <a
                    href={`tel:${pharmacy.phone}`}
                    className="p-2 text-gray-400 hover:text-teal-600 hover:bg-teal-50 rounded-lg transition-colors"
                  >
                    <Phone className="w-5 h-5" />
                  </a>
                )}
              </div>
            </Card>
          ))}
        </div>
      ) : (
        !loading && (
          <Card className="text-center py-12">
            <MapPin className="w-12 h-12 text-gray-300 mx-auto mb-4" />
            <h3 className="text-lg font-medium text-gray-900 mb-1">
              Find nearby pharmacies
            </h3>
            <p className="text-gray-500">
              Search by location or use your current location.
            </p>
          </Card>
        )
      )}
    </div>
  );
}
