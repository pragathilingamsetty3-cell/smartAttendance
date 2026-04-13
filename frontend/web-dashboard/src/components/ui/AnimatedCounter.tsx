'use client';

import React, { useEffect, useRef } from "react";
import { animate } from "framer-motion";

interface AnimatedCounterProps {
  value: number;
  isPercentage?: boolean;
}

export const AnimatedCounter: React.FC<AnimatedCounterProps> = ({ value, isPercentage = false }) => {
  const nodeRef = useRef<HTMLSpanElement>(null);

  useEffect(() => {
    const node = nodeRef.current;
    if (!node) return;
    
    // Using framer motion's manual animate function for counting up
    const controls = animate(0, value, {
      type: "spring",
      stiffness: 100,
      damping: 30,
      restDelta: 0.001,
      duration: 1.5,
      onUpdate(cur) {
        node.textContent = isPercentage 
          ? cur.toFixed(1) + "%" 
          : Math.floor(cur).toLocaleString();
      }
    });

    return () => controls.stop();
  }, [value, isPercentage]);

  return <span ref={nodeRef}>{isPercentage ? "0.0%" : "0"}</span>;
};
