"use client";

/**
 * Bulk Actions Menu Component
 * Displays bulk action options for selected invoices (placeholders for future implementation)
 */

import { Button } from "@/components/ui/button";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { ChevronDown, Send, Download, Trash2, X } from "lucide-react";
import type { BulkActionType } from "@/lib/api/types";

interface BulkActionsMenuProps {
  selectedCount: number;
  onAction: (action: BulkActionType) => void;
  onClearSelection: () => void;
}

export function BulkActionsMenu({
  selectedCount,
  onAction,
  onClearSelection,
}: BulkActionsMenuProps) {
  if (selectedCount === 0) return null;

  return (
    <div className="flex items-center gap-4 p-4 bg-muted rounded-lg border">
      <div className="flex items-center gap-2">
        <span className="font-semibold">{selectedCount}</span>
        <span className="text-muted-foreground">
          {selectedCount === 1 ? "invoice" : "invoices"} selected
        </span>
      </div>

      <div className="flex items-center gap-2 ml-auto">
        <DropdownMenu>
          <DropdownMenuTrigger asChild>
            <Button variant="outline" size="sm">
              Bulk Actions
              <ChevronDown className="h-4 w-4 ml-2" />
            </Button>
          </DropdownMenuTrigger>
          <DropdownMenuContent align="end">
            <DropdownMenuItem
              disabled
              className="cursor-not-allowed opacity-50"
            >
              <Send className="h-4 w-4 mr-2" />
              Send Selected
              <span className="ml-auto text-xs text-muted-foreground">
                Coming Soon
              </span>
            </DropdownMenuItem>
            <DropdownMenuItem
              disabled
              className="cursor-not-allowed opacity-50"
            >
              <Download className="h-4 w-4 mr-2" />
              Export Selected
              <span className="ml-auto text-xs text-muted-foreground">
                Coming Soon
              </span>
            </DropdownMenuItem>
            <DropdownMenuSeparator />
            <DropdownMenuItem
              disabled
              className="cursor-not-allowed opacity-50"
            >
              <Trash2 className="h-4 w-4 mr-2" />
              Delete Selected
              <span className="ml-auto text-xs text-muted-foreground">
                Coming Soon
              </span>
            </DropdownMenuItem>
          </DropdownMenuContent>
        </DropdownMenu>

        <Button variant="outline" size="sm" onClick={onClearSelection}>
          <X className="h-4 w-4 mr-2" />
          Clear Selection
        </Button>
      </div>
    </div>
  );
}
