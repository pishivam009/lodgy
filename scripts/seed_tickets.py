"""
One-time seed script: creates the initial ticket backlog for Lodgy under
board/tickets/ as individual JSON files, derived from docs/DESIGN.md.

Each ticket carries a `history` trail (list of {timestamp, actor, action,
from, to, note}) that every skill (Product Owner / Developer / Tester /
User) must append to whenever it touches the ticket — never overwrite it.

Re-running this script only creates tickets that don't already exist; it
never overwrites a ticket that's already in progress.
"""
import json
import os
from datetime import datetime, timezone

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
TICKETS_DIR = os.path.join(ROOT, "board", "tickets")

NOW = datetime.now(timezone.utc).isoformat()

# (id, epic, title, description, acceptance_criteria[], priority)
TICKETS = [
    # --- Project Setup ---
    ("LODGY-1", "Project Setup", "Scaffold Android project",
     "Create the Android Studio project: Kotlin, Jetpack Compose, Hilt, "
     "Navigation Compose, Room, WorkManager dependencies wired up.",
     ["Project builds and runs a blank Compose screen on a device/emulator",
      "Hilt, Room, Navigation, WorkManager dependencies compile"],
     "High"),
    ("LODGY-2", "Project Setup", "Define Room database schema & entities",
     "Implement all Room entities and DAOs per docs/DESIGN.md section 3: "
     "Warden, Hostel, Floor, Room, Bed, Tenant, TenancyAgreement, Invoice, "
     "Payment, TenantNote, Expense. UUID primary keys, createdAt/updatedAt "
     "on every entity.",
     ["All entities from DESIGN.md section 3 exist with matching fields",
      "IDs are UUID strings, not autoincrement",
      "Room compiles and generates a working schema export"],
     "High"),
    ("LODGY-3", "Project Setup", "App theme & navigation shell",
     "Set up Compose theme (colors/typography) and the top-level "
     "NavHost with placeholder destinations for each major module.",
     ["App has a consistent Material theme",
      "Navigating between placeholder screens works"],
     "Medium"),

    # --- Auth ---
    ("LODGY-4", "Auth", "Local PIN setup on first launch",
     "First-run flow where the warden sets a local PIN (or password), "
     "stored securely on device.",
     ["On first launch, user is prompted to set a PIN before anything else",
      "PIN is persisted and not stored in plaintext"],
     "High"),
    ("LODGY-5", "Auth", "PIN login gate on app open",
     "Require PIN entry every time the app is opened/resumed.",
     ["App shows a lock screen on cold start requiring correct PIN",
      "Wrong PIN is rejected with a clear error, no crash"],
     "High"),

    # --- Property Setup ---
    ("LODGY-6", "Property Setup", "Hostel CRUD + multi-hostel switcher",
     "Create/edit/list hostels (name, address, contact phone). Dashboard "
     "lets the warden switch between hostels.",
     ["Can create, edit, and list multiple hostels",
      "A hostel switcher is visible and changes the active hostel context"],
     "High"),
    ("LODGY-7", "Property Setup", "Floor CRUD within a hostel",
     "Add/edit/reorder/delete floors under a hostel.",
     ["Floors can be added, edited, and deleted under a hostel",
      "Floors display in the given sort order"],
     "Medium"),
    ("LODGY-8", "Property Setup", "Room CRUD within a floor",
     "Add/edit/delete rooms under a floor: room number, type "
     "(single/double/triple), price per bed, amenities.",
     ["Room type, price, and amenities are captured and editable",
      "Deleting a room is blocked or warned if it has active tenants"],
     "High"),
    ("LODGY-9", "Property Setup", "Bed generation & status tracking",
     "Auto-generate the right number of beds for a room based on its "
     "type, and track each bed's VACANT/OCCUPIED status independently.",
     ["Creating a DOUBLE room creates exactly 2 beds, TRIPLE creates 3",
      "Bed status flips correctly on tenant onboarding/checkout"],
     "High"),
    ("LODGY-10", "Property Setup", "Bulk room setup wizard",
     "Quick-add flow to create N rooms of a given type/price on a floor "
     "in one pass, instead of one at a time.",
     ["Warden can specify a count and type and get N rooms created at once",
      "Each generated room gets correctly numbered and its beds created"],
     "Low"),

    # --- Tenant Onboarding ---
    ("LODGY-11", "Tenant Onboarding", "Tenant profile capture",
     "Capture tenant name, phone, photo, ID proof photo, emergency "
     "contact name/phone.",
     ["Photo and ID proof photo are captured via camera or gallery and stored locally",
      "All fields are editable after creation"],
     "High"),
    ("LODGY-12", "Tenant Onboarding", "Tenancy agreement capture",
     "Capture agreed rent, advance deposit, billing cycle day, and "
     "move-in date. Move-in date must support past dates for onboarding "
     "an already-resident tenant.",
     ["Move-in date accepts past dates without validation errors",
      "Billing cycle day (1-28) is validated",
      "Agreement is linked to exactly one bed"],
     "High"),
    ("LODGY-13", "Tenant Onboarding", "Bed assignment & occupancy flip",
     "Assigning a tenant to a bed during onboarding flips that bed to "
     "OCCUPIED and prevents double-assignment.",
     ["Assigned bed shows OCCUPIED immediately",
      "An already-occupied bed cannot be selected during onboarding"],
     "High"),
    ("LODGY-14", "Tenant Onboarding", "Tenant checkout flow",
     "Close a tenancy agreement: move-out date, deposit settlement "
     "(deduct damages, log refund amount), bed released back to VACANT.",
     ["Checkout requires a move-out date and shows deposit vs deductions",
      "Bed becomes VACANT immediately after checkout",
      "Tenant's history remains viewable after checkout (status VACATED, not deleted)"],
     "High"),

    # --- Rent & Payments ---
    ("LODGY-15", "Rent & Payments", "Monthly invoice auto-generation",
     "WorkManager job that generates an invoice for every ACTIVE "
     "tenancy agreement on its billing cycle day each month.",
     ["Invoice is created automatically on the configured cycle day",
      "No duplicate invoice is created for the same tenant+period",
      "Job survives app restarts (WorkManager persistence)"],
     "High"),
    ("LODGY-16", "Rent & Payments", "Manual / backdated invoice entry",
     "Let the warden manually add an invoice for a past period, needed "
     "to backfill dues history for a tenant onboarded mid-tenancy.",
     ["Invoice can be created for a period earlier than the current month",
      "Manually added invoices behave identically to auto-generated ones for payment tracking"],
     "Medium"),
    ("LODGY-17", "Rent & Payments", "Payment recording & invoice status",
     "Record a payment against an invoice (amount, mode, date, note). "
     "Invoice status auto-updates UNPAID -> PARTIAL -> PAID.",
     ["Partial payment correctly reduces outstanding balance and sets status PARTIAL",
      "Full payment sets status PAID",
      "Multiple partial payments against one invoice are all retained"],
     "High"),
    ("LODGY-18", "Rent & Payments", "WhatsApp reminder (tap-to-send)",
     "Button on an unpaid/partial invoice opens WhatsApp via a wa.me "
     "deep link with a pre-filled reminder message; warden taps send.",
     ["Tapping the reminder opens WhatsApp with tenant's number and a pre-filled message",
      "Message includes tenant name, amount due, and due date",
      "No auto-send occurs without the warden's explicit tap"],
     "Medium"),
    ("LODGY-19", "Rent & Payments", "SMS reminder (tap-to-send)",
     "Same as WhatsApp reminder but via an SMS intent (smsto:), pre-"
     "filled, tap-to-send only.",
     ["Tapping opens the default SMS app pre-filled with tenant number and message",
      "No SEND_SMS permission is requested"],
     "Medium"),

    # --- Reporting & Dashboard ---
    ("LODGY-20", "Reporting & Dashboard", "Home dashboard",
     "Landing screen showing today's collections, overdue invoice "
     "count, vacant bed count, and upcoming move-outs.",
     ["All four metrics are visible and correct against seed data",
      "Dashboard reflects the currently selected hostel"],
     "High"),
    ("LODGY-21", "Reporting & Dashboard", "Vacant rooms/beds view",
     "Filterable view of vacant beds by hostel/floor.",
     ["Vacant beds are listed correctly and update immediately after checkout/onboarding",
      "Filtering by floor works"],
     "Medium"),
    ("LODGY-22", "Reporting & Dashboard", "Monthly report per hostel",
     "Report showing total collected, total dues, occupancy %, and "
     "income vs expense for a selected month.",
     ["Numbers reconcile with underlying invoice/payment/expense records",
      "Report can be viewed for any past month, not just the current one"],
     "Medium"),
    ("LODGY-23", "Reporting & Dashboard", "Export report as PDF/CSV",
     "Let the warden export the monthly report to a file via the "
     "system share sheet / SAF.",
     ["Export produces a readable PDF or CSV with the same numbers as the on-screen report"],
     "Low"),

    # --- Tenant Notes ---
    ("LODGY-24", "Tenant Notes", "Tenant note CRUD",
     "Create, edit, and delete tenant notes (COMPLAINT/DAMAGE/GENERAL) "
     "with an occurredOn date independent of createdAt/updatedAt, plus "
     "an optional photo.",
     ["Note occurredOn can be set in the past",
      "Editing and deleting an existing note both work",
      "Deleting is a hard delete (no undo required)"],
     "Medium"),
    ("LODGY-25", "Tenant Notes", "Tenant timeline view",
     "Per-tenant chronological view of all notes, sorted by occurredOn.",
     ["Timeline sorts by occurredOn, not createdAt",
      "Editing a note's occurredOn re-sorts it correctly in the timeline"],
     "Medium"),

    # --- Expenses ---
    ("LODGY-26", "Expenses", "Expense entry CRUD",
     "Log an expense per hostel: category (wifi/water/electricity/tax/"
     "maintenance/repair/other), amount, date, recurring flag, note.",
     ["All categories from DESIGN.md are selectable",
      "Recurring flag is stored and editable"],
     "Medium"),
    ("LODGY-27", "Expenses", "Expense rollup into monthly report",
     "Roll logged expenses into the income-vs-expense figure on the "
     "monthly report (LODGY-22).",
     ["Monthly report's expense total matches the sum of that month's expense entries"],
     "Medium"),

    # --- Backup & Restore ---
    ("LODGY-28", "Backup & Restore", "Export data (DB + photos) to zip",
     "Zip the Room DB file and the photos directory, and write it via "
     "SAF to a location the warden picks.",
     ["Export produces a single zip file at a user-chosen location",
      "Zip contains both the DB file and all referenced photos"],
     "High"),
    ("LODGY-29", "Backup & Restore", "Import data from a backup zip",
     "Pick a previously exported zip and restore DB + photos, for "
     "warden phone-migration scenarios.",
     ["Importing a valid export fully restores hostels/tenants/invoices/photos",
      "Importing does not silently merge with existing data - warns before overwrite"],
     "High"),
]


def make_ticket(id_, epic, title, description, acceptance_criteria, priority):
    return {
        "id": id_,
        "epic": epic,
        "title": title,
        "description": description,
        "acceptanceCriteria": acceptance_criteria,
        "status": "Todo",
        "priority": priority,
        "assigneeRole": None,
        "createdAt": NOW,
        "updatedAt": NOW,
        "history": [
            {
                "timestamp": NOW,
                "actor": "Product Owner",
                "action": "created",
                "note": "Ticket created from initial DESIGN.md feature breakdown.",
            }
        ],
    }


def main():
    os.makedirs(TICKETS_DIR, exist_ok=True)
    created, skipped = 0, 0
    for id_, epic, title, description, ac, priority in TICKETS:
        path = os.path.join(TICKETS_DIR, f"{id_}.json")
        if os.path.exists(path):
            skipped += 1
            continue
        with open(path, "w", encoding="utf-8") as f:
            json.dump(make_ticket(id_, epic, title, description, ac, priority), f, indent=2)
        created += 1
    print(f"Created {created} tickets, skipped {skipped} existing.")


if __name__ == "__main__":
    main()
