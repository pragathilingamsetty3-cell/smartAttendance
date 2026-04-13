'use client';

import { useEffect } from 'react';
import { MapContainer, TileLayer, Polygon, Circle, Rectangle } from 'react-leaflet';
import 'leaflet/dist/leaflet.css';

interface BoundaryProps {
  roomId: string;
  coordinates?: [number, number][]; // Polygon Points
  center?: [number, number];       // For Circle
  radius?: number;                 // For Circle
  bounds?: [[number, number], [number, number]]; // For Rectangle
  type: 'POLYGON' | 'CIRCLE' | 'RECTANGLE';
}

export default function GeospatialBoundary({ roomId, coordinates, center, radius, bounds, type }: BoundaryProps) {
  // Leaflet requires window, making sure we are on client side
  if (typeof window === 'undefined') return null;

  // Default coordinate if none provided
  const mapCenter: [number, number] = center || (coordinates && coordinates[0]) || [0, 0];

  return (
    <div style={{ height: '400px', width: '100%', borderRadius: '12px', overflow: 'hidden' }} className="shadow-lg border border-slate-700">
      <MapContainer center={mapCenter} zoom={18} style={{ height: '100%', width: '100%' }}>
        <TileLayer
          attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
          url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
        />
        
        {type === 'POLYGON' && coordinates && (
          <Polygon positions={coordinates} color="cyan" fillColor="rgba(0, 255, 255, 0.4)" fillOpacity={0.6} />
        )}
        
        {type === 'CIRCLE' && center && radius && (
          <Circle center={center} radius={radius} color="magenta" fillColor="rgba(255, 0, 255, 0.4)" fillOpacity={0.6} />
        )}
        
        {type === 'RECTANGLE' && bounds && (
          <Rectangle bounds={bounds} color="yellow" fillColor="rgba(255, 255, 0, 0.4)" fillOpacity={0.6} />
        )}
      </MapContainer>
    </div>
  );
}
