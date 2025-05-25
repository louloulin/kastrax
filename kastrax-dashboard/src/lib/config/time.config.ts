/**
 * 时间单位映射表
 * 用于在展示时间值时添加对应的单位
 */
export type TimeUnit = 'MILLISECONDS' | 'SECONDS' | 'MINUTES' | 'HOURS' | 'DAYS';

export const timeUnitMap: Record<TimeUnit, string> = {
  'MILLISECONDS': 'ms',
  'SECONDS': 's',
  'MINUTES': 'm',
  'HOURS': 'h',
  'DAYS': 'd'
} 