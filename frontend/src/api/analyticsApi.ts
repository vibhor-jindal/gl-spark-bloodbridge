import client from "./client";
import { DashboardResponse } from "../types";

export const analyticsApi = {
  dashboard: (city?: string) =>
    client.get<DashboardResponse>("/api/analytics/dashboard", { params: city ? { city } : {} }).then((r) => r.data),
  exportUrl: (city?: string) => {
    const base = (import.meta.env.VITE_API_BASE_URL || "http://localhost:8080") + "/api/analytics/export";
    return city ? `${base}?city=${encodeURIComponent(city)}` : base;
  }
};
