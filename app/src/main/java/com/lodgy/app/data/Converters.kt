package com.lodgy.app.data

import androidx.room.TypeConverter
import com.lodgy.app.data.entity.AgreementStatus
import com.lodgy.app.data.entity.BedStatus
import com.lodgy.app.data.entity.ExpenseCategory
import com.lodgy.app.data.entity.InvoiceStatus
import com.lodgy.app.data.entity.NoteType
import com.lodgy.app.data.entity.PaymentMode
import com.lodgy.app.data.entity.RoomType
import com.lodgy.app.data.entity.TenantStatus

class Converters {
    @TypeConverter
    fun fromRoomType(value: RoomType): String = value.name

    @TypeConverter
    fun toRoomType(value: String): RoomType = RoomType.valueOf(value)

    @TypeConverter
    fun fromBedStatus(value: BedStatus): String = value.name

    @TypeConverter
    fun toBedStatus(value: String): BedStatus = BedStatus.valueOf(value)

    @TypeConverter
    fun fromTenantStatus(value: TenantStatus): String = value.name

    @TypeConverter
    fun toTenantStatus(value: String): TenantStatus = TenantStatus.valueOf(value)

    @TypeConverter
    fun fromAgreementStatus(value: AgreementStatus): String = value.name

    @TypeConverter
    fun toAgreementStatus(value: String): AgreementStatus = AgreementStatus.valueOf(value)

    @TypeConverter
    fun fromInvoiceStatus(value: InvoiceStatus): String = value.name

    @TypeConverter
    fun toInvoiceStatus(value: String): InvoiceStatus = InvoiceStatus.valueOf(value)

    @TypeConverter
    fun fromPaymentMode(value: PaymentMode): String = value.name

    @TypeConverter
    fun toPaymentMode(value: String): PaymentMode = PaymentMode.valueOf(value)

    @TypeConverter
    fun fromNoteType(value: NoteType): String = value.name

    @TypeConverter
    fun toNoteType(value: String): NoteType = NoteType.valueOf(value)

    @TypeConverter
    fun fromExpenseCategory(value: ExpenseCategory): String = value.name

    @TypeConverter
    fun toExpenseCategory(value: String): ExpenseCategory = ExpenseCategory.valueOf(value)
}
