# Lodgy — Hostel Management App (Design Doc v2)

v1 was the pre-build design. v2 folds in what the warden field test and the
v2 feedback session changed, and what the build actually settled — every
epic in section 4 is implemented. Section 7 is the decision log: what
changed since v1, why, and the ticket that carries the detail.

Companion docs: [`USER-MANUAL.md`](USER-MANUAL.md) for what the app does from
the warden's side, `design/wireframes.html` and `design/wardens-guide.html`
for the screens, `SECURITY-REVIEW.md` for the security pass (LODGY-46).

## 1. Overview

Lodgy is a single-warden, offline-first Android app for managing one or more
hostels/PGs: property setup, tenant onboarding, rent collection, expenses,
and reporting. No backend, no hosting — all data lives on the phone. Data
portability is handled via manual export/import (not cloud sync).

Built for Android-only, native Kotlin.

## 2. Tech stack

| Layer | Choice |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose |
| Architecture | MVVM (ViewModel + StateFlow/UiState) |
| Local DB | Room (SQLite) |
| Async | Kotlin Coroutines + Flow |
| Navigation | Navigation Compose |
| DI | Hilt |
| Background work | WorkManager — three daily periodic workers: invoice generation, vacancy check, dues/expense nudge |
| Notifications | Local only, `NotificationCompat` on two channels (vacant rooms; payments and expenses), fired by the workers above. No push, no backend |
| PDF | Native `android.graphics.pdf.PdfDocument` behind one shared renderer (`pdf/LodgyPdfRenderer`), used by both PDF consumers |
| Preferences | DataStore for auth (PIN length), selected hostel, theme mode, notification switches |
| Theme | Material 3, light/dark/system, with shared RAG status tokens (`ui/theme/StatusColors`) that both themes resolve |
| Image storage | App-private internal storage (`filesDir`), path referenced in DB |
| Backup/restore | SAF (Storage Access Framework) — zip of DB + photos, exported to user-chosen location |
| Reminders & contact | Android Intents — `wa.me` deep link for WhatsApp, `smsto:` for SMS, `ACTION_DIAL` for calls (never `ACTION_CALL`) — all tap-to-send/tap-to-dial, no auto-send, no special permissions |
| Localization | Android resource-based i18n (`values/`, `values-hi/`), Hindi + English, in-app language switcher (not just device locale) |
| Auth | Local PIN, 4–6 digits at the warden's choice, hashed with BCrypt; AndroidX `BiometricPrompt` (fingerprint/face) as an optional faster unlock |

No network permissions requested at all. No `SEND_SMS` permission (tap-to-send only, per decision).

## 3. Data model

Entity-relationship diagrams, the full field list and the delete/cascade rules live in
[`DATA-MODEL.md`](DATA-MODEL.md). This section covers the reasoning; that file covers
the shape.

IDs are **UUID strings**, not auto-increment ints, and every entity carries
`createdAt` / `updatedAt` (epoch millis) and a `syncStatus`-ready shape —
even though there's no sync today. This is deliberate: it costs nothing now
and avoids a schema rewrite when a tenant-facing app or cloud backup shows
up later.

