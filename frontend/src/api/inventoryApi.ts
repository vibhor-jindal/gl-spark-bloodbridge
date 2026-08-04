import client from "./client";
import { InventoryRequest, InventoryResponse, LowStockAlert, ReserveResponse } from "../types";

export const inventoryApi = {
  add: (data: InventoryRequest) => client.post<InventoryResponse>("/api/inventory", data).then((r) => r.data),
  mine: () => client.get<InventoryResponse[]>("/api/inventory/mine").then((r) => r.data),
  all: () => client.get<InventoryResponse[]>("/api/inventory").then((r) => r.data),
  search: (bloodGroup: string, city: string) =>
    client.get<InventoryResponse[]>("/api/inventory/search", { params: { bloodGroup, city } }).then((r) => r.data),
  update: (batchId: number, unitsAvailable: number) =>
    client.patch<InventoryResponse>(`/api/inventory/${batchId}`, { unitsAvailable }).then((r) => r.data),
  remove: (batchId: number) => client.delete(`/api/inventory/${batchId}`),
  reserve: (bloodGroup: string, city: string, unitsNeeded: number, batchId?: number) =>
    client
      .post<ReserveResponse>("/api/inventory/reserve", { bloodGroup, city, unitsNeeded, batchId })
      .then((r) => r.data),
  alerts: (mineOnly = false) =>
    client.get<LowStockAlert[]>("/api/inventory/alerts", { params: { mineOnly } }).then((r) => r.data)
};
