package com.lodgy.app.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.lodgy.app.R
import com.lodgy.app.data.prefs.NotificationPreferences
import com.lodgy.app.data.repository.BedRepository
import com.lodgy.app.notify.CHANNEL_VACANCY
import com.lodgy.app.notify.LodgyNotifications
import com.lodgy.app.notify.ROUTE_VACANT_VIEW
import com.lodgy.app.notify.decideVacancyNudges
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.first

/** Daily nudge to advertise a bed that has been sitting empty past the warden's own threshold. */
@HiltWorker
class VacancyCheckWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val bedRepository: BedRepository,
    private val notificationPreferences: NotificationPreferences,
    private val notifications: LodgyNotifications,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        if (!notificationPreferences.vacancyEnabled.first()) return Result.success()

        val thresholdDays = notificationPreferences.vacancyThresholdDays.first()
        val cutoff = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(thresholdDays.toLong())

        val decision = decideVacancyNudges(
            longVacant = bedRepository.getLongVacantBeds(cutoff),
            currentlyVacantIds = bedRepository.getVacantBedIds().toSet(),
            alreadyNotified = notificationPreferences.notifiedBedIds.first(),
        )

        // Written even when nothing is posted, so beds that filled up stop being remembered.
        notificationPreferences.setNotifiedBedIds(decision.nextNotifiedIds)

        if (decision.toNotify.isEmpty()) return Result.success()

        val context = applicationContext
        decision.toNotify.forEach { bed ->
            notifications.post(
                channelId = CHANNEL_VACANCY,
                notificationId = bed.bedId.hashCode(),
                title = context.getString(R.string.notify_vacancy_title, thresholdDays),
                text = context.getString(
                    R.string.notify_vacancy_text,
                    bed.hostelName,
                    bed.roomNumber,
                    bed.bedLabel,
                ),
                route = ROUTE_VACANT_VIEW,
            )
        }
        return Result.success()
    }
}
