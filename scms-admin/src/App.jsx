import React, { useState, useEffect, useRef } from 'react';
import { db } from './firebase';
import { collection, onSnapshot, query, orderBy, doc, updateDoc, addDoc, serverTimestamp } from 'firebase/firestore';
import {
  Chart as ChartJS, CategoryScale, LinearScale, PointElement, LineElement, Title, Tooltip, Legend, ArcElement, BarElement
} from 'chart.js';
import { Line, Pie, Bar } from 'react-chartjs-2';
import { MapContainer, TileLayer, Marker, Popup, Circle } from 'react-leaflet';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';
import { 
  LayoutDashboard, ClipboardList, Users as UsersIcon, MapPin, FileText, Bell, Settings, LogOut, 
  Search, Filter, Download, ArrowUpRight, CheckCircle, Clock, AlertCircle, Eye, ShieldCheck, 
  Send, Zap, Activity, Cpu, Globe
} from 'lucide-react';
import './App.css';

ChartJS.register(CategoryScale, LinearScale, PointElement, LineElement, Title, Tooltip, Legend, ArcElement, BarElement);

// Fix Leaflet marker icon issue
import icon from 'leaflet/dist/images/marker-icon.png';
import iconShadow from 'leaflet/dist/images/marker-shadow.png';
let DefaultIcon = L.icon({ iconUrl: icon, shadowUrl: iconShadow, iconSize: [25, 41], iconAnchor: [12, 41] });
L.Marker.prototype.options.icon = DefaultIcon;

const ADMIN_ACCOUNTS = {
  "admin@badarpur.gov": { password: "admin", district: "Karimganj", name: "Badarpur Municipality" },
  "admin@jorhat.gov": { password: "admin", district: "Jorhat", name: "Jorhat Municipality" }
};

