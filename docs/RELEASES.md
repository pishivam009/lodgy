# Releases

Every ticket carries a `version` field naming the release it ships in, and this
file is the list those versions refer to. The app's own `versionName` in
`app/build.gradle.kts` matches the current release, so the "Lodgy vX.Y.Z (build N)"
line on the More screen (LODGY-38) tells you exactly which of these a warden has.

Versions are **waves of delivered work**, not calendar dates. A wave closes when
the app is coherent enough to hand to the warden again: the first working app,
then each round of what they asked for after using it. Numbering stays below 1.0
until the app has run a full financial year on a real phone without a data loss —
the thing 1.0 would be claiming.

## Keeping this list

- A new ticket gets its `version` when it is created, set to the release
  currently open (the highest one below).
- The release closes when its last ticket reaches Delivered. Bump
  `versionCode`/`versionName` in `app/build.gradle.kts` in the same commit that
  closes it, and open the next section here.
- A ticket that slips does not silently move: change its `version`, and say in
  its history why it moved, so the list stays a record of what actually shipped.
- `Won't Do` tickets keep the version they were scoped into. They record a
  decision taken in that wave.

## 0.1.0 (build 1) — the first working app

**LODGY-1 … LODGY-30.** Everything needed to run a hostel end to end, offline:
project scaffold and Room schema, PIN and biometric lock, the property hierarchy
(hostel → floor → room → bed), tenant profiles and tenancy agreements, bed
assignment and checkout, monthly invoice generation and manual/backdated entry,
payment recording, SMS reminders, the home dashboard, vacancy views, monthly
reports with PDF/CSV export, tenant notes and timeline, expenses, zip
export/import for moving phones, and Hindi alongside English.

## 0.2.0 (build 2) — what the warden asked for after using it

**LODGY-31 … LODGY-45.** The first feedback wave, mostly about finding things:
quick call/WhatsApp/SMS, dark mode, room and bed shown next to every tenant,
room/bed transfers, rent credits for tenant-paid repairs, clearer occupancy
colours, single-invoice PDF receipts, the app version on screen, sort and filter
on the tenant directory, all-rooms without drilling through floors, floor
occupancy summaries, flagging odd multi-month payments, reconciliation against
paper registers, lightweight historical backfill, and the printable data packet.

## 0.3.0 (build 3) — hardening

**LODGY-46 … LODGY-62.** Less new surface, more trust: a security review pass and
its fixes, higher automated coverage, planned move-out dates, configurable PIN
length, orphaned photo cleanup, historical occupancy, filters on the room, invoice
and expense lists, a shared PDF rendering foundation, confirmation on destructive
actions, English as the first-launch default, long-vacancy nudges, local
notifications for dues and expenses, screen transitions, and status icons.
LODGY-48 (APK size) was closed Won't Do in this wave.

## 0.4.0 (build 4) — post-launch feedback and fixes — CURRENT

**LODGY-63 … LODGY-84.** What real use turned up, including two crashes and a
double-booking: delete that does not break history, RAG room tiles, daily
automatic backup with a dashboard tile, assigning a tenant by tapping a bed,
all-hostels Home and All-rooms, room amenities surfaced, notification bursts
collapsed to one summary, forgotten-PIN recovery and wrong-PIN backoff, shops and
warehouses and flats without inventing floors and beds, navigation older wardens
can actually find, warden and caretaker rooms that bill nobody, and confirmation
on the updates that silently move money.

Every ticket in this release has reached Done. The release itself stays open
until they reach Delivered — that is the warden's call after UAT, not QA's — so
`versionName` stays at 0.4.0 and the next section opens when UAT closes it.
