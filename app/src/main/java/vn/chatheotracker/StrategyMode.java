package vn.chatheotracker;

/** User-selected scoring objective. These are heuristic preferences, not calibrated rank probabilities. */
enum StrategyMode {
    FIRST("first", "Ưu tiên về nhất"),
    PLACEMENT("placement", "Xếp hạng 1–2–3–4");

    final String key;
    final String label;
    StrategyMode(String key, String label) { this.key=key; this.label=label; }
    static StrategyMode fromKey(String key) {
        return FIRST.key.equals(key) ? FIRST : PLACEMENT;
    }
}
