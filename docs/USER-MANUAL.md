# Lodgy — User Manual

Everything Lodgy does, in the order you'd meet it while running a hostel or PG.

Lodgy is an Android app for one warden managing one or more properties. It
runs entirely on your phone: there is no account to create, no server, and
the app asks for no internet permission at all. Nothing you type leaves the
device unless you export it yourself.

Screen-by-screen visuals live in
[`docs/design/screenshots.html`](design/screenshots.html) — real screenshots of
every screen described below, in light, dark and Hindi. Two earlier pages,
[`wardens-guide.html`](design/wardens-guide.html) and
[`wireframes.html`](design/wireframes.html), cover the same ground as a walkthrough
and as layout sketches. Open any of them in a browser.

---

## Where things live

The app has five tabs along the bottom:

| Tab | What's there |
|---|---|
| **Home** | Dashboard, vacant beds view, monthly report |
| **Property** | Hostels, floors, rooms, beds |
| **Tenants** | Tenant directory, profiles, notes, checkout |
| **Payments** | Invoices, payments, reminders, receipts |
| **More** | Expenses, backup, printable records, history import, notifications, appearance, language |

---

## 1. Getting in

- **PIN lock.** On first launch you choose a PIN and confirm it. You pick the
  length — 4, 5 or 6 digits. The PIN is required every time the app opens.
- **Fingerprint unlock (optional).** If your phone has a fingerprint or face
  sensor, you can turn it on during setup and use it instead of typing the
  PIN. The PIN always keeps working, so a failed or un-enrolled sensor never
  locks you out.
- There is no account, no email, no password reset — the lock is local to the
  phone and only gates this app.

## 2. Setting up your property

Property is a four-level structure: **hostel → floor → room → bed**.

- **Hostels.** Add as many as you run. Each has a name, address and contact
  phone. A switcher lets you flip between them; the dashboard, reports and
  vacancy views all follow the hostel you've selected.
- **Floors.** Named however you call them ("Ground", "1st"), reorderable with
  move up/down. Each floor card shows a vacant/occupied summary so you can see
  where the space is without opening it.
- **Rooms.** Room number, type (single, double or triple), price per bed, and
  free-text amenities. Deleting a room is blocked while one of its beds has an
  active tenant, and it asks for confirmation otherwise.
- **Beds.** Created automatically from the room type — a triple gets beds A, B
  and C — and each carries its own **vacant/occupied** status. Bed-level
  tracking is what makes the vacancy figures correct on shared rooms.
- **Bulk add rooms.** Give a starting number, a count, and the type, price and
  amenities they share, and Lodgy creates them in sequence (101 → 101, 102,
  103…) with their beds, instead of one form at a time.
- **All rooms view.** Every room in the hostel in one list, without drilling
  through floors, filterable by *all / has space / full*.

## 3. Bringing in a tenant

1. **Pick a vacant bed** from the list of what's actually free.
2. **Capture the profile** — full name, phone, photo, ID-proof photo, and an
   emergency contact name and number. Photos can be taken with the camera or
   picked from the gallery, and are stored inside the app's own storage.
