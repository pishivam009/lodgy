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
- The release closes when its last ticket reaches Delivered. Record that here
  and open the next section. Do **not** bump `versionCode`/`versionName` at that
  moment: the build carrying that work *is* the closing version, and bumping on
  closure would label it as the next one while containing nothing new. The bump
  belongs in the first commit that builds a ticket of the next release.
- A ticket that slips does not silently move: change its `version`, and say in
  its history why it moved, so the list stays a record of what actually shipped.
- `Won't Do` tickets keep the version they were scoped into. They record a
  decision taken in that wave.

## 0.1.0 (build 1) — the first working app — DELIVERED

Accepted under UAT on 7 Sep 2026, together with 0.2.0 and 0.3.0 — the three
releases had shipped without a user-acceptance pass, and that sweep is what
turned up LODGY-90.

**LODGY-1 … LODGY-30.** Everything needed to run a hostel end to end, offline:
project scaffold and Room schema, PIN and biometric lock, the property hierarchy
(hostel → floor → room → bed), tenant profiles and tenancy agreements, bed
assignment and checkout, monthly invoice generation and manual/backdated entry,
payment recording, SMS reminders, the home dashboard, vacancy views, monthly
reports with PDF/CSV export, tenant notes and timeline, expenses, zip
export/import for moving phones, and Hindi alongside English.

## 0.2.0 (build 2) — what the warden asked for after using it — DELIVERED

**LODGY-31 … LODGY-45.** The first feedback wave, mostly about finding things:
quick call/WhatsApp/SMS, dark mode, room and bed shown next to every tenant,
room/bed transfers, rent credits for tenant-paid repairs, clearer occupancy
colours, single-invoice PDF receipts, the app version on screen, sort and filter
on the tenant directory, all-rooms without drilling through floors, floor
occupancy summaries, flagging odd multi-month payments, reconciliation against
paper registers, lightweight historical backfill, and the printable data packet.

## 0.3.0 (build 3) — hardening — DELIVERED

**LODGY-46 … LODGY-62.** Less new surface, more trust: a security review pass and
its fixes, higher automated coverage, planned move-out dates, configurable PIN
length, orphaned photo cleanup, historical occupancy, filters on the room, invoice
and expense lists, a shared PDF rendering foundation, confirmation on destructive
actions, English as the first-launch default, long-vacancy nudges, local
notifications for dues and expenses, screen transitions, and status icons.
LODGY-48 (APK size) was closed Won't Do in this wave.

## 0.4.0 (build 4) — post-launch feedback and fixes — DELIVERED

**LODGY-63 … LODGY-84.** What real use turned up, including two crashes and a
double-booking: delete that does not break history, RAG room tiles, daily
automatic backup with a dashboard tile, assigning a tenant by tapping a bed,
all-hostels Home and All-rooms, room amenities surfaced, notification bursts
collapsed to one summary, forgotten-PIN recovery and wrong-PIN backoff, shops and
warehouses and flats without inventing floors and beds, navigation older wardens
can actually find, warden and caretaker rooms that bill nobody, and confirmation
on the updates that silently move money.

All 26 tickets reached Delivered under UAT on 6 Sep 2026, so this release is
closed. UAT bounced one of them — LODGY-68's dashboard tile told the warden to
tap text that was not a tap target — and it was fixed, re-tested and accepted
within the release rather than deferred.

## 0.5.0 (build 5) — open — CURRENT

- **LODGY-90** — invoice auto-generation fires only on the exact billing day
  with no catch-up, so a phone asleep on the 5th skips that month's rent and
  says nothing. Raised in UAT of 0.1.0; open.
- **LODGY-89** — the Home tiles. Overdue invoices now opens the Payments list
  already filtered to what is actually late and scoped to the property Home was
  showing, so the number and the list agree. A chevron marks the tiles that lead
  somewhere, so a warden can see which are buttons rather than learning it by
  tapping. Raised in UAT: the tile a warden reaches for is the one about money
  owed, and it was the one that did nothing.
