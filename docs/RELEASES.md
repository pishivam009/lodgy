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

- **LODGY-90** — invoice auto-generation fired only on the exact billing day
  with no catch-up, so a phone asleep on the 5th skipped that month's rent and
  said nothing. Raised in UAT of 0.1.0; now bills once the day has been reached,
  dated from that day so the arrears are honest, and only ever for the period in
  progress.
- **LODGY-89** — the Home tiles. Overdue invoices now opens the Payments list
  already filtered to what is actually late and scoped to the property Home was
  showing, so the number and the list agree. A chevron marks the tiles that lead
  somewhere, so a warden can see which are buttons rather than learning it by
  tapping. Raised in UAT: the tile a warden reaches for is the one about money
  owed, and it was the one that did nothing.
- **LODGY-91 … LODGY-95** — real occupancy history. A new `OccupancyPeriod`
  record replaces the free-text note a transfer used to write, so a tenant's
  full stay history (92), a bed or property's occupancy history (93), and a
  bed's actual empty-since duration shown directly on the vacant sheet (95,
  not just the gap between two tenants) are all queries, not prose. LODGY-94
  lets the warden backfill a stay that predates the app, so history is not
  empty on day one.
- **LODGY-96, LODGY-97** — 19 screens across the app had no vertical scroll;
  Save or the last field could be unreachable on a smaller phone. LODGY-96 was
  the first one found (Agreement Terms), LODGY-97 the audit of every other
  screen with the same gap.
- **LODGY-98** — a configurable advance dues reminder (off, or 1/2/3 days
  before the due date) alongside the existing overdue nudge, so a warden can
  hear about rent before it is late, not only after.
- **LODGY-99, LODGY-100** — recovery % (collected against what was expected,
  net of credits) in the monthly report export and on the Home dashboard,
  both reading the same `totalCollected + totalDues = expectedIncome`
  identity rather than two figures that could quietly disagree.
- **LODGY-101** — a tenant can genuinely hold more than one active tenancy
  agreement (two beds in different rooms), not just as LODGY-87's
  same-tenant-different-row workaround. The profile screen and the
  bed-scoped action screens (checkout, transfer, credits, forgone rent, the
  manual invoice form) now resolve by bed rather than picking one agreement
  for the tenant.
- **LODGY-102** — "Forgot PIN?" was rendered entirely behind the numeric
  keypad on some screen sizes, with no way to reach it; it now sits in a
  fixed position above the keypad.
- **LODGY-103** — backup export was missing the selected hostel, notification
  settings and theme preference; a restore came back partly reset. All three
  now travel with the backup. The PIN deliberately still does not, and never
  auto-syncs the backup folder - both documented, deliberate exclusions.
- **LODGY-104** — closed Won't Do: the existing hourly vacancy/dues check
  was already at least as frequent as the requested 3-hour interval, so no
  change was needed. Kept on the board as a record of that decision.
- **LODGY-105** — the tenant directory list still collapsed a multi-bed
  tenant to a single row, the one call site LODGY-101's audit did not name;
  now one row per active tenancy, same as the profile screen.
- **LODGY-106** — the Import screen and the post-restore restart dialog now
  tell the warden upfront that the PIN resets on a restore while everything
  else comes back, so it does not read as a partial restore.
- **LODGY-107** — the expense form and the Monthly Report used to read the
  app's globally selected hostel silently; both now show which property is
  in play and let a multi-hostel warden change it, with no extra step for a
  single-hostel warden.
- **LODGY-108** — invoice cards in the Payments list show their due date
  (flagged when overdue), and tapping a card opens a new detail screen with
  the same figures plus Record Payment / Send Reminder / View Receipt as
  onward actions - a hub, not a duplicate of the existing receipt screen.
- **LODGY-109** — three follow-ups on LODGY-107: an expense's property is
  editable, not just set on creation; the Expenses screen defaults to a
  total across every property instead of the selected one, with a filter to
  narrow; and the Monthly Report's property picker gained an All option that
  aggregates every figure across properties.
