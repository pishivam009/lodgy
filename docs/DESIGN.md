# Lodgy — Hostel Management App (Design Doc v1)

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
| Background work | WorkManager (monthly invoice generation, due-date checks) |
| Image storage | App-private internal storage (`filesDir`), path referenced in DB |
| Backup/restore | SAF (Storage Access Framework) — zip of DB + photos, exported to user-chosen location |
| Reminders | Android Intents — `wa.me` deep link for WhatsApp, `ACTION_SENDTO`/`smsto:` for SMS — both tap-to-send, no auto-send, no special permissions |

No network permissions requested at all. No `SEND_SMS` permission (tap-to-send only, per decision).

## 3. Data model

IDs are **UUID strings**, not auto-increment ints, and every entity carries
`createdAt` / `updatedAt` (epoch millis) and a `syncStatus`-ready shape —
even though there's no sync today. This is deliberate: it costs nothing now
and avoids a schema rewrite when a tenant-facing app or cloud backup shows
up later.

```
Warden (local profile — PIN/password hash, name)
  id, pinHash, name, createdAt

Hostel
  id, wardenId, name, address, contactPhone, createdAt, updatedAt

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
  status (ACTIVE | CLOSED), createdAt, updatedAt

Invoice (auto-generated monthly per active agreement)
  id, tenancyAgreementId, periodMonth, periodYear, amountDue, dueDate,
  status (UNPAID | PARTIAL | PAID), createdAt, updatedAt

Payment
  id, invoiceId, amount, paymentMode (CASH | UPI | BANK_TRANSFER | OTHER),
  paidOn, note, createdAt

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
  so later rent changes don't rewrite history.

## 4. Feature modules

### 4.1 Auth
- Local PIN (or password) set on first launch, required on app open.
- No account/server involved — this just gates the app on the device.

### 4.2 Property setup (static data, filled once, editable later)
- Multi-hostel support: hostel switcher on dashboard.
- Hostel → Floor → Room → Bed, with a bulk "add N rooms to this floor" flow
  to make initial setup fast.

### 4.3 Tenant onboarding
- Pick a vacant bed → capture tenant profile (name, phone, photo, ID proof
  photo, emergency contact) → capture agreement terms (agreed rent, advance,
  billing cycle day, move-in date).
- `moveInDate` can be set in the past — onboarding a tenant who was already
  living there before the warden started using the app is a first-class
  case, not an edge case.
- Bed flips to OCCUPIED automatically.

### 4.4 Rent & payments
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

### 4.5 Reporting & dashboard
- Home dashboard: today's collections, count of overdue invoices, vacant
  bed count, upcoming move-outs.
- Vacant rooms/beds view, filterable by hostel/floor.
- Monthly report per hostel: total collected, total dues, occupancy %,
  income vs expense.
- Export report as PDF/CSV (Phase 2).

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

## 5. Explicit non-goals for MVP
- No auto-sent WhatsApp/SMS (tap-to-send only — no paid API, no backend).
- No DB encryption at rest (deferred; SQLCipher can be dropped in later
  without a schema change since it wraps the same Room DB).
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
