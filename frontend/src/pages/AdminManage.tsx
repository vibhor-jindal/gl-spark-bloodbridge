import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { requestApi } from "../api/requestApi";
import { inventoryApi } from "../api/inventoryApi";
import { authApi } from "../api/authApi";
import { Field, PageHeader, SelectInput } from "../components/ui";
import { InventoryResponse, PlatformUser, RequestResponse, RequestStatus } from "../types";

const STATUSES: RequestStatus[] = [
  "PENDING", "MATCHED", "CONFIRMED", "BANK_RESERVED", "OUT_FOR_DELIVERY", "FULFILLED", "NO_DONORS_FOUND", "CANCELLED"
];

export default function AdminManage() {
  const [requests, setRequests] = useState<RequestResponse[]>([]);
  const [inventory, setInventory] = useState<InventoryResponse[]>([]);
  const [users, setUsers] = useState<PlatformUser[]>([]);
  const [error, setError] = useState("");
  const [message, setMessage] = useState("");

  async function load() {
    setError("");
    try {
      const [r, i, u] = await Promise.all([requestApi.all(), inventoryApi.all(), authApi.listUsers()]);
      setRequests(r);
      setInventory(i);
      setUsers(u);
    } catch (err: any) {
      setError(err.response?.data?.error || "Could not load admin data");
    }
  }

  useEffect(() => {
    load();
  }, []);

  async function updateStatus(requestId: number, status: string) {
    setMessage("");
    try {
      await requestApi.adminUpdate(requestId, { status });
      setMessage(`Request #${requestId} → ${status}`);
      await load();
    } catch (err: any) {
      setError(err.response?.data?.error || "Update failed");
    }
  }

  async function deleteRequest(requestId: number) {
    if (!confirm(`Delete request #${requestId}?`)) return;
    await requestApi.adminDelete(requestId);
    await load();
  }

  async function deleteBatch(batchId: number) {
    if (!confirm(`Delete inventory batch #${batchId}?`)) return;
    await inventoryApi.remove(batchId);
    await load();
  }

  async function deleteUser(userId: number) {
    if (!confirm(`Delete user #${userId}?`)) return;
    await authApi.deleteUser(userId);
    await load();
  }

  return (
    <div className="max-w-6xl mx-auto px-6 py-16">
      <PageHeader
        eyebrow="Admin"
        title="Manage platform data"
        subtitle="Modify or delete requests, inventory, and users. Analytics stay on the dashboard."
      />
      {error && <p className="text-urgent text-sm mb-4">{error}</p>}
      {message && <p className="text-primary text-sm mb-4">{message}</p>}

      <section className="mb-12">
        <div className="flex items-center justify-between mb-4">
          <h2 className="font-display text-2xl">Requests</h2>
          <Link to="/admin/dashboard" className="btn-secondary text-sm">Analytics</Link>
        </div>
        <div className="space-y-3">
          {requests.map((r) => (
            <div key={r.requestId} className="card p-4 flex flex-wrap gap-3 items-center justify-between">
              <div>
                <p className="font-medium">#{r.requestId} · {r.patientName}</p>
                <p className="text-xs text-muted font-mono">
                  {r.bloodGroup} · {r.city} · {r.status}
                </p>
              </div>
              <div className="flex flex-wrap gap-2 items-center">
                <SelectInput
                  value={r.status}
                  onChange={(e) => updateStatus(r.requestId, e.target.value)}
                  className="input py-1.5 text-sm"
                >
                  {STATUSES.map((s) => <option key={s} value={s}>{s}</option>)}
                </SelectInput>
                <Link to={`/requests/${r.requestId}`} className="btn-secondary text-sm">Open</Link>
                <button className="btn-urgent text-sm" onClick={() => deleteRequest(r.requestId)}>Delete</button>
              </div>
            </div>
          ))}
        </div>
      </section>

      <section className="mb-12">
        <h2 className="font-display text-2xl mb-4">Inventory</h2>
        <div className="space-y-3">
          {inventory.map((b) => (
            <div key={b.batchId} className="card p-4 flex justify-between gap-3 items-center">
              <div>
                <p className="font-medium">{b.bloodBankName} · {b.bloodGroup}</p>
                <p className="text-xs text-muted font-mono">
                  #{b.batchId} · {b.unitsAvailable} units · {b.city} · {b.status}
                </p>
              </div>
              <button className="btn-urgent text-sm" onClick={() => deleteBatch(b.batchId)}>Delete</button>
            </div>
          ))}
        </div>
      </section>

      <section>
        <h2 className="font-display text-2xl mb-4">Users</h2>
        <div className="space-y-3">
          {users.map((u) => (
            <div key={u.userId} className="card p-4 flex justify-between gap-3 items-center">
              <div>
                <p className="font-medium">{u.fullName}</p>
                <p className="text-xs text-muted font-mono">{u.email} · {u.role} · #{u.userId}</p>
              </div>
              <button className="btn-urgent text-sm" onClick={() => deleteUser(u.userId)}>Delete</button>
            </div>
          ))}
        </div>
      </section>
    </div>
  );
}
