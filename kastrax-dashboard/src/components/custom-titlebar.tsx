import { useCallback, useEffect, ReactNode } from "react";

interface WindowTitlebarProps {
  children: ReactNode;
}

export function WindowTitlebar({ children }: WindowTitlebarProps) {
  const setupWindowControls = useCallback(async () => {
    try {
      // For Tauri v2 alpha, we need to use the plugin differently
      const windowPlugin = await import("@tauri-apps/plugin-window");
      
      // With the updated API, we can listen to events if needed
      // Documentation for Tauri v2 alpha is still evolving
    } catch (error) {
      console.error("Failed to initialize window controls:", error);
    }
  }, []);

  useEffect(() => {
    setupWindowControls();
  }, [setupWindowControls]);

  return (
    <div className="window-titlebar">
      {children}
    </div>
  );
} 