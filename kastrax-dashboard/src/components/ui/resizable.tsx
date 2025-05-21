import React, { createContext, useContext, useState } from "react";
import * as ResizablePrimitive from "react-resizable-panels";

const ResizableContext = createContext<{ resizing: boolean }>({ resizing: false });

export interface ResizablePanelGroupProps {
  direction: "horizontal" | "vertical";
  className?: string;
  children: React.ReactNode;
  onDragStart?: () => void;
  onDragEnd?: () => void;
  [key: string]: any;
}

export function ResizablePanelGroup({
  className = "",
  children,
  direction,
  ...props
}: ResizablePanelGroupProps) {
  const [resizing, setResizing] = useState(false);

  return (
    <ResizableContext.Provider value={{ resizing }}>
      <ResizablePrimitive.PanelGroup
        className={`${direction === "horizontal" ? "flex flex-row" : "flex flex-col"} ${className}`}
        onDragStart={() => {
          setResizing(true);
          props.onDragStart?.();
        }}
        onDragEnd={() => {
          setResizing(false);
          props.onDragEnd?.();
        }}
        direction={direction}
        {...props}
      >
        {children}
      </ResizablePrimitive.PanelGroup>
    </ResizableContext.Provider>
  );
}

export interface ResizablePanelProps {
  className?: string;
  children: React.ReactNode;
  defaultSize?: number;
  minSize?: number;
  maxSize?: number;
  [key: string]: any;
}

export function ResizablePanel({
  className = "",
  children,
  ...props
}: ResizablePanelProps) {
  return (
    <ResizablePrimitive.Panel
      className={className}
      {...props}
    >
      {children}
    </ResizablePrimitive.Panel>
  );
}

export interface ResizableHandleProps {
  className?: string;
  direction?: "horizontal" | "vertical";
  withHandle?: boolean;
  [key: string]: any;
}

export function ResizableHandle({
  className = "",
  direction = "horizontal",
  withHandle = false,
  ...props
}: ResizableHandleProps) {
  return (
    <ResizablePrimitive.PanelResizeHandle
      className={`${
        direction === "horizontal" 
          ? "border-x border-border w-1.5 mx-0.5 focus:outline-none focus:ring-2 focus:ring-primary"
          : "border-y border-border h-1.5 my-0.5 focus:outline-none focus:ring-2 focus:ring-primary"
      } ${className}`}
      {...props}
    >
      {withHandle && (
        <div
          className={`absolute ${
            direction === "horizontal"
              ? "top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-1 h-6"
              : "top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 h-1 w-6"
          } rounded-full bg-border opacity-0 transition-opacity group-hover:opacity-100`}
        />
      )}
    </ResizablePrimitive.PanelResizeHandle>
  );
} 