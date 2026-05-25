import { useRef, useState, useEffect, useCallback } from 'react';
import { X, Camera, Loader as Loader2 } from 'lucide-react';

type CameraScannerProps = {
  isOpen: boolean;
  onClose: () => void;
  onCapture: (name: string, dosage: string) => void;
};

type ToastType = 'success' | 'warning' | 'error' | null;

export function CameraScanner({ isOpen, onClose, onCapture }: CameraScannerProps) {
  const videoRef = useRef<HTMLVideoElement>(null);
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const streamRef = useRef<MediaStream | null>(null);

  const [status, setStatus] = useState<'ready' | 'capturing' | 'processing'>('ready');
  const [error, setError] = useState<string | null>(null);
  const [hasCamera, setHasCamera] = useState<boolean | null>(null);
  const [toast, setToast] = useState<{ message: string; type: ToastType } | null>(null);

  // Check camera availability
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

  // Start camera when modal opens
  const startCamera = useCallback(async () => {
    if (!hasCamera) return;

    setError(null);
    setStatus('ready');

    try {
      const stream = await navigator.mediaDevices.getUserMedia({
        video: { facingMode: 'environment' },
        audio: false
      });

      streamRef.current = stream;

      if (videoRef.current) {
        videoRef.current.srcObject = stream;
        await videoRef.current.play();
      }
    } catch (err) {
      if (err instanceof Error) {
        if (err.name === 'NotAllowedError' || err.name === 'PermissionDeniedError') {
          setError('Camera permission denied. Please allow camera access in your browser settings.');
        } else if (err.name === 'NotFoundError') {
          setError('Camera not available on this device.');
        } else {
          setError('Failed to access camera. Please try again.');
        }
      }
    }
  }, [hasCamera]);

  // Stop camera
  const stopCamera = useCallback(() => {
    if (streamRef.current) {
      streamRef.current.getTracks().forEach(track => track.stop());
      streamRef.current = null;
    }
    if (videoRef.current) {
      videoRef.current.srcObject = null;
    }
  }, []);

  // Handle modal open/close
  useEffect(() => {
    if (isOpen && hasCamera) {
      startCamera();
    } else if (!isOpen) {
      stopCamera();
      setStatus('ready');
      setError(null);
    }

    return () => {
      stopCamera();
    };
  }, [isOpen, hasCamera, startCamera, stopCamera]);

  const captureAndProcess = async () => {
    if (!videoRef.current || !canvasRef.current || status !== 'ready') return;

    setStatus('capturing');

    const video = videoRef.current;
    const canvas = canvasRef.current;

    // Set canvas size to video size
    canvas.width = video.videoWidth;
    canvas.height = video.videoHeight;

    const ctx = canvas.getContext('2d');
    if (!ctx) {
      setStatus('ready');
      return;
    }

    // Draw video frame to canvas
    ctx.drawImage(video, 0, 0, canvas.width, canvas.height);

    // Convert to base64 JPEG
    const base64Image = canvas.toDataURL('image/jpeg', 0.9);
    const base64Data = base64Image.split(',')[1];

    setStatus('processing');

    try {
      // Call our edge function to process with Gemini
      const response = await fetch(
        `${import.meta.env.VITE_SUPABASE_URL}/functions/v1/scan-medicine`,
        {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${import.meta.env.VITE_SUPABASE_ANON_KEY}`,
          },
          body: JSON.stringify({ image: base64Data }),
        }
      );

      if (!response.ok) {
        throw new Error('API request failed');
      }

      const result = await response.json();

      if (result.name && result.dosage) {
        showToast('Medicine details extracted successfully!', 'success');
        onCapture(result.name, result.dosage);
        onClose();
      } else if (result.name || result.dosage) {
        showToast('Could not read clearly. Please type manually.', 'warning');
        onCapture(result.name || '', result.dosage || '');
        onClose();
      } else {
        showToast('Could not read clearly. Please type manually.', 'warning');
        setStatus('ready');
      }
    } catch (err) {
      console.error('Scan error:', err);
      showToast('Scan failed. Please try again.', 'error');
      setStatus('ready');
    }
  };

  const showToast = (message: string, type: ToastType) => {
    setToast({ message, type });
    setTimeout(() => setToast(null), 4000);
  };

  const handleClose = () => {
    stopCamera();
    onClose();
  };

  if (!isOpen) return null;

  // Camera not available
  if (hasCamera === false) {
    return (
      <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
        <div className="bg-white rounded-xl p-6 max-w-sm text-center">
          <Camera className="w-12 h-12 text-gray-300 mx-auto mb-4" />
          <p className="text-gray-600">Camera not available on this device.</p>
          <button
            onClick={onClose}
            className="mt-4 px-4 py-2 bg-gray-100 text-gray-700 rounded-lg hover:bg-gray-200 transition-colors"
          >
            Close
          </button>
        </div>
      </div>
    );
  }

  const toastStyles = {
    success: 'bg-teal-500 text-white',
    warning: 'bg-amber-500 text-white',
    error: 'bg-red-500 text-white',
  };

  return (
    <div className="fixed inset-0 bg-black z-50 flex flex-col">
      {/* Live camera preview */}
      <video
        ref={videoRef}
        autoPlay
        playsInline
        muted
        className="absolute inset-0 w-full h-full object-cover"
      />

      {/* Captured image canvas (hidden) */}
      <canvas ref={canvasRef} className="hidden" />

      {/* Close button */}
      <button
        onClick={handleClose}
        className="absolute top-4 right-4 z-10 w-10 h-10 bg-black/50 text-white rounded-full flex items-center justify-center hover:bg-black/70 transition-colors"
        disabled={status === 'processing'}
      >
        <X className="w-6 h-6" />
      </button>

      {/* Error message */}
      {error && (
        <div className="absolute top-20 left-4 right-4 z-10 bg-white rounded-xl p-4 text-center">
          <p className="text-gray-700">{error}</p>
          <button
            onClick={handleClose}
            className="mt-3 px-4 py-2 bg-gray-100 text-gray-700 rounded-lg hover:bg-gray-200 transition-colors"
          >
            Close
          </button>
        </div>
      )}

      {/* Processing overlay */}
      {status === 'processing' && (
        <div className="absolute inset-0 bg-black/70 flex flex-col items-center justify-center z-10">
          <Loader2 className="w-12 h-12 text-white animate-spin mb-4" />
          <p className="text-white text-lg">Reading medicine strip with AI...</p>
        </div>
      )}

      {/* Capture button */}
      {status === 'ready' && !error && (
        <div className="absolute bottom-8 left-0 right-0 flex justify-center z-10">
          <button
            onClick={captureAndProcess}
            className="w-20 h-20 rounded-full bg-white border-4 border-teal-400 flex items-center justify-center hover:bg-gray-100 transition-colors active:scale-95"
          >
            <div className="w-14 h-14 rounded-full bg-teal-500" />
          </button>
        </div>
      )}

      {/* Instructions */}
      {status === 'ready' && !error && (
        <div className="absolute bottom-32 left-0 right-0 text-center z-10">
          <p className="text-white text-sm bg-black/50 inline-block px-4 py-2 rounded-lg">
            Point camera at medicine strip or box
          </p>
        </div>
      )}

      {/* Toast */}
      {toast && toast.type && (
        <div className={`absolute top-20 left-4 right-4 z-20 px-4 py-3 rounded-lg text-center font-medium ${toastStyles[toast.type]}`}>
          {toast.message}
        </div>
      )}
    </div>
  );
}
