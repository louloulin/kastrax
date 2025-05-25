/**
 * Process Polyfill
 * 
 * This module provides a polyfill for the Node.js process object in browser environments.
 */

// Check if process is already defined (e.g., in Node.js environment)
if (typeof window !== 'undefined' && typeof (window as any).process === 'undefined') {
  // Create a minimal process object with the properties needed by jsonwebtoken
  (window as any).process = {
    env: {
      NODE_ENV: 'production'
    },
    nextTick: (fn: Function, ...args: any[]) => {
      setTimeout(() => fn(...args), 0);
    },
    browser: true,
    version: '',
    versions: {
      node: '0.0.0'
    }
  };
}

export default {};