```
Warden (local profile — PIN/password hash, name)
  id, pinHash, name, createdAt

Hostel (any property: a hostel, or a shop/warehouse/flat let as a whole)
  id, wardenId, name, address, contactPhone,
  propertyType (HOSTEL | SHOP | WAREHOUSE | FLAT, default HOSTEL),
  createdAt, updatedAt
  — propertyType decides how much of the hierarchy below is shown, not how
    much of it exists. See 4.2.

Floor
  id, hostelId, label (e.g. "Ground", "1st"), sortOrder, createdAt, updatedAt

Room
  id, floorId, roomNumber, type (SINGLE | DOUBLE | TRIPLE), pricePerBed,
  amenities (text/tags), createdAt, updatedAt

Bed
  id, roomId, label (e.g. "A", "B", "C"), status (VACANT | OCCUPIED),
  createdAt, updatedAt

Tenant
  id, name, phone, photoPath, idProofPhotoPath, emergencyContactName,
  emergencyContactPhone, status (ACTIVE | VACATED), createdAt, updatedAt

TenancyAgreement (links tenant to a bed; one active per tenant)
  id, tenantId, bedId, agreedRent, advanceDeposit, billingCycleDay (1–28),
  moveInDate, moveOutDate (nullable), depositRefundAmount (nullable),
  nonRevenue (bool, default false), status (ACTIVE | CLOSED),
  createdAt, updatedAt
  — nonRevenue marks a room the warden or a caretaker lives in: real
    occupancy that bills nobody. See 4.3.
  — moveOutDate on an ACTIVE agreement means notice given, not departure;
    on a CLOSED one it is the actual move-out. See 4.3.
  — a bed transfer rewrites bedId on the same row rather than closing and
    reopening an agreement, so one tenancy stays one tenancy.

Invoice (auto-generated monthly per active agreement)
  id, tenancyAgreementId, periodMonth, periodYear, amountDue, dueDate,
  status (UNPAID | PARTIAL | PAID), createdAt, updatedAt

Payment
  id, invoiceId, amount, paymentMode (CASH | UPI | BANK_TRANSFER | OTHER),
  paidOn, note, multiPeriodGroupId (nullable), createdAt, updatedAt
  — a payment still belongs to exactly one invoice. One lump sum covering
    several months is written as several rows sharing a multiPeriodGroupId,
    which is what makes the arrangement visible again later (4.4).

Credit (money the tenant is owed back — e.g. a repair they paid for)
  id, tenantId, invoiceId (nullable — null means "apply to their next
  invoice"), amount, reason, createdAt, updatedAt

ReconciliationMark (the warden's own "this month matches my register")
  id, hostelId, periodMonth, periodYear, note (nullable), createdAt,
  updatedAt — unique per hostel+period

TenantNote (complaints, damages, general notes)
  id, tenantId, type (COMPLAINT | DAMAGE | GENERAL), text, photoPath (nullable),
  occurredOn (date the incident actually happened — editable, can be in the
  past), createdAt, updatedAt

Expense
  id, hostelId, category (WIFI | WATER | ELECTRICITY | TAX | MAINTENANCE | REPAIR | OTHER),
  amount, isRecurring, incurredOn, note, createdAt
```

Notes:
- Bed-level (not just room-level) occupancy is required for the vacant-room
  view to be accurate on double/triple rooms.
- `TenancyAgreement` is separate from `Tenant` so a returning tenant's
  history isn't lost across move-in/move-out cycles.
- Invoices are immutable snapshots (amount due locked at generation time),
  so later rent changes don't rewrite history. This is why a credit is its
  own row instead of a discount written back onto `amountDue`: the invoice
  stays what was billed, and the reason for the relief stays on the record.
- A reconciliation mark asserts nothing about the data — it never alters,
  hides or gates a record, and nothing is diffed automatically. It only
  records that a person looked.
- Schema is at **version 4**, with migrations 1→2 (credits), 2→3
  (reconciliation_marks) and 3→4 (`payments.multiPeriodGroupId`). Migrations
  are written by hand and tested; destructive fallback is never enabled,
  because the only copy of a warden's data is on their phone.

## 4. Feature modules

### 4.1 Auth
- Local PIN set on first launch, required on app open. The warden picks the
  length (4, 5 or 6 digits) at setup; a variable length needs an explicit
  confirm control, which the original 4-digit-only flow didn't have
  (LODGY-50).
- Optional biometric unlock (fingerprint/face, via AndroidX `BiometricPrompt`)
  as a faster alternative to typing the PIN each time. PIN remains the
  required baseline — biometrics can fail, get un-enrolled, or simply not
  exist on cheaper devices, so it's always an addition to the PIN, never a
  replacement for it.
- No account/server involved — this just gates the app on the device.

### 4.2 Property setup (static data, filled once, editable later)
- Multi-property support: property switcher on dashboard.
- Hostel → Floor → Room → Bed, with a bulk "add N rooms to this floor" flow
  to make initial setup fast.
- **Not every property is a hostel.** Wardens also let shops, warehouses and
  whole flats, where the rentable unit IS the property — there is no floor and
  no bed, and before this the warden had to invent both to rent anything at
  all. A `propertyType` on the property decides how much of the hierarchy the
  UI shows. A hostel keeps all four levels. A shop, warehouse or flat gets one
  implicit floor, one implicit unit and one implicit bed created for it at
  setup, and the warden simply never sees those layers: they see a property
  and its tenant. Tapping such a property goes straight to the unit, which
  shows the monthly rent and whether it is let, with the same one-tap assign
  or view-tenant as a bed (4.3). Floors, rooms and bulk-add are never reached
  (LODGY-79).
