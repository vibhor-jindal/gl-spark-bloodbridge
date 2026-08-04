import { FormEvent, useState } from "react";
import { useNavigate } from "react-router-dom";
import { requestApi } from "../api/requestApi";
import { matchApi } from "../api/matchApi";
import { Field, PageHeader, SelectInput, TextInput } from "../components/ui";
import { BloodGroup, Urgency } from "../types";

const bloodGroups: BloodGroup[] = ["A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-"];

export default function CreateRequest() {
  const navigate = useNavigate();
  const [form, setForm] = useState({
    patientName: "", bloodGroup: "" as BloodGroup | "", unitsNeeded: "1", hospitalName: "",
    city: "", latitude: "", longitude: "", urgency: "CRITICAL" as Urgency
  });
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  function update(field: keyof typeof form, value: string) {
    setForm((f) => ({ ...f, [field]: value }));
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError("");
    setLoading(true);
    try {
      const result = await requestApi.create({
        patientName: form.patientName,
        bloodGroup: form.bloodGroup as BloodGroup,
        unitsNeeded: Number(form.unitsNeeded),
        hospitalName: form.hospitalName,
        city: form.city,
        latitude: form.latitude ? Number(form.latitude) : undefined,
        longitude: form.longitude ? Number(form.longitude) : undefined,
        urgency: form.urgency
      });
      // Always trigger matching from the UI (Kafka auto-match is a backup).
      try {
        await matchApi.triggerMatch(result.requestId);
      } catch (matchErr: any) {
        const matchMsg =
          matchErr.response?.data?.error ||
          matchErr.response?.data?.message ||
          "Matching failed — open the request and tap Find / rematch donors.";
        // Still navigate so requester can rematch; surface why alerts may be empty.
        sessionStorage.setItem(`bb-match-warn-${result.requestId}`, matchMsg);
      }
      navigate(`/requests/${result.requestId}`);
    } catch (err: any) {
      setError(err.response?.data?.error || "Could not create the request");
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="max-w-lg mx-auto px-6 py-16">
      <PageHeader eyebrow="Emergency request" title="Request blood" subtitle="Matching runs as soon as you submit (same blood group; nearby cities like Delhi NCR are included when local donors are few). Rematch anytime from the request page." />
      <form onSubmit={handleSubmit} className="card p-6">
        <Field label="Patient name">
          <TextInput required value={form.patientName} onChange={(e) => update("patientName", e.target.value)} />
        </Field>
        <div className="grid grid-cols-2 gap-4">
          <Field label="Blood group needed">
            <SelectInput required value={form.bloodGroup} onChange={(e) => update("bloodGroup", e.target.value)}>
              <option value="">Select</option>
              {bloodGroups.map((bg) => <option key={bg} value={bg}>{bg}</option>)}
            </SelectInput>
          </Field>
          <Field label="Units needed">
            <TextInput type="number" min={1} required value={form.unitsNeeded} onChange={(e) => update("unitsNeeded", e.target.value)} />
          </Field>
        </div>
        <Field label="Hospital name">
          <TextInput required value={form.hospitalName} onChange={(e) => update("hospitalName", e.target.value)} />
        </Field>
        <Field label="City">
          <TextInput required value={form.city} onChange={(e) => update("city", e.target.value)} placeholder="e.g. Delhi — same as donor city" />
        </Field>
        <div className="grid grid-cols-2 gap-4">
          <Field label="Hospital latitude (optional)">
            <TextInput value={form.latitude} onChange={(e) => update("latitude", e.target.value)} placeholder="28.6129" />
          </Field>
          <Field label="Hospital longitude (optional)">
            <TextInput value={form.longitude} onChange={(e) => update("longitude", e.target.value)} placeholder="77.2295" />
          </Field>
        </div>
        <Field label="Urgency">
          <SelectInput value={form.urgency} onChange={(e) => update("urgency", e.target.value)}>
            <option value="CRITICAL">Critical — life-threatening, immediate</option>
            <option value="HIGH">High — needed today</option>
            <option value="NORMAL">Normal — planned procedure</option>
          </SelectInput>
        </Field>
        {error && <p className="text-urgent text-sm mb-4">{error}</p>}
        <button type="submit" disabled={loading} className="btn-urgent w-full">
          {loading ? "Submitting…" : "Submit request"}
        </button>
      </form>
    </div>
  );
}
