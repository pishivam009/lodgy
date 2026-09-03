package com.lodgy.app.data

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** v1 shipped to a real warden, so schema changes migrate rather than wipe. */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `credits` (" +
                "`id` TEXT NOT NULL, `tenantId` TEXT NOT NULL, `invoiceId` TEXT, " +
                "`amount` REAL NOT NULL, `reason` TEXT NOT NULL, " +
                "`createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, " +
                "PRIMARY KEY(`id`), " +
                "FOREIGN KEY(`tenantId`) REFERENCES `tenants`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , " +
                "FOREIGN KEY(`invoiceId`) REFERENCES `invoices`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL )",
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_credits_tenantId` ON `credits` (`tenantId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_credits_invoiceId` ON `credits` (`invoiceId`)")
    }
}