- **The implicit rows are real rows, not nulls.** That is the whole reason this
  was chosen over collapsing the hierarchy: every existing query, occupancy
  rollup, invoice, notification, PDF packet and CSV export keeps working
  untouched, because the shape of the data has not changed — only what is
  rendered. Tenancy stays keyed to `bedId`. The alternative, making floor a
  label and bed optional, would have touched invoicing, occupancy, the
  dashboard, the packet and the import, and needed a migration on data that
  exists only on the warden's phone. This delivers the user-visible value with
  a plain ADD COLUMN, and stays reversible if a real remodel later proves
  necessary (LODGY-79).
- Because a warden can now own a hostel and two shops, the screens that span
  them say "property" rather than "hostel"; only genuinely hostel-shaped
  concepts keep the word.
- Floor cards carry a vacant/occupied summary, and an **All rooms** view lists
  every room across floors — a warden scanning the property shouldn't have to
  open each floor in turn (LODGY-40, LODGY-41).
- All rooms spans **every hostel** and hangs off the Property tab directly,
  rather than being reachable only after picking a hostel. Drilling through a
  hostel to see rooms was the same drill-down LODGY-40 set out to remove; it
  had removed one level of it rather than two. A hostel filter narrows the
  view, each tile names the hostel its room belongs to so two properties with
  a room 101 aren't confusable, and the empty/part-full/full summary reflects
  the current filter rather than the whole estate. A warden with one hostel
  sees no filter at all (LODGY-70).
- Rooms are **RAG tiles, not a list** — green empty, amber part-full, red
  full — so occupancy is legible at a glance across the estate. Each tile
  pairs its colour with a status icon rather than relying on colour alone
  (LODGY-67).
- Room and bed views filter to vacant/occupied (and rooms to has-space/full),
  the same filter vocabulary as the vacant-beds view (LODGY-53). All rooms
  carries the hostel filter and the fill filter as two chip rows designed
  together, not as competing controls (LODGY-70, LODGY-72).
- The dashboard's **vacant-beds tile opens All rooms with the fill filter
  already applied**, showing rooms with space — wholly empty and part-full
  both qualify, since a part-full room still contains a vacant bed. The filter
  renders visibly active on arrival so the warden can see why they're looking
  at a subset, and clear it (LODGY-72).
- `VacantViewScreen` is deliberately **kept** rather than absorbed, even though
  the dashboard no longer points at it: an already-delivered long-vacancy
  notification sitting in a warden's tray still carries its route in the
  intent, so removing the destination would break notifications already on the
  phone (LODGY-72).
- Every delete and every high-impact edit confirms first, and a room whose
  bed has an active tenant can't be deleted at all. Deleting a floor cascades
  to its rooms and beds, so it says so in the prompt (LODGY-57).

### 4.3 Tenant onboarding
- Pick a vacant bed → capture tenant profile (name, phone, photo, ID proof
  photo, emergency contact) → capture agreement terms (agreed rent, advance,
  billing cycle day, move-in date).
- **Onboarding can start from the bed itself.** Tapping any bed in the bed
  grid opens a sheet rather than acting immediately, so nothing navigates on a
  stray touch of a dense grid. The sheet shows the room's description — type,
  price per bed and amenities — and then one action that depends on the bed:
  a vacant bed offers *assign a tenant*, an occupied one offers *view tenant*,
  which opens the profile where every operation on that tenancy already lives.
  Assign jumps straight into the existing flow with the bed already chosen, so
  the bed picker is skipped. Assign is never offered on an occupied bed, so no
  second tenancy can be created on one bed. A bed whose tenancy has somehow
  gone missing falls back to the vacant behaviour rather than opening a
  profile that isn't there. The Tenants-tab route through the bed picker keeps
  working unchanged (LODGY-69).
- Finishing an agreement **unwinds the whole onboarding chain** back to
  wherever it started — the tenant list via the bed picker, the bed grid via a
  bed tap. Aiming the pop at a fixed destination stranded the second route,
  whose back stack has no Tenants entry, leaving a form whose Save appeared
  dead while still writing a tenancy on every press. The save is also guarded
  against re-entry, because a bed being assignable twice is data corruption
  and shouldn't depend on navigation being right (LODGY-69).
