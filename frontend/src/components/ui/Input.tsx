import React, { InputHTMLAttributes, forwardRef } from 'react';

interface InputProps extends InputHTMLAttributes<HTMLInputElement> {
  label?: string;
  error?: string;
  leftIcon?: React.ReactNode;
  rightIcon?: React.ReactNode;
}

const Input = forwardRef<HTMLInputElement, InputProps>(({
  label,
  error,
  leftIcon,
  rightIcon,
  className = '',
  ...props
}, ref) => {
  const baseInputClasses = 'w-full px-4 py-2.5 bg-white dark:bg-slate-700 border border-gray-300 dark:border-slate-600 rounded-lg text-gray-900 dark:text-white placeholder-gray-500 dark:placeholder-slate-400 focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 transition-colors';
  const errorClasses = error ? 'border-red-500 focus:ring-red-500 focus:border-red-500' : '';
  
  return (
    <div className="space-y-1">
      {label && (
        <label className="block text-sm font-medium text-gray-700 dark:text-slate-300">
          {label}
        </label>
      )}
      
      <div className="relative">
        {leftIcon && (
          <div className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400">
            {leftIcon}
          </div>
        )}
        
        <input
          ref={ref}
          className={`${baseInputClasses} ${errorClasses} ${leftIcon ? 'pl-10' : ''} ${rightIcon ? 'pr-10' : ''} ${className}`}
          {...props}
        />
        
        {rightIcon && (
          <div className="absolute right-3 top-1/2 transform -translate-y-1/2 text-gray-400">
            {rightIcon}
          </div>
        )}
      </div>
      
      {error && (
        <p className="text-sm text-red-600 dark:text-red-400">{error}</p>
      )}
    </div>
  );
});

Input.displayName = 'Input';

export default Input;