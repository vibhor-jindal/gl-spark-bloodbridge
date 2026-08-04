import { FormEvent, useEffect, useState } from "react";
import { analyticsApi } from "../api/analyticsApi";
import { Field, PageHeader, TextInput } from "../components/ui";
import { DashboardResponse } from "../types";

export default function AdminDashboard() {
  const [city, setCity] = useState("");
  const [data, setData] = useState<DashboardResponse | null>(null);

  async function load() {
    const result = await analyticsApi.dashboard(city || undefined);
    setData(result);
  }

  useEffect(() => {
    load();
  }, []);

  function handleFilter(e: FormEvent) {
    e.preventDefault();
    load();
  }

  return (
    <div className="max-w-4xl mx-auto px-6 py-16">
      <PageHeader eyebrow="Admin" title="Analytics Dashboard" subtitle="Platform-wide request and fulfillment trends." />

      <form onSubmit={handleFilter} className="flex items-end gap-3 mb-8">
        <div className="flex-1">
          <Field label="Filter by city (optional)">
            <TextInput value={city} onChange={(e) => setCity(e.target.value)} placeholder="e.g. Delhi" />
          </Field>
        </div>
        <button type="submit" className="btn-secondary mb-4">Apply</button>
        <a href={analyticsApi.exportUrl(city || undefined)} className="btn-primary mb-4">Export CSV</a>
      </form>

      {data && (
        <>
          <div className="grid grid-cols-4 gap-4 mb-8">
            <Stat label="Total requests" value={data.totalRequests} />
            <Stat label="Fulfilled / confirmed" value={data.fulfilledOrConfirmedCount} />
            <Stat label="Fulfillment rate" value={`${data.fulfillmentRatePercent}%`} />
            <Stat label="Avg. match time" value={data.averageMatchTimeSeconds != null ? `${Math.round(data.averageMatchTimeSeconds)}s` : "—"} />
          </div>

          <div className="grid grid-cols-2 gap-6">
            <div className="card p-5">
              <h3 className="font-display text-lg mb-3">By blood group</h3>
              {Object.entries(data.requestsByBloodGroup).map(([k, v]) => (
                <div key={k} className="flex justify-between text-sm py-1 border-b border-border last:border-0">
                  <span>{k}</span><span className="font-mono">{v}</span>
                </div>
              ))}
            </div>
            <div className="card p-5">
              <h3 className="font-display text-lg mb-3">By status</h3>
              {Object.entries(data.requestsByStatus).map(([k, v]) => (
                <div key={k} className="flex justify-between text-sm py-1 border-b border-border last:border-0">
                  <span>{k}</span><span className="font-mono">{v}</span>
                </div>
              ))}
            </div>
          </div>
        </>
      )}
    </div>
  );
}

function Stat({ label, value }: { label: string; value: string | number }) {
  return (
    <div className="card p-4">
      <p className="text-2xl font-display font-semibold text-ink">{value}</p>
      <p className="text-xs text-muted mt-1">{label}</p>
    </div>
  );
}
