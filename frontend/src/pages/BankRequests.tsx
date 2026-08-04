import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { requestApi } from "../api/requestApi";
import { inventoryApi } from "../api/inventoryApi";
import { PageHeader } from "../components/ui";
import { useAuth } from "../context/AuthContext";
import { RequestResponse } from "../types";

const STATUS_LABEL: Record<string, string> = {
  PENDING: "Open — awaiting reserve",
  MATCHED: "Open — donors matched",
  NO_DONORS_FOUND: "Open — no donors yet",
  BANK_RESERVED: "Reserved by you — start delivery",
  OUT_FOR_DELIVERY: "Out for delivery — OTP sent to requester",
  FULFILLED: "Completed — blood received"
};

function isOtpExpired(r: RequestResponse): boolean {
  if (r.status !== "OUT_FOR_DELIVERY") return false;
  if (r.otpExpired === true) return true;
  if (r.otpExpired === false) return false;
  if (!r.otpExpiresAt) return true;
  return new Date(r.otpExpiresAt).getTime() <= Date.now();
}

function RequestCard({
  r,
  busy,
  onReserve,
  onStartDelivery,
  onRestartDelivery,
  showReserve,
  showStart,
  showRestart
}: {
  r: RequestResponse;
  busy: boolean;
  onReserve?: () => void;
  onStartDelivery?: () => void;
  onRestartDelivery?: () => void;
  showReserve?: boolean;
  showStart?: boolean;
  showRestart?: boolean;
}) {
  return (
    <div className="card p-5">
      <div className="flex justify-between gap-3">
        <div>
          <p className="font-display text-xl">{r.patientName}</p>
          <p className="text-sm text-muted">
            {r.bloodGroup} · {r.unitsNeeded} unit(s) · {r.hospitalName}, {r.city}
          </p>
          <p className="font-mono text-xs text-muted mt-1">
            {STATUS_LABEL[r.status] || r.status}
            {r.urgency ? ` · ${r.urgency}` : ""}
          </p>
          {r.reservedBatchId != null && (
            <p className="text-xs text-muted mt-1">Batch #{r.reservedBatchId}</p>
          )}
          {showRestart && (
            <p className="text-xs text-urgent mt-1">OTP expired — restart delivery to email a new code.</p>
          )}
        </div>
        <Link to={`/requests/${r.requestId}`} className="btn-secondary text-sm h-fit">
          Details
        </Link>
      </div>
      {(showReserve || showStart || showRestart) && (
        <div className="flex flex-wrap gap-3 mt-4">
          {showReserve && (
            <button disabled={busy} className="btn-primary" onClick={onReserve}>
              Confirm & reserve stock
            </button>
          )}
          {showStart && (
            <button disabled={busy} className="btn-urgent" onClick={onStartDelivery}>
              Start delivery (email OTP)
            </button>
          )}
          {showRestart && (
            <button disabled={busy} className="btn-urgent" onClick={onRestartDelivery}>
              Restart delivery
            </button>
          )}
        </div>
      )}
    </div>
  );
}

