import { FormEvent, KeyboardEvent, useEffect, useState } from "react";
import { inventoryApi } from "../api/inventoryApi";
import { Field, PageHeader, SelectInput, TextInput } from "../components/ui";
import { useAuth } from "../context/AuthContext";
import { BloodGroup, InventoryResponse, LowStockAlert } from "../types";

const bloodGroups: BloodGroup[] = ["A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-"];

function parseUnits(raw: string): { ok: true; value: number } | { ok: false; error: string } {
  const trimmed = raw.trim();
  if (trimmed === "") {
    return { ok: false, error: "Enter a number of units" };
  }
  if (!/^\d+$/.test(trimmed)) {
    return { ok: false, error: "Units must be a whole number (digits only)" };
  }
  const value = Number(trimmed);
  if (!Number.isInteger(value) || value < 0) {
    return { ok: false, error: "Units cannot be negative" };
  }
  return { ok: true, value };
}

function apiErrorMessage(err: any, fallback: string): string {
  return err.response?.data?.error
    || err.response?.data?.fieldErrors?.unitsAvailable
    || fallback;
}

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
  const [unitDrafts, setUnitDrafts] = useState<Record<number, string>>({});
  const [rowErrors, setRowErrors] = useState<Record<number, string>>({});

  async function load() {
    try {
      const batches = await inventoryApi.mine();
      setMine(batches);
      setUnitDrafts((prev) => {
        const next: Record<number, string> = {};
        for (const b of batches) {
          next[b.batchId] = prev[b.batchId] ?? String(b.unitsAvailable);
        }
        return next;
      });
      setAlerts(await inventoryApi.alerts(true));
    } catch (err: any) {
      setError(apiErrorMessage(err, "Could not load inventory"));
    }
  }

  useEffect(() => {
    load();
  }, []);

  function update(field: keyof typeof form, value: string) {
    setForm((f) => ({ ...f, [field]: value }));
  }

  function onUnitsKeyDown(e: KeyboardEvent<HTMLInputElement>) {
    // Block letters / symbols that type="number" still allows (e, E, +, -)
    if (["e", "E", "+", "-", "."].includes(e.key)) {
      e.preventDefault();
    }
  }

  function onUnitsChange(value: string, apply: (v: string) => void) {
    if (value === "" || /^\d+$/.test(value)) {
      apply(value);
    }
  }

  async function handleAdd(e: FormEvent) {
    e.preventDefault();
    setMessage("");
    setError("");
    const parsed = parseUnits(form.unitsAvailable);
    if (!parsed.ok) {
      setError(parsed.error);
      return;
    }
    if (parsed.value < 1) {
      setError("Add at least 1 unit when creating stock");
      return;
    }
    try {
      await inventoryApi.add({
        bloodBankName: form.bloodBankName,
        city: form.city,
        bloodGroup: form.bloodGroup as BloodGroup,
        unitsAvailable: parsed.value,
        collectedDate: form.collectedDate,
        expiryDate: form.expiryDate
      });
      setMessage("Stock added to your bank inventory.");
      setForm((f) => ({ ...f, bloodGroup: "", unitsAvailable: "", collectedDate: "", expiryDate: "" }));
      await load();
    } catch (err: any) {
      setError(apiErrorMessage(err, "Could not add stock"));
    }
  }

  async function handleDelete(batchId: number) {
    setMessage("");
    setError("");
    try {
      await inventoryApi.remove(batchId);
      setRowErrors((prev) => {
        const next = { ...prev };
        delete next[batchId];
        return next;
      });
      await load();
    } catch (err: any) {
      setError(apiErrorMessage(err, "Could not delete batch"));
    }
  }

  async function handleUpdate(batchId: number) {
    setMessage("");
    setError("");
    setRowErrors((prev) => ({ ...prev, [batchId]: "" }));

    const raw = unitDrafts[batchId] ?? "";
    const parsed = parseUnits(raw);
    if (!parsed.ok) {
      setRowErrors((prev) => ({ ...prev, [batchId]: parsed.error }));
      return;
    }

    try {
      const updated = await inventoryApi.update(batchId, parsed.value);
      setUnitDrafts((prev) => ({ ...prev, [batchId]: String(updated.unitsAvailable) }));
      if (updated.status === "ACTIVE" && parsed.value > 0) {
        setMessage(`Batch #${batchId} updated to ${parsed.value} unit(s) and is ACTIVE.`);
      } else if (updated.status === "DEPLETED") {
        setMessage(`Batch #${batchId} set to 0 units (DEPLETED). Enter units > 0 and Update to reactivate.`);
      } else {
        setMessage(`Batch #${batchId} updated to ${parsed.value} unit(s) (${updated.status}).`);
      }
      await load();
    } catch (err: any) {
      const msg = apiErrorMessage(err, "Could not update units");
      setRowErrors((prev) => ({ ...prev, [batchId]: msg }));
      setError(msg);
    }
  }

  return (
    <div className="max-w-5xl mx-auto px-6 py-16">
      <PageHeader
        eyebrow="Blood bank portal"
        title="Manage inventory"
        subtitle="Add, update, and retire stock. Set units to 0 to mark DEPLETED; update units above 0 on a non-expired batch to reactivate as ACTIVE."
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
              <TextInput
                type="number"
                inputMode="numeric"
                min={1}
                step={1}
                required
                value={form.unitsAvailable}
                onKeyDown={onUnitsKeyDown}
                onChange={(e) => onUnitsChange(e.target.value, (v) => update("unitsAvailable", v))}
              />
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
                    {b.status === "DEPLETED" && (
                      <p className="text-xs text-muted mt-1">Set units above 0 and Update to reactivate (if not expired).</p>
                    )}
                  </div>
                  <button className="btn-secondary text-sm" onClick={() => handleDelete(b.batchId)}>Delete</button>
                </div>
                <div className="flex items-center gap-2 mt-3">
                  <TextInput
                    type="number"
                    inputMode="numeric"
                    min={0}
                    step={1}
                    value={unitDrafts[b.batchId] ?? String(b.unitsAvailable)}
                    className="input max-w-[7rem]"
                    aria-label={`Units for batch ${b.batchId}`}
                    onKeyDown={onUnitsKeyDown}
                    onChange={(e) =>
                      onUnitsChange(e.target.value, (v) => {
                        setUnitDrafts((prev) => ({ ...prev, [b.batchId]: v }));
                        setRowErrors((prev) => ({ ...prev, [b.batchId]: "" }));
                      })
                    }
                  />
                  <button
                    type="button"
                    className="btn-primary text-sm"
                    onClick={() => handleUpdate(b.batchId)}
                  >
                    {b.status === "DEPLETED" ? "Update / reactivate" : "Update units"}
                  </button>
                </div>
                {rowErrors[b.batchId] && (
                  <p className="text-urgent text-sm mt-2">{rowErrors[b.batchId]}</p>
                )}
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
}
