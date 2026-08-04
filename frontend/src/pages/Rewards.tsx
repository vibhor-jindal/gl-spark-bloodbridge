import { FormEvent, useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { rewardsApi } from "../api/rewardsApi";
import { donorApi } from "../api/donorApi";
import { Field, PageHeader, TextInput } from "../components/ui";
import { LeaderboardEntry, RewardProfileResponse } from "../types";

export default function Rewards() {
  const [profile, setProfile] = useState<RewardProfileResponse | null>(null);
  const [city, setCity] = useState("");
  const [leaderboard, setLeaderboard] = useState<LeaderboardEntry[]>([]);
  const [error, setError] = useState("");
  const [donorName, setDonorName] = useState("");

  useEffect(() => {
    donorApi
      .me()
      .then(async (donor) => {
        setDonorName(donor.name);
        const p = await rewardsApi.profile(donor.donorId);
        const resolved = {
          ...p,
          city: p.city || donor.city,
          donorName: p.donorName || donor.name
        };
        setProfile(resolved);
        const c = resolved.city || "";
        if (c) {
          setCity(c);
          setLeaderboard(await rewardsApi.leaderboard(c));
        }
      })
      .catch((err) => {
        setError(err.response?.data?.error || "Register as a donor first to see rewards.");
      });
  }, []);

  async function handleLeaderboardSearch(e: FormEvent) {
    e.preventDefault();
    if (!city) return;
    const data = await rewardsApi.leaderboard(city);
    setLeaderboard(data);
  }

  return (
    <div className="max-w-3xl mx-auto px-6 py-16">
      <PageHeader
        eyebrow="Recognition"
        title="Rewards"
        subtitle="Points and badges unlock when a donation is fulfilled (OTP confirmed)."
      />

      {error && (
        <p className="text-urgent text-sm mb-4">
          {error}{" "}
          <Link to="/donor/register" className="text-primary font-medium">Create donor profile</Link>
        </p>
      )}

      {profile && (
        <div className="card p-6 mb-8 flex items-center justify-between gap-4">
          <div>
            <p className="text-3xl font-display font-semibold text-ink">{profile.totalPoints} pts</p>
            <p className="text-sm text-muted">
              {profile.donationCount} donation(s) fulfilled · {profile.donorName || donorName || "Donor"}
            </p>
            {profile.totalPoints === 0 && (
              <p className="text-sm text-muted mt-2">
                No points yet — accept a match, complete delivery, and wait for the requester to confirm OTP.
              </p>
            )}
          </div>
          <div className="flex gap-2 flex-wrap justify-end max-w-xs">
            {profile.badges.length === 0 && <span className="text-sm text-muted">No badges yet</span>}
            {profile.badges.map((b) => (
              <span key={b} className="bg-primary-light text-primary-dark text-xs font-medium px-3 py-1 rounded-full">{b}</span>
            ))}
          </div>
        </div>
      )}

      <h2 className="font-display text-xl mb-4">City leaderboard</h2>
      <form onSubmit={handleLeaderboardSearch} className="flex gap-3 mb-4">
        <Field label="">
          <TextInput value={city} onChange={(e) => setCity(e.target.value)} placeholder="City" />
        </Field>
        <button type="submit" className="btn-secondary h-10">View</button>
      </form>

      <div className="card divide-y divide-border">
        {leaderboard.length === 0 && <p className="p-4 text-muted text-sm">No ranked donors yet for this city.</p>}
        {leaderboard.map((entry) => (
          <div key={entry.donorId} className="flex items-center justify-between p-4">
            <div className="flex items-center gap-3">
              <span className="font-mono text-muted w-6">#{entry.rank}</span>
              <span className="text-ink">{entry.donorName || `Donor #${entry.donorId}`}</span>
            </div>
            <span className="font-mono text-sm text-muted">{entry.totalPoints} pts · {entry.donationCount} donations</span>
          </div>
        ))}
      </div>
    </div>
  );
}
