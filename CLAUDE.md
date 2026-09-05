# CLAUDE.md

Lodgy — offline-first native Android app (Kotlin + Jetpack Compose) for hostel/PG
management by a single warden. No backend, no network permissions; all data
lives on-device (Room/SQLite), with manual export/import for phone migration.

Design doc: `docs/DESIGN.md` — read this before making architecture or schema
decisions.

User manual: `docs/USER-MANUAL.md` — what the app does, from the warden's side.

Status: built. Every epic through LODGY-62 is implemented, and the board now runs
to LODGY-84 with post-launch feedback and fixes; the board is the source of truth
for what is still awaiting work or verification.

## Standing constraints

Requirements from the product owner, not suggestions. They apply to every change.

**Every build reports its test results and coverage.** Run
`./gradlew testDebugUnitTest jacocoTestReport jacocoCoverageVerification` and
state the numbers — tests passed, failures, line coverage — in the handoff. The
gate is 0.93 LINE and it is a gate, not a target: if it fails, the change is not
done. Do not report a build as green without having read the actual results.

**Every new build stays backward compatible.** The warden's only copy of their
data is on the phone, so a schema change ships a real migration or it does not
ship. See the emulator notes below for how to test one properly.

**A feature that has been tested is not finished until the docs match.**
Architecture and schema go in `docs/DESIGN.md` and `docs/DATA-MODEL.md`,
warden-facing behaviour in `docs/USER-MANUAL.md`, and a settled choice gets a row
in the DESIGN.md decision log. Screens that changed visibly belong in
`docs/design/screenshots.html`. Do this in the same commit as the work, not as a
follow-up — CLAUDE.md tells every session to read DESIGN.md before making
architecture decisions, so a doc that lags behind silently misleads the next
change.

## Working efficiently

These are not style preferences. Each one is a specific way earlier sessions
burned effort for no gain, and none of them trades away correctness.

**Run Gradle in the foreground.** Every build in this project finishes well
inside the tool timeout — a full `testDebugUnitTest jacocoTestReport
jacocoCoverageVerification assembleDebug` takes about two minutes. Backgrounding
it and piping through `tail`/`grep` buffers all output until exit, so the output
file reads as empty and invites polling that returns nothing. Never poll a
background task; wait for its completion notification. Never run two Gradle
invocations at once — the second blocks on the daemon lock.

**Seed the emulator through the database, drive the UI only for what is under
test.** Setting up state through forms is where sessions bleed: the emulator's
IME mangles input (`Corner` → `Corner shop`, `RentTest` → `RentTesty`), and each
mangle costs several round-trips to spot and repair. Pull the database, edit it
with `sqlite3` locally, and push it back:

```sh
adb exec-out "run-as com.lodgy.app cat databases/lodgy.db" > local.db   # also -wal and -shm
adb push local.db /data/local/tmp/x.db
adb shell "run-as com.lodgy.app sh -c 'cp /data/local/tmp/x.db databases/lodgy.db \
  && rm -f databases/lodgy.db-wal databases/lodgy.db-shm'"
```

Then exercise the actual behaviour on the device. Do keep testing on the device:
the LODGY-69 double-booking was a navigation bug that every unit test passed
through while the app wrote two tenancies onto one bed.

**Grep the UI dump, don't read it whole.** `ui.sh dump` returns 15–60 lines and
you almost always want two. Pipe it through `grep -E` by default.

**Read the exact lines before writing a patch.** A narrow `sed -n 'X,Yp'` is
cheaper than a failed `assert old in s`, which costs the read you skipped plus a
rewrite. Do not re-read a file to confirm an edit landed — Edit/Write fail loudly.

## Emulator gotchas

- **Room migrations are lazy.** They run on first real database access, not on
  install or launch. The PIN screen reads DataStore, not Room, so you must get
  past it before the migration has happened.
- **Always pull `-wal` and `-shm` alongside the database.** Without them recent
  writes are invisible and data looks missing that is not.
- **Advance the clock through the Settings UI** (`settings put global auto_time 0`
  then set the date). `date -s` and `cmd jobscheduler run` need root. Restore
  `auto_time 1` afterwards.
- **Never `fallbackToDestructiveMigration`.** The warden's only copy of their
  data is on the phone. Every schema change gets a real migration, and it is
  tested against a populated database from the previous version — pull the live
  one off the emulator rather than building a synthetic one.
