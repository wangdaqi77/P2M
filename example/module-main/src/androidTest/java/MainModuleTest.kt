import android.content.Intent
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.p2m.core.P2M
import com.p2m.core.channel.Channel
import com.p2m.core.channel.ChannelRedirectionMode
import com.p2m.core.channel.NavigationCallback
import com.p2m.example.account.p2m.api.AccountLaunchActivityInterceptorForLogin
import com.p2m.example.main.p2m.api.Main

import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch


/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class MainModuleTest {
    @Test
    fun testModuleExample() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val countDownLatch = CountDownLatch(1)
        appContext.mainExecutor.execute {
            // 1. init
            P2M.init(appContext)

            // 2. testing
            P2M.apiOf(Main::class.java)
                .launcher
                .activityOfMain
                .launchChannel { createdIntent ->
                    createdIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    appContext.startActivity(createdIntent)
                }
                .addAnnotatedInterceptorAfter(AccountLaunchActivityInterceptorForLogin::class)
                .redirectionMode(ChannelRedirectionMode.RADICAL)
                .navigation(object : NavigationCallback {

                    override fun onStarted(channel: Channel) {
                        println("onStarted")
                    }

                    override fun onCompleted(channel: Channel) {
                        println("onCompleted")
                        countDownLatch.countDown()
                    }

                    override fun onInterrupt(channel: Channel, e: Throwable) {
                        println("onInterrupt")
                    }

                    override fun onRedirect(channel: Channel, redirectChannel: Channel) {
                        println("onRedirect")
                    }
                })
        }
        countDownLatch.await()
    }
}