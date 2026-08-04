import { Link } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import PulseLine from "../components/PulseLine";

const FLOW = [
  { step: "01", title: "Raise a request", body: "Patients and hospitals submit blood need, location, and urgency." },
  { step: "02", title: "Alert donors & banks", body: "Nearby donors and blood banks get email + in-app alerts instantly." },
  { step: "03", title: "Confirm availability", body: "A donor accepts, or a bank reserves units from live inventory." },
  { step: "04", title: "Deliver & OTP", body: "Delivery starts, requester receives an email OTP, and confirms receipt." }
];

export default function Home() {
  const { user } = useAuth();

  return (
    <div>
      <section className="relative overflow-hidden border-b border-border">
        <div className="absolute inset-0 bg-[radial-gradient(ellipse_at_top_right,_rgba(11,110,79,0.12),_transparent_55%),radial-gradient(ellipse_at_bottom_left,_rgba(214,34,76,0.08),_transparent_50%)]" />
        <div className="relative max-w-6xl mx-auto px-6 py-20 md:py-28">
          <p className="text-urgent font-mono text-xs tracking-[0.2em] uppercase mb-5">Emergency blood logistics</p>
          <h1 className="font-display text-5xl md:text-6xl font-semibold text-ink leading-[1.05] max-w-3xl mb-5">
            BloodBridge
          </h1>
          <PulseLine className="w-72 h-10 mb-6 text-primary" animated />
          <p className="text-lg text-muted max-w-2xl mb-10">
            From emergency request to OTP-confirmed delivery — donors, blood banks, and admins
            stay connected on one live platform. No SMS spam. Email alerts and a clear audit trail.
          </p>

          {!user && (
            <div className="flex flex-wrap gap-3">
              <Link to="/register" className="btn-primary">Create account</Link>
              <Link to="/login" className="btn-secondary">Log in</Link>
            </div>
          )}
          {user?.role === "REQUESTER" && (
            <Link to="/requests/new" className="btn-urgent">Raise emergency request</Link>
          )}
          {user?.role === "DONOR" && (
            <div className="flex flex-wrap gap-3">
              <Link to="/donor/alerts" className="btn-primary">View match alerts</Link>
              <Link to="/donor/register" className="btn-secondary">Donor profile</Link>
            </div>
          )}
          {user?.role === "BLOOD_BANK" && (
            <div className="flex flex-wrap gap-3">
              <Link to="/bank/portal" className="btn-primary">Open bank portal</Link>
              <Link to="/bank/requests" className="btn-secondary">Open requests</Link>
            </div>
          )}
          {user?.role === "ADMIN" && (
            <Link to="/admin/dashboard" className="btn-primary">Admin analytics</Link>
          )}
        </div>
      </section>

      <section className="max-w-6xl mx-auto px-6 py-16">
        <h2 className="font-display text-3xl mb-2">How a request moves</h2>
        <p className="text-muted mb-10 max-w-2xl">One path for every role — everything driven by live backend APIs.</p>
        <div className="grid md:grid-cols-4 gap-6">
          {FLOW.map((item) => (
            <div key={item.step} className="border-t-2 border-primary/40 pt-4">
              <p className="font-mono text-xs text-muted mb-2">{item.step}</p>
              <h3 className="font-display text-xl mb-2">{item.title}</h3>
              <p className="text-sm text-muted leading-relaxed">{item.body}</p>
            </div>
          ))}
        </div>
      </section>
    </div>
  );
}
