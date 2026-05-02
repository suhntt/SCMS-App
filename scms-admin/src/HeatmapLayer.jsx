import React, { useEffect } from 'react';
import { useMap } from 'react-leaflet';
import L from 'leaflet';
import 'leaflet.heat';

export default function HeatmapLayer({ points }) {
    const map = useMap();

    useEffect(() => {
        if (!map || !points || points.length === 0) return;

        // Convert to Leaflet Heatmap format: [lat, lng, intensity]
        const heatData = points.map(p => {
            const severityMultiplier = p.severity === 'High' ? 1.0 : p.severity === 'Low' ? 0.3 : 0.6;
            return [p.latitude, p.longitude, severityMultiplier];
        });

        const heatLayer = L.heatLayer(heatData, {
            radius: 25,
            blur: 15,
            maxZoom: 14,
            gradient: { 0.4: 'blue', 0.6: 'lime', 0.8: 'orange', 1: 'red' }
        });

        heatLayer.addTo(map);

        return () => {
            map.removeLayer(heatLayer);
        };
    }, [map, points]);

    return null;
}