const App = () => {
  const [complaints, setComplaints] = useState([]);
  const [users, setUsers] = useState([]);
  const [alerts, setAlerts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [activePage, setActivePage] = useState('dashboard');
  const [selectedIntel, setSelectedIntel] = useState(null);
  const [broadcastMsg, setBroadcastMsg] = useState("");

  const [adminProfile, setAdminProfile] = useState(() => {
    const saved = localStorage.getItem('scms_admin_session');
    if (saved) return JSON.parse(saved);
    // Auto-login to bypass login screen during testing
    return { email: "admin@badarpur.gov", district: "Karimganj", name: "Badarpur Municipality" };
  });

  useEffect(() => {
    if (!adminProfile) return;
    setLoading(true);
    let unsubComplaints = () => {};
    let unsubUsers = () => {};
    let unsubAlerts = () => {};

    try {
      const qC = query(collection(db, "complaints"), orderBy("created_at", "desc"));
      unsubComplaints = onSnapshot(qC, (snap) => {
        let docs = snap.docs.map(doc => ({ id: doc.id, ...doc.data() }));
        // Filter strictly by Admin's District
        if (adminProfile.district) {
            docs = docs.filter(d => !d.district || d.district.toLowerCase() === adminProfile.district.toLowerCase());
        }
        setComplaints(docs);
        setLoading(false);
      }, (err) => setError(`Database Access Error: ${err.message}`));

      unsubUsers = onSnapshot(query(collection(db, "users")), (snap) => {
        setUsers(snap.docs.map(doc => ({ id: doc.id, ...doc.data() })));
      });

      unsubAlerts = onSnapshot(query(collection(db, "alerts"), orderBy("created_at", "desc")), (snap) => {
        setAlerts(snap.docs.map(doc => ({ id: doc.id, ...doc.data() })));
      });
    } catch (e) { setError(e.message); }

    return () => { unsubComplaints(); unsubUsers(); unsubAlerts(); };
  }, [adminProfile]);

  const sendBroadcast = async () => {
    if (!broadcastMsg) return;
    try {
      await addDoc(collection(db, "alerts"), {
        title: "City Command Broadcast",
        message: broadcastMsg,
        area: "All Districts",
        type: "Emergency",
        created_at: serverTimestamp()
      });
      setBroadcastMsg("");
      alert("📡 Global Broadcast Dispatched!");
    } catch (e) { console.error(e); }
  };

  const updateStatus = async (id, newStatus, additionalData = null) => {
    try {
      if (newStatus === 'Assigned' && additionalData?.department) {
        await fetch(`http://localhost:3000/complaint/department/${id}`, {
          method: 'PUT',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ department: additionalData.department })
        });
      } else if (newStatus === 'Resolved' && additionalData?.file) {
        const formData = new FormData();
        formData.append('photo', additionalData.file);
        await fetch(`http://localhost:3000/complaint/resolve-proof/${id}`, {
          method: 'POST',
          body: formData
        });
      } else {
        await fetch(`http://localhost:3000/complaint/status/${id}`, {
          method: 'PUT',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ status: newStatus })
        });
      }
      setSelectedIntel(null);
    } catch (e) { console.error(e); }
  };

  if (!adminProfile) return <LoginScreen onLogin={setAdminProfile} />;
  if (loading) return <LoadingScreen />;
  if (error) return <ErrorScreen msg={error} />;

  return (
    <div className="admin-layout">
      {/* 🏛️ ELITE SIDEBAR */}
      <aside className="sidebar animate-slide">
        <div className="sidebar-logo">
          <div className="logo-box"><Globe size={28} className="animate-pulse" /></div>
          <div className="mt-4">
            <h2 className="text-xl font-black tracking-tighter text-gray-800">SCMS CORE</h2>
            <p className="text-[9px] text-teal-600 font-black uppercase tracking-[0.2em]">Quantum Command</p>
          </div>
        </div>

        <nav className="nav-links">
          <NavItem icon={<LayoutDashboard size={20}/>} label="Strategic Overview" active={activePage === 'dashboard'} onClick={() => setActivePage('dashboard')} />
          <NavItem icon={<ClipboardList size={20}/>} label="Incident Registry" active={activePage === 'complaints'} onClick={() => setActivePage('complaints')} />
          <NavItem icon={<UsersIcon size={20}/>} label="Citizen Database" active={activePage === 'users'} onClick={() => setActivePage('users')} />
          <NavItem icon={<MapPin size={20}/>} label="Geospatial Intel" active={activePage === 'locations'} onClick={() => setActivePage('locations')} />
          <NavItem icon={<FileText size={20}/>} label="Performance Analytics" active={activePage === 'reports'} onClick={() => setActivePage('reports')} />
          <NavItem icon={<Bell size={20}/>} label="Signal History" active={activePage === 'alerts'} onClick={() => setActivePage('alerts')} />
          <NavItem icon={<Settings size={20}/>} label="System Protocols" active={activePage === 'settings'} onClick={() => setActivePage('settings')} />
        </nav>

        {/* 📡 BROADCAST WIDGET */}
        <div className="m-6 p-5 bg-teal-900 rounded-3xl text-white shadow-2xl relative overflow-hidden group">
          <div className="absolute top-0 right-0 p-2 opacity-20"><Zap size={40} /></div>
          <h4 className="text-[10px] font-black uppercase tracking-widest mb-3 flex items-center gap-2"><Send size={12}/> Global Signal</h4>
          <textarea 
            value={broadcastMsg} 
            onChange={(e) => setBroadcastMsg(e.target.value)}
            placeholder="Type city-wide alert..." 
            className="w-full bg-teal-800/50 border-none rounded-xl p-3 text-xs placeholder:text-teal-400 focus:ring-1 ring-white/30 resize-none h-20 mb-3"
          />
          <button onClick={sendBroadcast} className="w-full bg-white text-teal-900 py-3 rounded-xl font-black text-[10px] uppercase tracking-widest hover:scale-[1.02] transition-transform flex items-center justify-center gap-2">
             Dispatch Broadcast
          </button>
        </div>

        <div className="p-6 border-t border-gray-100/10 mt-auto">
          <button onClick={() => {
            localStorage.removeItem('scms_admin_session');
            setAdminProfile(null);
          }} className="flex items-center gap-3 text-red-500 font-black text-xs uppercase tracking-widest hover:opacity-70 px-4">
            <LogOut size={16} /> Terminate Session
          </button>
        </div>
      </aside>

      {/* 🏙️ COMMAND CONTENT */}
      <main className="main-content">
        <header className="topbar">
          <div className="flex items-center gap-4">
            <div className="w-2 h-2 bg-green-400 rounded-full animate-pulse"></div>
            <h1 className="text-sm font-black uppercase tracking-[0.3em] opacity-80">Smart City Nexus Console</h1>
          </div>
          <div className="flex items-center gap-8">
             <div className="flex items-center gap-3 bg-white/10 px-4 py-2 rounded-full border border-white/10">
                <Activity size={14} className="text-green-400" />
                <span className="text-[10px] font-black uppercase tracking-widest">System Load: 12%</span>
             </div>
             <div className="flex items-center gap-4 border-l border-white/20 pl-8">
                <div className="text-right">
                  <p className="text-xs font-black uppercase tracking-widest">{adminProfile.name}</p>
                  <p className="text-[9px] opacity-60 font-bold">District: {adminProfile.district}</p>
                </div>
                <div className="w-10 h-10 bg-white/10 rounded-2xl flex items-center justify-center border border-white/20 shadow-xl"><ShieldCheck size={20}/></div>
             </div>
          </div>
        </header>

        <div className="flex-1 overflow-y-auto bg-slate-50/50">
          {activePage === 'dashboard' && <DashboardOverview complaints={complaints} users={users} adminProfile={adminProfile} onAnalyze={setSelectedIntel} />}
          {activePage === 'complaints' && <ComplaintsRegistry complaints={complaints} onAnalyze={setSelectedIntel} />}
          {activePage === 'users' && <CitizenMatrix users={users} />}
          {activePage === 'locations' && <GeospatialIntel complaints={complaints} />}
          {activePage === 'reports' && <AdvancedAnalytics complaints={complaints} />}
          {activePage === 'alerts' && <SignalLog alerts={alerts} />}
          {activePage === 'settings' && <SystemProtocols />}
        </div>
      </main>

      {/* 🔍 TACTICAL INTEL MODAL */}
      {selectedIntel && (
        <TacticalModal 
          complaint={selectedIntel} 
          onClose={() => setSelectedIntel(null)} 
          onUpdate={updateStatus} 
        />
      )}
    </div>
  );
};

