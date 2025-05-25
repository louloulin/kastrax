/**
 * DataFlare UI API
 * 
 * This is the main entry point for the API modules.
 * It exports APIs as namespaces (v1 and v2) to avoid conflicts.
 */

// API Client and Utilities
export { default as client } from './client';
export { getRequest, postRequest, putRequest, deleteRequest } from './utils';

// Export API versions as namespaces
import * as v1API from './v1';
import * as v2API from './v2';

// Export namespaced versions
export const v1 = v1API;
export const v2 = v2API;

// Export the default version (v2) as 'api'
export const api = v2API;

// For backwards compatibility, re-export v1 modules
// These will be deprecated in the future
// Only keep what's necessary for backward compatibility
export * from './v1/auth';
export * from './v1/rules';
export * from './v1/resources';
export * from './v1/scripts';
export * from './variables';
export * from './backups';
export * from './database';
export * from './settings';
export * from './workflows'; 