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

        // One notification for the run, not one per bed. A new property left empty for a few days
        // would otherwise greet the warden with a notification per vacant bed - 48 was observed on a
        // two-hostel test set - which is the surest way to get the whole category switched off.
        val context = applicationContext
        val first = decision.toNotify.first()
        val text = if (decision.toNotify.size == 1) {
            context.getString(
                R.string.notify_vacancy_text,
                first.hostelName,
                first.roomNumber,
                first.bedLabel,
            )
        } else {
            context.getString(
                R.string.notify_vacancy_text_many,
                decision.toNotify.size,
                first.hostelName,
                first.roomNumber,
                first.bedLabel,
            )
        }
        notifications.post(
            channelId = CHANNEL_VACANCY,
            notificationId = VACANCY_SUMMARY_NOTIFICATION_ID,
            title = context.getString(R.string.notify_vacancy_title, thresholdDays),
            text = text,
            route = ROUTE_VACANT_VIEW,
        )
        return Result.success()
    }

}

/** Fixed id so a later run replaces the previous summary instead of stacking another one up.
 *  A literal rather than a hash so it cannot collide with a per-record id derived from a UUID. */
private const val VACANCY_SUMMARY_NOTIFICATION_ID = 1_000_102
