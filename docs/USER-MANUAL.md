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

The app has five tabs along the bottom — **Home, Property, Tenants, Payments,
More** — and that bar stays on the screen everywhere you go, not just on the
five main tabs. So no matter how deep you are, **Home is always one tap away**,
and you can jump to any section without pressing back over and over. The back
arrow (top-left) and your phone's own back gesture still work as before — the
bottom bar is an extra way around, not a replacement.

The five tabs:

| Tab | What's there |
|---|---|
| **Home** | Dashboard, what's vacant, monthly report |
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
- **Too many wrong tries.** A few wrong PINs cost you nothing. But after
  several in a row, the app makes you wait a little before trying again — a few
  seconds at first, longer if wrong tries keep coming — so nobody can sit and
  guess your PIN. It tells you how long to wait and counts down; it is never a
  permanent lock, and **Forgot PIN?** stays available the whole time. Waiting
  it out or restarting the app won't skip the wait.
- **Forgot your PIN?** There's a **Forgot PIN?** link right on the lock
  screen. Because there's no account to recover through, it does the safe
  thing instead: first it lets you **save a backup** of everything — without
  unlocking — and only then offers to **reset your PIN**. After the reset you
  set a new PIN on the next screen. Your hostels, tenants and payments stay on
  the phone the whole time; the reset clears only the PIN. If you have
  fingerprint unlock set up, the screen points you back to that first, since
  it's the quickest way in. Keep the backup you saved — if anything looks
  wrong after resetting, you can restore it from **More → Backup**.

## 2. Setting up your property

### What kind of property is it?

The first thing Lodgy asks when you add a property is what it is, because that
changes how much you have to set up.

- **Hostel / PG** — let out bed by bed. You build it up as
  **hostel → floor → room → bed**, and everything below applies.
- **Shop, warehouse or flat** — let out as a whole. There is nothing to build:
  you give it a name and a monthly rent, and that's the setup finished. Lodgy
  never asks you for floors, rooms or beds, because for these there aren't any.
  Tapping the property shows you the rent, whether it's let, and one button to
  put a tenant in or open the tenant who's already there.

You can run both kinds side by side — a hostel and two shops is normal. Pick the
type when you create the property; it can't be changed afterwards, because the
structure underneath is already built.

The rest of this section is about hostels.

- **Properties.** Add as many as you run. Each has a name, address and contact
  phone. A switcher lets you flip between them; the dashboard, reports and
  vacancy views all follow the property you've selected.
- **Floors.** Named however you call them ("Ground", "1st"), reorderable with
  move up/down. Each floor card shows a vacant/occupied summary so you can see
  where the space is without opening it.
- **Rooms.** Room number, type (single, double or triple), price per bed, and
  free-text amenities. Deleting a room is blocked while one of its beds has an
  active tenant, and it asks for confirmation otherwise. Changing the **price
  per bed** also asks, showing the old and new figure: it re-rates every bed in
  the room for invoices generated from then on, though invoices already raised
  never change. Fixing a room number or the amenities just saves.
- **Beds.** Created automatically from the room type — a triple gets beds A, B
  and C — and each carries its own **vacant/occupied** status. Bed-level
  tracking is what makes the vacancy figures correct on shared rooms.
- **Bulk add rooms.** Give a starting number, a count, and the type, price and
  amenities they share, and Lodgy creates them in sequence (101 → 101, 102,
  103…) with their beds, instead of one form at a time.
- **All rooms view.** Every room you have, as coloured tiles rather than a
  list — **green** for empty, **amber** for part-full, **red** for full — so
  you can see where the space is at a glance. It opens straight from the
  Property tab, and it covers **all your hostels at once**, not just the one
  you have selected. Each tile says which hostel and floor its room is on, so
  two buildings that both have a room 101 don't get confused. Two filters sit
  above it: one to narrow to a single hostel, one for *all / has space*. The
  empty/part-full/full counts above the tiles follow whatever you've filtered
  to. If you run a single hostel, the hostel filter doesn't appear at all.
- **Tapping a bed.** Anywhere you see the beds in a room, tapping one opens a
  small panel with the room's type, price per bed and amenities, plus one
  action: **assign a tenant** if the bed is free, or **view tenant** if it
  isn't. Nothing happens from the tap alone, so a mis-touch on a crowded grid
  can't take you somewhere unexpected. Assigning from here skips the
  bed-picking step, since you've already told Lodgy which bed you mean.

