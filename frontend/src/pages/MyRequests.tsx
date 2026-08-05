import { FormEvent, useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { requestApi } from "../api/requestApi";
import { PageHeader, Field, TextInput } from "../components/ui";
import { RequestResponse } from "../types";

const STATUS_LABEL: Record<string, string> = {
  PENDING: "Submitted — alerting donors & blood banks",
  MATCHED: "Donor matched — awaiting accept",
  CONFIRMED: "Donor confirmed — ready for delivery",
  BANK_RESERVED: "Blood bank reserved stock — delivery will start soon",
  OUT_FOR_DELIVERY: "Out for delivery — check email for OTP",
  FULFILLED: "Blood received & confirmed",
  NO_DONORS_FOUND: "No donors matched — try a blood bank",
  CANCELLED: "Cancelled"
};

function isOtpExpired(r: RequestResponse): boolean {
  if (r.status !== "OUT_FOR_DELIVERY" && !r.otpPending) return false;
  if (r.otpExpired === true) return true;
  if (r.otpExpired === false) return false;
  if (!r.otpExpiresAt) return true;
  return new Date(r.otpExpiresAt).getTime() <= Date.now();
}

export default function MyRequests() {
  const [requests, setRequests] = useState<RequestResponse[]>([]);
  const [otpById, setOtpById] = useState<Record<number, string>>({});
  const [busyId, setBusyId] = useState<number | null>(null);
  const [error, setError] = useState("");
  const [message, setMessage] = useState("");

  async function load() {
    try {
      setRequests(await requestApi.mine());
      setError("");
    } catch (err: any) {
      // Keep prior rows if a poll fails; surface why the list looks empty.
      setError(err.response?.data?.error || err.response?.data?.message || "Could not load your requests");
    }
  }

  useEffect(() => {
    load();
    const t = setInterval(load, 12000);
    return () => clearInterval(t);
  }, []);

  async function handleConfirmOtp(e: FormEvent, requestId: number) {
    e.preventDefault();
    setBusyId(requestId);
    setError("");
    setMessage("");
    try {
      await requestApi.confirmOtp(requestId, { otp: otpById[requestId] || "" });
      setMessage(`Request #${requestId} confirmed — blood received.`);
      setOtpById((prev) => ({ ...prev, [requestId]: "" }));
      await load();
    } catch (err: any) {
      setError(err.response?.data?.error || err.response?.data?.message || "OTP confirmation failed");
    } finally {
      setBusyId(null);
    }
  }

  return (
    <div className="max-w-3xl mx-auto px-6 py-16">
      <PageHeader
        eyebrow="Your history"
        title="My Requests"
        subtitle="When a donor or blood bank starts delivery, enter the email OTP here — no need to open Track Request."
      />
      {error && <p className="text-urgent text-sm mb-4">{error}</p>}
      {message && <p className="text-primary text-sm mb-4">{message}</p>}
      {requests.length === 0 ? (
        <p className="text-muted">No requests yet. Raise one when you need blood urgently.</p>
      ) : (
        <div className="space-y-3">
          {requests.map((r) => (
            <div key={r.requestId} className="card p-4">
              <div className="flex items-center justify-between gap-3">
                <div>
                  <p className="font-medium text-ink">{r.patientName} · {r.bloodGroup}</p>
                  <p className="text-sm text-muted">{r.hospitalName}, {r.city}</p>
                  <p className="text-sm text-ink mt-1">{STATUS_LABEL[r.status] || r.status}</p>
                  {r.fulfillmentSource && (
                    <p className="text-xs text-muted mt-1">
                      Source: {r.fulfillmentSource === "BLOOD_BANK" ? "Blood bank" : "Donor"}
                    </p>
                  )}
                  {r.status === "BANK_RESERVED" && r.fulfillmentSource === "BLOOD_BANK" && (
                    <p className="text-xs text-primary mt-1">
                      A blood bank reserved stock for you. Delivery OTP will arrive by email when they start.
                    </p>
                  )}
                  {(r.status === "OUT_FOR_DELIVERY" || r.otpPending) && !isOtpExpired(r) && (
                    <p className="text-xs text-primary mt-1">
                      Delivery started{r.fulfillmentSource === "BLOOD_BANK" ? " by the blood bank" : ""}.
                      Check your email for the OTP and confirm below.
                    </p>
                  )}
                  {(r.status === "OUT_FOR_DELIVERY" || r.otpPending) && isOtpExpired(r) && (
                    <p className="text-xs text-urgent mt-1">
                      OTP expired — ask the{" "}
                      {r.fulfillmentSource === "BLOOD_BANK" ? "blood bank" : "donor"} to restart delivery.
                    </p>
                  )}
                </div>
                <div className="text-right shrink-0">
                  <span className="text-sm font-mono text-muted block">{r.status}</span>
                  <Link to={`/requests/${r.requestId}`} className="text-sm text-primary font-medium">
                    Open
                  </Link>
                </div>
              </div>

              {(r.status === "OUT_FOR_DELIVERY" || r.otpPending) && !isOtpExpired(r) && (
                <form
                  onSubmit={(e) => handleConfirmOtp(e, r.requestId)}
                  className="mt-4 flex flex-wrap items-end gap-3 border-t border-border pt-4"
                >
                  <div className="flex-1 min-w-[10rem]">
                    <Field label="OTP from your email">
                      <TextInput
                        value={otpById[r.requestId] || ""}
                        onChange={(e) => setOtpById((prev) => ({ ...prev, [r.requestId]: e.target.value }))}
                        required
                        maxLength={8}
                        placeholder="6-digit OTP"
                      />
                    </Field>
                  </div>
                  <button type="submit" disabled={busyId === r.requestId} className="btn-urgent mb-4">
                    Confirm blood received
                  </button>
                </form>
              )}
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
