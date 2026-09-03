"""One-off script: adds tickets from the backlog critique follow-up -
splitting LODGY-39, adding the shared PDF renderer prerequisite for
LODGY-37/45, and new tickets from the latest feedback round (confirmation
dialogs, default language, notifications, nav transitions).
Run once from the repo root: python scripts/add_feedback_tickets_v3.py
"""
import json
import os

TICKETS_DIR = os.path.join(os.path.dirname(__file__), "..", "board", "tickets")
CREATED_AT = "2026-09-03T21:00:00+00:00"

TICKETS = [
    {
        "id": "LODGY-53",
        "epic": "Usability",
        "title": "Sort and filter controls on Room/Bed views",
        "description": "Split out of the original LODGY-39 (too broad as one ticket). Wardens want to filter Room/Bed views to vacant-only or occupied-only, so a full property scan doesn't require mentally filtering a long mixed list. Applies to RoomListScreen, BedGridScreen, and VacantViewScreen (LODGY-21) consistently.",
        "acceptanceCriteria": [
            "Room/bed views can filter to vacant-only or occupied-only",
            "Filter state is per-screen and obvious when active (not a silent, easy-to-forget toggle)",
        ],
        "priority": "High",
    },
    {
        "id": "LODGY-54",
        "epic": "Usability",
        "title": "Date/period filter and sort on the Invoice list",
        "description": "Split out of the original LODGY-39. Extends LODGY-17's existing Unpaid/Partial/Paid status filter chips with a date/period filter and a sort option, so a warden isn't scrolling through every invoice ever created to find this month's.",
        "acceptanceCriteria": [
            "Invoice list's existing status filter chips (LODGY-17) are joined by a month/year (or date-range) filter",
            "Invoice list can be sorted by due date or amount",
            "Combining a status filter and a date filter narrows correctly (both apply together, not one replacing the other)",
        ],
        "priority": "Medium",
    },
    {
        "id": "LODGY-55",
        "epic": "Usability",
        "title": "Filter and sort controls on the Expense list",
        "description": "Split out of the original LODGY-39. Wardens want to filter expenses by category and sort by date or amount, especially once a hostel has months of recurring entries (WiFi, water, etc. from LODGY-26/27).",
        "acceptanceCriteria": [
            "Expense list can filter by category",
            "Expense list can sort by date or amount",
        ],
        "priority": "Medium",
    },
    {
        "id": "LODGY-56",
        "epic": "Reporting & Dashboard",
        "title": "Shared PDF rendering foundation",
        "description": "LODGY-37 (per-invoice PDF acknowledgement) and LODGY-45 (full-data printable PDF export) both need to generate PDFs, and the app currently has zero PDF infrastructure - LODGY-23 deliberately shipped CSV-only and explicitly skipped PDF generation. Build one shared renderer (e.g. via Android's PdfDocument API, laid out with reusable header/table/footer building blocks) that both consumers use, instead of two independent, inevitably-inconsistent PDF pipelines. This is a prerequisite for LODGY-37 and LODGY-45 - land this first.",
        "acceptanceCriteria": [
            "A single PDF-rendering module exists (page setup, text/table layout helpers, Devanagari-safe font handling for Hindi-language content per LODGY-30) that isn't tied to one specific document's content",
            "Produces valid, correctly-paginated multi-page PDFs when content overflows a single page",
            "LODGY-37 and LODGY-45 both consume this module rather than each rolling their own PDF generation",
        ],
        "priority": "High",
    },
    {
        "id": "LODGY-57",
        "epic": "Usability",
        "title": "Confirmation dialogs on all delete and high-impact update actions",
        "description": "Real bug hit directly by the Product Owner: deleting a room produced no confirmation at all and it was gone immediately. Root cause found in code - RoomListViewModel.requestDelete() only shows an AlertDialog when the room has an OCCUPIED bed (a hard block, not a confirmation prompt); for an ordinary vacant room it calls roomRepository.delete() immediately with no prompt whatsoever. FloorListViewModel.delete() is worse - a bare one-liner with no confirmation and no occupied-bed check at all, despite deleting a floor cascading to every room/bed under it. By contrast, NoteFormScreen already does this correctly (showDeleteConfirm state + AlertDialog + explicit Delete/Cancel) - that's the pattern to replicate everywhere. Scope: every delete action needs a plain 'Are you sure?' confirmation; a handful of especially high-impact, hard-to-reverse updates (tenant checkout - already has one, room-type change on a room with an occupied bed, manually overriding a calculated invoice amount) should get one too. Routine field edits (name, address, phone, price) do not need a confirmation dialog - Save is the confirmation there, and adding one would just be friction with no safety benefit.",
        "acceptanceCriteria": [
            "Deleting a Room shows a plain confirmation dialog for the ordinary (no occupied bed) case, in addition to the existing occupied-bed hard block",
            "Deleting a Floor shows a confirmation dialog, and the dialog states plainly that every room/bed under it goes too",
            "Deleting a Note continues to use its existing (already-correct) confirmation pattern - used as the template for the other two",
            "Changing a Room's type while it has an occupied bed shows a confirmation before applying",
            "Routine non-destructive edits (renaming a hostel, editing a phone number, etc.) are explicitly NOT changed - no new confirmation added where there's nothing destructive happening",
        ],
        "priority": "High",
    },
    {
        "id": "LODGY-58",
        "epic": "Localization",
        "title": "Change default first-launch language to English",
        "description": "Product decision reversal: LODGY-30 was built with Hindi as the first-launch default per an earlier explicit requirement ('primary users are Hindi-based'). Product Owner has since reversed that - default should be English on first launch. The switcher itself (LODGY-30, More -> App language) is unaffected; only AppLocale.applyDefaultIfUnset()'s default value changes.",
        "acceptanceCriteria": [
            "Fresh install with no locale preference set defaults to English, not Hindi",
            "Existing installs that already have an explicit choice stored (Hindi or English) are unaffected - this only changes the unset-default case",
            "Hindi remains fully available and switchable at any time via the existing More -> App language picker",
        ],
        "priority": "Medium",
    },
    {
        "id": "LODGY-59",
        "epic": "Notifications",
        "title": "Notify warden to advertise long-vacant rooms",
        "description": "Wardens want a nudge to go put up a To-Let board or post an ad when a room/bed has been sitting vacant for a while, rather than only noticing when they happen to check the Vacant view (LODGY-21). Local notification only - no backend, matching this app's offline-first architecture and the existing WorkManager periodic-check pattern already established by LODGY-15's invoice generation worker.",
        "acceptanceCriteria": [
            "A periodic background check (WorkManager, daily) flags beds that have been VACANT longer than a configurable threshold (e.g. 7 days)",
            "Warden receives a local notification naming the hostel/room/bed(s) that have been vacant past the threshold",
            "Notification does not repeat daily for the same still-vacant bed (once per bed until it's occupied again, or a sensible re-notify cadence, not a daily nag)",
            "Runtime POST_NOTIFICATIONS permission (Android 13+) is requested appropriately and the feature degrades gracefully if denied",
        ],
        "priority": "Medium",
    },
    {
        "id": "LODGY-60",
        "epic": "Notifications",
        "title": "Local notifications for payment dues, reminders, and monthly expenses",
        "description": "Wardens want to be notified about payment due dates, overdue invoices, and recurring monthly expenses (WiFi, water, etc. from LODGY-26/27) coming due - so this doesn't rely on the warden remembering to open the app and check the Dashboard/Invoices. Local notification only, same WorkManager-based approach as LODGY-59 and LODGY-15.",
        "acceptanceCriteria": [
            "Warden is notified when an invoice becomes overdue (past due date, still Unpaid/Partial)",
            "Warden is notified ahead of a recurring expense's typical due date (e.g. WiFi/water bill window) based on past Expense entries for that category",
            "Notifications are actionable - tapping one opens the relevant invoice or expense, not just the app's home screen",
            "Warden can turn this category of notification off from the More tab without turning off the vacant-room notifications from LODGY-59",
        ],
        "priority": "Medium",
    },
    {
        "id": "LODGY-61",
        "epic": "Appearance",
        "title": "Smooth slide transitions for screen navigation",
        "description": "LodgyNavHost's composable() destinations currently specify no enter/exit/popEnter/popExit transitions, so every navigation - especially back navigation - is an abrupt cut rather than feeling like moving through a stack of screens. Add consistent slide transitions (forward: slide in from the right / out to the left; back: reverse) across the app's single NavHost.",
        "acceptanceCriteria": [
            "Navigating forward (e.g. tenant list -> tenant profile) slides the new screen in from the right",
            "Navigating back slides the previous screen back in from the left, reversing the forward animation - not an instant cut",
            "Transition is applied consistently across the whole NavHost, not just a few hand-picked screens",
            "Bottom-nav tab switches (Home/Property/Tenants/Payments/More) keep their own distinct feel and are not forced through the same slide (a tab switch is not a stack push)",
        ],
        "priority": "Medium",
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
                    "note": "Added following backlog review: splitting LODGY-39, giving LODGY-37/45 a shared PDF prerequisite, and folding in the latest round of direct feedback (a real delete-without-confirmation bug hit on Rooms, a default-language reversal, and new notification/navigation-polish requests).",
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