- Tenant profile screen has quick-contact buttons: Call, WhatsApp, SMS.
  Each opens the respective app pre-filled with the tenant's number —
  dialer (`ACTION_DIAL`, not `ACTION_CALL`), WhatsApp chat (`wa.me`), SMS
  compose (`smsto:`) — and stops there. Nothing is sent or dialed
  automatically; the warden takes the final action themselves, same
  tap-to-send principle as the payment reminders in 4.4. This is a general
  contact shortcut, separate from the reminder buttons in 4.4 which are
  specifically tied to an unpaid invoice and pre-fill a payment-due
  message.
- `moveInDate` can be set in the past — onboarding a tenant who was already
  living there before the warden started using the app is a first-class
  case, not an edge case.
- An optional **dues carried forward** amount on the agreement form becomes a
  single opening invoice dated to the move-in, so a mid-tenancy onboarding
  starts with the right balance without re-entering past months (LODGY-44).
- Bed flips to OCCUPIED automatically.
- **Non-revenue rooms.** A room or bed occupied by the warden themselves or by
  a caretaker is marked with a switch on the agreement form. It is a flag on
  the tenancy (`nonRevenue`), not a separate occupancy type: that reuses the
  tenancy record so the room's history still reads normally, and keeps one
  code path through everything that touches occupancy. Turning it on hides and
  zeroes the rent, deposit and opening balance, and the save forces the rent
  to zero regardless, so the flag and the figure can never disagree. Such a
  bed counts as genuinely occupied, generates no invoices, and therefore never
  appears as dues or overdue anywhere and never triggers the long-vacancy
  nudge. Before this there was no third option: the bed had to be left vacant,
  which corrupted occupancy and invited the vacancy nudge, or set up as a real
  tenancy, which billed forever and polluted the money figures (LODGY-82).
  Recording the forgone rent as an expense is a separate, discretionary
  choice, deliberately deferred (LODGY-84).
- **Notice** is separate from checkout: setting a planned move-out date on an
  ACTIVE agreement records intent and nothing else — the bed stays occupied,
  the agreement stays active, and checkout remains an explicit action on the
  day. This is what finally gives the dashboard's "upcoming move-outs" a real
  data source (LODGY-49).
- **Transfer** moves a tenant to another bed on the same agreement row, with
  an optional rent change that applies to future invoices only. Checkout and
  re-onboard would have split one tenancy into two histories and mis-flipped
  bed states (LODGY-34).
- Wardens think room-first, name-second, so room and bed are shown next to
  the tenant everywhere a tenant is identified — directory, profile,
  invoices, payments, reminder previews (LODGY-33).

### 4.4 Rent & payments
- Invoice generation skips agreements flagged `nonRevenue` (4.3), so a warden's
  or caretaker's room never produces a due (LODGY-82).
- WorkManager job generates the month's invoice for every ACTIVE agreement
  on its billing cycle day.
- Record a payment against an invoice — full or partial; invoice status
  updates automatically (UNPAID → PARTIAL → PAID).
- Reminder button per unpaid/partial invoice: opens WhatsApp (`wa.me`) or
  SMS (`smsto:`) with a pre-filled message (tenant name, amount due, due
  date); warden reviews and taps send.
- Tenant checkout flow: close agreement, record move-out date, settle
  deposit (deduct damages if any, log refund amount), bed flips back to
  VACANT.
- Invoices/payments can also be added manually for a past period (not just
  auto-generated forward from today) — needed to backfill dues history for
  a tenant onboarded mid-tenancy, so their running balance is correct from
  day one instead of only from when the app started tracking them.
