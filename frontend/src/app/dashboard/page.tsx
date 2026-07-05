"use client";

import React, { useState, useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { useAuth } from '@/context/AuthContext';
import { roomApi } from '@/services/api';
import { Room } from '@/types';
import { Code2, LogOut, Plus, LogIn, FolderKanban, ArrowRight, Calendar } from 'lucide-react';
import styles from '@/styles/dashboard.module.css';

export default function Dashboard() {
  const { user, logout } = useAuth();
  const router = useRouter();

  const [rooms, setRooms] = useState<Room[]>([]);
  const [createName, setCreateName] = useState('');
  const [createLang, setCreateLang] = useState('python');
  const [joinCode, setJoinCode] = useState('');

  const [createError, setCreateError] = useState('');
  const [joinError, setJoinError] = useState('');
  const [loadingRooms, setLoadingRooms] = useState(true);
  const [actionLoading, setActionLoading] = useState(false);

  const fetchRooms = async () => {
    try {
      const response = await roomApi.getMyRooms();
      setRooms(response.data);
    } catch (err) {
      // Log room loading errors
    } finally {
      setLoadingRooms(false);
    }
  };

  useEffect(() => {
    if (user) {
      fetchRooms();
    }
  }, [user]);

  const handleCreateRoom = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!createName.trim()) {
      setCreateError('Room name is required');
      return;
    }

    setCreateError('');
    setActionLoading(true);

    try {
      const response = await roomApi.createRoom(createName.trim(), createLang);
      const createdRoom = response.data;
      router.push(`/room/${createdRoom.code}`);
    } catch (err: any) {
      setCreateError(err.response?.data?.error || 'Failed to create room');
      setActionLoading(false);
    }
  };

  const handleJoinRoom = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!joinCode.trim()) {
      setJoinError('Room code is required');
      return;
    }

    setJoinError('');
    setActionLoading(true);

    const formattedCode = joinCode.trim().toUpperCase();

    try {
      const response = await roomApi.joinRoom(formattedCode);
      const room = response.data;
      router.push(`/room/${room.code}`);
    } catch (err: any) {
      setJoinError(err.response?.data?.error || 'Room not found or inactive');
      setActionLoading(false);
    }
  };

  const formatDate = (dateString: string) => {
    const d = new Date(dateString);
    return d.toLocaleDateString(undefined, { year: 'numeric', month: 'short', day: 'numeric' });
  };

  return (
    <div className={styles.container}>
      <header className={styles.header}>
        <div className={styles.logo}>
          <Code2 size={24} className={styles.logoIcon} />
          <span>CollabCode Workspace</span>
        </div>
        <div className={styles.userInfo}>
          <span className={styles.username}>Hi, {user?.username}</span>
          <button onClick={logout} className="btn-secondary" style={{ padding: '8px 14px' }}>
            <LogOut size={16} />
            <span>Logout</span>
          </button>
        </div>
      </header>

      <main className={styles.main}>
        <div className={styles.actionsGrid}>
          {/* Create Room Card */}
          <div className={`glass-card ${styles.actionCard} animate-fade-in`}>
            <div className={styles.cardHeader}>
              <div className={styles.iconWrapper}>
                <Plus size={24} />
              </div>
              <h3>Create Coding Room</h3>
            </div>
            <form onSubmit={handleCreateRoom} className={styles.form}>
              <div className="inputGroup" style={{ gap: '6px' }}>
                <input
                  type="text"
                  className="input-field"
                  placeholder="Project Name (e.g. Chat App)"
                  value={createName}
                  onChange={(e) => setCreateName(e.target.value)}
                  disabled={actionLoading}
                  required
                />
              </div>
              <div className="inputGroup" style={{ gap: '6px' }}>
                <select
                  className={styles.selectField}
                  value={createLang}
                  onChange={(e) => setCreateLang(e.target.value)}
                  disabled={actionLoading}
                >
                  <option value="python">Python</option>
                  <option value="java">Java</option>
                  <option value="cpp">C++</option>
                </select>
              </div>
              {createError && <div className={styles.error}>{createError}</div>}
              <button type="submit" className="btn-primary" style={{ width: '100%', justifyContent: 'center' }} disabled={actionLoading}>
                {actionLoading ? 'Creating...' : 'Create Room'}
              </button>
            </form>
          </div>

          {/* Join Room Card */}
          <div className={`glass-card ${styles.actionCard} animate-fade-in`} style={{ animationDelay: '0.1s' }}>
            <div className={styles.cardHeader}>
              <div className={styles.iconWrapper}>
                <LogIn size={24} />
              </div>
              <h3>Join via Room Code</h3>
            </div>
            <form onSubmit={handleJoinRoom} className={styles.form}>
              <div className="inputGroup" style={{ gap: '6px' }}>
                <input
                  type="text"
                  className="input-field"
                  placeholder="Room Code (e.g. ABC-XYZ)"
                  value={joinCode}
                  onChange={(e) => setJoinCode(e.target.value)}
                  disabled={actionLoading}
                  required
                />
              </div>
              {joinError && <div className={styles.error}>{joinError}</div>}
              <button type="submit" className="btn-primary" style={{ width: '100%', justifyContent: 'center' }} disabled={actionLoading}>
                {actionLoading ? 'Joining...' : 'Join Room'}
              </button>
            </form>
          </div>
        </div>

        {/* Previous Projects Section */}
        <section className={styles.roomsSection}>
          <div className={styles.sectionHeader}>
            <FolderKanban size={22} className={styles.logoIcon} />
            <h2>Your Coding Rooms</h2>
          </div>

          {loadingRooms ? (
            <div style={{ color: 'var(--text-secondary)', textAlign: 'center', padding: '40px' }}>Loading rooms...</div>
          ) : (
            <div className={styles.roomsGrid}>
              {rooms.length === 0 ? (
                <div className={`glass-card ${styles.emptyState}`}>
                  <p>You haven&apos;t created or joined any rooms yet. Start by creating a new room above!</p>
                </div>
              ) : (
                rooms.map((room, idx) => (
                  <div
                    key={room.id}
                    className={`glass-card ${styles.roomCard} animate-fade-in`}
                    style={{ animationDelay: `${0.15 + idx * 0.05}s` }}
                  >
                    <div className={styles.roomMeta}>
                      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
                        <span className={styles.roomTitle} title={room.name}>{room.name}</span>
                        <span className={`${styles.badge} ${
                          room.role === 'OWNER' ? styles.ownerBadge :
                          room.role === 'EDITOR' ? styles.editorBadge : styles.viewerBadge
                        }`}>
                          {room.role}
                        </span>
                      </div>
                      <span className={styles.roomCode}>{room.code}</span>
                      <div className={styles.roomDetails}>
                        <span>Owner: {room.role === 'OWNER' ? 'You' : room.ownerUsername}</span>
                        <span style={{ display: 'flex', alignItems: 'center', gap: '4px' }}>
                          <Calendar size={12} />
                          {formatDate(room.createdAt)}
                        </span>
                      </div>
                    </div>

                    <div className={styles.cardFooter}>
                      <button
                        onClick={() => router.push(`/room/${room.code}`)}
                        className="btn-primary"
                        style={{ padding: '8px 12px', fontSize: '13px', width: '100%', justifyContent: 'center' }}
                      >
                        <span>Open Project</span>
                        <ArrowRight size={14} />
                      </button>
                    </div>
                  </div>
                ))
              )}
            </div>
          )}
        </section>
      </main>
    </div>
  );
}
