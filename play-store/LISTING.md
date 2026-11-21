# Flux Calc: Google Play submission kit

Everything you need to paste into Play Console. The graphics are in this folder.

| Play Console field | File / value |
|---|---|
| App icon (512×512) | `icon-512.png` |
| Feature graphic (1024×500) | `feature-graphic-1024x500.png` |
| Phone screenshots (2–8) | `phone-1-result.png`, `phone-2-second-functions.png`, `phone-3-history.png`, `phone-4-landscape.png` |
| App bundle | `app/build/outputs/bundle/release/app-release.aab` (after a signed build) |
| Package name | `flux_calc.app` (registered to the "Flux calculator" console app; permanent) |
| Version | versionCode 1 / versionName 1.0 (raise versionCode for every new upload) |

---

## Store listing

**App name** (max 30)
```
Flux Calc
```

**Short description** (max 80)
```
A beautiful scientific calculator with fluid animations and full history.
```

**Full description** (max 4000)
```
Flux Calc is a scientific calculator that is powerful enough for class and work, and calm and simple enough for everyday sums.

SCIENTIFIC, WITHOUT THE CLUTTER
• Trigonometry in degrees or radians: sin, cos, tan and their inverses
• Powers, roots, factorials, logarithms (ln, log, log₂) and exponentials
• Constants π and e, brackets, percentages, and Ans to reuse your last result
• A 2nd-function layer keeps advanced keys one tap away without crowding the keypad
• Turn your phone sideways for the full scientific keypad at once

SEE THE ANSWER AS YOU TYPE
The result updates live while you enter an expression, so you catch mistakes before you press equals.

YOUR HISTORY, ALWAYS AT HAND
Every calculation is saved on your device. Open the history to review past results or tap one to use it again. Long-press a result to copy it.

FLUID BY DESIGN
Soft, glassy keys with smooth animations and gentle haptic feedback make every tap feel responsive.

PRIVATE BY DEFAULT
No ads. No account. No internet permission. Your calculations never leave your phone.
```

**App category:** Tools  ·  **Tags:** Calculator, Math, Education (pick what the console offers)

**Contact email:** boomboom1995x@gmail.com (shown publicly)
**Privacy policy URL:** https://omsk1996.github.io/flux-calc-privacy/ (repo: github.com/omsk1996/flux-calc-privacy)

---

## App content (Policy → App content)

| Section | Answer |
|---|---|
| Privacy policy | URL of the hosted `privacy-policy.html` |
| Ads | **No**, the app does not contain ads |
| App access | **All functionality is available without special access** |
| Content rating | Category **Utility, Productivity, Communication or Other**; answer **No** to every question → rated Everyone / PEGI 3 |
| Target audience | **13+** (choosing under-13 brings Families-policy reviews; there is no benefit for a calculator) |
| Data safety | **No** data collected, **No** data shared. Data is not encrypted in transit (there is none). Users can't request deletion because nothing is collected (history can be cleared in the app). |
| Government app | No |
| Financial features | None |
| Health | None |
| News app | No |

---

## Release steps

1. **Testing → Closed testing** (required first for personal accounts created after 13 Nov 2023):
   create a track, upload `app-release.aab`, add **at least 12 testers** (Google Group or email list),
   share the opt-in link, and keep them opted in for **14 days in a row**. Then apply for production access
   on the Dashboard. Organisation accounts can skip this and go straight to Production.
2. Accept **Play App Signing** when uploading the first bundle (Google holds the app-signing key;
   your `upload-keystore.jks` is only the upload key and can be reset through support if lost).
3. Fill in the store listing and every App content section above.
4. **Production → Create new release**, upload the bundle, add release notes, then **Send for review**.
   First reviews usually take a few days.

**Release notes for 1.0**
```
First release of Flux Calc: scientific functions, live results, history, and a fluid new design.
```