- **Credits** (4.3's repair-paid-by-tenant case): recorded with a reason and
  applied to a chosen invoice or held for the tenant's next one, which the
  invoice-generation worker attaches automatically. The invoice keeps its
  billed amount and the credit shows as its own line (LODGY-35).
- **One payment across several months**: the warden enters the lump sum once
  and it is split oldest period first, each invoice settled in full before
  the next. Anything left over after every open invoice is settled is
  dropped rather than parked on the last one — the app has no notion of an
  unallocated balance, and overpaying a month would make that invoice's own
  arithmetic wrong. The resulting rows share a `multiPeriodGroupId` and are
  badged, so the arrangement is legible months later (LODGY-42).
- **Payment acknowledgement**: any invoice exports as a one-page PDF receipt
  — tenant, room/bed, period, billed amount, credit applied, each payment
  with date and mode, and the balance (LODGY-37).
- The invoice list filters by status and period and sorts by due date or
  amount, so finding this month doesn't mean scrolling every invoice ever
  created (LODGY-54).

### 4.5 Reporting & dashboard
- Home dashboard: today's collections, count of overdue invoices, vacant
  bed count, upcoming move-outs.
- Vacant rooms/beds view, filterable by hostel/floor.
- Monthly report per hostel: total collected, total dues, occupancy %,
  income vs expense, expenses and credits for the period.
- **Occupancy is a current-state figure, and the report says so** whenever
  the period being viewed has already closed. The schema keeps current bed
  rows and agreement history, not bed-state snapshots, so a past month's
  occupancy can't be reconstructed. Resolved (LODGY-52) as
  disclose-in-place rather than a schema change: periodic snapshots would add
  write volume and a second source of truth for a number wardens read as a
  rough gauge. Revisit only if a real reporting need appears.
- **Reconciliation**: a warden can mark a month as checked against their
  paper register. Wardens keep decades on paper and don't trust that the two
  records stay in step; the mark is an attestation, not an audit — nothing is
  compared automatically (LODGY-43). The mark is set on the monthly report
  and shown in both places it matters — the report and the invoice list —
  since a warden reviewing payments needs to see which periods they have
  already checked. Because the invoice list spans every hostel, a mark is
  matched on hostel *and* period; matching on period alone would flag another
  property's invoices.
- Export the month as CSV, and see 4.8 for the printable PDF packet
  (LODGY-23, LODGY-45).

### 4.6 Tenant notes
- Per-tenant timeline: complaints, damage incidents (with optional photo),
  general notes.
- Each note has its own `occurredOn` date, separate from `createdAt` —
  lets the warden log a missed entry after the fact, or backfill history
  for a tenant who was already living there before the app was adopted.
  Timeline sorts by `occurredOn`, not by when the record was typed in.
- Notes are fully editable and deletable (hard delete — no undo/audit
  trail needed for MVP; `updatedAt` just reflects the last edit).

### 4.7 Expenses
- Log expense per hostel: category, amount, date, recurring flag, note.
- Rolls into the monthly income-vs-expense report.
- Filter by category, sort by date or amount — a hostel accumulates months of
  wifi/water rows fast (LODGY-55).
- The `isRecurring` flag the warden already sets is what drives the
  recurring-expense notification in 4.10. No pattern inference over past
  entries: the tag is the signal (LODGY-60).

### 4.8 Backup & restore (replaces cloud sync for now)
- **Export**: zips the Room DB file + the photos directory, writes it via
  SAF to a location the warden picks (e.g. Downloads, a Drive-synced
  folder, an SD card).
- **Import**: pick a previously exported zip, restore DB + photos. This is
  the explicit "warden switches phones" recovery path the user asked for —
  no server round-trip, just a file the warden manages themselves (they can
  put it in Google Drive/WhatsApp-to-self/USB manually).
- Because IDs are UUIDs, a restored DB never collides with a fresh install's
  IDs — this also happens to be what makes a future real sync feasible.
- **Printable records (PDF)**: a human-readable packet of hostels, tenants,
  agreements and invoice history, for one hostel or all of them. The zip is
  for machines; wardens said plainly that they trust paper they can read and
  hand over, and a file they can't open does not settle the fear of losing
  everything (LODGY-45).
- **Historical backfill (CSV)**: optional import of past months as
  `phone, month, year, amount_due, amount_paid`, matched to existing tenants
  by phone. Bad lines are reported with their line number rather than
  silently dropped, and unmatched phones are listed — a warden transcribing
  years of a register will get some rows wrong and needs telling which.
  Skipping it costs nothing; the app works from today onward (LODGY-44).
- **Orphaned photos** are cleaned up: a photo is written to app storage the
  moment it's picked, so an abandoned form used to leave a file nothing
  referenced, accumulating and riding along in every backup (LODGY-51).

### 4.9 Localization
- Primary users (wardens) are Hindi-speaking, so Hindi is a first-class
  language, not an afterthought bolted on later — all UI strings are
  externalized to resources from the start (`values-hi/strings.xml`),
  never hardcoded, so no screen has to be retrofitted.
- In-app language switcher (Settings), independent of device system
  locale — many phones stay on an English system locale even when the
  owner prefers Hindi in-app, so relying on device locale alone would
  under-serve the actual users.
- **First launch defaults to English** (LODGY-58). v1 shipped Hindi as the
  default on the "primary users are Hindi-speaking" requirement; the Product
  Owner reversed that afterwards. Only the unset-default changes — the
  switcher is untouched, and an install that already chose Hindi keeps it.
- WhatsApp/SMS reminder templates (4.4) are also available in Hindi and
  English — the reminder goes out under the warden's identity, so the
  language should match who they're texting, not just the app's own UI
  language. Picked per reminder on the send screen, where the warden sees the
  exact text first; there is no stored default, since the right language
  varies by tenant rather than by warden.
- Tenant-entered data (names, notes, addresses) is stored exactly as
  typed, in whatever script/language the warden enters — Compose text
  fields already support Devanagari input via the system IME, so this
  needs no special handling. Only the app's own UI chrome (labels,
  buttons, generated invoice/report text) is translated.
