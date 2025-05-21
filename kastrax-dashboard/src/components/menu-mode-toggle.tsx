"use client"

import * as React from "react"
import { Moon, Sun } from "lucide-react"
import { useTheme } from "./theme-provider"

import { Button } from "@/components/ui/button"
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu"
import {
  MenubarContent,
  MenubarMenu,
  MenubarRadioGroup,
  MenubarRadioItem,
  MenubarTrigger,
} from "@/components/ui/menubar"
import { Icons } from "@/components/icons"

export function MenuModeToggle() {
  const { theme, setTheme } = useTheme()

  const toggleTheme = () => {
    setTheme(theme === "dark" ? "light" : "dark")
  }

  return (
    <button
      onClick={toggleTheme}
      className="p-2 text-muted-foreground hover:text-foreground"
    >
      {theme === "dark" ? (
        <Moon size={18} />
      ) : (
        <Sun size={18} />
      )}
    </button>
  )
}
