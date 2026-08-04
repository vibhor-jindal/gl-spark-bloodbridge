import { FormEvent, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { authApi } from "../api/authApi";
import { useAuth } from "../context/AuthContext";
import { Field, PageHeader, SelectInput, TextInput } from "../components/ui";
import { Role } from "../types";

export default function Register() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const [fullName, setFullName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [role, setRole] = useState<Role>("DONOR");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError("");
    setLoading(true);
    try {
      const user = await authApi.register({ fullName, email, password, role });
      login(user);
      if (role === "DONOR") navigate("/donor/register");
      else if (role === "BLOOD_BANK") navigate("/bank/portal");
      else if (role === "REQUESTER") navigate("/requests/new");
      else if (role === "ADMIN") navigate("/admin/dashboard");
      else navigate("/");
    } catch (err: any) {
      setError(err.response?.data?.error || "Could not create your account");
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="max-w-md mx-auto px-6 py-16">
      <PageHeader eyebrow="Join BloodBridge" title="Create your account" />
      <form onSubmit={handleSubmit} className="card p-6">
        <Field label="Full name">
          <TextInput required value={fullName} onChange={(e) => setFullName(e.target.value)} />
        </Field>
        <Field label="Email">
          <TextInput type="email" required value={email} onChange={(e) => setEmail(e.target.value)} />
        </Field>
        <Field label="Password">
          <TextInput type="password" required minLength={8} value={password} onChange={(e) => setPassword(e.target.value)} />
        </Field>
        <Field label="I am a...">
          <SelectInput value={role} onChange={(e) => setRole(e.target.value as Role)}>
            <option value="DONOR">Blood Donor</option>
            <option value="REQUESTER">Requester (patient / hospital / NGO)</option>
            <option value="BLOOD_BANK">Blood Bank</option>
            <option value="ADMIN">Platform Admin</option>
          </SelectInput>
        </Field>
        {error && <p className="text-urgent text-sm mb-4">{error}</p>}
        <button type="submit" disabled={loading} className="btn-primary w-full">
          {loading ? "Creating account…" : "Create account"}
        </button>
      </form>
      <p className="text-sm text-muted mt-4">
        Already registered? <Link to="/login" className="text-primary font-medium">Log in</Link>
      </p>
    </div>
  );
}