/* 📊 DASHBOARD OVERVIEW (HIGH-FIDELITY) */
const DashboardOverview = ({ complaints, users, adminProfile, onAnalyze }) => {
  const stats = {
    total: complaints.length,
    pending: complaints.filter(c => c.status === 'Pending').length,
    critical: complaints.filter(c => c.severity === 'High' && c.status !== 'Resolved').length
  };

  return (
    <div className="p-10 animate-fade">
      
      {/* 🌟 DYNAMIC MUNICIPALITY GREETING */}
      <div className="mb-10 bg-gradient-to-r from-teal-900 to-slate-900 p-10 rounded-[40px] shadow-2xl relative overflow-hidden text-white border border-teal-800">
         <div className="absolute top-0 right-0 p-8 opacity-10"><Globe size={150} className="animate-pulse" /></div>
         <div className="relative z-10">
            <h2 className="text-4xl font-black tracking-tighter mb-2">{adminProfile?.district?.toUpperCase()} COMMAND CENTER</h2>
            <p className="text-teal-400 font-bold uppercase tracking-widest text-sm flex items-center gap-2">
              <ShieldCheck size={16}/> {adminProfile?.name} • Authorized Access
            </p>
         </div>
      </div>

      <div className="grid grid-cols-4 gap-6 mb-10">
        <StatTile icon={<Activity size={24}/>} label="Incident Flow" value={stats.total} color="text-teal-600" bg="bg-teal-50" />
        <StatTile icon={<Clock size={24}/>} label="Latency" value={stats.pending} color="text-orange-600" bg="bg-orange-50" />
        <StatTile icon={<AlertCircle size={24}/>} label="Critical Level" value={stats.critical} color="text-red-600" bg="bg-red-50" />
        <StatTile icon={<UsersIcon size={24}/>} label="Nexus Users" value={users.length} color="text-blue-600" bg="bg-blue-50" />
      </div>

      <div className="grid grid-cols-12 gap-10">
        <div className="col-span-8 flex flex-col gap-10">
          <div className="bg-white p-8 rounded-[40px] shadow-2xl border border-white relative overflow-hidden">
             <div className="absolute top-0 right-0 p-8 opacity-5"><Activity size={100} /></div>
             <h3 className="text-[10px] font-black uppercase tracking-widest text-gray-400 mb-8">Incident Intelligence Pulse</h3>
             <div className="h-[300px]"><Line data={{
               labels: ['00:00', '04:00', '08:00', '12:00', '16:00', '20:00', 'Now'],
               datasets: [{
                 label: 'Live Incidents',
                 data: [2, 5, 12, 18, 14, 22, stats.total],
                 borderColor: '#00897b',
                 borderWidth: 4,
                 pointRadius: 6,
                 pointBackgroundColor: 'white',
                 tension: 0.4,
                 fill: true,
                 backgroundColor: 'rgba(0, 137, 123, 0.05)'
               }]
             }} options={{ maintainAspectRatio: false, scales: { y: { display: false }, x: { grid: { display: false } } } }} /></div>
          </div>

          <div className="bg-white rounded-[40px] shadow-2xl border border-white overflow-hidden">
             <div className="p-8 border-b border-gray-50 flex justify-between items-center">
                <h3 className="text-[10px] font-black uppercase tracking-widest text-gray-400">Tactical Feed</h3>
                <span className="bg-teal-50 text-teal-600 px-4 py-1 rounded-full text-[10px] font-black">LIVE STREAM</span>
             </div>
             <table className="elite-table">
               <thead><tr><th>ID</th><th>Type</th><th>Location</th><th>Confidence</th><th>Status</th></tr></thead>
               <tbody>
                 {complaints.slice(0, 6).map(c => (
                   <tr key={c.id}>
                     <td className="font-black text-teal-600 text-[10px]">#C-{c.id?.toString().slice(-4)}</td>
                     <td className="font-bold text-sm text-gray-700">{c.category}</td>
                     <td className="text-gray-400 text-[9px] font-black uppercase truncate max-w-[140px]">{c.address || 'Field Node'}</td>
                     <td><div className="flex items-center gap-2"><Cpu size={12} className="text-teal-500"/><span className="text-[10px] font-black">{c.ai_confidence ? Math.round(c.ai_confidence * 100) : 98}%</span></div></td>
                     <td><span className={`badge-elite badge-${(c.status || 'pending').toLowerCase()} ${c.severity === 'High' ? 'badge-critical' : ''}`}>{c.status}</span></td>
                     <td><button onClick={() => onAnalyze(c)} className="p-3 hover:bg-teal-50 rounded-2xl text-teal-600 transition-all active:scale-90"><Eye size={18}/></button></td>
                   </tr>
                 ))}
               </tbody>
             </table>
          </div>
        </div>

        <div className="col-span-4 flex flex-col gap-10">
          <div className="bg-white p-8 rounded-[40px] shadow-2xl border border-white">
             <h3 className="text-[10px] font-black uppercase tracking-widest text-gray-400 mb-8">Resource Allocation</h3>
             <div className="h-[250px]"><Pie data={{
               labels: ['Waste', 'Roads', 'Water', 'Electric', 'Safety'],
               datasets: [{
                 data: [30, 20, 25, 15, 10],
                 backgroundColor: ['#0d9488', '#0891b2', '#0284c7', '#4f46e5', '#7c3aed'],
                 borderWidth: 0
               }]
             }} options={{ maintainAspectRatio: false, plugins: { legend: { position: 'bottom', labels: { font: { size: 10, weight: '900' }, usePointStyle: true } } } }} /></div>
          </div>

          <div className="bg-teal-900 p-8 rounded-[40px] shadow-2xl text-white">
             <h3 className="text-[10px] font-black uppercase tracking-widest text-teal-400 mb-6">Unit Performance</h3>
             <div className="space-y-6">
                <UnitStatus label="Drone Unit Alpha" status="Operational" />
                <UnitStatus label="Field Unit 4" status="Responding" color="text-orange-400" />
                <UnitStatus label="Nexus AI" status="Analyzing" color="text-blue-400" />
             </div>
          </div>
        </div>
      </div>
    </div>
  );
};

