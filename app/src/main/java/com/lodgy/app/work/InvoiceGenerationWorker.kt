package com.lodgy.app.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.lodgy.app.data.repository.InvoiceRepository
import com.lodgy.app.data.repository.TenancyAgreementRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.Calendar

@HiltWorker
class InvoiceGenerationWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val tenancyAgreementRepository: TenancyAgreementRepository,
    private val invoiceRepository: InvoiceRepository,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val today = Calendar.getInstance()
        val dayOfMonth = today.get(Calendar.DAY_OF_MONTH)
        val periodMonth = today.get(Calendar.MONTH) + 1
        val periodYear = today.get(Calendar.YEAR)
        val dueDate = today.timeInMillis

        tenancyAgreementRepository.getAllActive()
            .filter { it.billingCycleDay == dayOfMonth }
            .forEach { agreement ->
                if (!invoiceRepository.existsForPeriod(agreement.id, periodMonth, periodYear)) {
                    invoiceRepository.create(agreement.id, periodMonth, periodYear, agreement.agreedRent, dueDate)
                }
            }

        return Result.success()
    }
}
