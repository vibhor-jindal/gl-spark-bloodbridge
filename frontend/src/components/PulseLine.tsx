interface PulseLineProps {
  className?: string;
  animated?: boolean;
  color?: string;
}

export default function PulseLine({ className = "", animated = false, color = "text-urgent" }: PulseLineProps) {
  return (
    <svg viewBox="0 0 240 40" className={`${className} ${color}`} preserveAspectRatio="none">
      <path
        className={`pulse-line ${animated ? "pulse-line-animated" : ""}`}
        d="M0 20 H70 L85 20 L95 4 L110 36 L122 20 L134 20 L145 8 L156 20 H240"
      />
    </svg>
  );
}