## 3. Bringing in a tenant

1. **Pick where they're moving in** from the list of what's actually free.
   The list covers **every property you have**, grouped by property and floor,
   so you don't have to switch property first. A shop, warehouse or flat is
   offered as *the whole property* — there are no beds to choose between.
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
4. The bed flips to **occupied** on save, and you're taken back to wherever
   you started — the tenant list, or the room you tapped the bed in.

### Rooms you or a caretaker live in

Some rooms aren't let out: you live in one yourself, or a caretaker does as
part of their pay.

**The quick way:** go to the room, tap the bed, and choose **Mark as warden /
caretaker room**. It asks one thing — a name, already filled in with yours, so
marking your own room is a tap and a confirm. Type the caretaker's name over it
if the room is theirs. You do **not** have to add yourself as a tenant first.

**The long way**, still there if you want to record a phone number, a photo and
the rest: onboard them as you would any tenant, and turn on **"Warden /
caretaker room"** on the agreement form.

Either way Lodgy treats it honestly — the bed counts as occupied, but no rent is
charged, no invoice is ever raised, it never shows up as dues or overdue, and
you won't be nagged that a bed has been empty too long.

The rent and deposit fields disappear when you turn the switch on, because
there's nothing to charge. Everything else works normally: the person appears
in your tenant directory with their room and bed, and the room's history reads
like any other.

**If you want the rent you're giving up to count as a cost**, you can ask for
it — it is off unless you turn it on. Open the person's profile and tap
**Forgone rent as an expense**. If you live in the room yourself you have not
actually spent anything, so leave it off. If the room is part of what you pay a
caretaker, turning it on means the rent you give up is recorded as an expense
every month, so your monthly report shows what that help really costs you.

The amount starts at the room's own price per bed — the rent you're forgoing —
and you can change it. It is only ever a *cost*: nobody is billed for it, and it
never appears in dues or overdue rent. It stops on its own when the tenancy
ends, and if you switch it off, the months already recorded stay in your books —
only future ones stop.

### The tenant directory and profile

- Search by name or phone; filter *active only* or *all* (it tells you how
  many vacated tenants a filter is hiding); sort by name or by room.
- Room and bed are shown alongside the name wherever a tenant appears, so you
  never have to remember who is in 104-B.
- **Quick contact** buttons on the profile — Call, WhatsApp, SMS — open the
  dialer or app with the number filled in. Nothing dials or sends by itself.
- **Notes & timeline**, **notice**, **transfer**, **credit**, **multi-month
  payment** and **checkout** all hang off the profile.
- **Deleting.** A tenant, an expense, or a whole property added by mistake can
  be removed — open its edit screen and use the delete (trash) button, which
  always asks first. Lodgy protects your records: it won't let you delete a
  tenant who still has a tenancy or a credit, or a property that still has
  tenants or expenses, so history is never broken. Check them out and clear
  their records first, then delete.

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
  as they were. Picking a bed fills in that room's rate for you, so if the move
  changes what the tenant pays, Lodgy shows you the old and new figure and asks
  you to confirm before it saves. The move is logged on the tenant's timeline.
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
- **Fixing mistakes on the receipt screen.** The receipt is also where you
  correct one. Each payment and each credit has a delete button, and the invoice
  itself has one at the top. Delete a payment or a credit entered in error and
  the invoice's paid/unpaid status updates itself to match — you never have to
  fix it by hand. An invoice can only be deleted once its payments and credits
  are gone, so nothing is ever left dangling. Everything asks before it deletes.
- The invoice list filters by status and by period, and sorts by due date or
  amount.

## 5. Expenses

- Log an expense against a hostel: category (wifi, water, electricity, tax,
  maintenance, repair, accommodation, other), amount, date, an optional note,
  and a **recurring monthly** flag.
- **Accommodation** entries are the ones Lodgy writes for you, if you asked for
  a warden or caretaker room's forgone rent to be recorded (section 3). They
  read *Forgone rent, Room 101 · Bed B* so you can tell them from what you paid
  out of pocket.
