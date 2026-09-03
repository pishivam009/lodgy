"""One-off script: adds new backlog tickets from the v2 user-feedback session
plus buried enhancement/bug comments found while re-reading the existing
board history. Run once from the repo root: python scripts/add_feedback_tickets.py
"""
import json
import os

TICKETS_DIR = os.path.join(os.path.dirname(__file__), "..", "board", "tickets")
CREATED_AT = "2026-09-03T20:00:00+00:00"

TICKETS = [
    {
        "id": "LODGY-32",
        "epic": "Appearance",
        "title": "Dark mode",
        "description": "Add a dark theme and a way to switch to it, independent of the app's language setting (LODGY-30). Requested directly by wardens using the app at night/in dim common areas.",
        "acceptanceCriteria": [
            "A dark color scheme exists for LodgyTheme with adequate contrast on all screens",
            "Warden can switch between Light/Dark/System default from the More tab",
            "Switching applies immediately without restarting the app",
            "Preference persists across app restarts",
        ],
        "priority": "Medium",
    },
    {
        "id": "LODGY-33",
        "epic": "Property Setup",
        "title": "Surface room & bed number alongside tenant identity everywhere",
        "description": "Room.roomNumber already exists at the schema/property-setup level, but wardens think of tenants by room number first, name second - the reverse of how the app currently presents them. Tenant list rows, tenant profile, invoices, payment records, and reminder previews all show name/phone with no room reference. Surface 'Room 204 - Bed B' (or equivalent) next to the tenant's name everywhere a tenant is identified.",
        "acceptanceCriteria": [
            "Tenant directory list rows show each tenant's current room+bed alongside their name",
            "Tenant profile screen shows room+bed prominently near the top",
            "Invoice list rows and the payment recording screen show room+bed, not just tenant name",
            "A vacated tenant's historical room+bed still displays (not blanked out after checkout)",
        ],
        "priority": "High",
    },
    {
        "id": "LODGY-34",
        "epic": "Tenant Onboarding",
        "title": "Tenant room/bed transfer flow",
        "description": "Tenants sometimes move from one room to another mid-tenancy - same bed type or a different one (e.g. moving in with a partner into a double). There is currently no way to change a tenant's assigned bed without a full checkout + re-onboard, which loses continuity (new agreement, resets history grouping) and double-frees/occupies beds incorrectly if done by hand.",
        "acceptanceCriteria": [
            "A tenant profile action lets the warden move an active tenant to a different vacant bed (same or different room type)",
            "The old bed flips to VACANT and the new bed flips to OCCUPIED atomically - no double-booking, no orphaned OCCUPIED bed",
            "The tenant's history (notes, past invoices/payments) stays associated with the same tenant, not split across two records",
            "Rent amount can be updated at transfer time if the new bed/room has a different price",
            "The transfer is visible in the tenant's notes/timeline as an event",
        ],
        "priority": "High",
    },
    {
        "id": "LODGY-35",
        "epic": "Rent & Payments",
        "title": "Rent adjustment/credit for tenant-paid maintenance or repairs",
        "description": "Tenants sometimes pay out-of-pocket for plumbing, repairs, or maintenance and ask the warden to deduct that amount from their rent instead of reimbursing in cash. There is currently no way to record this - a warden either has to manually shrink the invoice amount (losing the paper trail of why) or track it outside the app.",
        "acceptanceCriteria": [
            "Warden can record a maintenance/repair credit against a specific tenant with an amount and a short note",
            "The credit is applied to reduce that tenant's next (or a chosen) invoice's amount due, and is visible on the invoice as a labeled line item, not just a smaller total",
            "Credit history is visible on the tenant's notes/timeline",
            "Reports (Monthly Report, CSV export) reflect the adjusted amounts, not the pre-credit amounts",
        ],
        "priority": "High",
    },
    {
        "id": "LODGY-36",
        "epic": "Appearance",
        "title": "Improve color coding for vacant/occupied beds and status labels",
        "description": "Current vacant/occupied indicators and status badges (invoice Unpaid/Partial/Paid, tenant Active/Vacated, etc.) use similar-looking neutral colors that are hard to tell apart at a glance. Wardens want a clear, consistent, colorblind-safe color language across bed grids, invoice lists, and tenant status badges.",
        "acceptanceCriteria": [
            "Vacant vs Occupied beds use a clearly distinct, consistent color pair across BedGridScreen and VacantViewScreen",
            "Invoice status badges (Unpaid/Partial/Paid) and tenant status badges (Active/Vacated) use a consistent status-color system app-wide, not per-screen ad hoc colors",
            "Color choices remain distinguishable for common color-vision deficiencies (not color-alone - pair with icon/label as already done)",
            "Both light and dark mode (LODGY-32) use appropriately adjusted versions of the same color language",
        ],
        "priority": "Medium",
    },
    {
        "id": "LODGY-37",
        "epic": "Reporting & Dashboard",
        "title": "Export a single invoice / payment acknowledgement as PDF",
        "description": "Wardens want to hand (or WhatsApp) a tenant a proper receipt-style PDF for a specific invoice or payment, distinct from LODGY-23's whole-month CSV/PDF report export. This is a per-transaction acknowledgement a tenant can keep.",
        "acceptanceCriteria": [
            "From an invoice or a recorded payment, warden can export a PDF acknowledgement showing tenant name, room, period, amount, payment mode, and date",
            "PDF is saved via SAF to a location the warden picks, same pattern as LODGY-23/LODGY-28",
            "Numbers on the PDF match the underlying invoice/payment records exactly",
        ],
        "priority": "High",
    },
    {
        "id": "LODGY-38",
        "epic": "Settings & About",
        "title": "App version info for support/debugging",
        "description": "Small addition so a warden reporting an issue (or the developer supporting them remotely) can quickly see which build they're running. A one-line version string somewhere reachable, not a full About screen.",
        "acceptanceCriteria": [
            "More tab shows the app's versionName/versionCode somewhere visible (e.g. a footer line)",
            "Value updates automatically per build - not a hardcoded string that goes stale",
        ],
        "priority": "Low",
    },
    {
        "id": "LODGY-39",
        "epic": "Usability",
        "title": "Sort and filter controls on all list screens",
        "description": "Long-running hostels accumulate a large list of vacated tenants, closed agreements, and old invoices that clutter the day-to-day views. Wardens want to sort and filter lists (tenants, rooms/beds, invoices, expenses) so they can see just what's relevant - e.g. only vacant rooms, only active tenants, only this month's invoices - without scrolling past history.",
        "acceptanceCriteria": [
            "Tenant directory can filter to Active-only (hide vacated) and sort by name or room",
            "Room/bed views can filter to vacant-only or occupied-only",
            "Invoice list's existing status filter chips (LODGY-17) are joined by a date/period filter and a sort option",
            "Expense list can filter by category and sort by date/amount",
            "Filter/sort state is per-screen and resets sensibly (doesn't silently hide data the warden forgot was filtered)",
        ],
        "priority": "High",
    },
    {
        "id": "LODGY-40",
        "epic": "Property Setup",
        "title": "View all rooms of a hostel without drilling into floors",
        "description": "Currently seeing a hostel's rooms requires Property -> hostel -> floor -> room list, one floor at a time. Wardens want a single flat view of every room in a hostel across all floors, for a quick full-property scan.",
        "acceptanceCriteria": [
            "A hostel-level 'All Rooms' view lists every room across every floor, with the floor labeled per row",
            "Reachable directly from the hostel screen, not just via each floor",
            "Tapping a room still opens its normal bed grid / edit flow",
        ],
        "priority": "Medium",
    },
    {
        "id": "LODGY-41",
        "epic": "Property Setup",
        "title": "Floor cards show a vacant/occupied room summary",
        "description": "Floor list rows currently show just the floor label. Wardens want an at-a-glance vacant/occupied summary on each floor card so they don't have to open every floor to see where space is available.",
        "acceptanceCriteria": [
            "Each floor card shows counts like 'X vacant / Y occupied' (beds or rooms, consistent with how VacantViewScreen counts)",
            "Counts update live when a checkout/onboarding changes a bed's status, without needing to leave and reopen the floor list",
        ],
        "priority": "Medium",
    },
    {
        "id": "LODGY-42",
        "epic": "Rent & Payments",
        "title": "Flag exceptional multi-month/delayed payments for warden review",
        "description": "Occasionally a tenant delays a month and later pays for two months together in one lump sum. This is a legitimate but exceptional pattern that today just looks like two ordinary payments - the warden has no way to spot it happened, or reconcile it, later.",
        "acceptanceCriteria": [
            "A payment that clears more than one period's dues in a single transaction is flagged/labeled as such on the invoice(s) and payment record",
            "Dashboard or payment list surfaces these flagged multi-period payments so a warden can review them",
            "No change to how a normal single-period payment is recorded or displayed",
        ],
        "priority": "Medium",
    },
    {
        "id": "LODGY-43",
        "epic": "Backup & Restore",
        "title": "Reconciliation workflow between paper registers and the app",
        "description": "Wardens keep decades of history on paper and are only entering new activity into the app going forward - they are not confident the two records will stay in sync, and have no way to mark 'this paper entry is now also in the app' or spot gaps. Needs a lightweight way to track reconciliation status rather than forcing a full migration (see LODGY-44).",
        "acceptanceCriteria": [
            "Warden can mark a period (e.g. a given month for a given hostel) as 'reconciled with paper records'",
            "Reconciled periods are visually distinguished from not-yet-reconciled ones in the Monthly Report / Payments views",
            "This is a manual attestation, not an automated diff against anything - it does not block or alter any data",
        ],
        "priority": "Medium",
    },
    {
        "id": "LODGY-44",
        "epic": "Backup & Restore",
        "title": "Lightweight historical backfill mode (avoid full manual re-entry)",
        "description": "Wardens are reluctant to type years of paper history into the app tenant-by-tenant, invoice-by-invoice - it's described as boring, slow, and low-value for them. Rather than requiring full ledger reconstruction, offer a fast, minimal way to seed historical context (e.g. a tenant's move-in date and a running balance/opening due, or a bulk CSV import) instead of every past invoice/payment.",
        "acceptanceCriteria": [
            "Onboarding an existing (already-resident) tenant supports an optional 'opening balance / dues carried forward' field instead of requiring every past invoice to be re-entered",
            "A bulk import path (e.g. CSV) exists for wardens who do want to bring over structured history, as an alternative to one-by-one manual entry",
            "Skipping historical backfill entirely does not block using the app for new activity going forward",
        ],
        "priority": "Low",
    },
    {
        "id": "LODGY-45",
        "epic": "Backup & Restore",
        "title": "Full data export as a human-readable, printable PDF packet",
        "description": "Wardens trust a physical, readable paper trail more than a zip file they can't open themselves (LODGY-28 exports a DB+photos zip, not something a warden can read or hand to someone). To ease the fear of 'losing everything in the app', offer a printable/PDF export of the underlying records - hostels, tenants, invoices, payments - as a human-readable document, distinct from the machine-readable backup zip.",
        "acceptanceCriteria": [
            "Warden can export a printable PDF summary of all tenants, active agreements, and payment history for a hostel (or all hostels)",
            "PDF is legible and organized (grouped by hostel/room/tenant), not a raw data dump",
            "Exported via SAF like other exports (LODGY-23/28), so it can be saved, printed, or shared",
        ],
        "priority": "Medium",
    },
    {
        "id": "LODGY-46",
        "epic": "Engineering Quality",
        "title": "Security review pass",
        "description": "Explicit ask to check the app for security vulnerabilities before wider rollout - PIN/bcrypt storage, exported components, file provider/URI permission scoping, backup file handling, and any other OWASP Mobile Top 10-relevant surface.",
        "acceptanceCriteria": [
            "Manifest components (activities, providers, services) reviewed for unintended exported=true / missing permission scoping",
            "FileProvider paths and granted URI permissions reviewed for over-broad access",
            "PIN hashing (bcrypt via jbcrypt) and storage reviewed for correct salt/rounds usage",
            "Backup export/import (LODGY-28/29) reviewed for zip-slip/path-traversal safety (stageImport's existing '..'/'/' filtering should be re-verified, not assumed)",
            "Findings written up with severity and suggested fixes, even if some are accepted as out-of-scope for a local-only, no-backend app",
        ],
        "priority": "High",
    },
    {
        "id": "LODGY-47",
        "epic": "Engineering Quality",
        "title": "Raise automated test coverage further",
        "description": "Current unit test coverage sits around 94.6% (JaCoCo). Push further into thinly-covered areas - especially ones this QA pass had to verify manually because no test existed, like InvoiceGenerationWorker's WorkManager scheduling logic and the backup/restore file-handling paths.",
        "acceptanceCriteria": [
            "JaCoCo coverage report identifies remaining low-coverage classes/packages",
            "New unit tests added for BackupManager (export/stageImport/applyStaged) and InvoiceGenerationWorker's doWork() logic, which currently have no dedicated tests",
            "Coverage gate in build.gradle.kts reviewed and raised if the new tests support it",
        ],
        "priority": "Medium",
    },
    {
        "id": "LODGY-48",
        "epic": "Engineering Quality",
        "title": "Reduce APK size without regressing performance",
        "description": "Investigate what's contributing to APK size (dependencies, resources, unused code) and trim it for easier installs on lower-end devices/slower connections common among the target users, without hurting startup time or runtime performance.",
        "acceptanceCriteria": [
            "APK analyzer run to identify largest contributors (libraries, resources, generated code)",
            "R8/ProGuard minification and resource shrinking enabled and verified not to break any feature (needs a full manual pass, not just a successful build)",
            "Unused dependencies or oversized assets removed or replaced",
            "Cold start time and key screen transitions measured before/after to confirm no regression",
        ],
        "priority": "Low",
    },
    {
        "id": "LODGY-49",
        "epic": "Tenant Onboarding",
        "title": "Planned/notice move-out date for tenants",
        "description": "Buried gap found while building LODGY-20 (Home dashboard): 'Upcoming move-outs' has no real data source today. Checkout (LODGY-14) is immediate - bed freed, agreement closed, tenant vacated all in one action - there's no 'tenant gave notice, will leave on date X' concept anywhere, so an ACTIVE agreement with a future moveOutDate never actually occurs through any existing flow. The dashboard's query is correct but will always render empty until this exists.",
        "acceptanceCriteria": [
            "Warden can record a tenant's notice/planned move-out date while the tenant is still ACTIVE, without checking them out immediately",
            "Home dashboard's 'Upcoming move-outs' tile (LODGY-20) now genuinely populates from real data",
            "The planned date does not auto-checkout the tenant - checkout (LODGY-14) remains an explicit separate action on or after that date",
        ],
        "priority": "Medium",
    },
    {
        "id": "LODGY-50",
        "epic": "Auth",
        "title": "Configurable PIN length (4-6 digits)",
        "description": "Buried scope note from LODGY-4: PIN length was fixed at 4 digits since neither DESIGN.md nor the ticket specified one, and a variable-length flow needs an explicit confirm control the wireframe didn't show. Revisit if wardens want a longer PIN.",
        "acceptanceCriteria": [
            "PIN setup supports a warden-chosen length between 4 and 6 digits",
            "Setup flow has an explicit confirm step regardless of chosen length",
            "Existing 4-digit PINs from before this change continue to work unchanged",
        ],
        "priority": "Low",
    },
    {
        "id": "LODGY-51",
        "epic": "Backup & Restore",
        "title": "Clean up orphaned photo files",
        "description": "Buried finding from LODGY-11/LODGY-28: PhotoStorage persists a picked/captured image to app-private storage immediately, before the tenant form is ever saved. An abandoned pick (form closed without saving, or interrupted) leaves a permanently dangling file that nothing ever cleans up - these accumulate over time and get included in every future backup export (LODGY-28) even though no record references them.",
        "acceptanceCriteria": [
            "A cleanup routine (e.g. run on app start, or via a maintenance action) identifies photo files under filesDir/photos not referenced by any tenant's photoPath/idProofPhotoPath and deletes them",
            "Referenced photos are never touched by the cleanup, verified against real tenant records including vacated tenants",
            "Backup export (LODGY-28) no longer includes orphaned files after cleanup has run",
        ],
        "priority": "Low",
    },
    {
        "id": "LODGY-52",
        "epic": "Reporting & Dashboard",
        "title": "Investigate historical occupancy % tracking for past-period reports",
        "description": "Buried limitation from LODGY-22: Monthly Report's occupancy % is computed against the hostel's CURRENT bed count, not a true historical reconstruction, because the schema only stores current bed rows and agreement history - there's no way to know how many beds existed or were occupied at an arbitrary past date. Flagged rather than shipping a number that looks precise but isn't. Worth a product decision: is this worth a schema change (e.g. periodic bed-state snapshots), or should the report keep disclosing it's a current-state figure?",
        "acceptanceCriteria": [
            "Product decision recorded: either (a) implement periodic occupancy snapshots so past-period reports show true historical occupancy, or (b) keep current-state occupancy but label it clearly on the report so it isn't misread as historical",
            "If (a): Monthly Report's occupancy % for a past period reflects that period's actual bed/occupancy state",
        ],
        "priority": "Low",
    },
]


def main():
    for t in TICKETS:
        ticket = {
            "id": t["id"],
            "epic": t["epic"],
            "title": t["title"],
            "description": t["description"],
            "acceptanceCriteria": t["acceptanceCriteria"],
            "status": "Todo",
            "priority": t["priority"],
            "assigneeRole": "Developer",
            "createdAt": CREATED_AT,
            "updatedAt": CREATED_AT,
            "history": [
                {
                    "timestamp": CREATED_AT,
                    "actor": "Product Owner",
                    "action": "created",
                    "note": "Added from the v2 feedback session (warden interviews after the v1 field test) and from buried enhancement/limitation notes surfaced while re-reading the full v1 board history.",
                }
            ],
        }
        path = os.path.join(TICKETS_DIR, f"{t['id']}.json")
        with open(path, "w", encoding="utf-8") as f:
            json.dump(ticket, f, indent=2, ensure_ascii=False)
            f.write("\n")
        print(f"Wrote {path}")


if __name__ == "__main__":
    main()
