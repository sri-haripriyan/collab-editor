"use client";

import Link from 'next/link';
import { useAuth } from '@/context/AuthContext';
import { Code2, Users2, Zap, Shield } from 'lucide-react';
import styles from '@/styles/landing.module.css';

export default function Home() {
  const { user } = useAuth();

  return (
    <div className={styles.container}>
      <header className={styles.header}>
        <div className={styles.logo}>
          <Code2 size={32} className={styles.logoIcon} />
          <span>CollabCode</span>
        </div>
        <div className={styles.navActions}>
          {user ? (
            <Link href="/dashboard" className="btn-primary">
              Go to Dashboard
            </Link>
          ) : (
            <>
              <Link href="/login" className="btn-secondary">
                Login
              </Link>
              <Link href="/register" className="btn-primary">
                Sign Up
              </Link>
            </>
          )}
        </div>
      </header>

      <main className={styles.main}>
        <div className={styles.heroSection}>
          <h1 className={`${styles.title} animate-fade-in`}>
            Real-Time Code Collaboration, <span className={styles.highlight}>Redefined.</span>
          </h1>
          <p className={styles.subtitle}>
            Code, talk, and execute projects in real-time with your team. Backed by Monaco Editor, WebRTC voice communication, and a secure multi-language playground.
          </p>
          <div className={styles.ctaButtons}>
            <Link href={user ? "/dashboard" : "/register"} className="btn-primary glow-effect">
              Start Coding Now
            </Link>
            <a href="#features" className="btn-secondary">
              Explore Features
            </a>
          </div>
        </div>

        <section id="features" className={styles.featuresSection}>
          <div className="glass-card className={styles.featureCard}">
            <Zap size={40} className={styles.featureIcon} />
            <h3>Zero-Latency Sync</h3>
            <p>CRDT-based collaborative text editing ensures sync conflicts are mathematically impossible. Code seamlessly with 10+ users.</p>
          </div>

          <div className="glass-card className={styles.featureCard}">
            <Users2 size={40} className={styles.featureIcon} />
            <h3>Crystal Clear Voice Chat</h3>
            <p>Peer-to-peer WebRTC voice conferencing integrated directly into the workspace. Mute/unmute microphone dynamically with a keystroke.</p>
          </div>

          <div className="glass-card className={styles.featureCard}">
            <Code2 size={40} className={styles.featureIcon} />
            <h3>Secure Polyglot Run</h3>
            <p>Run your files in Python, Java, or C++ instantly inside our isolated execution sandbox. Get live console inputs and outputs.</p>
          </div>

          <div className="glass-card className={styles.featureCard}">
            <Shield size={40} className={styles.featureIcon} />
            <h3>Permission Controls</h3>
            <p>Room owners maintain absolute control. Elevate active participants to Editors or demote them to Viewers in real-time.</p>
          </div>
        </section>
      </main>

      <footer className={styles.footer}>
        <p>&copy; {new Date().getFullYear()} CollabCode. Built for developers, by developers.</p>
      </footer>
    </div>
  );
}
