import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Mail, Lock, User, Eye, EyeOff, Box } from 'lucide-react';
import { useAuth } from '../../contexts/AuthContext';
import Button from '../../components/ui/Button';
import Input from '../../components/ui/Input';

const RegisterPage: React.FC = () => {
  const { register } = useAuth();
  const navigate = useNavigate();
  const [formData, setFormData] = useState({
    name: '',
    email: '',
    password: '',
    confirmPassword: '',
  });
  const [showPassword, setShowPassword] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError('');

    if (formData.password !== formData.confirmPassword) {
      setError('Passwords do not match');
      setLoading(false);
      return;
    }

    try {
      const result = await register(formData.name, formData.email, formData.password);
      if (result.success) {
        navigate('/verify-otp', { state: { email: formData.email } });
      } else {
        setError(result.message || 'Registration failed');
      }
    } catch (err) {
      setError('An error occurred during registration');
    } finally {
      setLoading(false);
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
          <h2 className="text-3xl font-bold text-gray-900 dark:text-white mb-2">Create account</h2>
          <p className="text-gray-600 dark:text-slate-400">Join the inventory management system</p>
        </div>

        <div className="bg-white dark:bg-slate-800 rounded-2xl shadow-xl p-8">
          {error && (
            <div className="mb-6 p-4 bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 rounded-lg">
              <p className="text-red-700 dark:text-red-400 text-sm">{error}</p>
            </div>
          )}

          <form onSubmit={handleSubmit} className="space-y-6">
            <Input
              type="text"
              label="Full Name"
              placeholder="Enter your full name"
              value={formData.name}
              onChange={(e) => setFormData({ ...formData, name: e.target.value })}
              leftIcon={<User size={18} />}
              required
            />

            <Input
              type="email"
              label="Email"
              placeholder="Enter your email"
              value={formData.email}
              onChange={(e) => setFormData({ ...formData, email: e.target.value })}
              leftIcon={<Mail size={18} />}
              required
            />

            <Input
              type={showPassword ? 'text' : 'password'}
              label="Password"
              placeholder="Enter your password"
              value={formData.password}
              onChange={(e) => setFormData({ ...formData, password: e.target.value })}
              leftIcon={<Lock size={18} />}
              rightIcon={
                <button
                  type="button"
                  onClick={() => setShowPassword(!showPassword)}
                  className="hover:text-gray-600"
                >
                  {showPassword ? <EyeOff size={18} /> : <Eye size={18} />}
                </button>
              }
              required
            />

            <Input
              type="password"
              label="Confirm Password"
              placeholder="Confirm your password"
              value={formData.confirmPassword}
              onChange={(e) => setFormData({ ...formData, confirmPassword: e.target.value })}
              leftIcon={<Lock size={18} />}
              required
            />

            <Button type="submit" loading={loading} className="w-full">
              Create Account
            </Button>
          </form>

          <div className="mt-8 text-center">
            <p className="text-gray-600 dark:text-slate-400">
              Already have an account?{' '}
              <Link 
                to="/login" 
                className="text-indigo-600 dark:text-indigo-400 hover:underline font-medium"
              >
                Sign in
              </Link>
            </p>
          </div>
        </div>
      </div>
    </div>
  );
};

export default RegisterPage;