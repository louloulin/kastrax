/**
 * DataFlare UI API v2
 * 
 * This module exports all the v2 API modules
 * Version 2 includes improved type safety, standard request functions,
 * and more consistent API patterns
 */

// Export v2 modules
export * from './auth';
export * from './rules';
export * from './resources';
export * from './scripts';
export * from './variables';
export * from './backups';
export * from './database';

// For modules not yet migrated to v2, use v1 versions
// When a v2 module is created, remove its import from v1
import {
  // Other APIs...
} from '../v1';

// Re-export v1 APIs
export {
  // Other APIs...
}; 