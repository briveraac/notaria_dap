package com.duoc.plomoredsocial

import com.duoc.plomoredsocial.ui.activities.selectLocationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DeviceLocationProviderTest {
    @Test fun precisePermissionPrefersGps() {
        assertEquals("gps", selectLocationProvider(true, true, true, true))
    }

    @Test fun approximatePermissionUsesNetworkOnly() {
        assertEquals("network", selectLocationProvider(false, true, true, true))
        assertNull(selectLocationProvider(false, true, true, false))
    }

    @Test fun deniedPermissionCannotSelectProvider() {
        assertNull(selectLocationProvider(false, false, true, true))
    }
}
