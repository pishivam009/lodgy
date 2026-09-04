package com.lodgy.app.notify

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationRoutesTest {

    @Test
    fun `the routes the app itself builds are accepted`() {
        assertTrue(isSupportedNotificationRoute(ROUTE_VACANT_VIEW))
        assertTrue(isSupportedNotificationRoute(routeToRecordPayment("inv-1")))
        assertTrue(isSupportedNotificationRoute(routeToExpense("e1")))
    }

    @Test
    fun `an arbitrary route from another app is rejected rather than crashing the NavHost`() {
        assertFalse(isSupportedNotificationRoute("tenant_profile/t1"))
        assertFalse(isSupportedNotificationRoute("nonsense"))
        assertFalse(isSupportedNotificationRoute(""))
        assertFalse(isSupportedNotificationRoute("record_payment"))
        assertFalse(isSupportedNotificationRoute("record_payment/inv-1/extra"))
        assertFalse(isSupportedNotificationRoute("expense_form"))
    }
}
