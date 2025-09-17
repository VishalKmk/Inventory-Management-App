import React, { useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { Mail, Box } from 'lucide-react';
import { useAuth } from '../../contexts/AuthContext';
import Button from '../../components/ui/Button';
import Input from '../../components/ui/Input';

const VerifyOTPPage: React.FC = () => {
  const { verifyOTP, resendOTP } = useAuth();
  const location = useLocation();
  const navigate = useNavigate();
  const email = location.state?.email || '';

  const [otp, setOtp] = useState('');
  const [loading, setLoading] = useState(false);
  const [resending, setResending] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError('');

    try {
      const result = await verifyOTP(email, otp);
      if (result.success) {
        setSuccess('Email verified successfully! You can now sign in.');
        setTimeout(() => navigate('/login'), 2000);
      } else {
        setError(result.message || 'OTP verification failed');
      }
    } catch (err) {
      setError('An error occurred during verification');
    } finally {
      setLoading(false);
    }
  };

  const handleResendOTP = async () => {
    setResending(true);
    setError('');
    setSuccess('');

    try {
      const result = await resendOTP(email);
      if (result.success) {
        setSuccess('OTP resent successfully!');
      } else {
        setError(result.message || 'Failed to resend OTP');
      }
    } catch (err) {
      setError('An error occurred while resending OTP');
    } finally {
      setResending(false);
    }
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-indigo-50 to-purple-50 dark:from-slate-900 dark:to-slate-800 flex items-center justify-center p-4">
      <div className="w-full max-w-md">
        <div className="text-center mb-8">
          <div className="inline-flex items-center gap-3 mb-4">
            <div className="p-3 bg-indigo-600 rounded-xl">
              <Box size={32} className="text-white" />
            </div>
            <h1 className="text-2xl font-bold text-gray-900 dark:text-white">IMS</h1>
          </div>
          <h2 className="text-3xl font-bold text-gray-900 dark:text-white mb-2">Verify Email</h2>
          <p className="text-gray-600 dark:text-slate-400">
            We sent a verification code to {email}
          </p>
        </div>

        <div className="bg-white dark:bg-slate-800 rounded-2xl shadow-xl p-8">
          {error && (
            <div className="mb-6 p-4 bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 rounded-lg">
              <p className="text-red-700 dark:text-red-400 text-sm">{error}</p>
            </div>
          )}

          {success && (
            <div className="mb-6 p-4 bg-green-50 dark:bg-green-900/20 border border-green-200 dark:border-green-800 rounded-lg">
              <p className="text-green-700 dark:text-green-400 text-sm">{success}</p>
            </div>
          )}

          <form onSubmit={handleSubmit} className="space-y-6">
            <Input
              type="text"
              label="Verification Code"
              placeholder="Enter 6-digit code"
              value={otp}
              onChange={(e) => setOtp(e.target.value)}
              leftIcon={<Mail size={18} />}
              maxLength={6}
              required
            />

            <Button type="submit" loading={loading} className="w-full">
              Verify Email
            </Button>
          </form>

          <div className="mt-6 text-center">
            <p className="text-gray-600 dark:text-slate-400 mb-2">
              Didn't receive the code?
            </p>
            <Button 
              variant="ghost" 
              onClick={handleResendOTP} 
              loading={resending}
              disabled={resending}
            >
              Resend OTP
            </Button>
          </div>
        </div>
      </div>
    </div>
  );
};

export default VerifyOTPPage;