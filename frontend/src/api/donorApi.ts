import client from "./client";
import { DonorRequest, DonorResponse } from "../types";

export const donorApi = {
  register: (data: DonorRequest) => client.post<DonorResponse>("/api/donors", data).then((r) => r.data),
  me: () => client.get<DonorResponse>("/api/donors/me").then((r) => r.data),
  updateAvailability: (donorId: number, isAvailable: boolean) =>
    client.patch<DonorResponse>(`/api/donors/${donorId}/availability`, { isAvailable }).then((r) => r.data)
};
