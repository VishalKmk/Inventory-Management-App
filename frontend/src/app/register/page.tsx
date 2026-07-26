'use client';

import Link from 'next/link';
import { useState } from 'react';
import { Eye, EyeOff } from 'lucide-react';
import { useForm } from 'react-hook-form';
import { toast } from 'sonner';
import AppLogo from '@/components/ui/AppLogo';
import { apiFetch } from '@/lib/api';

interface RegisterFormValues {
  name: string;
  email: string;
  password: string;
  confirmPassword: string;
}

export default function RegisterPage() {
  const [showPassword, setShowPassword] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const {
    register,
    handleSubmit,
    watch,
    formState: { errors },
  } = useForm<RegisterFormValues>();

  const onSubmit = async ({ name, email, password }: RegisterFormValues) => {
    setIsLoading(true);
    try {
      await apiFetch('/api/auth/register', {
        method: 'POST',
        body: JSON.stringify({ name, email, password }),
      });
      toast.success('Account created. Check your email for the verification code.');
      window.location.assign(`/verify-email?email=${encodeURIComponent(email)}`);
    } catch (error) {
      toast.error(error instanceof Error ? error.message : 'Unable to create account');
    } finally {
      setIsLoading(false);
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
          <p className="text-sm text-muted-foreground mt-1">Create your account</p>
        </div>

        <div className="bg-card rounded-xl border border-border shadow-card p-8">
          <h2 className="text-base font-semibold text-foreground text-center mb-6">Get started</h2>
          <form onSubmit={handleSubmit(onSubmit)} noValidate className="space-y-4">
            <FieldError error={errors.name?.message}>
              <label htmlFor="name" className="block text-sm font-medium text-foreground mb-1.5">Full name</label>
              <input id="name" autoComplete="name" className="input-field" placeholder="Your name" {...register('name', { required: 'Name is required', minLength: { value: 2, message: 'Enter at least 2 characters' } })} />
            </FieldError>
            <FieldError error={errors.email?.message}>
              <label htmlFor="email" className="block text-sm font-medium text-foreground mb-1.5">Work email</label>
              <input id="email" type="email" autoComplete="email" className="input-field" placeholder="name@company.com" {...register('email', { required: 'Work email is required', pattern: { value: /^[^\s@]+@[^\s@]+\.[^\s@]+$/, message: 'Enter a valid email address' } })} />
            </FieldError>
            <FieldError error={errors.password?.message}>
              <label htmlFor="password" className="block text-sm font-medium text-foreground mb-1.5">Password</label>
              <div className="relative">
                <input id="password" type={showPassword ? 'text' : 'password'} autoComplete="new-password" className="input-field pr-10" placeholder="At least 8 characters" {...register('password', { required: 'Password is required', pattern: { value: /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&])[A-Za-z\d@$!%*?&]{8,}$/, message: 'Use 8+ characters with uppercase, lowercase, number, and special character' } })} />
                <button type="button" onClick={() => setShowPassword((value) => !value)} className="absolute right-3 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground" aria-label={showPassword ? 'Hide password' : 'Show password'}>
                  {showPassword ? <EyeOff size={16} /> : <Eye size={16} />}
                </button>
              </div>
            </FieldError>
            <FieldError error={errors.confirmPassword?.message}>
              <label htmlFor="confirmPassword" className="block text-sm font-medium text-foreground mb-1.5">Confirm password</label>
              <input id="confirmPassword" type={showPassword ? 'text' : 'password'} autoComplete="new-password" className="input-field" placeholder="Repeat your password" {...register('confirmPassword', { required: 'Please confirm your password', validate: (value) => value === watch('password') || 'Passwords do not match' })} />
            </FieldError>
            <button type="submit" disabled={isLoading} className="btn-primary w-full justify-center py-2.5">
              {isLoading ? 'Creating account...' : 'Create account'}
            </button>
          </form>
          <p className="mt-5 pt-5 border-t border-border text-center text-sm text-muted-foreground">
            Already have an account? <Link href="/login" className="text-accent font-medium hover:underline">Sign in</Link>
          </p>
        </div>
      </section>
    </main>
  );
}

function FieldError({ children, error }: { children: React.ReactNode; error?: string }) {
  return <div>{children}{error && <p className="text-danger text-xs mt-1">{error}</p>}</div>;
}
