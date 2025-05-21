"use client"

import * as React from "react"
import { ThemeProvider as NextThemesProvider, useTheme as useNextTheme } from "next-themes"
import { type ThemeProviderProps } from "next-themes/dist/types"

// Customize options for the theme provider
export interface CustomThemeProviderProps extends ThemeProviderProps {
  /** Forces a particular theme mode (light, dark, or system) */
  forcedTheme?: string;
  /** Whether to disable client-side cookies to detect theme preference */
  disableTransitionOnChange?: boolean;
  /** Enables or disables the color scheme change */
  enableColorScheme?: boolean;
  /** Enables or disables the persistence of theme selection */
  enableSystem?: boolean;
  /** Suffix of the local storage key */
  storageKey?: string;
}

export function ThemeProvider({ 
  children, 
  forcedTheme,
  disableTransitionOnChange = false,
  enableColorScheme = true,
  enableSystem = true,
  storageKey = "dataflare-ui-theme",
  ...props 
}: CustomThemeProviderProps) {
  return (
    <NextThemesProvider 
      forcedTheme={forcedTheme}
      disableTransitionOnChange={disableTransitionOnChange}
      enableColorScheme={enableColorScheme}
      enableSystem={enableSystem}
      storageKey={storageKey}
      {...props}
    >
      {children}
    </NextThemesProvider>
  )
}

// Reexport the useTheme hook with proper typing
export interface UseThemeReturn {
  /** Current theme name */
  theme: string | undefined;
  /** All available theme names */
  themes: string[];
  /** Change theme name */
  setTheme: (theme: string) => void;
  /** If the theme is currently resolving. This can be used to suppress the initial client render */
  resolvedTheme: string | undefined;
  /** If `enableSystem` is true and the active theme is "system", returns the system theme resolved with `getSystemTheme`, otherwise returns the active theme. */
  systemTheme?: string;
}

export const useTheme = useNextTheme as () => UseThemeReturn;