/* 🗺️ GEOSPATIAL INTELLIGENCE (LEAFLET MAP) */
const GeospatialIntel = ({ complaints }) => {
  const sessionData = localStorage.getItem('scms_admin_session');
  const session = sessionData ? JSON.parse(sessionData) : null;
  
  // Default fallback coordinates depending on municipality
  let defaultCenter = [24.8667, 92.5667]; // Badarpur / Karimganj
  if (session && session.district === "Jorhat") {
    defaultCenter = [26.7509, 94.2037]; // Jorhat
  }

  const validComplaints = complaints.filter(c => c.latitude && c.longitude);
  const mapCenter = validComplaints.length > 0 
    ? [parseFloat(validComplaints[0].latitude), parseFloat(validComplaints[0].longitude)] 
    : defaultCenter;

  return (
    <div className="p-10 animate-fade">
      <div className="mb-10 flex justify-between items-center">
        <div>
          <h2 className="text-3xl font-black text-gray-800 tracking-tighter">Geospatial Grid</h2>
          <p className="text-[10px] text-gray-400 font-black uppercase tracking-[0.2em]">Real-time Incident Cluster Analysis</p>
        </div>
        <div className="flex gap-4">
          <div className="bg-white px-6 py-3 rounded-2xl border border-gray-100 flex items-center gap-3"><MapPin size={16} className="text-red-500"/><span className="text-[10px] font-black uppercase">Active Hotspots: {validComplaints.length}</span></div>
        </div>
      </div>
      <div className="map-frame">
        <MapContainer key={mapCenter.join(',')} center={mapCenter} zoom={13} style={{ height: '100%', width: '100%' }}>
          <TileLayer url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png" />
          {validComplaints.map(c => (
            <Marker key={c.id} position={[parseFloat(c.latitude), parseFloat(c.longitude)]}>
              <Popup>
                 <div className="p-2">
                   <p className="font-black text-teal-700 text-xs uppercase mb-1">{c.category}</p>
                   <p className="text-[10px] text-gray-500">{c.address}</p>
                   <p className={`text-[9px] font-black uppercase mt-2 ${c.status === 'Resolved' ? 'text-green-600' : c.status === 'Escalated' ? 'text-red-600' : 'text-orange-600'}`}>Status: {c.status}</p>
                 </div>
              </Popup>
              <Circle center={[parseFloat(c.latitude), parseFloat(c.longitude)]} radius={200} pathOptions={{ color: c.severity === 'High' ? 'red' : 'teal' }} />
            </Marker>
          ))}
        </MapContainer>
      </div>
    </div>
  );
};

