# CI

| Workflow | Trigger | What it does |
|---|---|---|
| `build.yml` | push to `main` | Assembles the debug APK, runs unit tests, writes `ci/last-build.txt` and commits `dist/range-debug.apk` back to the branch. |
| `smoke.yml` | push touching `app/src/main/**` | Boots an API 34 emulator, installs the app, walks through onboarding → home → explore → detail → settings in both themes, and commits the screenshots to `ci/shots/`. |

Both push back with `[skip ci]` so they never trigger themselves.
