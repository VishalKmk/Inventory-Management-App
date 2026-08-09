'use client';

import Link from 'next/link';
import { useSearchParams } from 'next/navigation';
import { FormEvent, Suspense, useState } from 'react';
import { toast } from 'sonner';
import AppLogo from '@/components/ui/AppLogo';
import { apiFetch } from '@/lib/api';

interface VerifyOtpResponse {
  verified: boolean;
  attemptsRemaining: number;
  locked: boolean;
}

interface ApiErrorWithData extends Error {
  data?: Partial<VerifyOtpResponse>;
}

const MAX_OTP_ATTEMPTS = 5;

export default function VerifyEmailPage() {
  return (
    <Suspense fallback={<VerifyEmailFallback />}>
      <VerifyEmailForm />
    </Suspense>
  );
}

function VerifyEmailForm() {
  const searchParams = useSearchParams();
  const [email, setEmail] = useState(searchParams.get('email') || '');
  const [code, setCode] = useState('');
  const [attemptsRemaining, setAttemptsRemaining] = useState<number | null>(null);
  const [locked, setLocked] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const [isResending, setIsResending] = useState(false);

  const applyOtpState = (data?: Partial<VerifyOtpResponse>) => {
    if (typeof data?.attemptsRemaining === 'number') {
      setAttemptsRemaining(data.attemptsRemaining);
    }

    if (typeof data?.locked === 'boolean') {
      setLocked(data.locked);
    }
  };

  const verifyEmail = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();

    if (locked) {
      toast.error('Your OTP is locked. Request a new code to try again.');
      return;
    }

    setIsLoading(true);

    try {
      const response = await apiFetch<VerifyOtpResponse>('/api/auth/verify-otp', {
        method: 'POST',
        body: JSON.stringify({ email, code }),
      });

      applyOtpState(response);

      if (response.verified) {
        toast.success('Email verified. You can now sign in.');
        window.location.assign('/login');
        return;
      }

      toast.error('Unable to verify your email');
    } catch (error) {
      const apiError = error as ApiErrorWithData;
      applyOtpState(apiError.data);

      const message = error instanceof Error ? error.message : 'Unable to verify your email';
      toast.error(message);
    } finally {
      setIsLoading(false);
    }
  };

  const resendCode = async () => {
    if (!email) {
      toast.error('Enter your email address first');
      return;
    }

    setIsResending(true);

    try {
      await apiFetch('/api/auth/resend-otp', {
        method: 'POST',
        body: JSON.stringify({ email }),
      });

      setCode('');
      setAttemptsRemaining(MAX_OTP_ATTEMPTS);
      setLocked(false);
      toast.success('A new verification code has been sent.');
    } catch (error) {
      toast.error(error instanceof Error ? error.message : 'Unable to resend the code');
    } finally {
      setIsResending(false);
    }
  };

  return (
    <main className="min-h-screen bg-secondary flex items-center justify-center px-6 py-12">
      <section className="w-full max-w-[400px]">
        <div className="flex flex-col items-center mb-8">
          <div className="w-14 h-14 bg-primary rounded-xl flex items-center justify-center mb-4 shadow-elevated">
            <AppLogo size={36} />
          </div>
          <h1 className="text-2xl font-bold text-foreground tracking-tight">StockRoom</h1>
          <p className="text-sm text-muted-foreground mt-1">Verify your email address</p>
        </div>

        <div className="bg-card rounded-xl border border-border shadow-card p-8">
          <p className="text-sm text-muted-foreground text-center mb-6">
            Enter the six-digit code sent to your email to activate your account.
          </p>

          <form onSubmit={verifyEmail} className="space-y-4">
            <div>
              <label htmlFor="email" className="block text-sm font-medium text-foreground mb-1.5">Email</label>
              <input
                id="email"
                type="email"
                autoComplete="email"
                required
                value={email}
                onChange={(event) => setEmail(event.target.value)}
                className="input-field"
              />
            </div>

            <div>
              <label htmlFor="code" className="block text-sm font-medium text-foreground mb-1.5">Verification code</label>
              <input
                id="code"
                inputMode="numeric"
                autoComplete="one-time-code"
                required
                pattern="[0-9]{6}"
                maxLength={6}
                value={code}
                disabled={locked}
                onChange={(event) => setCode(event.target.value.replace(/\D/g, ''))}
                className="input-field disabled:opacity-50"
                placeholder="123456"
              />
            </div>

            {attemptsRemaining !== null && !locked && (
              <p className="text-xs text-muted-foreground text-center">
                {attemptsRemaining === 1
                  ? '1 verification attempt remaining.'
                  : `${attemptsRemaining} verification attempts remaining.`}
              </p>
            )}

            {locked && (
              <div className="rounded-lg border border-danger/20 bg-danger/10 px-3 py-2 text-sm text-danger text-center">
                Too many failed attempts. Request a new verification code to continue.
              </div>
            )}

            <button
              type="submit"
              disabled={isLoading || locked}
              className="btn-primary w-full justify-center py-2.5 disabled:opacity-50"
            >
              {isLoading ? 'Verifying...' : locked ? 'Verification locked' : 'Verify email'}
            </button>
          </form>

          <button
            type="button"
            onClick={resendCode}
            disabled={isResending}
            className="w-full mt-4 text-sm text-accent font-medium hover:underline disabled:opacity-50"
          >
            {isResending ? 'Sending code...' : 'Resend verification code'}
          </button>

          <p className="mt-5 pt-5 border-t border-border text-center text-sm text-muted-foreground">
            Already verified? <Link href="/login" className="text-accent font-medium hover:underline">Sign in</Link>
          </p>
        </div>
      </section>
    </main>
  );
}

function VerifyEmailFallback() {
  return (
    <main className="min-h-screen bg-secondary flex items-center justify-center px-6 py-12">
      <p className="text-sm text-muted-foreground">Loading verification form...</p>
    </main>
  );
}