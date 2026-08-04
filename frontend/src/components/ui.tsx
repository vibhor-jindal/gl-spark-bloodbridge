import { InputHTMLAttributes, ReactNode, SelectHTMLAttributes } from "react";

export function Field({ label, error, children }: { label: string; error?: string; children: ReactNode }) {
  return (
    <div className="mb-4">
      <label className="label">{label}</label>
      {children}
      {error && <p className="text-urgent text-sm mt-1">{error}</p>}
    </div>
  );
}

export function TextInput(props: InputHTMLAttributes<HTMLInputElement>) {
  return <input {...props} className={`input ${props.className || ""}`} />;
}

export function SelectInput(props: SelectHTMLAttributes<HTMLSelectElement>) {
  return <select {...props} className={`input ${props.className || ""}`} />;
}

export function PageHeader({ eyebrow, title, subtitle }: { eyebrow?: string; title: string; subtitle?: string }) {
  return (
    <div className="mb-8">
      {eyebrow && <p className="text-urgent font-mono text-xs tracking-wide uppercase mb-2">{eyebrow}</p>}
      <h1 className="text-3xl font-semibold text-ink">{title}</h1>
      {subtitle && <p className="text-muted mt-2">{subtitle}</p>}
    </div>
  );
}
