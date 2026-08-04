import client from "./client";
import { NotificationLogResponse } from "../types";

export const notificationApi = {
  history: (recipientId: number) =>
    client.get<NotificationLogResponse[]>(`/api/notifications/${recipientId}`).then((r) => r.data)
};