- Numbers/dates/currency: use locale-aware formatting (`NumberFormat`,
  date formatters) so amounts and dates render correctly under either
  language setting.

### 4.10 Notifications (added in v2)
Local notifications only — no push, no backend, same WorkManager pattern the
invoice generator already established. Two channels, each with its own switch
in More → Notifications, and both workers scheduled unconditionally so
turning a category off just makes its next run a no-op.
- **Long-vacant beds** (LODGY-59): a nudge to advertise a bed empty past a
  warden-set threshold (default 7 days, 1–90). Each bed is nudged once and
  remembered, so a month-empty room produces one notification rather than
  thirty; the memory is pruned to beds that are still vacant, so a bed that
  fills and later empties again counts as new — which is exactly when the
  warden wants to hear about it.
- **Payments and expenses** (LODGY-60): rent overdue once a due date has
  fully passed, and a recurring expense three days before its usual day of
  the month (clamped to the last day for a "31st" expense in February).
- Tapping a notification routes into the app behind the PIN gate, and the
  route is replayed after unlock rather than dropped.

### 4.11 Appearance (added in v2)
- **Status colours as shared tokens** (LODGY-36): a RAG system — red for
  unpaid/overdue, amber for partial, green for paid/vacant/active — rather
  than an invented colour language. Landed before dark mode deliberately, so
  dark mode adjusts these tokens instead of the two drifting apart.
- **Dark mode** (LODGY-32): light, dark or system, independent of the
  language setting. Asked for by wardens working in dim common areas at
  night.
- **Icons alongside status text** (LODGY-62): icon plus colour, never colour
  alone, with text kept as the secondary cue — a symbol reads the same in
  Hindi and English and shrinks what has to fit in both.
- **Slide transitions** (LODGY-61): forward slides in from the right, back
  reverses, so navigation reads as a stack rather than a cut.
- **Version and build number** on the More screen, for support questions
  (LODGY-38).

### 4.12 UI invariants

Three rules that hold app-wide. Each is here because breaking it produced a
bug that looked like a feature fault rather than a layout or plumbing one,
and each is invisible on the screen that introduces it.

- **The theme paints its own ground.** `LodgyTheme` wraps its content in a
  `Surface` using the scheme's background colour. Without it nothing paints a
  themed background and `LocalContentColor` is never set, so any screen not
  inside a `Scaffold` inherits the static window background — which is how
  the PIN lock screen ended up light-backed with near-black digits in dark
  mode while every other screen looked fine (LODGY-32).
- **Screen content scrolls.** A screen whose content can grow — a profile
  that gains actions, a form that gains fields — is scrollable, or the
  content past the fold is unreachable rather than merely off-screen. The
  tenant profile clipped Record a credit, Move to another bed and Checkout on
  an ordinary phone, which read as three separate missing features
  (LODGY-34, LODGY-35, and a regression against LODGY-14).
- **Confirm what cannot be seen, not every edit.** A destructive action always
  confirms. An *update* confirms only when it silently changes money, occupancy
  or history that the warden cannot see on the screen they are on — a room's
  price per bed, an agreed rent, an invoice amount with payments already against
  it. Renaming a hostel or fixing a phone number does not confirm, and that is
  deliberate: a dialog on every edit teaches wardens to dismiss dialogs without
  reading them, which costs exactly the ones that matter (LODGY-57 AC5, upheld
  under LODGY-65).

- **Derive UI state from observed queries, not one-shot reads.** A value read
  once in a ViewModel's `init` cannot notice later writes. Room's `Flow`
  queries are the source of truth, and a screen that shows data from a table
  it doesn't observe will go stale. Tenant room/bed was resolved by a single
  read, so it was blank after onboarding (the agreement is written after the
  tenant row) and stale after a transfer (which touches only
  `tenancy_agreements`) — correcting itself only when the ViewModel happened
  to be recreated (LODGY-33). The same rule is why a write should not also
  optimistically set the state it just persisted: that is a second source of
  truth waiting to disagree.

