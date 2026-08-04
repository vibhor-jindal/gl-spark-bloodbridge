import client from "./client";
import { RequestCreateRequest, RequestResponse } from "../types";

export type AdminRequestUpdate = {
  patientName?: string;
  bloodGroup?: string;
  unitsNeeded?: number;
  hospitalName?: string;
  city?: string;
  urgency?: string;
  status?: string;
  confirmedDonorId?: number;
};

export type BankReservePayload = {
  bloodBankUserId: number;
  batchId?: number;
  bloodBankName: string;
};

export type OtpConfirmPayload = { otp: string };

export const requestApi = {
  create: (data: RequestCreateRequest) =>
    client.post<RequestResponse>("/api/requests", data).then((r) => r.data),
  get: (requestId: number) =>
    client.get<RequestResponse>(`/api/requests/${requestId}`).then((r) => r.data),
  mine: () => client.get<RequestResponse[]>("/api/requests").then((r) => r.data),
  open: (city?: string) =>
    client.get<RequestResponse[]>("/api/requests/open", { params: city ? { city } : {} }).then((r) => r.data),
  /** Requests reserved by the logged-in blood bank (BANK_RESERVED / OUT_FOR_DELIVERY / FULFILLED). */
  bankMine: () => client.get<RequestResponse[]>("/api/requests/bank").then((r) => r.data),
  all: () => client.get<RequestResponse[]>("/api/requests/all").then((r) => r.data),
  cancel: (requestId: number) =>
    client.patch<RequestResponse>(`/api/requests/${requestId}/cancel`).then((r) => r.data),
  reserveBank: (requestId: number, data: BankReservePayload) =>
    client.post<RequestResponse>(`/api/requests/${requestId}/reserve-bank`, data).then((r) => r.data),
  startDelivery: (requestId: number) =>
    client.post<RequestResponse>(`/api/requests/${requestId}/start-delivery`).then((r) => r.data),
  /** Re-issue OTP after expiry while still OUT_FOR_DELIVERY. */
  restartDelivery: (requestId: number) =>
    client.post<RequestResponse>(`/api/requests/${requestId}/restart-delivery`).then((r) => r.data),
  confirmOtp: (requestId: number, data: OtpConfirmPayload) =>
    client.post<RequestResponse>(`/api/requests/${requestId}/confirm-otp`, data).then((r) => r.data),
  markFulfilled: (requestId: number) =>
    client.patch<RequestResponse>(`/api/requests/${requestId}/fulfill`).then((r) => r.data),
  adminUpdate: (requestId: number, data: AdminRequestUpdate) =>
    client.put<RequestResponse>(`/api/requests/${requestId}`, data).then((r) => r.data),
  adminDelete: (requestId: number) => client.delete(`/api/requests/${requestId}`)
};
