package com.lodgy.app.pdf

/** Everything the packet says about one tenancy, already formatted. */
data class PacketTenancy(
    val tenantName: String,
    val phone: String,
    val roomAndBed: String,
    val status: String,
    val agreedRent: String,
    val moveInDate: String,
    val moveOutDate: String?,
    /** Date, period, amount, paid - one row per invoice. */
    val invoiceRows: List<List<String>>,
)

data class PacketFloor(val floorLabel: String, val tenancies: List<PacketTenancy>)

data class PacketHostel(
    val hostelName: String,
    val address: String,
    val bedSummary: String,
    val floors: List<PacketFloor>,
)

data class PacketLabels(
    val title: String,
    val address: String,
    val beds: String,
    val phone: String,
    val status: String,
    val rent: String,
    val movedIn: String,
    val movedOut: String,
    val invoicesHeading: String,
    val columnPeriod: String,
    val columnDue: String,
    val columnPaid: String,
    val columnStatus: String,
    val noInvoices: String,
    val noTenants: String,
    val generatedOn: String,
)

/**
 * The human-readable counterpart to LODGY-28's zip: grouped hostel -> floor -> tenant so a warden
 * can read it, print it, and find a specific tenant by walking the property the way they do in
 * person - rather than a flat dump ordered by whatever the database returned.
 */
fun buildDataPacket(
    hostels: List<PacketHostel>,
    labels: PacketLabels,
    generatedOn: String,
): PdfDocumentContent = PdfDocumentContent(
    title = labels.title,
    subtitle = "${labels.generatedOn}: $generatedOn",
    blocks = buildList {
        hostels.forEach { hostel ->
            add(PdfBlock.Title(hostel.hostelName))
            add(PdfBlock.KeyValue(labels.address, hostel.address))
            add(PdfBlock.KeyValue(labels.beds, hostel.bedSummary))
            add(PdfBlock.Spacer(8f))

            if (hostel.floors.none { it.tenancies.isNotEmpty() }) {
                add(PdfBlock.Paragraph(labels.noTenants))
                add(PdfBlock.Spacer(14f))
                return@forEach
            }

            hostel.floors.filter { it.tenancies.isNotEmpty() }.forEach { floor ->
                add(PdfBlock.Heading(floor.floorLabel))
                floor.tenancies.forEach { tenancy ->
                    add(PdfBlock.Heading("${tenancy.roomAndBed} - ${tenancy.tenantName}"))
                    add(PdfBlock.KeyValue(labels.phone, tenancy.phone))
                    add(PdfBlock.KeyValue(labels.status, tenancy.status))
                    add(PdfBlock.KeyValue(labels.rent, tenancy.agreedRent))
                    add(PdfBlock.KeyValue(labels.movedIn, tenancy.moveInDate))
                    tenancy.moveOutDate?.let { add(PdfBlock.KeyValue(labels.movedOut, it)) }
                    add(PdfBlock.Paragraph(labels.invoicesHeading))
                    if (tenancy.invoiceRows.isEmpty()) {
                        add(PdfBlock.Paragraph(labels.noInvoices))
                    } else {
                        add(
                            PdfBlock.Table(
                                headers = listOf(
                                    labels.columnPeriod,
                                    labels.columnDue,
                                    labels.columnPaid,
                                    labels.columnStatus,
                                ),
                                rows = tenancy.invoiceRows,
                            ),
                        )
                    }
                    add(PdfBlock.Spacer(10f))
                }
            }
            add(PdfBlock.Spacer(14f))
        }
    },
)