### 4.13 What "delete" means

Delete is **permanent**. There is no deleted flag, no recycle bin and no undo
screen — considered under LODGY-66 and declined.

Soft delete was rejected on cost, not on principle. Every read in the app would
have to filter, across fifteen DAOs including the joins and aggregates behind
occupancy, the dashboard, the monthly report and the PDF exports; a single
missed clause silently inflates a total, which is a bug nobody notices. Undoing
a cascade would need its own grouping record to know what one action removed, so
it is a feature rather than a flag. A soft-deleted room keeps its number, so
recreating it collides. And it breaks the orphan-photo sweep (4.x, LODGY-51),
which would delete the photos of a tenant the bin still claims is restorable.

What protects the warden instead, in order of how much work each does:

1. **Prevention.** A delete that would take tenancies or financial history with
   it is blocked, not confirmed — the pattern room delete already uses and floor
   delete is getting (LODGY-63, LODGY-64). What stays deletable is small and
   cheap to recreate.
2. **Money is voided, never deleted.** A wrong invoice or payment is reversed
   into a visible corrected record. That is ordinary bookkeeping and it sidesteps
   undo entirely.
3. **A backup before the dangerous ones.** Cascading deletes write a backup
   first, and a daily automatic backup runs regardless (LODGY-68). This is the
   real safety net, and unlike a bin it also survives a lost phone.

If a bin is ever revisited, size it as an epic across every DAO and export path,
not as a column on a few tables.

## 5. Explicit non-goals
- No auto-sent WhatsApp/SMS (tap-to-send only — no paid API, no backend).
  The same principle governs the quick-contact buttons: `ACTION_DIAL`, never
  `ACTION_CALL`, so no call is placed and no `CALL_PHONE`/`SEND_SMS`
  permission is requested.
- No DB encryption at rest (deferred; SQLCipher can be dropped in later
  without a schema change since it wraps the same Room DB). The security
  review (LODGY-46) records this as an accepted, documented risk rather than
  an oversight, and fixed what it did find — the exported activity now
  allow-lists its notification route, and the FileProvider no longer shares
  the whole cache directory.
- No historical occupancy reconstruction — see 4.5 (LODGY-52).
- No APK-size programme: declined (LODGY-48). The APK is already small enough
  in practice and minification carries regression risk; revisit only on a
  real installability complaint. This also freed the PDF work to pick
  whichever approach was simplest to build correctly.
- No tenant-facing app or login (Phase 3+).
- No AI insights (Phase 3+ — trend/defaulter prediction, expense anomalies;
  on-device only, to preserve the offline/local-data constraint, unless the
  user opts into a cloud AI feature explicitly later).

## 6. Future roadmap (not now, but designed for)
1. **DB encryption** — swap Room's SQLite driver for SQLCipher; no entity
   changes needed.
2. **Tenant-facing app** — tenant login, view dues, pay via UPI deep link,
   raise complaints, see notices. Requires a real backend + sync at that
   point (local-only breaks down once two people need to see live shared
   state) — UUIDs and `updatedAt` fields already in place make this a sync
   layer addition, not a schema migration.
3. **AI insights** — payment-default risk, expense anomaly detection,
   occupancy optimization suggestions.
4. **Multi-staff per hostel** — more than one warden/admin login per
   property.
5. **Notice board / announcements** — meaningful once there's a tenant app
   to display them in; until then, WhatsApp broadcast covers it.
6. **Historical bed-state snapshots** — the only route to a true past-period
   occupancy figure (4.5). Considered and parked, not forgotten.

## 7. Decision log since v1

What the warden field test, the v2 feedback session and the build itself
changed. The ticket holds the full argument; this is the shape of it.

