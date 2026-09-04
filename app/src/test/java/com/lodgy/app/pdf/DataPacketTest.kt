package com.lodgy.app.pdf

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DataPacketTest {

    private val labels = PacketLabels(
        title = "Printable records",
        address = "Address",
        beds = "Beds",
        phone = "Phone",
        status = "Status",
        rent = "Agreed rent",
        amenitiesLabel = "Amenities",
        movedIn = "Moved in",
        movedOut = "Moved out",
        noticeGiven = "Leaving on (notice given)",
        invoicesHeading = "Invoice history",
        columnPeriod = "Period",
        columnDue = "Amount due",
        columnPaid = "Total paid",
        columnStatus = "Status",
        noInvoices = "No invoices.",
        noTenants = "No tenancies recorded for this hostel.",
        generatedOn = "Generated on",
    )

    private fun tenancy(name: String, room: String, invoices: Int = 1, movedOut: String? = null, planned: Boolean = false, amenities: String = "") = PacketTenancy(
        tenantName = name,
        phone = "999",
        roomAndBed = room,
        amenities = amenities,
        status = "ACTIVE",
        agreedRent = "5000",
        moveInDate = "1 Jan 2026",
        moveOutDate = movedOut,
        moveOutIsPlanned = planned,
        invoiceRows = List(invoices) { listOf("9/2026", "5000", "5000", "PAID") },
    )

    private fun hostel(name: String, floors: List<PacketFloor>) =
        PacketHostel(hostelName = name, address = "Main Road", bedSummary = "2 vacant / 6 occupied", floors = floors)

    @Test
    fun `content is grouped hostel then floor then tenant, in that order`() {
        val content = buildDataPacket(
            listOf(
                hostel(
                    "Sunrise",
                    listOf(
                        PacketFloor("Ground", listOf(tenancy("Ravi", "Room 101 - Bed A"))),
                        PacketFloor("First", listOf(tenancy("Sita", "Room 201 - Bed A"))),
                    ),
                ),
            ),
            labels,
            generatedOn = "4 Sep 2026",
        )

        val headings = content.blocks.mapNotNull {
            when (it) {
                is PdfBlock.Title -> it.text
                is PdfBlock.Heading -> it.text
                else -> null
            }
        }

        assertEquals(
            listOf(
                "Sunrise",
                "Ground",
                "Room 101 - Bed A - Ravi",
                "First",
                "Room 201 - Bed A - Sita",
            ),
            headings,
        )
    }

    @Test
    fun `each tenancy carries its invoice history as a table`() {
        val content = buildDataPacket(
            listOf(hostel("Sunrise", listOf(PacketFloor("Ground", listOf(tenancy("Ravi", "Room 101 - Bed A", invoices = 3)))))),
            labels,
            generatedOn = "4 Sep 2026",
        )

        val table = content.blocks.filterIsInstance<PdfBlock.Table>().single()
        assertEquals(listOf("Period", "Amount due", "Total paid", "Status"), table.headers)
        assertEquals(3, table.rows.size)
    }

    @Test
    fun `a tenancy with no invoices says so rather than emitting an empty table`() {
        val content = buildDataPacket(
            listOf(hostel("Sunrise", listOf(PacketFloor("Ground", listOf(tenancy("Ravi", "Room 101 - Bed A", invoices = 0)))))),
            labels,
            generatedOn = "4 Sep 2026",
        )

        assertTrue(content.blocks.none { it is PdfBlock.Table })
        assertTrue(content.blocks.filterIsInstance<PdfBlock.Paragraph>().any { it.text == labels.noInvoices })
    }

    @Test
    fun `a hostel with no tenancies is still listed, with an explanation`() {
        val content = buildDataPacket(
            listOf(hostel("Empty House", listOf(PacketFloor("Ground", emptyList())))),
            labels,
            generatedOn = "4 Sep 2026",
        )

        assertTrue(content.blocks.filterIsInstance<PdfBlock.Title>().any { it.text == "Empty House" })
        assertTrue(content.blocks.filterIsInstance<PdfBlock.Paragraph>().any { it.text == labels.noTenants })
        assertTrue(content.blocks.filterIsInstance<PdfBlock.Heading>().none { it.text == "Ground" })
    }

    @Test
    fun `a vacated tenancy shows its move-out date, an active one omits the row`() {
        val withMoveOut = buildDataPacket(
            listOf(hostel("Sunrise", listOf(PacketFloor("G", listOf(tenancy("Ravi", "R1", movedOut = "1 Aug 2026")))))),
            labels,
            generatedOn = "4 Sep 2026",
        )
        val active = buildDataPacket(
            listOf(hostel("Sunrise", listOf(PacketFloor("G", listOf(tenancy("Ravi", "R1")))))),
            labels,
            generatedOn = "4 Sep 2026",
        )

        assertTrue(withMoveOut.blocks.filterIsInstance<PdfBlock.KeyValue>().any { it.label == labels.movedOut })
        assertTrue(active.blocks.filterIsInstance<PdfBlock.KeyValue>().none { it.label == labels.movedOut })
    }

    @Test
    fun `a whole property spans several pages without dropping a tenancy`() {
        val floors = List(4) { floorIndex ->
            PacketFloor("Floor $floorIndex", List(8) { tenancy("Tenant $floorIndex-$it", "Room $it", invoices = 6) })
        }
        val content = buildDataPacket(listOf(hostel("Sunrise", floors)), labels, generatedOn = "4 Sep 2026")

        val pages = paginate(layoutBlocks(content, TextMeasurer { text, _ -> text.length.toFloat() }))

        assertTrue(pages.size > 1)
        assertEquals(
            32,
            pages.sumOf { page -> page.count { it.style == PdfLineStyle.HEADING && it.cells.single().contains(" - Tenant ") } },
        )
    }
    @Test
    fun `a planned move-out on an active tenancy is labelled as notice, not as moved out`() {
        val content = buildDataPacket(
            hostels = listOf(
                hostel("Sunrise PG", listOf(PacketFloor("Ground", listOf(
                    tenancy("Ramesh", "Room 102", movedOut = "20 Sep 2026", planned = true),
                )))),
            ),
            labels = labels,
            generatedOn = "4 Sep 2026",
        )
        val keys = content.blocks.filterIsInstance<PdfBlock.KeyValue>().map { it.label }
        assertTrue(keys.contains("Leaving on (notice given)"))
        assertFalse(keys.contains("Moved out"))
    }

    @Test
    fun `a closed tenancy still reads as moved out`() {
        val content = buildDataPacket(
            hostels = listOf(
                hostel("Sunrise PG", listOf(PacketFloor("Ground", listOf(
                    tenancy("Ramesh", "Room 102", movedOut = "20 Sep 2026", planned = false),
                )))),
            ),
            labels = labels,
            generatedOn = "4 Sep 2026",
        )
        val keys = content.blocks.filterIsInstance<PdfBlock.KeyValue>().map { it.label }
        assertTrue(keys.contains("Moved out"))
        assertFalse(keys.contains("Leaving on (notice given)"))
    }

    @Test
    fun `amenities appear on the room when recorded, so they are readable outside the edit form`() {
        val content = buildDataPacket(
            hostels = listOf(hostel("Sunrise PG", listOf(PacketFloor("Ground", listOf(
                tenancy("Ramesh", "Room 102", amenities = "AC, attached bath"),
            ))))),
            labels = labels, generatedOn = "5 Sep 2026",
        )
        val kv = content.blocks.filterIsInstance<PdfBlock.KeyValue>()
        assertTrue(kv.any { it.label == "Amenities" && it.value == "AC, attached bath" })
    }

    @Test
    fun `a room with no amenities prints no amenities line at all`() {
        val content = buildDataPacket(
            hostels = listOf(hostel("Sunrise PG", listOf(PacketFloor("Ground", listOf(
                tenancy("Ramesh", "Room 102"),
            ))))),
            labels = labels, generatedOn = "5 Sep 2026",
        )
        assertFalse(content.blocks.filterIsInstance<PdfBlock.KeyValue>().any { it.label == "Amenities" })
    }
}
