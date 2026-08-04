import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { notificationApi } from "../api/notificationApi";
import { PageHeader } from "../components/ui";
import { useAuth } from "../context/AuthContext";
import { NotificationLogResponse } from "../types";

export default function Notifications() {
  const { user } = useAuth();
  const [rows, setRows] = useState<NotificationLogResponse[]>([]);
  const [error, setError] = useState("");

  useEffect(() => {
    if (!user) return;
    notificationApi
      .history(user.userId)
      .then(setRows)
      .catch((err) => setError(err.response?.data?.error || "Could not load notifications"));
  }, [user]);

  return (
    <div className="max-w-3xl mx-auto px-6 py-16">
      <PageHeader
        eyebrow="Inbox"
        title="Email & in-app alerts"
        subtitle="Delivery history from the notification service. SMS is disabled — email is the primary channel."
      />
      {error && <p className="text-urgent text-sm mb-4">{error}</p>}
      <div className="space-y-3">
        {rows.length === 0 && !error && <p className="text-muted">No notifications yet.</p>}
        {rows.map((n) => (
          <div key={n.notificationId} className="card p-4">
            <div className="flex justify-between gap-3">
              <p className="font-medium">{n.subject}</p>
              <span className="font-mono text-xs text-muted">{n.channel} · {n.status}</span>
            </div>
            <p className="text-xs text-muted mt-2">
              Request #{n.requestId} · {n.deliveredAt ? new Date(n.deliveredAt).toLocaleString() : "—"}
            </p>
            {n.requestId && (
              <Link to={`/requests/${n.requestId}`} className="text-primary text-sm font-medium mt-2 inline-block">
                Open request
              </Link>
            )}
          </div>
        ))}
      </div>
    </div>
  );
}
