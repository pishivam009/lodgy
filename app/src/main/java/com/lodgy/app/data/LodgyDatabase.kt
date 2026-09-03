package com.lodgy.app.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.lodgy.app.data.dao.BedDao
import com.lodgy.app.data.dao.CreditDao
import com.lodgy.app.data.dao.ExpenseDao
import com.lodgy.app.data.dao.FloorDao
import com.lodgy.app.data.dao.HostelDao
import com.lodgy.app.data.dao.InvoiceDao
import com.lodgy.app.data.dao.PaymentDao
import com.lodgy.app.data.dao.ReconciliationMarkDao
import com.lodgy.app.data.dao.RoomDao
import com.lodgy.app.data.dao.TenancyAgreementDao
import com.lodgy.app.data.dao.TenantDao
import com.lodgy.app.data.dao.TenantNoteDao
import com.lodgy.app.data.dao.WardenDao
import com.lodgy.app.data.entity.Bed
import com.lodgy.app.data.entity.Credit
import com.lodgy.app.data.entity.Expense
import com.lodgy.app.data.entity.Floor
import com.lodgy.app.data.entity.Hostel
import com.lodgy.app.data.entity.Invoice
import com.lodgy.app.data.entity.Payment
import com.lodgy.app.data.entity.ReconciliationMark
import com.lodgy.app.data.entity.Room
import com.lodgy.app.data.entity.Tenant
import com.lodgy.app.data.entity.TenancyAgreement
import com.lodgy.app.data.entity.TenantNote
import com.lodgy.app.data.entity.Warden

@Database(
    entities = [
        Warden::class,
        Hostel::class,
        Floor::class,
        Room::class,
        Bed::class,
        Tenant::class,
        TenancyAgreement::class,
        Invoice::class,
        Payment::class,
        TenantNote::class,
        Expense::class,
        Credit::class,
        ReconciliationMark::class,
    ],
    version = 3,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class LodgyDatabase : RoomDatabase() {
    abstract fun wardenDao(): WardenDao
    abstract fun hostelDao(): HostelDao
    abstract fun floorDao(): FloorDao
    abstract fun roomDao(): RoomDao
    abstract fun bedDao(): BedDao
    abstract fun tenantDao(): TenantDao
    abstract fun tenancyAgreementDao(): TenancyAgreementDao
    abstract fun invoiceDao(): InvoiceDao
    abstract fun paymentDao(): PaymentDao
    abstract fun tenantNoteDao(): TenantNoteDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun creditDao(): CreditDao
    abstract fun reconciliationMarkDao(): ReconciliationMarkDao

    companion object {
        const val DATABASE_NAME = "lodgy.db"
    }
}
