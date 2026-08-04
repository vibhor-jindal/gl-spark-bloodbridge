import { FormEvent, useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { donorApi } from "../api/donorApi";
import { Field, PageHeader, SelectInput, TextInput } from "../components/ui";
import { useAuth } from "../context/AuthContext";
import { BloodGroup, DonorResponse } from "../types";

const bloodGroups: BloodGroup[] = ["A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-"];

export default function DonorRegister() {
  const { user } = useAuth();
  const [form, setForm] = useState({
    name: "", bloodGroup: "" as BloodGroup | "", phone: "", email: "", city: "", latitude: "", longitude: ""
  });
  const [profile, setProfile] = useState<DonorResponse | null>(null);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);
  const [checking, setChecking] = useState(true);

  useEffect(() => {
    if (user) {
      setForm((f) => ({
        ...f,
        name: f.name || user.fullName || "",
        email: f.email || user.email || ""
      }));
    }
    donorApi
      .me()
      .then(setProfile)
      .catch(() => {})
      .finally(() => setChecking(false));
  }, [user]);

  function update(field: keyof typeof form, value: string) {
    setForm((f) => ({ ...f, [field]: value }));
  }

  function useCurrentLocation() {
    if (!navigator.geolocation) return;
    navigator.geolocation.getCurrentPosition((pos) => {
      update("latitude", String(pos.coords.latitude.toFixed(4)));
      update("longitude", String(pos.coords.longitude.toFixed(4)));
    });
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError("");
    setLoading(true);
    try {
      const result = await donorApi.register({
        name: form.name,
        bloodGroup: form.bloodGroup as BloodGroup,
        phone: form.phone.trim() ? form.phone.trim() : undefined,
        email: form.email.trim() ? form.email.trim() : undefined,
        city: form.city,
        latitude: Number(form.latitude),
        longitude: Number(form.longitude)
      });
      setProfile(result);
    } catch (err: any) {
      const status = err.response?.status;
      const msg = err.response?.data?.error || "Could not save your donor profile";
      if (status === 409) {
        try {
          setProfile(await donorApi.me());
          return;
        } catch {
          /* ignore */
        }
      }
      setError(msg);
    } finally {
      setLoading(false);
    }
  }

  if (checking) {
    return <div className="max-w-lg mx-auto px-6 py-16 text-muted">Loading profile…</div>;
  }

  if (profile) {
    return (
      <div className="max-w-lg mx-auto px-6 py-16">
        <PageHeader eyebrow="Donor profile" title="You're registered" />
        <div className="card p-6">
          <p className="text-ink mb-1"><strong>{profile.name}</strong> · {profile.bloodGroup}</p>
          <p className="text-muted text-sm mb-4">
            {profile.city}
            {profile.email ? ` · ${profile.email}` : ""}
            {profile.phone ? ` · ${profile.phone}` : ""}
          </p>
          <p className="text-sm text-muted mb-2">
            Availability:{" "}
            <strong className={profile.isAvailable ? "text-primary" : "text-urgent"}>
              {profile.isAvailable ? "Available for matching" : "Unavailable (hidden from new matches)"}
            </strong>
          </p>
          <button
            type="button"
            className="btn-secondary text-sm mb-4"
            onClick={async () => {
              setError("");
              try {
                const updated = await donorApi.updateAvailability(profile.donorId, !profile.isAvailable);
                setProfile(updated);
              } catch (err: any) {
                setError(err.response?.data?.error || "Could not update availability");
              }
            }}
          >
            {profile.isAvailable ? "Set unavailable" : "Set available again"}
          </button>
          {error && <p className="text-urgent text-sm mb-4">{error}</p>}
          <p className="text-sm text-muted mb-4">
            Compatible requests for <strong>{profile.bloodGroup}</strong> appear under Match alerts (nearby cities included).
          </p>
          <Link to="/donor/alerts" className="btn-primary inline-block">Open match alerts</Link>
        </div>
      </div>
    );
  }

  return (
    <div className="max-w-lg mx-auto px-6 py-16">
      <PageHeader eyebrow="Donor profile" title="Register as a donor" subtitle="Phone is optional. City + blood group must match emergency requests." />
      <form onSubmit={handleSubmit} className="card p-6">
        <Field label="Full name">
          <TextInput required value={form.name} onChange={(e) => update("name", e.target.value)} />
        </Field>
        <div className="grid grid-cols-2 gap-4">
          <Field label="Blood group">
            <SelectInput required value={form.bloodGroup} onChange={(e) => update("bloodGroup", e.target.value)}>
              <option value="">Select</option>
              {bloodGroups.map((bg) => <option key={bg} value={bg}>{bg}</option>)}
            </SelectInput>
          </Field>
          <Field label="Phone number (optional)">
            <TextInput value={form.phone} onChange={(e) => update("phone", e.target.value.replace(/\D/g, "").slice(0, 10))} placeholder="9876543210" />
          </Field>
        </div>
        <Field label="Email (used for match alerts)">
          <TextInput type="email" value={form.email} onChange={(e) => update("email", e.target.value)} placeholder="you@example.com" />
        </Field>
        <Field label="City">
          <TextInput required value={form.city} onChange={(e) => update("city", e.target.value)} placeholder="e.g. Delhi" />
        </Field>
        <div className="grid grid-cols-2 gap-4">
          <Field label="Latitude">
            <TextInput required value={form.latitude} onChange={(e) => update("latitude", e.target.value)} placeholder="28.6139" />
          </Field>
          <Field label="Longitude">
            <TextInput required value={form.longitude} onChange={(e) => update("longitude", e.target.value)} placeholder="77.2090" />
          </Field>
        </div>
        <button type="button" onClick={useCurrentLocation} className="text-sm text-primary font-medium mb-4">
          Use my current location
        </button>
        {error && <p className="text-urgent text-sm mb-4">{error}</p>}
        <button type="submit" disabled={loading} className="btn-primary w-full">
          {loading ? "Saving…" : "Register as donor"}
        </button>
      </form>
    </div>
  );
}