3. **Capture the agreement** — agreed rent per month, advance deposit, billing
   cycle day (1–28, so it's valid in every month), and move-in date.
   - The **move-in date can be in the past**. Onboarding someone who was
     already living there before you started using Lodgy is a normal case.
   - **Dues carried forward** is optional: whatever they already owed on the
     day you started using the app becomes a single opening invoice, so their
     balance is right from day one without re-typing past months.
4. The bed flips to **occupied** on save.

### The tenant directory and profile

- Search by name or phone; filter *active only* or *all* (it tells you how
  many vacated tenants a filter is hiding); sort by name or by room.
- Room and bed are shown alongside the name wherever a tenant appears, so you
  never have to remember who is in 104-B.
- **Quick contact** buttons on the profile — Call, WhatsApp, SMS — open the
  dialer or app with the number filled in. Nothing dials or sends by itself.
- **Notes & timeline**, **notice**, **transfer**, **credit**, **multi-month
  payment** and **checkout** all hang off the profile.

### Notes and timeline

- Log a **complaint**, a **damage** incident, or a **general** note, with an
  optional photo.
- Each note carries the date it *happened*, which you can set to the past —
  the timeline sorts by that, not by when you typed it in. Notes are editable
  and deletable, with a confirmation before deleting.

### Notice, transfer and checkout

- **Notice given.** Record the date a tenant says they're leaving. This is a
  flag only; it doesn't check anyone out, and it can be withdrawn. The date
  feeds the **Upcoming move-outs** tile on the dashboard, and on the printable
  packet it reads *Leaving on (notice given)* — never *Moved out*, which is
  reserved for a tenant who has actually been checked out.
- **Move to another bed.** Transfer a tenant to a different bed, optionally
  changing the rent — the new rent applies to future invoices, past ones stay
  as they were. The move is logged on the tenant's timeline.
- **Checkout.** Set the move-out date, enter any damage deduction, and Lodgy
  works out the deposit refund from the advance on file. The agreement closes
  and the bed goes back to vacant — and the tenant's history stays, so a
  returning tenant doesn't start from nothing.

## 4. Rent and payments

- **Invoices generate themselves.** A daily background check creates the
  month's invoice for every active agreement whose billing cycle day is today,
  at the agreed rent. Invoices are immutable snapshots: changing the rent
  later never rewrites what was already billed.
- **Add an invoice by hand** for any past period — pick the tenant, month,
  year, amount and due date. Duplicate periods are refused.
- **Record a payment** against an invoice, full or partial, with the mode
  (cash, UPI, bank transfer, other), the date it was paid, and an optional
  note. The invoice moves *unpaid → partial → paid* on its own.
- **One payment, several months.** When a tenant hands over a lump sum
  covering more than one open month, Lodgy splits it oldest month first,
  settling each in full before moving on, and shows you the split before you
  save. Those payments are badged so the arrangement is visible later.
- **Credits.** Money the tenant is owed back — a repair they paid for out of
  pocket, say — is recorded with its reason and applied to a chosen invoice or
  to their next one. The invoice keeps its original amount and the credit
  shows as its own line, so the reason for every rupee stays on the record.
- **Reminders.** For an unpaid or partial invoice, open a pre-filled reminder
  in **WhatsApp** or **SMS**, in **Hindi or English**, picked per message. You
  see the exact text before it goes; the app never sends anything itself and
  asks for no SMS permission.
- **Receipt.** Any invoice can be saved as a PDF payment acknowledgement —
  tenant, room, period, invoice amount, credit applied, every payment with its
  date and mode, and the balance.
- The invoice list filters by status and by period, and sorts by due date or
  amount.

## 5. Expenses

- Log an expense against a hostel: category (wifi, water, electricity, tax,
  maintenance, repair, other), amount, date, an optional note, and a
  **recurring monthly** flag.
- Filter by category, sort by date or amount.
- Expenses roll into the monthly report's income-versus-expense figure.

## 6. Seeing where you stand

- **Dashboard** — collected today, overdue invoices, vacant beds, and upcoming
  move-outs, for the selected hostel.
- **Vacant beds view** — every free bed grouped by floor, filterable to one
  floor.
- **Monthly report** — per hostel, per month: total collected, total dues,
  occupancy, income minus expense, total expenses and credits for the period.
  - Occupancy is measured from the beds **as they stand today**, not as they
    stood during a past month. Open a period that has already closed and the
    report says so on the screen, rather than letting the figure read as
    history the app doesn't keep.
  - **Export CSV** for the period.
  - **Checked against paper records** — mark a month as reconciled with your
    register, so you can tell at a glance which months you've verified. Once
    a month is marked, its invoices carry a **Checked against register** tick
    on the Payments tab too, so you can see it while you're working through
    payments instead of only on the report. The mark is per hostel: marking
    August at one property leaves the other property's August untouched.

## 7. Reminders the app sends you

Under **More → Notifications**, each category has its own switch:

- **Long-vacant rooms.** A nudge when a bed has sat empty past a threshold you
  set (default 7 days, anywhere from 1 to 90). Each bed is nudged once, not
  daily; if it fills and later empties again, it counts as new.
- **Payments and expenses.** Overdue rent once a due date has fully passed,
  and a heads-up about three days before a recurring expense's usual day comes
  round again.

If Android is blocking notifications for Lodgy, the screen tells you and
points you at system settings.

## 8. Your data

- **Backup (export).** Writes a zip of the whole database plus every photo to
  a location you choose — Downloads, an SD card, a synced folder. You control
  the file from there; it can go to Drive or a USB cable by hand.
- **Restore (import).** Pick a backup zip to restore. It **replaces
  everything** currently on the device, warns you clearly first, and restarts
  the app afterwards. This is the "moved to a new phone" path.
- **Printable records (PDF).** A readable PDF packet of tenants, agreements
  and payment history — for this hostel or all of them — that you can print or
  hand over. The backup zip is for machines; this is for people. Each tenancy
  shows its status alongside its dates, so a tenant who has only given notice
  reads as *Leaving on*, and one who has left reads as *Moved out*.
- **Import past history (CSV).** Optional. If you keep past months in a
  spreadsheet, bring them in as `phone, month, year, amount_due, amount_paid`
  (one row per tenant per month, header optional). Lodgy matches rows to
  tenants by phone number, tells you how many rows it can use, names any line
  it couldn't read and why, and lists rows whose phone number no tenant uses.
  Skipping this changes nothing — the app works fine from today onward.

## 9. Appearance and language

- **Appearance** — light, dark, or follow the system setting.
- **App language** — English or Hindi, switched inside the app, independent of
  your phone's system language. New installs start in English.
- Your own typed data (names, notes, addresses) is stored exactly as entered,
  in whatever script you type; only the app's own labels are translated.
- **More** also shows the app version and build number, for support questions.

---

## What Lodgy deliberately does not do

- **It never sends a message by itself.** WhatsApp and SMS reminders open with
  the text pre-filled and stop there — you review and tap send. The app
  requests no SMS permission and no network access.
- **No cloud sync and no account.** Moving phones is the backup zip, by hand.
  Two people cannot share live data.
- **No tenant-facing app.** Tenants don't log in or see anything.
- **No history of past occupancy.** Reports for an old month use today's bed
  status for the occupancy figure.
- **The database is not encrypted at rest** — the PIN gates the app, not the
  file. Keep backup zips somewhere you're comfortable with.
