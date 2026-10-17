# Chặt Heo Tracker v2.5 — Offline opponent threat samples

Source Android + GitHub Actions. Build with `.github/workflows/build-apk.yml`, download the artifact `ChatHeoTracker-v2.5-behavior-conditional-threats-apk`.

This research build maintains three visible fields beneath each opponent: 3 consecutive pairs, 4 consecutive pairs and 4 of a kind. Numbers are **sample frequencies, not calibrated opponent probabilities and not probabilities of chopping**. Unknown/corrupt counters display `—`, and incomplete history displays `ước lượng yếu`.

See `CHANGELOG_V2_5.md` and `RESEARCH_THREAT_INFERENCE_VI.md` for usage, research references, limitations, offline-learning data handling and test scope.
