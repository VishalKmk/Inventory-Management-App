'use client';
import React, { useEffect, useState } from 'react';
import Link from 'next/link';
import { useForm } from 'react-hook-form';
import { Eye, EyeOff, Copy, Check, TrendingUp, ShieldCheck, BarChart3 } from 'lucide-react';
import { toast } from 'sonner';
import AppLogo from '@/components/ui/AppLogo';
import AppImage from '@/components/ui/AppImage';
import { apiFetch, API_BASE_URL } from '@/lib/api';

interface LoginResponse {
  token: string;
}

interface LoginFormValues {
  email: string;
  password: string;
  rememberDevice: boolean;
}

const DEMO_CREDENTIALS = {
  email: 'alex.rivera@nexacorp.io',
  password: 'Warehouse@2026',
};

export default function LoginPageContent() {
  const [showPassword, setShowPassword] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const [copiedField, setCopiedField] = useState<'email' | 'password' | null>(null);

  useEffect(() => {
    const params = new URLSearchParams(window.location.hash.slice(1));
    const token = params.get('token');
    const error = new URLSearchParams(window.location.search).get('error');

    if (token) {
      localStorage.setItem('inventory_token', token);
      window.history.replaceState(null, '', window.location.pathname);
      toast.success('Signed in with Google');
      window.location.assign('/spaces');
    } else if (error) {
      toast.error('Google sign-in did not return an email address');
      window.history.replaceState(null, '', window.location.pathname);
    }
  }, []);

  const {
    register,
    handleSubmit,
    setValue,
    formState: { errors },
  } = useForm<LoginFormValues>({
    defaultValues: { email: '', password: '', rememberDevice: false },
  });

  const onSubmit = async (data: LoginFormValues) => {
    setIsLoading(true);
    try {
      const response = await apiFetch<LoginResponse>('/api/auth/login', {
        method: 'POST',
        body: JSON.stringify({ email: data.email, password: data.password }),
      });
      localStorage.setItem('inventory_token', response.token);
      toast.success('Signed in successfully');
      window.location.href = '/spaces';
    } catch (error) {
      toast.error(error instanceof Error ? error.message : 'Unable to sign in');
    } finally {
      setIsLoading(false);
    }
  };

  const handleGoogleSSO = () => {
    window.location.assign(`${API_BASE_URL}/oauth2/authorization/google`);
  };

  const copyToClipboard = async (field: 'email' | 'password') => {
    const value = field === 'email' ? DEMO_CREDENTIALS.email : DEMO_CREDENTIALS.password;
    await navigator.clipboard.writeText(value);
    setCopiedField(field);
    setTimeout(() => setCopiedField(null), 2000);
  };

  const autofillCredentials = () => {
    setValue('email', DEMO_CREDENTIALS.email);
    setValue('password', DEMO_CREDENTIALS.password);
    toast.success('Demo credentials filled');
  };

  return (
    <div className="min-h-screen bg-secondary flex">
      {/* Left — Form Panel */}
      <div className="flex-1 flex flex-col items-center justify-center px-6 py-12 lg:px-12">
        {/* Logo */}
        <div className="flex flex-col items-center mb-8">
          <div className="w-14 h-14 bg-primary rounded-xl flex items-center justify-center mb-4 shadow-elevated">
            <AppLogo size={36} />
          </div>
          <h1 className="text-2xl font-bold text-foreground tracking-tight">StockRoom</h1>
          <p className="text-sm text-muted-foreground mt-1">Smart Inventory Management</p>
        </div>

        {/* Form Card */}
        <div className="w-full max-w-[400px] bg-card rounded-xl border border-border shadow-card p-8">
          <h2 className="text-base font-semibold text-foreground text-center mb-6">
            Sign in to your account
          </h2>

          {/* Google SSO */}
          <button
            type="button"
            onClick={handleGoogleSSO}
            className="w-full flex items-center justify-center gap-3 border border-border rounded-lg py-2.5 text-sm font-medium text-foreground hover:bg-secondary transition-colors mb-4"
          >
            <svg width="18" height="18" viewBox="0 0 18 18" fill="none">
              <path d="M17.64 9.2c0-.637-.057-1.251-.164-1.84H9v3.481h4.844c-.209 1.125-.843 2.078-1.796 2.717v2.258h2.908c1.702-1.567 2.684-3.875 2.684-6.615z" fill="#4285F4"/>
              <path d="M9 18c2.43 0 4.467-.806 5.956-2.18l-2.909-2.259c-.806.54-1.837.86-3.047.86-2.344 0-4.328-1.584-5.036-3.711H.957v2.332A8.997 8.997 0 0 0 9 18z" fill="#34A853"/>
              <path d="M3.964 10.71A5.41 5.41 0 0 1 3.682 9c0-.593.102-1.17.282-1.71V4.958H.957A8.996 8.996 0 0 0 0 9c0 1.452.348 2.827.957 4.042l3.007-2.332z" fill="#FBBC05"/>
              <path d="M9 3.58c1.321 0 2.508.454 3.44 1.345l2.582-2.58C13.463.891 11.426 0 9 0A8.997 8.997 0 0 0 .957 4.958L3.964 6.29C4.672 4.163 6.656 3.58 9 3.58z" fill="#EA4335"/>
            </svg>
            Continue with Google
          </button>

          {/* Divider */}
          <div className="flex items-center gap-3 mb-4">
            <div className="flex-1 h-px bg-border" />
            <span className="text-xs text-muted-foreground font-medium">OR</span>
            <div className="flex-1 h-px bg-border" />
          </div>

          {/* Form */}
          <form onSubmit={handleSubmit(onSubmit)} noValidate className="space-y-4">
            <div>
              <label htmlFor="email" className="block text-sm font-medium text-foreground mb-1.5">
                Work Email
              </label>
              <input
                id="email"
                type="email"
                placeholder="name@company.com"
                className="input-field"
                {...register('email', {
                  required: 'Work email is required',
                  pattern: {
                    value: /^[^\s@]+@[^\s@]+\.[^\s@]+$/,
                    message: 'Enter a valid email address',
                  },
                })}
              />
              {errors.email && (
                <p className="text-danger text-xs mt-1">{errors.email.message}</p>
              )}
            </div>

            <div>
              <div className="flex items-center justify-between mb-1.5">
                <label htmlFor="password" className="block text-sm font-medium text-foreground">
                  Password
                </label>
                <a href="#" className="text-xs text-accent font-medium hover:underline">
                  Forgot password?
                </a>
              </div>
              <div className="relative">
                <input
                  id="password"
                  type={showPassword ? 'text' : 'password'}
                  placeholder="••••••••"
                  className="input-field pr-10"
                  {...register('password', {
                    required: 'Password is required',
                    minLength: { value: 6, message: 'Minimum 6 characters' },
                  })}
                />
                <button
                  type="button"
                  onClick={() => setShowPassword((p) => !p)}
                  className="absolute right-3 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground transition-colors"
                  aria-label={showPassword ? 'Hide password' : 'Show password'}
                >
                  {showPassword ? <EyeOff size={16} /> : <Eye size={16} />}
                </button>
              </div>
              {errors.password && (
                <p className="text-danger text-xs mt-1">{errors.password.message}</p>
              )}
            </div>

            <div className="flex items-center gap-2">
              <input
                id="rememberDevice"
                type="checkbox"
                className="w-4 h-4 rounded border-border accent-primary cursor-pointer"
                {...register('rememberDevice')}
              />
              <label htmlFor="rememberDevice" className="text-sm text-foreground cursor-pointer">
                Remember this device for 30 days
              </label>
            </div>

            <button
              type="submit"
              disabled={isLoading}
              className="btn-primary w-full justify-center py-2.5 mt-1"
            >
              {isLoading ? (
                <>
                  <svg className="animate-spin w-4 h-4" viewBox="0 0 24 24" fill="none">
                    <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"/>
                    <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"/>
                  </svg>
                  Signing in...
                </>
              ) : (
                'Sign In'
              )}
            </button>
          </form>

          <div className="mt-5 pt-5 border-t border-border text-center">
            <p className="text-sm text-muted-foreground">
              New to StockRoom?{' '}
              <Link href="/register" className="text-accent font-medium hover:underline">
                Create an account
              </Link>
            </p>
          </div>
        </div>

        {/* Demo Credentials Box */}
        <div className="w-full max-w-[400px] mt-4 bg-card border border-border rounded-xl p-4 shadow-card">
          <div className="flex items-center justify-between mb-3">
            <p className="text-xs font-semibold text-muted-foreground uppercase tracking-wider">
              Demo Account
            </p>
            <button
              onClick={autofillCredentials}
              className="text-xs text-accent font-semibold hover:underline"
            >
              Autofill
            </button>
          </div>
          <div className="space-y-2">
            <div className="flex items-center justify-between bg-secondary rounded-lg px-3 py-2">
              <div>
                <p className="text-xs text-muted-foreground">Email</p>
                <p className="text-xs font-medium text-foreground font-mono">
                  {DEMO_CREDENTIALS.email}
                </p>
              </div>
              <button
                onClick={() => copyToClipboard('email')}
                className="text-muted-foreground hover:text-foreground transition-colors ml-2"
                aria-label="Copy email"
              >
                {copiedField === 'email' ? (
                  <Check size={14} className="text-success" />
                ) : (
                  <Copy size={14} />
                )}
              </button>
            </div>
            <div className="flex items-center justify-between bg-secondary rounded-lg px-3 py-2">
              <div>
                <p className="text-xs text-muted-foreground">Password</p>
                <p className="text-xs font-medium text-foreground font-mono">
                  {DEMO_CREDENTIALS.password}
                </p>
              </div>
              <button
                onClick={() => copyToClipboard('password')}
                className="text-muted-foreground hover:text-foreground transition-colors ml-2"
                aria-label="Copy password"
              >
                {copiedField === 'password' ? (
                  <Check size={14} className="text-success" />
                ) : (
                  <Copy size={14} />
                )}
              </button>
            </div>
          </div>
        </div>

        {/* Footer links */}
        <div className="flex items-center gap-4 mt-6 text-xs text-muted-foreground">
          <a href="#" className="hover:text-foreground transition-colors">Privacy Policy</a>
          <span>•</span>
          <a href="#" className="hover:text-foreground transition-colors">Terms of Service</a>
          <span>•</span>
          <span className="flex items-center gap-1.5">
            <span className="w-1.5 h-1.5 rounded-full bg-success inline-block" />
            System Operational
          </span>
        </div>
      </div>

      {/* Right — Feature Panel */}
      <div className="hidden lg:flex flex-1 bg-secondary items-center justify-center p-12">
        <div className="w-full max-w-sm">
          {/* Mock UI card */}
          <div className="bg-card rounded-2xl shadow-modal p-6 mb-6 border border-border">
            <div className="flex items-center gap-1.5 mb-5">
              <div className="w-2.5 h-2.5 rounded-full bg-muted" />
              <div className="w-2.5 h-2.5 rounded-full bg-muted" />
              <div className="w-2.5 h-2.5 rounded-full bg-muted" />
            </div>
            {/* Mock KPI row */}
            <div className="grid grid-cols-2 gap-3 mb-4">
              <div className="bg-secondary rounded-lg p-3">
                <p className="text-xs text-muted-foreground mb-1">Total Products</p>
                <p className="text-xl font-bold text-foreground tabular-nums">12,842</p>
              </div>
              <div className="bg-danger/10 rounded-lg p-3">
                <p className="text-xs text-danger mb-1">Low Stock Alerts</p>
                <p className="text-xl font-bold text-danger tabular-nums">24</p>
              </div>
            </div>
            {/* Mock bars */}
            <div className="space-y-2.5">
              {[
                { label: 'Steel Bolts TL-B-990', pct: 73, variant: 'healthy' },
                { label: 'Servo Motor MOT-22', pct: 8, variant: 'critical' },
                { label: 'Cargo Pallets LOG-X5', pct: 42, variant: 'low' },
              ].map((item) => (
                <div key={`mock-bar-${item.label}`}>
                  <div className="flex justify-between text-xs mb-1">
                    <span className="text-muted-foreground truncate">{item.label}</span>
                    <span className="font-semibold text-foreground ml-2">{item.pct}%</span>
                  </div>
                  <div className="stock-bar-track">
                    <div
                      className={`stock-bar-fill-${item.variant}`}
                      style={{ width: `${item.pct}%` }}
                    />
                  </div>
                </div>
              ))}
            </div>
            {/* Build badge */}
            <div className="mt-5 inline-block bg-primary text-primary-foreground text-xs font-bold px-3 py-1 rounded-md tracking-widest uppercase">
              BUILD v2.4.1
            </div>
          </div>

          {/* Feature headline */}
          <h3 className="text-2xl font-bold text-foreground leading-snug mb-3">
            Advanced stock monitoring at scale.
          </h3>

          {/* Testimonial */}
          <blockquote className="text-sm text-muted-foreground mb-4 leading-relaxed">
            "The transition to Precision Logic reduced our audit discrepancies by 84% in the first quarter."
          </blockquote>
          <div className="flex items-center gap-3">
            <div className="w-9 h-9 rounded-full overflow-hidden border-2 border-border">
              <AppImage
                src="https://i.pravatar.cc/40?img=47"
                alt="Sarah Jenkins, Director of Logistics at NexaCorp"
                width={36}
                height={36}
                className="w-full h-full object-cover"
              />
            </div>
            <div>
              <p className="text-sm font-semibold text-foreground">Sarah Jenkins</p>
              <p className="text-xs text-muted-foreground">Director of Logistics, NexaCorp</p>
            </div>
          </div>

          {/* Feature bullets */}
          <div className="mt-6 space-y-2">
            {[
              { icon: <ShieldCheck size={14} />, text: 'Role-based access control (OWNER / ADMIN / MEMBER / VIEWER)' },
              { icon: <TrendingUp size={14} />, text: 'Real-time low-stock alerts with severity tiers' },
              { icon: <BarChart3 size={14} />, text: 'Full audit trail for every stock movement' },
            ].map((item) => (
              <div key={`feat-${item.text.slice(0, 20)}`} className="flex items-center gap-2 text-xs text-muted-foreground">
                <span className="text-accent">{item.icon}</span>
                {item.text}
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
}
