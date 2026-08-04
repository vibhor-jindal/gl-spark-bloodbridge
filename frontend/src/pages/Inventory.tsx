import { FormEvent, useEffect, useState } from "react";
import { inventoryApi } from "../api/inventoryApi";
import { Field, PageHeader, SelectInput, TextInput } from "../components/ui";
import { BloodGroup, InventoryResponse, LowStockAlert } from "../types";

const bloodGroups: BloodGroup[] = ["A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-"];

export default function Inventory() {
  const [form, setForm] = useState({
    bloodBankName: "", city: "", bloodGroup: "" as BloodGroup | "", unitsAvailable: "",
    collectedDate: "", expiryDate: ""
  });
  const [searchGroup, setSearchGroup] = useState<BloodGroup | "">("");
  const [searchCity, setSearchCity] = useState("");
  const [results, setResults] = useState<InventoryResponse[]>([]);
  const [alerts, setAlerts] = useState<LowStockAlert[]>([]);
  const [message, setMessage] = useState("");

  useEffect(() => {
    inventoryApi.alerts().then(setAlerts).catch(() => {});
  }, []);

  function update(field: keyof typeof form, value: string) {
    setForm((f) => ({ ...f, [field]: value }));
  }

  async function handleAddStock(e: FormEvent) {
    e.preventDefault();
    setMessage("");
    try {
      await inventoryApi.add({
        bloodBankName: form.bloodBankName,
        city: form.city,
        bloodGroup: form.bloodGroup as BloodGroup,
        unitsAvailable: Number(form.unitsAvailable),
        collectedDate: form.collectedDate,
        expiryDate: form.expiryDate
      });
      setMessage("Stock added.");
      setForm({ bloodBankName: "", city: "", bloodGroup: "", unitsAvailable: "", collectedDate: "", expiryDate: "" });
    } catch (err: any) {
      setMessage(err.response?.data?.error || "Could not add stock");
    }
  }

  async function handleSearch(e: FormEvent) {
    e.preventDefault();
    if (!searchGroup || !searchCity) return;
    const data = await inventoryApi.search(searchGroup, searchCity);
    setResults(data);
  }

  return (
    <div className="max-w-4xl mx-auto px-6 py-16">
      <PageHeader eyebrow="Blood bank" title="Inventory" subtitle="Track stock levels, reservations, and expiry across partner banks." />

      {alerts.length > 0 && (
        <div className="card p-4 mb-8 border-urgent/40 bg-urgent-light">
          <p className="font-medium text-urgent-dark mb-2">Low stock alerts</p>
          <ul className="text-sm text-urgent-dark space-y-1">
            {alerts.map((a, i) => (
              <li key={i}>{a.bloodGroup} in {a.city}: only {a.availableUnits} unit(s) left (threshold {a.threshold})</li>
            ))}
          </ul>
        </div>
      )}

      <div className="grid grid-cols-2 gap-8">
        <div>
          <h2 className="font-display text-xl mb-4">Add stock</h2>
          <form onSubmit={handleAddStock} className="card p-6">
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
                <TextInput type="number" min={0} required value={form.unitsAvailable} onChange={(e) => update("unitsAvailable", e.target.value)} />
              </Field>
            </div>
            <div className="grid grid-cols-2 gap-4">
              <Field label="Collected date">
                <TextInput type="date" required value={form.collectedDate} onChange={(e) => update("collectedDate", e.target.value)} />
              </Field>
              <Field label="Expiry date">
                <TextInput type="date" required value={form.expiryDate} onChange={(e) => update("expiryDate", e.target.value)} />
              </Field>
            </div>
            {message && <p className="text-sm text-muted mb-4">{message}</p>}
            <button type="submit" className="btn-primary w-full">Add stock</button>
          </form>
        </div>

        <div>
          <h2 className="font-display text-xl mb-4">Search stock</h2>
          <form onSubmit={handleSearch} className="card p-6 mb-4">
            <div className="grid grid-cols-2 gap-4">
              <Field label="Blood group">
                <SelectInput value={searchGroup} onChange={(e) => setSearchGroup(e.target.value as BloodGroup)}>
                  <option value="">Select</option>
                  {bloodGroups.map((bg) => <option key={bg} value={bg}>{bg}</option>)}
                </SelectInput>
              </Field>
              <Field label="City">
                <TextInput value={searchCity} onChange={(e) => setSearchCity(e.target.value)} />
              </Field>
            </div>
            <button type="submit" className="btn-secondary w-full">Search</button>
          </form>

          <div className="space-y-2">
            {results.map((r) => (
              <div key={r.batchId} className="card p-3 text-sm flex justify-between">
                <span>{r.bloodBankName} — {r.bloodGroup}</span>
                <span className="font-mono text-muted">{r.unitsAvailable} units · exp {r.expiryDate}</span>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
}