export default function BankRequests() {
  const { user } = useAuth();
  const [openRows, setOpenRows] = useState<RequestResponse[]>([]);
  const [mineRows, setMineRows] = useState<RequestResponse[]>([]);
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);
  const [message, setMessage] = useState("");

  async function load() {
    try {
      const [open, mine] = await Promise.all([requestApi.open(), requestApi.bankMine()]);
      setOpenRows(open);
      setMineRows(mine);
    } catch (err: any) {
      setError(err.response?.data?.error || "Could not load requests");
    }
  }

  useEffect(() => {
    load();
    const t = setInterval(load, 12000);
    return () => clearInterval(t);
  }, []);

  async function confirmAvailability(req: RequestResponse) {
    if (!user) return;
    setBusy(true);
    setError("");
    setMessage("");
    try {
      const stock = await inventoryApi.search(req.bloodGroup, req.city);
      const batch = stock.find((s) => s.unitsAvailable >= req.unitsNeeded);
      if (!batch) {
        setError(`No sufficient ${req.bloodGroup} stock in ${req.city}. Add inventory first.`);
        return;
      }
      const reserved = await inventoryApi.reserve(req.bloodGroup, req.city, req.unitsNeeded, batch.batchId);
      await requestApi.reserveBank(req.requestId, {
        bloodBankUserId: Number(user.userId),
        batchId: reserved.batchId || batch.batchId,
        bloodBankName: reserved.bloodBankName || batch.bloodBankName || user.fullName
      });
      setMessage(`Reserved stock for request #${req.requestId}. It now appears under My reserved — start delivery next.`);
      await load();
    } catch (err: any) {
      setError(err.response?.data?.error || err.message || "Could not reserve");
    } finally {
      setBusy(false);
    }
  }

  async function startDelivery(requestId: number) {
    setBusy(true);
    setError("");
    setMessage("");
    try {
      await requestApi.startDelivery(requestId);
      setMessage("Delivery started — OTP emailed to requester. Request moved to Out for delivery.");
      await load();
    } catch (err: any) {
      setError(err.response?.data?.error || err.message || "Could not start delivery");
    } finally {
      setBusy(false);
    }
  }

  async function restartDelivery(requestId: number) {
    setBusy(true);
    setError("");
    setMessage("");
    try {
      await requestApi.restartDelivery(requestId);
      setMessage("Delivery restarted — a new OTP was emailed to the requester.");
      await load();
    } catch (err: any) {
      setError(err.response?.data?.error || err.message || "Could not restart delivery");
    } finally {
      setBusy(false);
    }
  }

  const reserved = mineRows.filter((r) => r.status === "BANK_RESERVED");
  const outForDelivery = mineRows.filter((r) => r.status === "OUT_FOR_DELIVERY");
  const completed = mineRows.filter((r) => r.status === "FULFILLED");

  return (
    <div className="max-w-3xl mx-auto px-6 py-16">
      <PageHeader
        eyebrow="Blood bank"
        title="Request board"
        subtitle="Like donor match alerts: reserve open requests, keep your active ones here, start delivery so the requester gets an email OTP."
      />
      {error && <p className="text-urgent text-sm mb-4">{error}</p>}
      {message && <p className="text-primary text-sm mb-4">{message}</p>}

      <section className="mb-10">
        <h2 className="font-display text-xl mb-3">Open requests</h2>
        <p className="text-sm text-muted mb-4">Confirm availability from your inventory to reserve stock.</p>
        <div className="space-y-4">
          {openRows.length === 0 && <p className="text-muted text-sm">No open requests right now.</p>}
          {openRows.map((r) => (
            <RequestCard
              key={r.requestId}
              r={r}
              busy={busy}
              showReserve={["PENDING", "MATCHED", "NO_DONORS_FOUND"].includes(r.status)}
              onReserve={() => confirmAvailability(r)}
            />
          ))}
        </div>
      </section>

      <section className="mb-10">
        <h2 className="font-display text-xl mb-3">My reserved</h2>
        <p className="text-sm text-muted mb-4">Stock you reserved — start delivery to email the requester an OTP.</p>
        <div className="space-y-4">
          {reserved.length === 0 && <p className="text-muted text-sm">No reserved requests yet.</p>}
          {reserved.map((r) => (
            <RequestCard
              key={r.requestId}
              r={r}
              busy={busy}
              showStart
              onStartDelivery={() => startDelivery(r.requestId)}
            />
          ))}
        </div>
      </section>

      <section className="mb-10">
        <h2 className="font-display text-xl mb-3">Out for delivery</h2>
        <p className="text-sm text-muted mb-4">Waiting for the requester to confirm the email OTP.</p>
        <div className="space-y-4">
          {outForDelivery.length === 0 && <p className="text-muted text-sm">Nothing out for delivery.</p>}
          {outForDelivery.map((r) => (
            <RequestCard
              key={r.requestId}
              r={r}
              busy={busy}
              showRestart={isOtpExpired(r)}
              onRestartDelivery={() => restartDelivery(r.requestId)}
            />
          ))}
        </div>
      </section>

      <section>
        <h2 className="font-display text-xl mb-3">Completed</h2>
        <p className="text-sm text-muted mb-4">Fulfilled via your bank after OTP confirmation.</p>
        <div className="space-y-4">
          {completed.length === 0 && <p className="text-muted text-sm">No completed bank fulfillments yet.</p>}
          {completed.map((r) => (
            <RequestCard key={r.requestId} r={r} busy={busy} />
          ))}
        </div>
      </section>
    </div>
  );
}
