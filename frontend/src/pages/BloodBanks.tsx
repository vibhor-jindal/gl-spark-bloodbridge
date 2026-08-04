import { FormEvent, useState } from "react";
import { Link } from "react-router-dom";
import { inventoryApi } from "../api/inventoryApi";
import { Field, PageHeader, SelectInput, TextInput } from "../components/ui";
import { BloodGroup, InventoryResponse } from "../types";

const bloodGroups: BloodGroup[] = ["A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-"];

export default function BloodBanks() {
  const [bloodGroup, setBloodGroup] = useState<BloodGroup | "">("");
  const [city, setCity] = useState("");
  const [results, setResults] = useState<InventoryResponse[]>([]);
  const [error, setError] = useState("");

  async function handleSearch(e: FormEvent) {
    e.preventDefault();
    if (!bloodGroup || !city) return;
    setError("");
    try {
      setResults(await inventoryApi.search(bloodGroup, city));
    } catch (err: any) {
      setError(err.response?.data?.error || "Search failed");
    }
  }

  return (
    <div className="max-w-3xl mx-auto px-6 py-16">
      <PageHeader
        eyebrow="Requester"
        title="Find blood bank stock"
        subtitle="Search live inventory, then open your request to reserve units and start delivery."
      />
      <form onSubmit={handleSearch} className="card p-6 mb-6">
        <div className="grid grid-cols-2 gap-4">
          <Field label="Blood group">
            <SelectInput value={bloodGroup} onChange={(e) => setBloodGroup(e.target.value as BloodGroup)} required>
              <option value="">Select</option>
              {bloodGroups.map((bg) => <option key={bg} value={bg}>{bg}</option>)}
            </SelectInput>
          </Field>
          <Field label="City">
            <TextInput value={city} onChange={(e) => setCity(e.target.value)} required />
          </Field>
        </div>
        <button type="submit" className="btn-primary w-full">Search inventory</button>
      </form>
      {error && <p className="text-urgent text-sm mb-4">{error}</p>}
      <div className="space-y-3">
        {results.map((b) => (
          <div key={b.batchId} className="card p-4 flex justify-between gap-3 items-center">
            <div>
              <p className="font-medium">{b.bloodBankName}</p>
              <p className="text-xs text-muted font-mono">
                {b.bloodGroup} · {b.unitsAvailable} units · {b.city} · exp {b.expiryDate}
              </p>
            </div>
            <Link to="/requests" className="btn-secondary text-sm">Go to my requests</Link>
          </div>
        ))}
        {results.length === 0 && <p className="text-muted text-sm">Search to see partner bank stock.</p>}
      </div>
    </div>
  );
}
