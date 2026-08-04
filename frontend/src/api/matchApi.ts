import client from "./client";
import { MatchResponse } from "../types";

export const matchApi = {
  triggerMatch: (requestId: number) =>
    client.post<MatchResponse>(`/api/matches/requests/${requestId}`).then((r) => r.data),
  respond: (requestId: number, donorId: number, accepted: boolean) =>
    client
      .post<MatchResponse>(`/api/matches/requests/${requestId}/responses`, { donorId, accepted })
      .then((r) => r.data),
  getMatches: (requestId: number) =>
    client.get<MatchResponse[]>(`/api/matches/requests/${requestId}`).then((r) => r.data),
  mine: (pendingOnly = false) =>
    client
      .get<MatchResponse[]>("/api/matches/mine", { params: { pendingOnly } })
      .then((r) => r.data)
};
