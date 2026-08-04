import { FormEvent, useEffect, useState } from "react";
import { useParams } from "react-router-dom";
import { requestApi } from "../api/requestApi";
import { matchApi } from "../api/matchApi";
import { inventoryApi } from "../api/inventoryApi";
import { donorApi } from "../api/donorApi";
import { PageHeader, Field, TextInput } from "../components/ui";
import { useAuth } from "../context/AuthContext";
import { formatMatchScoreLabel } from "../lib/matchScore";
import { InventoryResponse, MatchResponse, RequestResponse } from "../types";

const STATUS_LABEL: Record<string, string> = {
  PENDING: "Request submitted — alerting donors & blood banks",
  MATCHED: "Donor matched — awaiting accept/decline",
  CONFIRMED: "Donor confirmed — ready for delivery",
  BANK_RESERVED: "Blood bank reserved stock — ready for delivery",
  OUT_FOR_DELIVERY: "Out for delivery — check email for OTP",
  FULFILLED: "Blood received & confirmed",
  NO_DONORS_FOUND: "No donors matched yet — try a blood bank",
  CANCELLED: "Cancelled"
};

function isOtpExpired(r: RequestResponse): boolean {
  if (r.status !== "OUT_FOR_DELIVERY" && !r.otpPending) return false;
  if (r.otpExpired === true) return true;
  if (r.otpExpired === false) return false;
  if (!r.otpExpiresAt) return true;
  return new Date(r.otpExpiresAt).getTime() <= Date.now();
}

