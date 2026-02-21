package cn.com.omnimind.omnibot

import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityPermissionBranchTest {
    @Test
    fun accessibilityServiceStatusCheck_shouldBeCallable() {
        val isEnabled = OmniOperatorService.isAccessibilityServiceEnabled()
        assertNotNull(isEnabled)
    }

    @Test
    fun batteryOptimizationStatusCheck_shouldBeCallable() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val method = MainActivity::class.java.getDeclaredMethod("isIgnoringBatteryOptimizations")
                method.isAccessible = true
                method.invoke(activity) as Boolean
            }
        }
    }
}
