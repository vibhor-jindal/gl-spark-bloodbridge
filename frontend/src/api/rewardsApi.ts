import client from "./client";
import { LeaderboardEntry, RewardProfileResponse } from "../types";

export const rewardsApi = {
  profile: (donorId: number) => client.get<RewardProfileResponse>(`/api/rewards/${donorId}`).then((r) => r.data),
  leaderboard: (city: string) =>
    client.get<LeaderboardEntry[]>("/api/rewards/leaderboard", { params: { city } }).then((r) => r.data)
};
