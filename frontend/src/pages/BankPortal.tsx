import { FormEvent, useEffect, useState } from "react";
import { inventoryApi } from "../api/inventoryApi";
import { Field, PageHeader, SelectInput, TextInput } from "../components/ui";
import { useAuth } from "../context/AuthContext";
import { BloodGroup, InventoryResponse, LowStockAlert } from "../types";

const bloodGroups: BloodGroup[] = ["A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-"];

export default function BankPortal() {
  const { user } = useAuth();
  const [form, setForm] = useState({
    bloodBankName: user?.fullName || "",
    city: "",
    bloodGroup: "" as BloodGroup | "",
    unitsAvailable: "",
    collectedDate: "",
    expiryDate: ""
  });
  const [mine, setMine] = useState<InventoryResponse[]>([]);
  const [alerts, setAlerts] = useState<LowStockAlert[]>([]);
  const [message, setMessage] = useState("");
  const [error, setError] = useState("");

  async function load() {
    try {
      setMine(await inventoryApi.mine());
      setAlerts(await inventoryApi.alerts(true));
    } catch (err: any) {
      setError(err.response?.data?.error || "Could not load inventory");
    }
  }

  useEffect(() => {
    load();
  }, []);

  function update(field: keyof typeof form, value: string) {
    setForm((f) => ({ ...f, [field]: value }));
  }

  async function handleAdd(e: FormEvent) {
    e.preventDefault();
    setMessage("");
    setError("");
    try {
      await inventoryApi.add({
        bloodBankName: form.bloodBankName,
        city: form.city,
        bloodGroup: form.bloodGroup as BloodGroup,
        unitsAvailable: Number(form.unitsAvailable),
        collectedDate: form.collectedDate,
        expiryDate: form.expiryDate
      });
      setMessage("Stock added to your bank inventory.");
      setForm((f) => ({ ...f, bloodGroup: "", unitsAvailable: "", collectedDate: "", expiryDate: "" }));
      await load();
    } catch (err: any) {
      setError(err.response?.data?.error || "Could not add stock");
    }
  }

  async function handleDelete(batchId: number) {
    await inventoryApi.remove(batchId);
    await load();
  }

  async function handleUpdate(batchId: number, units: number) {
    await inventoryApi.update(batchId, units);
    await load();
  }

  return (
    <div className="max-w-5xl mx-auto px-6 py-16">
      <PageHeader
        eyebrow="Blood bank portal"
        title="Manage inventory"
        subtitle="Add, update, and retire stock. Requesters reserve against this live inventory."
      />

      {alerts.length > 0 && (
        <div className="card p-4 mb-8 border-urgent/30 bg-urgent-light">
          <p className="font-medium text-urgent-dark mb-2">Low stock alerts</p>
          <ul className="text-sm text-urgent-dark space-y-1">
            {alerts.map((a, i) => (
              <li key={i}>{a.bloodGroup} in {a.city}: {a.availableUnits} unit(s) (threshold {a.threshold})</li>
            ))}
          </ul>
        </div>
      )}

      <div className="grid md:grid-cols-2 gap-8">
        <form onSubmit={handleAdd} className="card p-6 h-fit">
          <h2 className="font-display text-xl mb-4">Add stock</h2>
          <Field label="Blood bank name">
            <TextInput required value={form.bloodBankName} onChange={(e) => update("bloodBankName", e.target.value)} />
          </Field>
          <Field label="City">
            <TextInput required value={form.city} onChange={(e) => update("city", e.target.value)} />
          </Field>
          <div className="grid grid-cols-2 gap-4">
            <Field label="Blood group">
              <SelectInput required value={form.bloodGroup} onChange={(e) => update("bloodGroup", e.target.value)}>
                <option value="">Select</option>
                {bloodGroups.map((bg) => <option key={bg} value={bg}>{bg}</option>)}
              </SelectInput>
            </Field>
            <Field label="Units">
              <TextInput type="number" min={1} required value={form.unitsAvailable} onChange={(e) => update("unitsAvailable", e.target.value)} />
            </Field>
          </div>
          <div className="grid grid-cols-2 gap-4">
            <Field label="Collected">
              <TextInput type="date" required value={form.collectedDate} onChange={(e) => update("collectedDate", e.target.value)} />
            </Field>
            <Field label="Expiry">
              <TextInput type="date" required value={form.expiryDate} onChange={(e) => update("expiryDate", e.target.value)} />
            </Field>
          </div>
          {message && <p className="text-primary text-sm mb-3">{message}</p>}
          {error && <p className="text-urgent text-sm mb-3">{error}</p>}
          <button type="submit" className="btn-primary w-full">Add to inventory</button>
        </form>

        <div>
          <h2 className="font-display text-xl mb-4">Your batches</h2>
          <div className="space-y-3">
            {mine.length === 0 && <p className="text-muted text-sm">No batches yet — add your first stock lot.</p>}
            {mine.map((b) => (
              <div key={b.batchId} className="card p-4">
                <div className="flex justify-between gap-3">
                  <div>
                    <p className="font-medium">{b.bloodGroup} · {b.city}</p>
                    <p className="text-xs text-muted font-mono">{b.status} · exp {b.expiryDate} · #{b.batchId}</p>
                  </div>
                  <button className="btn-secondary text-sm" onClick={() => handleDelete(b.batchId)}>Delete</button>
                </div>
                <div className="flex items-center gap-2 mt-3">
                  <TextInput
                    type="number"
                    min={0}
                    defaultValue={b.unitsAvailable}
                    className="input max-w-[7rem]"
                    id={`units-${b.batchId}`}
                  />
                  <button
                    className="btn-primary text-sm"
                    onClick={() => {
                      const el = document.getElementById(`units-${b.batchId}`) as HTMLInputElement;
                      handleUpdate(b.batchId, Number(el.value));
                    }}
                  >
                    Update units
                  </button>
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
}
