import React, { forwardRef } from "react";
import { Moon, Sun, Laptop } from "lucide-react";
import { useTheme } from "./theme-provider";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "./ui/dropdown-menu";
import { Button } from "./ui/button";
import { Tooltip, TooltipContent, TooltipProvider, TooltipTrigger } from "./ui/tooltip";

// Create a forwardRef wrapper for the ThemeSwitcher component
const ThemeSwitcherContent = forwardRef<HTMLDivElement, React.HTMLAttributes<HTMLDivElement>>((props, ref) => {
  const { theme, setTheme } = useTheme();

  return (
    <div ref={ref} {...props}>
      <DropdownMenu>
        <DropdownMenuTrigger asChild>
          <Button variant="ghost" size="sm" className="h-8 w-8 p-0">
            <Sun className="h-4 w-4 rotate-0 scale-100 transition-all dark:-rotate-90 dark:scale-0" />
            <Moon className="absolute h-4 w-4 rotate-90 scale-0 transition-all dark:rotate-0 dark:scale-100" />
            <span className="sr-only">切换主题</span>
          </Button>
        </DropdownMenuTrigger>
        <DropdownMenuContent align="end">
          <DropdownMenuItem onClick={() => setTheme("light")}>
            <Sun className="mr-2 h-4 w-4" />
            <span>浅色</span>
          </DropdownMenuItem>
          <DropdownMenuItem onClick={() => setTheme("dark")}>
            <Moon className="mr-2 h-4 w-4" />
            <span>深色</span>
          </DropdownMenuItem>
          <DropdownMenuItem onClick={() => setTheme("system")}>
            <Laptop className="mr-2 h-4 w-4" />
            <span>系统</span>
          </DropdownMenuItem>
        </DropdownMenuContent>
      </DropdownMenu>
    </div>
  );
});

ThemeSwitcherContent.displayName = "ThemeSwitcherContent";

// Export the main component that uses the forwardRef wrapper
export function ThemeSwitcher() {
  return (
    <TooltipProvider>
      <Tooltip>
        <TooltipTrigger asChild>
          <ThemeSwitcherContent />
        </TooltipTrigger>
        <TooltipContent>
          <p>切换主题</p>
        </TooltipContent>
      </Tooltip>
    </TooltipProvider>
  );
}