/**
 * Dashboard API Client
 * Handles dashboard statistics queries
 */

import { apiClient } from './client';
import type { DashboardStatsDTO } from './types';

/**
 * Get dashboard statistics
 */
export const getDashboardStats = async (): Promise<DashboardStatsDTO> => {
  const response = await apiClient.get<DashboardStatsDTO>('/dashboard/stats');
  return response.data;
};

/**
 * Export dashboard API
 */
export const dashboardApi = {
  getDashboardStats,
};
