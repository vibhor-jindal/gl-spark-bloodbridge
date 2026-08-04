import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { requestApi } from "../api/requestApi";
import { matchApi } from "../api/matchApi";
import { donorApi } from "../api/donorApi";
import { PageHeader } from "../components/ui";
import { formatMatchScoreLabel } from "../lib/matchScore";
import { DonorResponse, MatchResponse, RequestResponse } from "../types";

type AlertRow = {
  match: MatchResponse;
  request: RequestResponse | null;
};

export default function DonorAlerts() {
  const [donor, setDonor] = useState<DonorResponse | null>(null);
  const [rows, setRows] = useState<AlertRow[]>([]);
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);

  async function load() {
    setError("");
    try {
      const me = await donorApi.me();
      setDonor(me);
      const matches = await matchApi.mine(false);
      // One alert per request+donor (legacy duplicate match rows can exist).
      const seen = new Set<string>();
      const actionable = matches.filter((m) => {
        if (m.responseStatus !== "PENDING" && m.responseStatus !== "ACCEPTED") return false;
        const key = `${m.requestId}:${m.donorId}`;
        if (seen.has(key)) return false;
        seen.add(key);
        return true;
      });
      const hydrated = await Promise.all(
        actionable.map(async (match) => {
          try {
            const request = await requestApi.get(match.requestId);
            return { match, request };
          } catch {
            return { match, request: null };
          }
        })
      );
      setRows(hydrated);
    } catch (err: any) {
      const status = err.response?.status;
      if (status === 404) {
        setError("Pehle donor profile banao — Profile page se register karo.");
      } else {
        setError(err.response?.data?.error || "Could not load your match alerts");
      }
      setRows([]);
    }
  }

  useEffect(() => {
    load();
    const t = setInterval(load, 10000);
    return () => clearInterval(t);
  }, []);

  async function respond(requestId: number, donorId: number, accepted: boolean) {
    setBusy(true);
    setError("");
    try {
      await matchApi.respond(requestId, donorId, accepted);
      await load();
    } catch (err: any) {
      setError(err.response?.data?.error || "Could not respond");
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="max-w-3xl mx-auto px-6 py-16">
      <PageHeader
        eyebrow="Donor"
        title="Match alerts"
        subtitle={
          donor
            ? `Showing matches for ${donor.name} (${donor.bloodGroup}, ${donor.city}). Same blood group matches you even across nearby cities.`
            : "Live match requests assigned to your donor profile."
        }
      />
      {error && (
        <p className="text-urgent text-sm mb-4">
          {error}{" "}
          {error.includes("profile") && (
            <Link to="/donor/register" className="text-primary font-medium">
              Register profile
            </Link>
          )}
        </p>
      )}
      <div className="space-y-4">
        {rows.length === 0 && !error && (
          <div className="card p-5 text-sm text-muted space-y-2">
            <p>Abhi koi match alert nahi hai.</p>
            <p>
              Tip: same blood group is required; nearby cities (e.g. Delhi NCR) are included when local donors are few or none.
              Requester must also trigger matching after creating the request.
            </p>
          </div>
        )}
        {rows.map(({ match, request }) => (
          <div key={match.matchId} className="card p-5">
            <div className="flex justify-between gap-3 items-start">
              <div>
                <p className="font-display text-xl">
                  {request?.patientName || `Request #${match.requestId}`}
                </p>
                <p className="text-sm text-muted">
                  {request
                    ? `${request.bloodGroup} · ${request.unitsNeeded} unit(s) · ${request.hospitalName}, ${request.city}`
                    : `Match #${match.matchId}`}
                </p>
                <p className="font-mono text-xs text-muted mt-1">
                  match {match.responseStatus} · request {request?.status || "—"} ·{" "}
                  {formatMatchScoreLabel(match.matchScore)}
                </p>
              </div>
              <Link to={`/requests/${match.requestId}`} className="btn-secondary text-sm">
                Open
              </Link>
            </div>
            {match.responseStatus === "PENDING" && (
              <div className="flex gap-3 mt-4">
                <button
                  disabled={busy}
                  className="btn-primary"
                  onClick={() => respond(match.requestId, match.donorId, true)}
                >
                  Accept — I can donate
                </button>
                <button
                  disabled={busy}
                  className="btn-secondary"
                  onClick={() => respond(match.requestId, match.donorId, false)}
                >
                  Decline
                </button>
              </div>
            )}
          </div>
        ))}
      </div>
    </div>
  );
}