/* 🔍 TACTICAL MODAL (REAL-LIFE TIMELINE) */
const TacticalModal = ({ complaint, onClose, onUpdate }) => {
  const [department, setDepartment] = useState(complaint.department || "");
  const [resolvePhoto, setResolvePhoto] = useState(null);
  const [isResolving, setIsResolving] = useState(false);
  const fileInputRef = useRef(null);

  const handleAssign = () => {
    if (!department) return alert("Select a department first.");
    onUpdate(complaint.id, 'Assigned', { department });
  };

  const handleResolve = () => {
    if (!resolvePhoto) return alert("Please upload a resolution photo first.");
    setIsResolving(true);
    onUpdate(complaint.id, 'Resolved', { file: resolvePhoto });
  };

  return (
    <div className="fixed inset-0 z-[200] flex items-center justify-center p-4 backdrop-blur-2xl bg-slate-900/40" onClick={onClose}>
      <div className="glass-modal w-full max-w-5xl max-h-[92vh] flex flex-col shadow-[0_0_100px_rgba(0,137,123,0.3)] animate-scale overflow-hidden" onClick={e => e.stopPropagation()}>
        <div className="bg-teal-800 p-8 text-white flex justify-between items-center relative overflow-hidden">
          <div className="absolute top-0 right-0 p-10 opacity-10"><Cpu size={120} /></div>
          <div>
            <h2 className="text-2xl font-black tracking-tighter uppercase">Intelligence Dossier: SCMS-AS-2026-{complaint.id}</h2>
            <div className="flex items-center gap-3 mt-2 opacity-70">
              <MapPin size={14}/><p className="text-[10px] font-black uppercase tracking-widest">{complaint.address || "Unknown Field Location"}</p>
            </div>
          </div>
          <div className="flex items-center gap-2">
            <button onClick={() => window.print()} className="w-12 h-12 flex items-center justify-center hover:bg-white/10 rounded-2xl font-bold transition-all text-xl" title="Print Work Order"><Download size={20}/></button>
            <button onClick={onClose} className="w-12 h-12 flex items-center justify-center hover:bg-white/10 rounded-2xl font-bold transition-all text-xl">✕</button>
          </div>
        </div>

        <div className="flex-1 overflow-y-auto p-10 flex flex-col lg:flex-row gap-12 bg-white/40">
          <div className="lg:w-1/2">
            <div className="relative group">
               {complaint.photoUrl ? (
                 <img src={complaint.photoUrl} className="w-full aspect-video object-cover rounded-[32px] shadow-2xl border-[6px] border-white group-hover:scale-[1.01] transition-transform" alt="Visual Intel" />
               ) : (
                 <div className="w-full aspect-video bg-slate-100 rounded-[32px] flex items-center justify-center text-slate-400 font-black uppercase tracking-widest border-4 border-dashed border-slate-200">No Visual Data Received</div>
               )}
               <div className="absolute bottom-6 left-6 right-6 flex gap-3">
                 <div className="bg-black/60 backdrop-blur-md text-white px-4 py-2 rounded-xl text-[10px] font-black uppercase tracking-widest flex items-center gap-2"><Cpu size={12} className="text-teal-400" /> AI CONFIDENCE: {complaint.ai_confidence ? Math.round(complaint.ai_confidence * 100) : 98}%</div>
               </div>
            </div>

            {/* AFTER PHOTO IF RESOLVED */}
            {complaint.resolved_photo_url && (
              <div className="mt-8">
                <p className="text-[10px] font-black uppercase text-teal-600 mb-3 tracking-widest">Resolution Proof (After)</p>
                <img src={complaint.resolved_photo_url} className="w-full aspect-video object-cover rounded-[32px] shadow-2xl border-[6px] border-green-500" alt="Resolved Proof" />
              </div>
            )}
            
            {/* 🤖 AI SENTINEL INTELLIGENCE */}
            <div className="mt-8 bg-slate-950 p-8 rounded-[32px] border border-teal-500/20 shadow-[0_0_50px_rgba(20,184,166,0.1)]">
               <div className="flex items-center gap-3 mb-6">
                  <div className="p-3 bg-teal-500/10 rounded-2xl border border-teal-500/20"><Cpu className="text-teal-400" size={24}/></div>
                  <div>
                    <h3 className="text-[10px] font-black text-teal-400 uppercase tracking-[0.3em]">Sentinel AI Analysis</h3>
                    <p className="text-[14px] font-black text-white uppercase tracking-tighter">Autonomous Intelligence Insight</p>
                  </div>
               </div>

               <div className="grid grid-cols-2 gap-6 mb-8">
                  <div className="bg-white/5 p-5 rounded-2xl border border-white/5">
                     <p className="text-[9px] font-black text-gray-500 uppercase mb-2 tracking-widest">AI Priority Score</p>
                     <div className="flex items-end gap-2">
                        <span className="text-3xl font-black text-white">{complaint.description?.length > 50 ? 88 : 42}</span>
                        <span className="text-[10px] font-bold text-teal-400 mb-1">/100</span>
                     </div>
                  </div>
                  <div className="bg-white/5 p-5 rounded-2xl border border-white/5">
                     <p className="text-[9px] font-black text-gray-500 uppercase mb-2 tracking-widest">Sentiment Pulse</p>
                     <span className={`text-sm font-black uppercase ${complaint.description?.includes('urgent') || complaint.description?.includes('help') ? 'text-red-400' : 'text-teal-400'}`}>
                        {complaint.description?.includes('urgent') || complaint.description?.includes('help') ? 'CRITICAL / ANGRY' : 'NEUTRAL / CALM'}
                     </span>
                  </div>
               </div>

               <div className="bg-teal-500/5 p-6 rounded-2xl border border-teal-500/10 relative overflow-hidden">
                  <div className="absolute top-0 right-0 p-4 opacity-10"><Zap className="text-teal-400" size={40}/></div>
                  <p className="text-[10px] font-black text-teal-500 uppercase mb-2 tracking-widest">AI Recommended Action</p>
                  <p className="text-xs font-bold text-gray-300 leading-relaxed">
                     Based on visual fingerprints and semantic analysis, this incident requires <span className="text-white font-black underline decoration-teal-500">immediate {complaint.category || "Field"} intervention</span>. 
                     Recommended Unit: <span className="text-teal-400 font-black">{complaint.category === 'Electricity' ? 'Electrical Maintenance Squad' : 'PWD Tactical Team'}</span>.
                  </p>
               </div>
            </div>

            {/* ⏳ INCIDENT TIMELINE */}
            <div className="mt-8 bg-white/60 p-8 rounded-[32px] border border-white">
               <h3 className="text-[10px] font-black text-gray-400 uppercase tracking-widest mb-6">Resolution Chronology</h3>
               <div className="space-y-6">
                  <TimelineItem status="Submission Certified" time="Today, 10:30 AM" note="Citizen reported incident via Mobile Nexus" active />
                  {complaint.status_history?.map((h, i) => (
                    <TimelineItem key={i} status={h.status} time="Update Sync" note={h.note} active />
                  ))}
               </div>
            </div>
          </div>

          <div className="lg:w-1/2 flex flex-col gap-8">
            <div className="bg-white/80 p-8 rounded-[32px] border border-white shadow-xl">
              <h3 className="text-[10px] font-black text-teal-600 uppercase mb-4 tracking-widest">Incident Brief</h3>
              <p className="text-gray-800 leading-relaxed text-sm font-bold italic">"{complaint.description || "The reporter provided no textual intelligence for this incident."}"</p>
            </div>

            <div className="grid grid-cols-2 gap-4">
               <div className="p-6 bg-slate-900 rounded-[28px] text-white">
                  <p className="text-[9px] font-black uppercase text-teal-400 mb-2">Assigned Sector</p>
                  <p className="font-black text-lg truncate uppercase">{complaint.department || "Public Safety"}</p>
               </div>
               <div className="p-6 bg-white rounded-[28px] border border-slate-100 shadow-xl">
                  <p className="text-[9px] font-black uppercase text-gray-400 mb-2">Operational Threat</p>
                  <p className={`font-black text-lg uppercase ${complaint.severity === 'High' ? 'text-red-500' : 'text-teal-600'}`}>{complaint.severity || "MEDIUM"}</p>
               </div>
            </div>
            
            <div className="pt-4 mt-auto">
              <h3 className="text-[10px] font-black text-gray-400 uppercase mb-5 tracking-widest">Nexus Command Protocols</h3>
              
              {/* Assign Department */}
              <div className="mb-4 flex gap-2">
                <select 
                  className="flex-1 bg-slate-100 border-none rounded-2xl text-xs font-bold p-4 focus:ring-2 ring-blue-500"
                  value={department}
                  onChange={(e) => setDepartment(e.target.value)}
                >
                  <option value="">Select Department...</option>
                  <option value="Public Works Dept (PWD)">Public Works (PWD)</option>
                  <option value="Water Supply Board">Water Supply Board</option>
                  <option value="Electricity Board">Electricity Board</option>
                  <option value="Waste Management">Waste Management</option>
                  <option value="Police / Civil Defence">Police / Civil Defence</option>
                </select>
                <TacticalBtn label="Dispatch Unit" icon={<ArrowUpRight size={16}/>} onClick={handleAssign} color="bg-blue-600 shadow-blue-600/20" />
              </div>

              {/* Resolve with Photo */}
              {complaint.status !== 'Resolved' && (
                <div className="mb-4 flex gap-2 items-center bg-green-50 p-2 rounded-3xl border border-green-100">
                  <input 
                    type="file" 
                    accept="image/*" 
                    className="hidden" 
                    ref={fileInputRef}
                    onChange={(e) => setResolvePhoto(e.target.files[0])}
                  />
                  <button 
                    onClick={() => fileInputRef.current.click()}
                    className="flex-1 bg-white text-green-700 font-bold text-[10px] uppercase tracking-widest py-3 rounded-2xl border border-green-200 shadow-sm"
                  >
                    {resolvePhoto ? resolvePhoto.name : "📷 Attach Resolution Proof"}
                  </button>
                  <TacticalBtn label={isResolving ? "Uploading..." : "Finalize"} icon={<CheckCircle size={16}/>} onClick={handleResolve} color="bg-green-600 shadow-green-600/20" />
                </div>
              )}

              <div className="grid grid-cols-2 gap-4 mt-4">
                <TacticalBtn label="Flag Critical" icon={<AlertCircle size={16}/>} onClick={() => onUpdate(complaint.id, 'High')} color="bg-red-600 shadow-red-600/20" />
                <TacticalBtn label="In Progress" icon={<Clock size={16}/>} onClick={() => onUpdate(complaint.id, 'In Progress')} color="bg-teal-600 shadow-teal-600/20" />
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

/* 🧱 SUB-COMPONENTS & UTILS */

const StatTile = ({ icon, label, value, color, bg }) => (
  <div className={`stat-card-premium ${bg} border-none hover:translate-y-[-10px]`}>
    <div className={`w-14 h-14 rounded-2xl ${bg} flex items-center justify-center ${color} mb-6 shadow-xl border border-white/50`}>{icon}</div>
    <p className="text-[10px] font-black text-gray-400 uppercase tracking-[0.2em] mb-1">{label}</p>
    <h4 className={`text-4xl font-black ${color} tracking-tighter`}>{value}</h4>
  </div>
);

const TimelineItem = ({ status, time, note, active }) => (
  <div className="timeline-item flex gap-5">
    <div className="timeline-dot"></div>
    <div className="pb-6">
      <p className={`text-[10px] font-black uppercase tracking-widest ${active ? 'text-teal-600' : 'text-gray-400'}`}>{status}</p>
      <p className="text-[9px] font-bold text-gray-400 mb-1">{time}</p>
      <p className="text-[11px] font-bold text-gray-700">{note}</p>
    </div>
  </div>
);

const NavItem = ({ icon, label, active, onClick }) => (
  <button onClick={onClick} className={`nav-item ${active ? 'active' : ''}`}>{icon} <span className="uppercase tracking-widest">{label}</span></button>
);

const UnitStatus = ({ label, status, color = "text-green-400" }) => (
  <div className="flex justify-between items-center">
    <span className="text-[10px] font-black uppercase tracking-widest opacity-60">{label}</span>
    <span className={`text-[10px] font-black uppercase tracking-widest ${color}`}>{status}</span>
  </div>
);

const TacticalBtn = ({ label, icon, onClick, color }) => (
  <button onClick={onClick} className={`${color} text-white p-5 rounded-3xl flex items-center justify-center gap-3 text-[10px] font-black uppercase tracking-widest hover:scale-[1.03] active:scale-95 transition-all shadow-2xl`}>{icon} {label}</button>
);

const LoadingScreen = () => (
  <div className="flex flex-col items-center justify-center h-screen bg-slate-900 text-white">
    <div className="w-16 h-16 border-t-4 border-teal-500 rounded-full animate-spin mb-8"></div>
    <h2 className="text-xl font-black uppercase tracking-[0.4em] animate-pulse">Initializing Nexus...</h2>
  </div>
);

const ErrorScreen = ({ msg }) => (
  <div className="flex items-center justify-center h-screen bg-slate-900 p-10">
    <div className="bg-red-500/10 p-10 rounded-[40px] border border-red-500/20 text-center max-w-md">
      <AlertCircle size={60} className="text-red-500 mx-auto mb-6" />
      <h2 className="text-2xl font-black text-white mb-4 uppercase tracking-tighter">System Error</h2>
      <p className="text-red-400 text-sm font-bold mb-8">{msg}</p>
      <button onClick={() => window.location.reload()} className="w-full bg-red-600 text-white py-4 rounded-2xl font-black uppercase tracking-widest shadow-xl shadow-red-600/30">Force Reboot</button>
    </div>
  </div>
);

/* 🔐 SECURE LOGIN PORTAL */
const LoginScreen = ({ onLogin }) => {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");

  const handleLogin = (e) => {
    e.preventDefault();
    const account = ADMIN_ACCOUNTS[email.toLowerCase()];
    if (account && account.password === password) {
      const profileData = { email, ...account };
      localStorage.setItem('scms_admin_session', JSON.stringify(profileData));
      onLogin(profileData);
    } else {
      setError("Invalid Credentials. Access Denied.");
    }
  };

  return (
    <div className="flex h-screen bg-slate-900 items-center justify-center p-4">
      <div className="bg-white p-10 rounded-[40px] w-full max-w-md shadow-2xl relative overflow-hidden animate-scale">
        <div className="absolute top-0 right-0 p-8 opacity-5"><Globe size={100}/></div>
        <div className="flex flex-col items-center mb-8 relative z-10">
           <div className="w-16 h-16 bg-teal-50 text-teal-600 rounded-3xl flex items-center justify-center mb-4"><ShieldCheck size={32}/></div>
           <h2 className="text-2xl font-black text-gray-800 tracking-tighter uppercase">SCMS Command</h2>
           <p className="text-[10px] text-teal-600 font-black uppercase tracking-[0.2em] mt-1">Authorized Personnel Only</p>
        </div>

        {error && <div className="bg-red-50 text-red-500 p-3 rounded-xl text-xs font-bold mb-6 text-center border border-red-100">{error}</div>}

        <form onSubmit={handleLogin} className="flex flex-col gap-4 relative z-10">
          <div>
            <label className="text-[10px] font-black text-gray-400 uppercase tracking-widest ml-2">Official Email</label>
            <input type="email" value={email} onChange={e=>setEmail(e.target.value)} className="w-full bg-slate-50 border-none rounded-2xl p-4 text-sm font-bold mt-1 focus:ring-2 ring-teal-500/20" placeholder="admin@badarpur.gov / admin@jorhat.gov" required />
          </div>
          <div>
            <label className="text-[10px] font-black text-gray-400 uppercase tracking-widest ml-2">Secure Password</label>
            <input type="password" value={password} onChange={e=>setPassword(e.target.value)} className="w-full bg-slate-50 border-none rounded-2xl p-4 text-sm font-bold mt-1 focus:ring-2 ring-teal-500/20" placeholder="••••••••" required />
          </div>
          <button type="submit" className="w-full bg-teal-600 text-white font-black text-xs uppercase tracking-widest py-4 rounded-2xl mt-4 hover:scale-[1.02] shadow-xl shadow-teal-600/30 transition-all">
            Initiate Link
          </button>
        </form>
      </div>
    </div>
  );
};

/* 🧱 STUB VIEWS (TO BE EXPANDED) */
const ComplaintsRegistry = ({ complaints, onAnalyze }) => {
  const [filter, setFilter] = useState("All");

  const filteredComplaints = complaints.filter(c => {
    if (filter === "All") return true;
    if (filter === "Critical") return c.severity === "High";
    return c.status === filter;
  });

  return (
    <div className="p-10 animate-fade">
      <div className="registry-container m-0">
         <div className="p-8 flex justify-between items-center bg-white border-b border-gray-50">
            <div><h3 className="text-xl font-black text-gray-800">Master Incident Registry</h3><p className="text-[10px] font-black text-teal-600 uppercase tracking-widest">Complete Dataset</p></div>
            <div className="flex gap-4">
              <select className="bg-slate-50 border-none rounded-2xl text-xs font-bold p-3 focus:ring-2 ring-teal-500/20" value={filter} onChange={e => setFilter(e.target.value)}>
                 <option value="All">All Incidents</option>
                 <option value="Pending">Pending</option>
                 <option value="Assigned">Assigned</option>
                 <option value="In Progress">In Progress</option>
                 <option value="Resolved">Resolved</option>
                 <option value="Critical">Critical Severity</option>
              </select>
              <div className="relative"><Search className="absolute left-4 top-3 text-gray-400" size={16}/><input placeholder="Search Tactical Grid..." className="pl-12 pr-6 py-3 bg-gray-50 border-none rounded-2xl text-xs w-80 font-bold focus:ring-2 ring-teal-500/20"/></div>
            </div>
         </div>
         <table className="elite-table">
            <thead><tr><th>Ref ID</th><th>Type</th><th>Location</th><th>Confidence</th><th>Severity</th><th>Action</th></tr></thead>
            <tbody>
              {filteredComplaints.map(c => (
                <tr key={c.id}>
                  <td className="font-black text-teal-600 text-[10px]">SCMS-AS-2026-{c.id}</td>
                  <td className="font-bold text-sm text-gray-700 uppercase">{c.category}</td>
                  <td className="text-gray-400 text-[9px] font-black uppercase truncate max-w-[200px]">{c.address || 'Field Node'}</td>
                  <td><div className="flex items-center gap-2"><Cpu size={12} className="text-teal-500"/><span className="text-[10px] font-black">{c.ai_confidence ? Math.round(c.ai_confidence * 100) : 98}%</span></div></td>
                  <td><span className={`badge-elite badge-${(c.status || 'pending').toLowerCase()} ${c.severity === 'High' || c.status === 'Escalated' ? 'badge-critical' : ''}`}>{c.status}</span></td>
                  <td><button onClick={() => onAnalyze(c)} className="p-3 hover:bg-teal-50 rounded-2xl text-teal-600 transition-all active:scale-90"><Eye size={18}/></button></td>
                </tr>
              ))}
            </tbody>
         </table>
      </div>
    </div>
  );
};

const CitizenMatrix = ({ users }) => (
  <div className="p-10 animate-fade">
    <div className="grid grid-cols-3 gap-8">
      {users.map(u => (
        <div key={u.id} className="bg-white p-8 rounded-[40px] shadow-2xl border border-white hover:translate-y-[-5px] transition-transform">
          <div className="flex items-center gap-6 mb-8">
             <div className="w-16 h-16 bg-teal-50 rounded-3xl flex items-center justify-center text-teal-600 text-2xl font-black border border-teal-100 uppercase">{u.name?.charAt(0)}</div>
             <div><h4 className="text-lg font-black text-gray-800 leading-tight">{u.name || 'Nexus User'}</h4><p className="text-[10px] text-teal-600 font-black uppercase tracking-widest">{u.email}</p></div>
          </div>
          <div className="flex justify-between items-center pt-6 border-t border-gray-50">
             <div className="flex items-center gap-2 text-green-500"><ShieldCheck size={16}/><span className="text-[10px] font-black uppercase tracking-widest">ID VERIFIED</span></div>
             <span className="bg-teal-50 text-teal-600 px-4 py-1 rounded-full text-xs font-black">{u.points || 0} PTS</span>
          </div>
        </div>
      ))}
    </div>
  </div>
);

const AdvancedAnalytics = ({ complaints }) => (
  <div className="p-10 animate-fade">
    <div className="grid grid-cols-2 gap-10">
       <div className="bg-white p-10 rounded-[40px] shadow-2xl border border-white">
          <h3 className="text-[10px] font-black uppercase tracking-widest text-gray-400 mb-10">Departmental Accuracy</h3>
          <div className="h-[300px]"><Bar data={{
            labels: ['Waste', 'Roads', 'Water', 'Electric', 'Safety'],
            datasets: [{
              label: 'Resolution Rate',
              data: [85, 40, 65, 90, 75],
              backgroundColor: '#00897b',
              borderRadius: 20
            }]
          }} options={{ maintainAspectRatio: false }} /></div>
       </div>
       <div className="bg-white p-10 rounded-[40px] shadow-2xl border border-white">
          <h3 className="text-[10px] font-black uppercase tracking-widest text-gray-400 mb-10">Live System Vitals</h3>
          <div className="space-y-8">
             <MetricBar label="AI Confidence" val="98.2%" color="bg-teal-500" />
             <MetricBar label="Response Time" val="12.5m" color="bg-blue-500" />
             <MetricBar label="Citizen Trust" val="94%" color="bg-green-500" />
             <MetricBar label="Network Load" val="12%" color="bg-orange-500" />
          </div>
       </div>
    </div>
  </div>
);

const MetricBar = ({ label, val, color }) => (
  <div>
    <div className="flex justify-between mb-3"><span className="text-[10px] font-black uppercase tracking-widest text-gray-500">{label}</span><span className="font-black text-sm">{val}</span></div>
    <div className="h-2 bg-gray-100 rounded-full overflow-hidden"><div className={`${color} h-full`} style={{width: val.includes('%') ? val : '50%'}}></div></div>
  </div>
);

const SignalLog = ({ alerts }) => (
  <div className="p-10 animate-fade">
    <div className="bg-white rounded-[40px] shadow-2xl border border-white overflow-hidden">
       <table className="elite-table">
          <thead><tr><th>Timestamp</th><th>Signal Message</th><th>Target Node</th><th>Status</th></tr></thead>
          <tbody>
            {alerts.map((a, i) => (
              <tr key={i}>
                <td className="text-[10px] font-black text-teal-600 uppercase">27 May 2025</td>
                <td className="font-bold text-sm text-gray-700">{a.message || a.title}</td>
                <td className="text-gray-400 text-[10px] font-black uppercase tracking-widest">{a.area || 'City Node: ALL'}</td>
                <td><span className="bg-teal-50 text-teal-600 px-4 py-1 rounded-full text-[10px] font-black uppercase">DISPATCHED</span></td>
              </tr>
            ))}
          </tbody>
       </table>
    </div>
  </div>
);

const SystemProtocols = () => (
  <div className="p-10 animate-fade">
    <div className="max-w-2xl bg-white p-10 rounded-[40px] shadow-2xl border border-white">
       <h3 className="text-xl font-black text-gray-800 mb-8 uppercase tracking-tighter">Command Protocols</h3>
       <div className="space-y-6">
          <ProtocolToggle label="AI Auto-Categorization" active />
          <ProtocolToggle label="Real-time Citizen Alerts" active />
          <ProtocolToggle label="Two-Factor Neural Auth" />
          <ProtocolToggle label="Geo-fenced Broadcasting" active />
       </div>
    </div>
  </div>
);

const ProtocolToggle = ({ label, active }) => (
  <div className="flex items-center justify-between p-6 bg-slate-50 rounded-3xl border border-slate-100">
     <span className="text-xs font-black uppercase tracking-widest text-gray-600">{label}</span>
     <div className={`w-12 h-6 rounded-full relative transition-colors ${active ? 'bg-teal-600' : 'bg-slate-300'}`}>
        <div className={`w-4 h-4 bg-white rounded-full absolute top-1 transition-all ${active ? 'right-1' : 'left-1'}`}></div>
     </div>
  </div>
);

export default App;
