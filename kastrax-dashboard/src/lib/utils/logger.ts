/**
 * Logger utility for consistent logging across the application
 */

// Log levels
export enum LogLevel {
  DEBUG = 'debug',
  INFO = 'info',
  WARN = 'warn',
  ERROR = 'error'
}

// Log categories
export enum LogCategory {
  API = 'API',
  AUTH = 'AUTH',
  UI = 'UI',
  STORE = 'STORE',
  SYSTEM = 'SYSTEM'
}

// Log entry interface
export interface LogEntry {
  timestamp: string;
  level: LogLevel;
  category: LogCategory;
  message: string;
  data?: any;
}

// Global log history (limited to last 100 entries)
const MAX_LOG_HISTORY = 100;
const logHistory: LogEntry[] = [];

/**
 * Add entry to log history
 */
function addToHistory(entry: LogEntry) {
  logHistory.push(entry);
  if (logHistory.length > MAX_LOG_HISTORY) {
    logHistory.shift();
  }
}

/**
 * Format log entry for console output
 */
function formatLogEntry(entry: LogEntry): string {
  return `[${entry.timestamp}] [${entry.category}] [${entry.level.toUpperCase()}] ${entry.message}`;
}

/**
 * Log a message with the specified level and category
 */
export function log(level: LogLevel, category: LogCategory, message: string, data?: any) {
  const entry: LogEntry = {
    timestamp: new Date().toISOString(),
    level,
    category,
    message,
    data
  };

  addToHistory(entry);
  
  const formattedMessage = formatLogEntry(entry);
  
  switch (level) {
    case LogLevel.DEBUG:
      console.debug(formattedMessage, data !== undefined ? data : '');
      break;
    case LogLevel.INFO:
      console.info(formattedMessage, data !== undefined ? data : '');
      break;
    case LogLevel.WARN:
      console.warn(formattedMessage, data !== undefined ? data : '');
      break;
    case LogLevel.ERROR:
      console.error(formattedMessage, data !== undefined ? data : '');
      break;
  }
}

/**
 * Log a debug message
 */
export function debug(category: LogCategory, message: string, data?: any) {
  log(LogLevel.DEBUG, category, message, data);
}

/**
 * Log an info message
 */
export function info(category: LogCategory, message: string, data?: any) {
  log(LogLevel.INFO, category, message, data);
}

/**
 * Log a warning message
 */
export function warn(category: LogCategory, message: string, data?: any) {
  log(LogLevel.WARN, category, message, data);
}

/**
 * Log an error message
 */
export function error(category: LogCategory, message: string, data?: any) {
  log(LogLevel.ERROR, category, message, data);
}

/**
 * Get the log history
 */
export function getLogHistory(): LogEntry[] {
  return [...logHistory];
}

/**
 * Clear the log history
 */
export function clearLogHistory() {
  logHistory.length = 0;
}

/**
 * Create a group in the console log
 */
export function group(title: string) {
  console.group(title);
}

/**
 * End a group in the console log
 */
export function groupEnd() {
  console.groupEnd();
}

// Default export
const logger = {
  debug,
  info,
  warn,
  error,
  group,
  groupEnd,
  getLogHistory,
  clearLogHistory
};

export default logger;
