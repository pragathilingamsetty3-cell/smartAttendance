'use client';

import React from 'react';
import { cn } from '@/utils/cn';

interface CardProps extends React.HTMLAttributes<HTMLDivElement> {
  glass?: boolean;
  children: React.ReactNode;
}

export const Card: React.FC<CardProps> = ({
  glass = false,
  className,
  children,
  ...props
}) => {
  const baseClasses = 'rounded-2xl transition-all duration-300';
  const variantClasses = 'bg-[#0F0F16] border border-[#ffffff10] shadow-2xl';
  
  const classes = cn(
    baseClasses,
    variantClasses,
    className
  );

  return (
    <div className={classes} {...props}>
      {children}
    </div>
  );
};

export const CardHeader: React.FC<React.HTMLAttributes<HTMLDivElement>> = ({
  className,
  children,
  ...props
}) => {
  const classes = cn('px-6 py-4 border-b border-gray-800/50', className);
  
  return (
    <div className={classes} {...props}>
      {children}
    </div>
  );
};

export const CardContent: React.FC<React.HTMLAttributes<HTMLDivElement>> = ({
  className,
  children,
  ...props
}) => {
  const classes = cn('px-6 py-4', className);
  
  return (
    <div className={classes} {...props}>
      {children}
    </div>
  );
};

export const CardTitle: React.FC<React.HTMLAttributes<HTMLHeadingElement>> = ({
  className,
  children,
  ...props
}) => {
  const classes = cn('text-xl font-bold text-white tracking-tight', className);
  
  return (
    <h3 className={classes} {...props}>
      {children}
    </h3>
  );
};

export const CardDescription: React.FC<React.HTMLAttributes<HTMLParagraphElement>> = ({
  className,
  children,
  ...props
}) => {
  const classes = cn('text-sm text-slate-400', className);
  
  return (
    <p className={classes} {...props}>
      {children}
    </p>
  );
};

export const CardFooter: React.FC<React.HTMLAttributes<HTMLDivElement>> = ({
  className,
  children,
  ...props
}) => {
  const classes = cn('px-6 py-4 border-t border-gray-800/50', className);
  
  return (
    <div className={classes} {...props}>
      {children}
    </div>
  );
};

export default Card;
