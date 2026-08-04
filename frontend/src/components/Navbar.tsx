import { Link, useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import PulseLine from "./PulseLine";

export default function Navbar() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  function handleLogout() {
    logout();
    navigate("/login");
  }

  return (
    <header className="border-b border-border bg-card/90 backdrop-blur sticky top-0 z-20">
      <div className="max-w-6xl mx-auto px-6 py-4 flex items-center justify-between gap-4">
        <Link to="/" className="flex items-center gap-2 shrink-0">
          <span className="font-display text-xl font-semibold text-ink">BloodBridge</span>
          <PulseLine className="w-10 h-5" animated />
        </Link>

        {user ? (
          <nav className="flex items-center gap-4 text-sm font-medium flex-wrap justify-end">
            {user.role === "DONOR" && (
              <>
                <Link to="/donor/register" className="text-muted hover:text-ink">Profile</Link>
                <Link to="/donor/alerts" className="text-muted hover:text-ink">Match alerts</Link>
                <Link to="/rewards" className="text-muted hover:text-ink">Rewards</Link>
                <Link to="/notifications" className="text-muted hover:text-ink">Inbox</Link>
              </>
            )}
            {user.role === "REQUESTER" && (
              <>
                <Link to="/requests/new" className="text-muted hover:text-ink">New request</Link>
                <Link to="/requests" className="text-muted hover:text-ink">My requests</Link>
                <Link to="/banks" className="text-muted hover:text-ink">Blood banks</Link>
                <Link to="/notifications" className="text-muted hover:text-ink">Inbox</Link>
              </>
            )}
            {user.role === "BLOOD_BANK" && (
              <>
                <Link to="/bank/portal" className="text-muted hover:text-ink">Bank portal</Link>
                <Link to="/bank/requests" className="text-muted hover:text-ink">Requests</Link>
                <Link to="/notifications" className="text-muted hover:text-ink">Inbox</Link>
              </>
            )}
            {user.role === "ADMIN" && (
              <>
                <Link to="/admin/dashboard" className="text-muted hover:text-ink">Analytics</Link>
                <Link to="/admin/manage" className="text-muted hover:text-ink">Manage</Link>
              </>
            )}
            <span className="hidden sm:inline text-border">|</span>
            <span className="text-ink text-xs sm:text-sm">
              {user.fullName}
              <span className="text-muted font-mono ml-2">{user.role.replace("_", " ")}</span>
            </span>
            <button onClick={handleLogout} className="btn-secondary py-1.5 px-3 text-sm">
              Log out
            </button>
          </nav>
        ) : (
          <nav className="flex items-center gap-3">
            <Link to="/login" className="btn-secondary py-1.5 px-3 text-sm">Log in</Link>
            <Link to="/register" className="btn-primary py-1.5 px-3 text-sm">Register</Link>
          </nav>
        )}
      </div>
    </header>
  );
}
