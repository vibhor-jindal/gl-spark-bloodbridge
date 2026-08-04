/** Format matchScore for UI — never show bare "1000" which looks like reward points. */
export function formatMatchScoreLabel(score: number | null | undefined): string {
  if (score == null || Number.isNaN(score) || score <= 0) {
    return "City match";
  }
  // Legacy sentinel when lat/lng were missing (same peak as 0 km distance score).
  if (score >= 999) {
    return "Nearby / City match";
  }
  return `Match score ${score.toFixed(1)}`;
}
