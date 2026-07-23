'use client';

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';

export default function OAuthCallbackPage() {
  const router = useRouter();
  const [message, setMessage] = useState('Completing your Google sign-in…');

  useEffect(() => {
    const token = new URLSearchParams(window.location.hash.slice(1)).get('token');
    const error = new URLSearchParams(window.location.search).get('error');

    if (token) {
      localStorage.setItem('inventory_token', token);
      router.replace('/spaces');
      return;
    }

    setMessage(error ? 'Google sign-in could not be completed.' : 'Google sign-in did not return a token.');
    const redirectTimer = window.setTimeout(() => router.replace('/login'), 2500);
    return () => window.clearTimeout(redirectTimer);
  }, [router]);

  return (
    <main className="min-h-screen flex items-center justify-center bg-secondary px-6">
      <p className="text-sm text-muted-foreground">{message}</p>
    </main>
  );
}