- Filter by category, sort by date or amount.
- Expenses roll into the monthly report's income-versus-expense figure.

## 6. Seeing where you stand

- **Dashboard** — collected today, overdue invoices, **Vacant**, and upcoming
  move-outs, across **all your properties**, with a filter to narrow to one.
  The tile reads just *Vacant*, not *vacant beds*, because a shop or warehouse
  has no beds to count.
- Tapping **Vacant** opens **All rooms** filtered to what still has space.
  Tapping **Overdue invoices** opens your payments list showing exactly those
  invoices — the late ones, including part-paid, and not next month's. Both carry
  whichever property you have Home filtered to, so the number and the list always
  match. A small **›** marks the tiles you can tap.
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

- **Long-vacant rooms.** A nudge when a space has sat empty past a threshold you
  set — **1, 2 or 3 days**, and nothing longer, because a room empty for a week
  is already a week of lost rent. Each bed is nudged once, not over and over; if
  it fills and later empties again, it counts as new.
- **Payments and expenses.** Overdue rent once a due date has fully passed,
  and a heads-up about three days before a recurring expense's usual day comes
  round again.
- **Both are checked every hour**, so you hear about an empty room or unpaid
  rent the hour it happens rather than the next morning. You still get **one**
  notification per category, not one per room or per tenant — the next check
  updates the one already there rather than adding another. Turn either
  category off and it goes quiet completely.

If Android is blocking notifications for Lodgy, the screen tells you and
points you at system settings.

## 8. Your data

- **Backup (export).** Writes a zip of the whole database plus every photo to
  a location you choose — Downloads, an SD card, a synced folder. You control
  the file from there; it can go to Drive or a USB cable by hand.
- **Automatic daily backup.** Under **More → Backup**, tap **Choose backup
  folder** once. Pick a folder that syncs to the cloud — like a Google Drive
  folder — so a lost or broken phone doesn't take your records with it. From
  then on Lodgy backs up there by itself, once a day, with no further action
  from you. It skips days when nothing has changed and keeps the last week of
  backups, deleting older ones so the folder stays tidy. The **home screen**
  shows how your backup is doing: green when it ran recently, amber when it's
  getting old or not set up yet, and red when it has never run or the last one
  failed — for example if you moved or deleted the folder. Tap the icon on
  that tile to back up right now, or, if you haven't chosen a folder yet, to
  go and choose one. You can change the folder any time from the same place.
- **Restore (import).** Pick a backup zip to restore. It **replaces
  everything** currently on the device, warns you clearly first, and restarts
  the app afterwards. This is the "moved to a new phone" path.
- **Keeping backups in Google Drive.** You don't need anything special for
  this — Lodgy uses your phone's own file picker, and Google Drive shows up in
  it like any other place. To **save to Drive**: tap **Export**, then in the
  picker choose **Drive** (you may need the "⋮" menu or "Browse" to find it)
  and pick a folder; the backup lands straight in Drive. To **restore from
  Drive** on a new phone: install Lodgy, sign in to the same Drive account on
  the phone, tap **Import**, choose **Drive** in the picker and pick the backup
  file. Because it goes through the phone's picker, Lodgy never needs your Drive
  password and asks for no internet permission of its own. A backup kept in
  Drive is the safest answer to a lost or broken phone. Tip: for the daily
  automatic backup (More → Backup → **Choose backup folder**), pick a
  Drive-synced folder and every day's backup goes to the cloud on its own.
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

## When Lodgy asks "are you sure?"

Deleting anything always asks. Ordinary edits — a phone number, an address, a
room number, a tenant's name — just save, on purpose: if the app asked every
time, you would stop reading the question, and then it would fail you on the one
that mattered.

So it only asks about an edit when the edit quietly changes money or history you
cannot see on the screen you're on. There are four:

- changing a room's **price per bed**, which re-rates every bed in it,
- changing a **shop, warehouse or flat's monthly rent**, for the same reason,
- a **move to another bed that changes the rent**, since picking a bed fills in
  that room's rate for you,
- **renaming a property you have already reconciled**, because your paper
  register and the PDFs you have exported still carry the old name.

Each one shows you the actual before and after figure, and cancelling leaves
everything exactly as it was.

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