| Change | Why | Ticket |
|---|---|---|
| Credits became their own entity, not an edit to `amountDue` | Shrinking an invoice loses the reason for the relief and breaks "every number traces to a record" | LODGY-35 |
| One payment can settle several months, oldest first | Tenants delay a month and pay two together; a payment is still one-invoice, so the split is several rows sharing a group id | LODGY-42 |
| Bed transfer edits the agreement instead of close-and-reopen | Checkout + re-onboard split one tenancy into two histories and mis-flipped bed states | LODGY-34 |
| Notice date separated from checkout | The dashboard's "upcoming move-outs" had no data source: nothing could produce an active agreement with a future move-out | LODGY-49 |
| Room/bed shown wherever a tenant is named | Wardens think room-first, name-second — the reverse of how v1 presented them | LODGY-33 |
| Opening balance and CSV backfill instead of full history entry | Wardens flatly won't retype years of a paper register; seed the balance, don't reconstruct the ledger | LODGY-44 |
| Reconciliation marks | Decades of paper stay authoritative; wardens wanted to record what they'd checked, not have the app claim to check it | LODGY-43 |
| Printable PDF packet alongside the backup zip | A zip a warden can't open doesn't settle the fear of losing everything | LODGY-45, LODGY-56 |
| Per-invoice PDF receipt | Something a tenant can be handed or sent, distinct from a month's report | LODGY-37 |
| Local notifications for vacancy, dues, recurring expenses | Wardens shouldn't have to remember to open the app; nudge once per bed, never daily | LODGY-59, LODGY-60 |
| RAG status colours, then dark mode, then status icons | Status was hard to read at a glance and worse at night; tokens first so the three don't drift | LODGY-36, LODGY-32, LODGY-62 |
| Confirmation on every delete and high-impact edit | A room deleted with no prompt at all, hit directly in review | LODGY-57 |
| Filters and sorting on tenants, rooms/beds, invoices, expenses | Long-running hostels bury today's work under years of history | LODGY-39, LODGY-53, LODGY-54, LODGY-55 |
| First-launch language reversed to English | Product decision reversal on the original "primary users are Hindi-speaking" requirement | LODGY-58 |
| Configurable 4–6 digit PIN | v1 fixed it at 4 because nothing specified a length | LODGY-50 |
| Orphaned photo cleanup | Photos are written on pick, so abandoned forms leaked files into every backup | LODGY-51 |
| Occupancy stays a current-state figure, disclosed in the UI | Snapshots would add a second source of truth for a number read as a rough gauge | LODGY-52 |
| APK-size work declined | Already small enough; minification risk not worth it, and it was constraining the PDF choice | LODGY-48 |
| Soft delete declined; delete stays permanent | Filtering every read across 15 DAOs to buy an undo a backup already provides — see 4.13 | LODGY-66 |
| Confirmations on destructive actions and invisible-change updates only | A dialog on every edit trains wardens to dismiss dialogs unread | LODGY-65 |
| Theme wraps content in a `Surface` | Without it no themed background is painted, so Scaffold-less screens break in dark mode only — see 4.12 | LODGY-32 |
| Growable screens scroll | Clipped content reads as a missing feature, not a layout fault — see 4.12 | LODGY-34, LODGY-35 |
| UI state comes from observed queries | A one-shot read in `init` cannot see later writes, so labels went stale — see 4.12 | LODGY-33 |
| Reconciliation marks match on hostel *and* period | The invoice list spans every property; period alone would flag the wrong hostel's invoices | LODGY-43 |
| `moveOutDate` is read together with agreement status | The field means notice on an ACTIVE agreement and departure on a CLOSED one (3); the printable packet read it without the status and told the warden a current resident had left | LODGY-45 |
| All rooms spans every hostel and moved to the Property tab | Drilling through a hostel to list rooms was the same drill-down LODGY-40 removed — it had removed one level of two | LODGY-70 |
| Vacant-beds tile opens All rooms filtered, VacantView kept | Notifications already delivered to a warden's phone still route to it, so it can't just be deleted | LODGY-72 |
| A bed tap opens a sheet, never an immediate navigation | Bed grids are dense; a stray touch shouldn't move the warden somewhere | LODGY-69 |
| Onboarding pops the chain, not a fixed destination | Aiming at Tenants stranded the bed-tap route, whose back stack has no Tenants entry — Save looked dead but wrote a tenancy per press | LODGY-69 |
| Warden/caretaker rooms are a flag on the tenancy, not an occupancy type | Reuses the tenancy record so history reads normally and one code path covers occupancy; a separate type would fork it | LODGY-82 |
| Non-revenue hides rent and deposit rather than only clearing them | Left editable, the warden could flip the switch and then type a rent, saving a tenancy claiming rent it would never charge | LODGY-82 |
| Property type decides which layers the UI shows, not which exist | Wardens let shops, warehouses and flats too, where the property IS the unit; collapsing the hierarchy would have touched invoicing, occupancy, the packet and the import, and needed a migration on data that lives only on the phone | LODGY-79 |
