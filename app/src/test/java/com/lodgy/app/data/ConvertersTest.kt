package com.lodgy.app.data

import com.lodgy.app.data.entity.AgreementStatus
import com.lodgy.app.data.entity.BedStatus
import com.lodgy.app.data.entity.ExpenseCategory
import com.lodgy.app.data.entity.InvoiceStatus
import com.lodgy.app.data.entity.NoteType
import com.lodgy.app.data.entity.PaymentMode
import com.lodgy.app.data.entity.RoomType
import com.lodgy.app.data.entity.TenantStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class ConvertersTest {

    private val converters = Converters()

    @Test
    fun `every RoomType round-trips through its string representation`() {
        RoomType.entries.forEach { assertEquals(it, converters.toRoomType(converters.fromRoomType(it))) }
    }

    @Test
    fun `every BedStatus round-trips through its string representation`() {
        BedStatus.entries.forEach { assertEquals(it, converters.toBedStatus(converters.fromBedStatus(it))) }
    }

    @Test
    fun `every TenantStatus round-trips through its string representation`() {
        TenantStatus.entries.forEach { assertEquals(it, converters.toTenantStatus(converters.fromTenantStatus(it))) }
    }

    @Test
    fun `every AgreementStatus round-trips through its string representation`() {
        AgreementStatus.entries.forEach { assertEquals(it, converters.toAgreementStatus(converters.fromAgreementStatus(it))) }
    }

    @Test
    fun `every InvoiceStatus round-trips through its string representation`() {
        InvoiceStatus.entries.forEach { assertEquals(it, converters.toInvoiceStatus(converters.fromInvoiceStatus(it))) }
    }

    @Test
    fun `every PaymentMode round-trips through its string representation`() {
        PaymentMode.entries.forEach { assertEquals(it, converters.toPaymentMode(converters.fromPaymentMode(it))) }
    }

    @Test
    fun `every NoteType round-trips through its string representation`() {
        NoteType.entries.forEach { assertEquals(it, converters.toNoteType(converters.fromNoteType(it))) }
    }

    @Test
    fun `every ExpenseCategory round-trips through its string representation`() {
        ExpenseCategory.entries.forEach { assertEquals(it, converters.toExpenseCategory(converters.fromExpenseCategory(it))) }
    }
}
