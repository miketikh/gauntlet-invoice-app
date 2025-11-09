/**
 * Auto-Save Hook
 * Automatically saves form data to localStorage with debouncing
 */

import { useEffect, useRef, useState } from 'react';

export interface AutoSaveMetadata {
  lastSaved: number; // timestamp
  version: number;
}

export interface SavedDraft<T> {
  data: T;
  metadata: AutoSaveMetadata;
}

export type SaveStatus = 'idle' | 'saving' | 'saved' | 'error';

export interface UseAutoSaveOptions<T> {
  storageKey: string;
  data: T;
  onSave?: (data: T) => void | Promise<void>;
  delay?: number; // milliseconds, default 30000 (30 seconds)
  enabled?: boolean;
}

export interface UseAutoSaveReturn {
  saveStatus: SaveStatus;
  lastSaved: Date | null;
  manualSave: () => Promise<void>;
  clearDraft: () => void;
  hasDraft: boolean;
}

/**
 * Auto-save hook with debouncing and localStorage persistence
 */
export function useAutoSave<T>({
  storageKey,
  data,
  onSave,
  delay = 30000,
  enabled = true,
}: UseAutoSaveOptions<T>): UseAutoSaveReturn {
  const [saveStatus, setSaveStatus] = useState<SaveStatus>('idle');
  const [lastSaved, setLastSaved] = useState<Date | null>(null);
  const [hasDraft, setHasDraft] = useState(false);
  const timeoutRef = useRef<NodeJS.Timeout>();
  const dataRef = useRef<T>(data);

  // Update ref when data changes
  useEffect(() => {
    dataRef.current = data;
  }, [data]);

  // Check for existing draft on mount
  useEffect(() => {
    const checkDraft = () => {
      try {
        const stored = localStorage.getItem(storageKey);
        setHasDraft(!!stored);
      } catch (error) {
        console.error('Failed to check for draft:', error);
      }
    };
    checkDraft();
  }, [storageKey]);

  // Save function
  const save = async () => {
    if (!enabled) return;

    setSaveStatus('saving');
    try {
      // Save to localStorage
      const draft: SavedDraft<T> = {
        data: dataRef.current,
        metadata: {
          lastSaved: Date.now(),
          version: 1,
        },
      };
      localStorage.setItem(storageKey, JSON.stringify(draft));
      setHasDraft(true);

      // Call optional onSave callback
      if (onSave) {
        await onSave(dataRef.current);
      }

      const savedTime = new Date();
      setLastSaved(savedTime);
      setSaveStatus('saved');
    } catch (error) {
      console.error('Auto-save error:', error);
      setSaveStatus('error');
    }
  };

  // Manual save function
  const manualSave = async () => {
    // Cancel pending auto-save
    if (timeoutRef.current) {
      clearTimeout(timeoutRef.current);
    }
    await save();
  };

  // Clear draft from storage
  const clearDraft = () => {
    try {
      localStorage.removeItem(storageKey);
      setHasDraft(false);
      setSaveStatus('idle');
      setLastSaved(null);
    } catch (error) {
      console.error('Failed to clear draft:', error);
    }
  };

  // Debounced auto-save effect
  useEffect(() => {
    if (!enabled) return;

    // Clear existing timeout
    if (timeoutRef.current) {
      clearTimeout(timeoutRef.current);
    }

    // Set new timeout for auto-save
    timeoutRef.current = setTimeout(() => {
      setSaveStatus('saving');
      save();
    }, delay);

    // Cleanup
    return () => {
      if (timeoutRef.current) {
        clearTimeout(timeoutRef.current);
      }
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [data, enabled, delay]);

  return {
    saveStatus,
    lastSaved,
    manualSave,
    clearDraft,
    hasDraft,
  };
}

/**
 * Load saved draft from localStorage
 */
export function loadDraft<T>(storageKey: string): SavedDraft<T> | null {
  try {
    const stored = localStorage.getItem(storageKey);
    if (!stored) return null;

    const draft = JSON.parse(stored) as SavedDraft<T>;
    return draft;
  } catch (error) {
    console.error('Failed to load draft:', error);
    return null;
  }
}

/**
 * Check if a draft exists
 */
export function hasDraft(storageKey: string): boolean {
  try {
    const stored = localStorage.getItem(storageKey);
    return !!stored;
  } catch (error) {
    console.error('Failed to check for draft:', error);
    return false;
  }
}
