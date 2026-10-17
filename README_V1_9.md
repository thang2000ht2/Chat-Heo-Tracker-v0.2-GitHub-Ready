# Chặt Heo Tracker 1.9 — Recover missing hands and heo warning

## Changes vs 1.8

1. **Read 13 cards after occlusions:** HandRecognizer now extracts *each* card corner independently. An obscured middle card no longer discards later corners. TemporalHandEvidence combines 3 or more consistent visual observations per position across a short sequence. A moved or differently sized fan resets the evidence. Distinct ranks/suits are still required. Before the first verified local play, a fan of 8–12 cards is not promoted as a completed initial hand.
2. **Missing history ≠ invalid 52-card deck:** A new `PartialDeckState` distinguishes missing public observations from impossible duplicated/overlapping cards. For up to 24 missing observations, the adviser runs a **low-confidence** Monte Carlo approximation that reserves these unseen cards as *unobserved/spent*, not as known opponent cards. The UI displays how many history cards are missing and marks confidence low for >12. Invalid / contradictory decks are still rejected.
3. **Independent heo warning:** On a detected local turn, the application may display `⚠ HEO: chưa loại trừ khả năng bị chặt` based on the unobserved structure, even if no action suggestion can be computed. This is only a structural warning; unknown cards do not prove anyone has a chopping combination.
4. **Tactics:** The placement objective still weights routes to finish in fewer turns, control risk, hand structure and opponent behavior. A legal immediate finish now receives additional priority. Advice remains on-device and displayed for 5 seconds.
5. **Status:** The top-left scanner label distinguishes missing public history from failed hand detection. Reset and the Android screen capture permission flows remain unchanged.

## Verify

- GitHub Actions -> Build Chặt Heo Tracker APK -> Run workflow.
- After a fresh deal, check 13 cards were actually recognized. Test chat/icon appearing across one card and whether the hand is recovered once that corner appears in 3 frames.
- If scan label says `thiếu lịch sử N`, inspect the on-screen previous-play records; that represents missing *visible history*, not an extra opponent card.
- Test heo warnings with actual 2 in hand and no confirmed complete advice.

## Known limitations

- No Android SDK is available in the current authoring environment, so this bundle was **not** built as an APK or live-tested on Xiaomi/Android 16.
- Recognizer templates are calibrated to the previously shared game UI. Card graphics, screen crop, selected/raised cards, or continuous full occlusion may still prevent recognition. Image and turn detection need on-device logs for further diagnosis.
- Simulated blocking percentages are **not empirical/calibrated win probabilities**. With missing history they are less reliable; no advice is given for a contradictory deck or more than 24 missing observations.
- Special chopping rules and betting penalties vary by game room; these must be verified in the game before financial optimization claims.
