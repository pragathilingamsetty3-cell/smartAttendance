'use client';

import React, { useState, useRef, useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { MapPin, Building, Users, Save, Map, Square, Circle, Triangle, Trash2, Plus, Eye, Camera, CameraOff, RefreshCw } from 'lucide-react';
import { roomManagementService } from '@/services/roomManagement.service';
import { Input } from '@/components/ui/Input';
import { Button } from '@/components/ui/Button';
import { Card, CardContent, CardHeader } from '@/components/ui/Card';
import { Loading } from '@/components/ui/Loading';
import { RoomCreationRequest, BoundaryCreationRequest, Coordinate } from '@/types/room-management';

interface RoomCreationFormProps {
  onSuccess?: () => void;
  onCancel?: () => void;
}

interface CanvasPoint {
  x: number;
  y: number;
}

export const RoomCreationForm: React.FC<RoomCreationFormProps> = ({
  onSuccess,
  onCancel
}) => {
  const router = useRouter();
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const [loading, setLoading] = useState(false);
  const [drawingMode, setDrawingMode] = useState<'rectangle' | 'circle' | 'polygon' | 'calibration' | null>(null);
  const [isDrawing, setIsDrawing] = useState(false);
  const [boundaryPoints, setBoundaryPoints] = useState<CanvasPoint[]>([]);
  const [showPreview, setShowPreview] = useState(false);
  const [isCalibrationMode, setIsCalibrationMode] = useState(false);
  const [calibrationDimensions, setCalibrationDimensions] = useState({ width: 8.5, length: 12.0 });
  const [backgroundImage, setBackgroundImage] = useState<HTMLImageElement | null>(null);
  const [calibrationSource, setCalibrationSource] = useState<'url' | 'upload' | 'camera'>('url');
  const [sensorUrl, setSensorUrl] = useState('');
  const [isCameraActive, setIsCameraActive] = useState(false);
  const [cameraStream, setCameraStream] = useState<MediaStream | null>(null);
  const [cameraError, setCameraError] = useState<string | null>(null);
  const videoRef = useRef<HTMLVideoElement>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);
  const [formData, setFormData] = useState<RoomCreationRequest>({
    name: '',
    capacity: 0,
    building: '',
    floor: 0,
    description: '',
    sensorUrl: '',
    boundaryPoints: []
  });

  const [boundaryData, setBoundaryData] = useState<BoundaryCreationRequest>({
    centerLat: 0,
    centerLng: 0,
    widthMeters: 0,
    heightMeters: 0,
    radiusMeters: 0
  });

  useEffect(() => {
    if (isCalibrationMode && calibrationSource === 'url' && sensorUrl) {
      const img = new Image();
      img.crossOrigin = "anonymous"; // Handle CORS for external cameras
      img.src = sensorUrl;
      img.onload = () => setBackgroundImage(img);
      img.onerror = () => console.error("Failed to load sensor stream from URL");
    }
  }, [isCalibrationMode, calibrationSource, sensorUrl]);

  const startCamera = async () => {
    setCameraError(null);
    try {
      const stream = await navigator.mediaDevices.getUserMedia({ 
        video: { facingMode: 'environment' }
      });
      setCameraStream(stream);
      setIsCameraActive(true);
    } catch (err) {
      console.error("Error accessing camera:", err);
      setCameraError("Could not access camera. Please ensure permissions are granted.");
    }
  };

  const stopCamera = () => {
    if (cameraStream) {
      cameraStream.getTracks().forEach(track => track.stop());
      setCameraStream(null);
    }
    if (videoRef.current) {
      videoRef.current.srcObject = null;
    }
    setIsCameraActive(false);
  };

  // Effect to attach stream when video element becomes available
  useEffect(() => {
    if (isCameraActive && cameraStream && videoRef.current) {
      videoRef.current.srcObject = cameraStream;
    }
  }, [isCameraActive, cameraStream]);

  const capturePhoto = () => {
    if (videoRef.current && cameraStream) {
      const video = videoRef.current;
      const canvas = document.createElement('canvas');
      canvas.width = video.videoWidth;
      canvas.height = video.videoHeight;
      const ctx = canvas.getContext('2d');
      if (ctx) {
        ctx.drawImage(video, 0, 0, canvas.width, canvas.height);
        const dataUrl = canvas.toDataURL('image/jpeg');
        const img = new Image();
        img.src = dataUrl;
        img.onload = () => {
          setBackgroundImage(img);
          stopCamera();
          // Automatically switch to drawing mode after capture
          setDrawingMode('calibration');
          setIsCalibrationMode(true);
        };
      }
    }
  };

  // Cleanup camera on unmount or tab switch
  useEffect(() => {
    if (calibrationSource !== 'camera' && isCameraActive) {
      stopCamera();
    }
    return () => stopCamera();
  }, [calibrationSource]);

  const handleFileUpload = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) {
      const reader = new FileReader();
      reader.onload = (event) => {
        const img = new Image();
        img.src = event.target?.result as string;
        img.onload = () => {
          setBackgroundImage(img);
          // Auto-start calibration if upload is used
          setDrawingMode('calibration');
          setIsCalibrationMode(true);
        };
      };
      reader.readAsDataURL(file);
    }
  };

  useEffect(() => {
    drawBoundary();
  }, [boundaryPoints, isCalibrationMode, backgroundImage]);

  const drawBoundary = () => {
    const canvas = canvasRef.current;
    if (!canvas) return;

    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    // Clear canvas
    ctx.clearRect(0, 0, canvas.width, canvas.height);

    // Draw Background if in calibration mode
    if (isCalibrationMode && backgroundImage) {
      ctx.drawImage(backgroundImage, 0, 0, canvas.width, canvas.height);
      // Add a slight dark overlay to make points pop
      ctx.fillStyle = 'rgba(0, 0, 0, 0.2)';
      ctx.fillRect(0, 0, canvas.width, canvas.height);
    }

    // Set drawing style
    ctx.strokeStyle = isCalibrationMode ? '#10B981' : '#3B82F6'; // Emerald for calibration
    ctx.lineWidth = 2;
    ctx.fillStyle = isCalibrationMode ? 'rgba(16, 185, 129, 0.2)' : 'rgba(59, 130, 246, 0.1)';

    if (boundaryPoints.length === 0) return;

    // Draw based on boundary type
    if (drawingMode === 'rectangle' && boundaryPoints.length === 2) {
      const [start, end] = boundaryPoints;
      const width = end.x - start.x;
      const height = end.y - start.y;
      
      ctx.fillRect(start.x, start.y, width, height);
      ctx.strokeRect(start.x, start.y, width, height);
    } else if (drawingMode === 'circle' && boundaryPoints.length === 2) {
      const [center, edge] = boundaryPoints;
      const radius = Math.sqrt(Math.pow(edge.x - center.x, 2) + Math.pow(edge.y - center.y, 2));
      
      ctx.beginPath();
      ctx.arc(center.x, center.y, radius, 0, 2 * Math.PI);
      ctx.fill();
      ctx.stroke();
    } else if (drawingMode === 'polygon' && boundaryPoints.length > 2) {
      ctx.beginPath();
      ctx.moveTo(boundaryPoints[0].x, boundaryPoints[0].y);
      
      for (let i = 1; i < boundaryPoints.length; i++) {
        ctx.lineTo(boundaryPoints[i].x, boundaryPoints[i].y);
      }
      
      ctx.closePath();
      ctx.fill();
      ctx.stroke();
    } else if (isCalibrationMode && boundaryPoints.length >= 2) {
      // Draw path for calibration pins
      ctx.beginPath();
      ctx.moveTo(boundaryPoints[0].x, boundaryPoints[0].y);
      for (let i = 1; i < boundaryPoints.length; i++) {
        ctx.lineTo(boundaryPoints[i].x, boundaryPoints[i].y);
      }
      if (boundaryPoints.length === 4) {
        ctx.closePath();
        ctx.fill();
      }
      ctx.stroke();
    }

    // Draw points
    ctx.fillStyle = isCalibrationMode ? '#10B981' : '#3B82F6';
    boundaryPoints.forEach((point, index) => {
      ctx.beginPath();
      ctx.arc(point.x, point.y, isCalibrationMode ? 6 : 4, 0, 2 * Math.PI);
      ctx.fill();
      
      if (isCalibrationMode) {
        ctx.fillStyle = 'white';
        ctx.font = 'bold 10px Inter';
        ctx.fillText(`${index + 1}`, point.x - 3, point.y + 4);
        ctx.fillStyle = '#10B981';
      }
    });
  };

  const handleCanvasClick = (e: React.MouseEvent<HTMLCanvasElement>) => {
    if (!drawingMode || !canvasRef.current) return;

    const rect = canvasRef.current.getBoundingClientRect();
    const x = e.clientX - rect.left;
    const y = e.clientY - rect.top;

    if (drawingMode === 'rectangle') {
      if (boundaryPoints.length === 0) {
        setBoundaryPoints([{ x, y }]);
      } else if (boundaryPoints.length === 1) {
        setBoundaryPoints([...boundaryPoints, { x, y }]);
        setIsDrawing(false);
      }
    } else if (drawingMode === 'circle') {
      if (boundaryPoints.length === 0) {
        setBoundaryPoints([{ x, y }]);
      } else if (boundaryPoints.length === 1) {
        setBoundaryPoints([...boundaryPoints, { x, y }]);
        setIsDrawing(false);
      }
    } else if (drawingMode === 'polygon' || isCalibrationMode) {
      if (isCalibrationMode && boundaryPoints.length >= 4) return;
      const newPoints = [...boundaryPoints, { x, y }];
      setBoundaryPoints(newPoints);
      
      // Auto-finish calibration mode when 4 points are pinned
      if (isCalibrationMode && newPoints.length === 4) {
        setIsDrawing(false);
        // We delay slightly to ensure state is ready, or pass points directly to a shared logic
        setTimeout(() => {
          const coordinates: Coordinate[] = newPoints.map(point => ({
            longitude: point.x,
            latitude: point.y
          }));
          setFormData(prev => ({ ...prev, boundaryPoints: coordinates }));
          setShowPreview(true);
        }, 100);
      }
    }
  };

  const startDrawing = (mode: 'rectangle' | 'circle' | 'polygon' | 'calibration') => {
    setDrawingMode(mode);
    setIsDrawing(true);
    setBoundaryPoints([]);
    setIsCalibrationMode(mode === 'calibration');
  };

  const finishDrawing = () => {
    if (boundaryPoints.length < 2) {
      alert('Please complete the boundary drawing');
      return;
    }

    // Convert canvas points to polygon format
    const coordinates: Coordinate[] = boundaryPoints.map(point => ({
      longitude: point.x,
      latitude: point.y
    }));

    setFormData(prev => ({ ...prev, boundaryPoints: coordinates, sensorUrl: sensorUrl }));
    
    // Update boundary data based on drawing mode
    if (isCalibrationMode) {
      setBoundaryData(prev => ({
        ...prev,
        widthMeters: calibrationDimensions.width,
        heightMeters: calibrationDimensions.length,
        centerLat: boundaryPoints.reduce((sum, p) => sum + p.y, 0) / boundaryPoints.length,
        centerLng: boundaryPoints.reduce((sum, p) => sum + p.x, 0) / boundaryPoints.length,
      }));
    } else if (drawingMode === 'circle' && boundaryPoints.length >= 2) {
      const center = boundaryPoints[0];
      const edge = boundaryPoints[1];
      const radius = Math.sqrt(Math.pow(edge.x - center.x, 2) + Math.pow(edge.y - center.y, 2));
      
      setBoundaryData(prev => ({
        ...prev,
        centerLat: center.y,
        centerLng: center.x,
        radiusMeters: radius
      }));
    } else if (drawingMode === 'rectangle' && boundaryPoints.length >= 2) {
      const [start, end] = boundaryPoints;
      setBoundaryData(prev => ({
        ...prev,
        centerLat: (start.y + end.y) / 2,
        centerLng: (start.x + end.x) / 2,
        widthMeters: Math.abs(end.x - start.x),
        heightMeters: Math.abs(end.y - start.y)
      }));
    }

    setDrawingMode(null);
    setIsDrawing(false);
    setShowPreview(true);
  };

  const clearBoundary = () => {
    setBoundaryPoints([]);
    setDrawingMode(null);
    setIsDrawing(false);
    setShowPreview(false);
    setIsCalibrationMode(false);
    setBackgroundImage(null);
    setSensorUrl('');
    setFormData(prev => ({ ...prev, boundaryPoints: [] }));
    
    const canvas = canvasRef.current;
    if (canvas) {
      const ctx = canvas.getContext('2d');
      if (ctx) {
        ctx.clearRect(0, 0, canvas.width, canvas.height);
      }
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    
    // Validation
    if (formData.boundaryPoints.length === 0) {
      alert('Please complete the room boundary definition (pin floor corners or draw polygon) before saving.');
      return;
    }

    setLoading(true);

    try {
      // Create room
      const room = await roomManagementService.createRoom(formData);
      
      // Then create boundary if exists
      if (formData.boundaryPoints.length > 0 && room.id) {
        await roomManagementService.updateBoundary(
          room.id,
          formData.boundaryPoints.map(coord => [coord.longitude, coord.latitude])
        );
      }

      onSuccess?.();
      router.push('/rooms');
    } catch (error) {
      console.error('Room creation failed:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { name, value } = e.target;
    
    setFormData(prev => ({ 
      ...prev, 
      [name]: (name === 'capacity' || name === 'floor') ? parseInt(value) || 0 : value 
    }));
  };

  return (
    <div className="max-w-6xl mx-auto">
      <Card glass>
        <CardHeader>
          <div className="flex items-center space-x-3">
            <div className="p-2 bg-green-500/20 rounded-lg">
              <Building className="h-5 w-5 text-green-400" />
            </div>
            <div>
              <h2 className="text-xl font-bold text-white">Create New Room</h2>
              <p className="text-gray-400 text-sm">Add a new room with boundary definition</p>
            </div>
          </div>
        </CardHeader>
        
        <CardContent>
          <form onSubmit={handleSubmit} className="space-y-8">
            {/* Basic Information */}
            <div className="space-y-6">
              <h3 className="text-lg font-semibold text-white flex items-center">
                <Building className="h-4 w-4 mr-2" />
                Basic Information
              </h3>
              
              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                <Input
                  name="name"
                  label="Room Name"
                  placeholder="e.g., Computer Lab 101"
                  value={formData.name}
                  onChange={handleChange}
                  icon={<Building className="h-4 w-4" />}
                  glass
                  required
                />

                <Input
                  name="capacity"
                  type="number"
                  label="Capacity"
                  placeholder="e.g., 60"
                  value={formData.capacity}
                  onChange={handleChange}
                  icon={<Users className="h-4 w-4" />}
                  glass
                  required
                />

                <Input
                  name="building"
                  label="Building"
                  placeholder="e.g., Main Building"
                  value={formData.building}
                  onChange={handleChange}
                  icon={<Building className="h-4 w-4" />}
                  glass
                  required
                />

                <Input
                  name="floor"
                  type="number"
                  label="Floor"
                  placeholder="e.g., 1"
                  value={formData.floor.toString()}
                  onChange={handleChange}
                  icon={<MapPin className="h-4 w-4" />}
                  glass
                  required
                />

                <div className="lg:col-span-2">
                  <Input
                    name="description"
                    label="Description"
                    placeholder="Room description and facilities"
                    value={formData.description}
                    onChange={handleChange}
                    icon={<MapPin className="h-4 w-4" />}
                    glass
                  />
                </div>
              </div>
            </div>

            {/* Boundary Definition */}
            <div className="space-y-6">
              <h3 className="text-lg font-semibold text-white flex items-center">
                <Map className="h-4 w-4 mr-2" />
                Boundary Definition
              </h3>
              
              <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
                {/* Drawing Controls */}
                <div className="space-y-4">
                  <div className="flex flex-wrap gap-2">
                    <Button
                      variant={drawingMode === 'rectangle' ? 'primary' : 'glass'}
                      size="sm"
                      onClick={() => startDrawing('rectangle')}
                      disabled={isDrawing}
                    >
                      <Square className="h-4 w-4 mr-2" />
                      Rectangle
                    </Button>
                    
                    <Button
                      variant={drawingMode === 'circle' ? 'primary' : 'glass'}
                      size="sm"
                      onClick={() => startDrawing('circle')}
                      disabled={isDrawing}
                    >
                      <Circle className="h-4 w-4 mr-2" />
                      Circle
                    </Button>
                    
                    <Button
                      variant={drawingMode === 'polygon' ? 'primary' : 'glass'}
                      size="sm"
                      onClick={() => startDrawing('polygon')}
                      disabled={isDrawing && !isCalibrationMode}
                    >
                      <Triangle className="h-4 w-4 mr-2" />
                      Polygon
                    </Button>

                    <Button
                      variant={isCalibrationMode ? 'success' : 'glass'}
                      size="sm"
                      onClick={() => startDrawing('calibration')}
                      disabled={isDrawing && isCalibrationMode}
                      className={isCalibrationMode ? "bg-emerald-600/20 border-emerald-500/50 text-emerald-400" : ""}
                    >
                      <MapPin className="h-4 w-4 mr-2" />
                      Visual Calibration
                    </Button>
                    
                    {isDrawing && (
                      <Button
                        variant="success"
                        size="sm"
                        onClick={finishDrawing}
                      >
                        <Save className="h-4 w-4 mr-2" />
                        Finish
                      </Button>
                    )}
                    
                    {(boundaryPoints.length > 0 || drawingMode) && (
                      <Button
                        variant="error"
                        size="sm"
                        onClick={clearBoundary}
                      >
                        <Trash2 className="h-4 w-4 mr-2" />
                        Clear
                      </Button>
                    )}
                  </div>

                  {/* Instructions */}
                  <div className="p-4 bg-gray-800/30 rounded-lg border border-gray-700">
                    <h4 className="text-white font-medium mb-2">Drawing Instructions:</h4>
                    <ul className="text-gray-400 text-sm space-y-1">
                      {isCalibrationMode ? (
                        <>
                          <li>• <strong>Step 1:</strong> Select source (URL or Upload)</li>
                          <li>• <strong>Step 2:</strong> Pin the 4 corners of the floor</li>
                          <li>• <strong>Step 3:</strong> Enter physical size in meters</li>
                        </>
                      ) : (
                        <>
                          <li>• <strong>Rectangle:</strong> Click two opposite corners</li>
                          <li>• <strong>Circle:</strong> Click center point, then edge point</li>
                          <li>• <strong>Polygon:</strong> Click multiple points, then "Finish"</li>
                        </>
                      )}
                    </ul>
                  </div>

                  {isCalibrationMode && (
                    <div className="space-y-4">
                      {/* Source Selection Tabs */}
                      <div className="flex bg-[#0F0F16] p-1 rounded-xl border border-white/10">
                        <button
                          type="button"
                          onClick={() => setCalibrationSource('url')}
                          className={`flex-1 flex items-center justify-center py-2 px-4 rounded-lg text-sm font-medium transition-all ${
                            calibrationSource === 'url' ? 'bg-[#7C3AED] text-white shadow-lg' : 'text-gray-400 hover:text-white'
                          }`}
                        >
                          <MapPin className="h-4 w-4 mr-2" />
                          Live Stream / URL
                        </button>
                        <button
                          type="button"
                          onClick={() => setCalibrationSource('upload')}
                          className={`flex-1 flex items-center justify-center py-2 px-4 rounded-lg text-sm font-medium transition-all ${
                            calibrationSource === 'upload' ? 'bg-[#7C3AED] text-white shadow-lg' : 'text-gray-400 hover:text-white'
                          }`}
                        >
                          <Save className="h-4 w-4 mr-2" />
                          Upload Snapshot
                        </button>
                        <button
                          type="button"
                          onClick={() => setCalibrationSource('camera')}
                          className={`flex-1 flex items-center justify-center py-2 px-4 rounded-lg text-sm font-medium transition-all ${
                            calibrationSource === 'camera' ? 'bg-[#7C3AED] text-white shadow-lg' : 'text-gray-400 hover:text-white'
                          }`}
                        >
                          <Camera className="h-4 w-4 mr-2" />
                          Live Camera
                        </button>
                      </div>

                      {calibrationSource === 'url' && (
                        <Input
                          label="Camera Stream / Snapshot URL"
                          placeholder="rtsp://... or http://192.168.1.100/snapshot.jpg"
                          value={sensorUrl}
                          onChange={(e) => setSensorUrl(e.target.value)}
                          glass
                          icon={<MapPin className="h-4 w-4" />}
                        />
                      )}

                      {calibrationSource === 'upload' && (
                        <div 
                          onClick={() => fileInputRef.current?.click()}
                          className="border-2 border-dashed border-white/10 rounded-xl p-8 text-center cursor-pointer hover:border-[#7C3AED]/50 hover:bg-[#7C3AED]/5 transition-all"
                        >
                          <input 
                            type="file" 
                            ref={fileInputRef} 
                            className="hidden" 
                            accept="image/*" 
                            onChange={handleFileUpload} 
                          />
                          <Save className="h-8 w-8 text-gray-500 mx-auto mb-2" />
                          <p className="text-gray-300 font-medium">Click to upload room snapshot</p>
                          <p className="text-gray-500 text-xs mt-1">Supports JPG, PNG from sensor perspective</p>
                        </div>
                      )}

                      {calibrationSource === 'camera' && (
                        <div className="space-y-4">
                          {!isCameraActive ? (
                            <div 
                              onClick={startCamera}
                              className="border-2 border-dashed border-white/10 rounded-xl p-12 text-center cursor-pointer hover:border-[#7C3AED]/50 hover:bg-[#7C3AED]/5 transition-all"
                            >
                              <Camera className="h-8 w-8 text-gray-500 mx-auto mb-2" />
                              <p className="text-gray-300 font-medium">Start Room Camera</p>
                              <p className="text-gray-500 text-xs mt-1">Take a real-time photo for calibration</p>
                              {cameraError && <p className="text-red-400 text-xs mt-2">{cameraError}</p>}
                            </div>
                          ) : (
                            <div className="relative rounded-xl overflow-hidden border border-white/10 bg-black aspect-video">
                              <video 
                                ref={videoRef} 
                                autoPlay 
                                playsInline 
                                className="w-full h-full object-cover"
                              />
                              <div className="absolute bottom-4 left-0 right-0 flex justify-center space-x-4">
                                <Button 
                                  type="button" 
                                  onClick={capturePhoto}
                                  className="bg-purple-600 hover:bg-purple-700 shadow-xl"
                                >
                                  <Camera className="h-4 w-4 mr-2" />
                                  Capture Room
                                </Button>
                                <Button 
                                  type="button" 
                                  variant="glass" 
                                  onClick={stopCamera}
                                >
                                  <CameraOff className="h-4 w-4 mr-2" />
                                  Cancel
                                </Button>
                              </div>
                            </div>
                          )}
                        </div>
                      )}
                    </div>
                  )}

                  {isCalibrationMode && (
                    <div className="p-4 bg-emerald-500/10 rounded-lg border border-emerald-500/20 space-y-4">
                      <h4 className="text-emerald-400 font-medium mb-2">Physical Dimensions:</h4>
                      <div className="grid grid-cols-2 gap-4">
                        <Input
                          label="Front Wall Width (m)"
                          type="number"
                          value={calibrationDimensions.width.toString()}
                          onChange={(e) => setCalibrationDimensions(prev => ({ ...prev, width: parseFloat(e.target.value) || 0 }))}
                          glass
                        />
                        <Input
                          label="Side Wall Length (m)"
                          type="number"
                          value={calibrationDimensions.length.toString()}
                          onChange={(e) => setCalibrationDimensions(prev => ({ ...prev, length: parseFloat(e.target.value) || 0 }))}
                          glass
                        />
                      </div>
                    </div>
                  )}

                  {/* Boundary Info */}
                  {boundaryPoints.length > 0 && (
                    <div className="p-4 bg-blue-500/10 rounded-lg border border-blue-500/20">
                      <h4 className="text-blue-400 font-medium mb-2">Boundary Points:</h4>
                      <div className="text-gray-300 text-sm space-y-1">
                        {boundaryPoints.map((point, index) => (
                          <div key={index}>
                            Point {index + 1}: ({Math.round(point.x)}, {Math.round(point.y)})
                          </div>
                        ))}
                      </div>
                    </div>
                  )}
                </div>

                {/* Canvas */}
                <div className="relative">
                  <div className="absolute top-2 right-2 z-10 flex space-x-2">
                    <Button
                      variant="glass"
                      size="sm"
                      onClick={() => setShowPreview(!showPreview)}
                    >
                      <Eye className="h-4 w-4" />
                    </Button>
                  </div>
                  
                  <canvas
                    ref={canvasRef}
                    width={400}
                    height={400}
                    onClick={handleCanvasClick}
                    className="w-full h-96 bg-gray-800/50 border-2 border-dashed border-gray-600 rounded-lg cursor-crosshair"
                    style={{ maxHeight: '400px' }}
                  />
                  
                  {isDrawing && (
                    <div className="absolute bottom-2 left-2 bg-blue-500/20 text-blue-400 px-3 py-1 rounded-lg text-sm">
                      Drawing {drawingMode}...
                    </div>
                  )}
                </div>
              </div>
            </div>

            {/* Preview */}
            {showPreview && formData.boundaryPoints.length > 0 && (
              <div className="p-4 bg-green-500/10 rounded-lg border border-green-500/20">
                <h4 className="text-green-400 font-medium mb-2">Boundary Preview:</h4>
                <div className="text-gray-300 text-sm">
                  <p>Type: Boundary</p>
                  <p>Points: {boundaryPoints.length}</p>
                  {boundaryData.radiusMeters && boundaryData.radiusMeters > 0 && (
                    <p>Radius: {Math.round(boundaryData.radiusMeters)}m</p>
                  )}
                </div>
              </div>
            )}

            {/* Form Actions */}
            <div className="flex justify-end space-x-4 pt-6 border-t border-gray-700">
              <Button
                type="button"
                variant="glass"
                onClick={onCancel}
                disabled={loading}
              >
                Cancel
              </Button>
              
              <Button
                type="submit"
                loading={loading}
                disabled={loading}
              >
                {loading ? 'Creating...' : 'Create Room'}
              </Button>
            </div>
          </form>
        </CardContent>
      </Card>
    </div>
  );
};

export default RoomCreationForm;
