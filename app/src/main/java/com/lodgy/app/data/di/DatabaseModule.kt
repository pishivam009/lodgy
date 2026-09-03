package com.lodgy.app.data.di

import android.content.Context
import androidx.room.Room
import com.lodgy.app.data.LodgyDatabase
import com.lodgy.app.data.MIGRATION_1_2
import com.lodgy.app.data.dao.BedDao
import com.lodgy.app.data.dao.CreditDao
import com.lodgy.app.data.dao.ExpenseDao
import com.lodgy.app.data.dao.FloorDao
import com.lodgy.app.data.dao.HostelDao
import com.lodgy.app.data.dao.InvoiceDao
import com.lodgy.app.data.dao.PaymentDao
import com.lodgy.app.data.dao.RoomDao
import com.lodgy.app.data.dao.TenancyAgreementDao
import com.lodgy.app.data.dao.TenantDao
import com.lodgy.app.data.dao.TenantNoteDao
import com.lodgy.app.data.dao.WardenDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): LodgyDatabase =
        Room.databaseBuilder(context, LodgyDatabase::class.java, LodgyDatabase.DATABASE_NAME)
            .addMigrations(MIGRATION_1_2)
            .build()

    @Provides
    fun provideWardenDao(db: LodgyDatabase): WardenDao = db.wardenDao()

    @Provides
    fun provideHostelDao(db: LodgyDatabase): HostelDao = db.hostelDao()

    @Provides
    fun provideFloorDao(db: LodgyDatabase): FloorDao = db.floorDao()

    @Provides
    fun provideRoomDao(db: LodgyDatabase): RoomDao = db.roomDao()

    @Provides
    fun provideBedDao(db: LodgyDatabase): BedDao = db.bedDao()

    @Provides
    fun provideTenantDao(db: LodgyDatabase): TenantDao = db.tenantDao()

    @Provides
    fun provideTenancyAgreementDao(db: LodgyDatabase): TenancyAgreementDao = db.tenancyAgreementDao()

    @Provides
    fun provideInvoiceDao(db: LodgyDatabase): InvoiceDao = db.invoiceDao()

    @Provides
    fun providePaymentDao(db: LodgyDatabase): PaymentDao = db.paymentDao()

    @Provides
    fun provideTenantNoteDao(db: LodgyDatabase): TenantNoteDao = db.tenantNoteDao()

    @Provides
    fun provideExpenseDao(db: LodgyDatabase): ExpenseDao = db.expenseDao()

    @Provides
    fun provideCreditDao(db: LodgyDatabase): CreditDao = db.creditDao()
}