export default function TrackRequest() {
  const { requestId } = useParams();
  const { user } = useAuth();
  const [request, setRequest] = useState<RequestResponse | null>(null);
  const [matches, setMatches] = useState<MatchResponse[]>([]);
  const [stock, setStock] = useState<InventoryResponse[]>([]);
  const [otp, setOtp] = useState("");
  const [error, setError] = useState("");
  const [message, setMessage] = useState("");
  const [busy, setBusy] = useState(false);
  const [myDonorId, setMyDonorId] = useState<number | null>(null);

  async function refresh() {
    if (!requestId) return;
    const id = Number(requestId);
    const data = await requestApi.get(id);
    setRequest(data);
    try {
      const raw = await matchApi.getMatches(id);
      const seen = new Set<string>();
      setMatches(
        raw.filter((m) => {
          const key = `${m.requestId}:${m.donorId}`;
          if (seen.has(key)) return false;
          seen.add(key);
          return true;
        })
      );
    } catch {
      setMatches([]);
    }
    if (data.city && data.bloodGroup) {
      try {
        setStock(await inventoryApi.search(data.bloodGroup, data.city));
      } catch {
        setStock([]);
      }
    }
  }

  useEffect(() => {
    if (user?.role === "DONOR") {
      donorApi.me().then((d) => setMyDonorId(d.donorId)).catch(() => setMyDonorId(null));
    }
  }, [user]);

  useEffect(() => {
    refresh();
    if (requestId) {
      const key = `bb-match-warn-${requestId}`;
      const warn = sessionStorage.getItem(key);
      if (warn) {
        setError(warn);
        sessionStorage.removeItem(key);
      }
    }
    const t = setInterval(refresh, 12000);
    return () => clearInterval(t);
  }, [requestId]);

  async function run(action: () => Promise<unknown>, okMsg?: string) {
    setBusy(true);
    setError("");
    setMessage("");
    try {
      await action();
      if (okMsg) setMessage(okMsg);
      await refresh();
    } catch (err: any) {
      setError(err.response?.data?.error || err.response?.data?.message || "Action failed");
    } finally {
      setBusy(false);
    }
  }

  async function handleReserve(batch: InventoryResponse) {
    if (!request || !user) return;
    await run(async () => {
      const reserved = await inventoryApi.reserve(
        request.bloodGroup,
        request.city,
        request.unitsNeeded,
        batch.batchId
      );
      await requestApi.reserveBank(request.requestId, {
        bloodBankUserId: Number(reserved.ownerUserId || batch.ownerUserId || user.userId),
        batchId: reserved.batchId || batch.batchId,
        bloodBankName: reserved.bloodBankName || batch.bloodBankName
      });
    }, "Stock reserved from blood bank.");
  }

  async function handleConfirmOtp(e: FormEvent) {
    e.preventDefault();
    if (!requestId) return;
    await run(() => requestApi.confirmOtp(Number(requestId), { otp }), "Delivery confirmed. Thank you.");
  }

  if (!request) {
    return <div className="max-w-3xl mx-auto px-6 py-16 text-muted">Loading request…</div>;
  }

  const isTerminalIssue = request.status === "CANCELLED";
  const myMatch = matches.find(
    (m) =>
      (m.responseStatus === "PENDING" || m.responseStatus === "ACCEPTED") &&
      (myDonorId == null || m.donorId === myDonorId)
  );
  const canTriggerMatch =
    user?.role === "REQUESTER" && (request.status === "PENDING" || request.status === "NO_DONORS_FOUND" || request.status === "MATCHED");
  return (
    <div className="max-w-3xl mx-auto px-6 py-16">
      <PageHeader
        eyebrow={`Request #${request.requestId}`}
        title={request.patientName}
        subtitle={`${request.bloodGroup} · ${request.unitsNeeded} unit(s) · ${request.hospitalName}, ${request.city} · ${request.urgency}`}
      />

      <div className="card p-6 mb-6">
        {!isTerminalIssue && (
          <div className="flex items-center mb-6 overflow-x-auto">
            {["PENDING", "MATCHED", "CONFIRMED", "OUT_FOR_DELIVERY", "FULFILLED"].map((step, i, arr) => {
              const activeIndex = ["PENDING", "MATCHED", "CONFIRMED", "OUT_FOR_DELIVERY", "FULFILLED"].indexOf(
                request.status === "BANK_RESERVED" ? "CONFIRMED" : request.status
              );
              return (
                <div key={step} className="flex items-center flex-1 last:flex-none min-w-[4rem]">
                  <div className={`w-3 h-3 rounded-full ${i <= activeIndex ? "bg-primary" : "bg-border"}`} />
                  {i < arr.length - 1 && (
                    <div className={`h-0.5 flex-1 ${i < activeIndex ? "bg-primary" : "bg-border"}`} />
                  )}
                </div>
              );
            })}
          </div>
        )}

        <p className={`font-medium ${isTerminalIssue ? "text-urgent" : "text-ink"}`}>
          {STATUS_LABEL[request.status] || request.status}
        </p>
        {(() => {
          if (request.fulfillmentSource === "BLOOD_BANK") {
            return (
              <p className="text-sm text-muted mt-2">
                Fulfillment: <span className="text-ink font-medium">Blood bank</span>
                {request.bloodBankUserId != null ? ` · bank user #${request.bloodBankUserId}` : ""}
              </p>
            );
          }
          const confirmed = matches.find((m) => m.donorId === request.confirmedDonorId)
            || matches.find((m) => m.responseStatus === "ACCEPTED");
          if (!confirmed?.donorName && !request.confirmedDonorId) return null;
          return (
            <p className="text-sm text-muted mt-2">
              Matched donor: <span className="text-ink font-medium">{confirmed?.donorName || "Confirmed donor"}</span>
              {confirmed?.donorPhone ? ` · ${confirmed.donorPhone}` : ""}
            </p>
          );
        })()}
        {request.fulfillmentSource && request.fulfillmentSource !== "BLOOD_BANK" && (
          <p className="text-sm text-muted mt-2">Source: {request.fulfillmentSource.replace("_", " ")}</p>
        )}
        {error && <p className="text-urgent text-sm mt-3">{error}</p>}
        {message && <p className="text-primary text-sm mt-3">{message}</p>}

        <div className="flex flex-wrap gap-3 mt-5">
          {canTriggerMatch && (
            <button
              onClick={() => run(() => matchApi.triggerMatch(request.requestId), "Matching triggered — check donor Match alerts.")}
              disabled={busy}
              className="btn-urgent"
            >
              Find / rematch donors
            </button>
          )}

          {request.status === "MATCHED" && user?.role === "DONOR" && myMatch?.responseStatus === "PENDING" && (
            <>
              <button
                onClick={() => run(() => matchApi.respond(request.requestId, myMatch.donorId, true), "Accepted.")}
                disabled={busy}
                className="btn-primary"
              >
                Accept — I can donate
              </button>
              <button
                onClick={() => run(() => matchApi.respond(request.requestId, myMatch.donorId, false))}
                disabled={busy}
                className="btn-secondary"
              >
                Decline
              </button>
            </>
          )}

          {request.status === "CONFIRMED" &&
            (user?.role === "DONOR" || user?.role === "REQUESTER") && (
              <button
                onClick={() =>
                  run(
                    () => requestApi.startDelivery(request.requestId),
                    "Delivery started. OTP emailed to requester."
                  )
                }
                disabled={busy}
                className="btn-primary"
              >
                Start delivery (email OTP)
              </button>
            )}

          {request.status === "BANK_RESERVED" &&
            user?.role === "BLOOD_BANK" &&
            Number(user.userId) === Number(request.bloodBankUserId) && (
              <button
                onClick={() =>
                  run(
                    () => requestApi.startDelivery(request.requestId),
                    "Delivery started. OTP emailed to requester — they can confirm on My Requests."
                  )
                }
                disabled={busy}
                className="btn-primary"
              >
                Start delivery (email OTP)
              </button>
            )}

          {request.status === "BANK_RESERVED" &&
            user != null &&
            Number(user.userId) === Number(request.requesterId) && (
              <p className="text-sm text-primary w-full mt-2">
                A blood bank reserved stock for you. When they start delivery, check your email for the OTP
                (also shown on My Requests).
              </p>
            )}

          {request.status === "OUT_FOR_DELIVERY" &&
            isOtpExpired(request) &&
            ((user?.role === "ADMIN") ||
              (user?.role === "BLOOD_BANK" &&
                request.fulfillmentSource === "BLOOD_BANK" &&
                Number(user.userId) === Number(request.bloodBankUserId)) ||
              (user?.role === "DONOR" &&
                request.fulfillmentSource !== "BLOOD_BANK" &&
                myDonorId != null &&
                Number(myDonorId) === Number(request.confirmedDonorId))) && (
              <button
                onClick={() =>
                  run(
                    () => requestApi.restartDelivery(request.requestId),
                    "Delivery restarted — a new OTP was emailed to the requester."
                  )
                }
                disabled={busy}
                className="btn-urgent"
              >
                Restart delivery
              </button>
            )}

          {(request.status === "OUT_FOR_DELIVERY" || request.otpPending) &&
            user != null &&
            Number(user.userId) === Number(request.requesterId) &&
            isOtpExpired(request) && (
              <p className="w-full text-sm text-urgent mt-2">
                OTP expired — ask the{" "}
                {request.fulfillmentSource === "BLOOD_BANK" ? "blood bank" : "donor"} to restart delivery.
                Confirm is disabled until a new OTP is sent.
              </p>
            )}

          {(request.status === "OUT_FOR_DELIVERY" || request.otpPending) &&
            user != null &&
            Number(user.userId) === Number(request.requesterId) &&
            !isOtpExpired(request) && (
            <form onSubmit={handleConfirmOtp} className="w-full flex flex-wrap items-end gap-3 mt-2">
              <p className="w-full text-sm text-primary">
                Delivery started{request.fulfillmentSource === "BLOOD_BANK" ? " by the blood bank" : ""}.
                Enter the OTP from your email to confirm receipt.
              </p>
              <div className="flex-1 min-w-[10rem]">
                <Field label="OTP from your email">
                  <TextInput value={otp} onChange={(e) => setOtp(e.target.value)} required maxLength={8} placeholder="6-digit OTP" />
                </Field>
              </div>
              <button type="submit" disabled={busy} className="btn-urgent mb-4">
                Confirm blood received
              </button>
            </form>
          )}

          {user?.role === "REQUESTER" && !["FULFILLED", "CANCELLED"].includes(request.status) && (
            <button onClick={() => run(() => requestApi.cancel(request.requestId))} disabled={busy} className="btn-secondary">
              Cancel request
            </button>
          )}
        </div>
      </div>

      {user?.role === "REQUESTER" &&
        !["FULFILLED", "CANCELLED", "OUT_FOR_DELIVERY", "BANK_RESERVED", "CONFIRMED"].includes(request.status) && (
          <div className="card p-6">
            <h3 className="font-display text-xl mb-2">Reserve from a blood bank</h3>
            <p className="text-sm text-muted mb-4">
              Live inventory for {request.bloodGroup} in {request.city}. Reserving deducts stock and marks this request as bank-reserved.
            </p>
            {stock.length === 0 ? (
              <p className="text-sm text-muted">No active stock found for this city/group yet.</p>
            ) : (
              <div className="space-y-2">
                {stock.map((b) => (
                  <div key={b.batchId} className="flex items-center justify-between gap-3 border border-border rounded-lg p-3">
                    <div>
                      <p className="font-medium">{b.bloodBankName}</p>
                      <p className="text-xs text-muted font-mono">
                        {b.unitsAvailable} units · exp {b.expiryDate} · batch #{b.batchId}
                      </p>
                    </div>
                    <button
                      disabled={busy || b.unitsAvailable < request.unitsNeeded}
                      onClick={() => handleReserve(b)}
                      className="btn-primary text-sm"
                    >
                      Reserve
                    </button>
                  </div>
                ))}
              </div>
            )}
          </div>
        )}

      {matches.length > 0 && (
        <div className="card p-6 mt-6">
          <h3 className="font-display text-xl mb-3">Donor matches</h3>
          <div className="space-y-2">
            {matches.map((m) => (
              <div key={m.matchId} className="flex justify-between gap-3 text-sm border-b border-border py-2 last:border-0">
                <div>
                  <p className="font-medium text-ink">{m.donorName || "Matched donor"}</p>
                  <p className="text-xs text-muted">
                    {[m.donorBloodGroup, m.donorCity, m.donorPhone].filter(Boolean).join(" · ") || "Details unavailable"}
                  </p>
                </div>
                <span className="font-mono text-muted shrink-0">
                  {m.responseStatus} · {formatMatchScoreLabel(m.matchScore)}
                </span>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}
