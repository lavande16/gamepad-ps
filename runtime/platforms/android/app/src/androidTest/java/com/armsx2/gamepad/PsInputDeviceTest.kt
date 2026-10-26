package com.armsx2.gamepad

import android.content.Intent
import android.os.SystemClock
import android.view.InputDevice
import android.view.MotionEvent
import android.view.View
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.armsx2.R
import com.armsx2.EmuState
import com.armsx2.runtime.MainActivityRuntime as Runtime
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.json.JSONObject
import java.io.File

/** Real MotionEvents traverse the copied NS shell into the production JNI adapter. No test input hook ships. */
@RunWith(AndroidJUnit4::class)
class PsInputDeviceTest {
    private val instrumentation=InstrumentationRegistry.getInstrumentation()
    private fun main(action:()->Unit)=instrumentation.runOnMainSync(action)
    private fun waitFor(condition:()->Boolean) {val until=SystemClock.uptimeMillis()+30000;while(!condition()&&SystemClock.uptimeMillis()<until)SystemClock.sleep(100);assertTrue("Timed out waiting for product state",condition())}
    private fun held(host:PsFrontendHost):Set<Int> {val f=PsControllerAdapter::class.java.getDeclaredField("down");f.isAccessible=true;return (f.get(host.input) as Set<Int>).toSet()}
    private fun axes(host:PsFrontendHost):Map<Int,Int> {val f=PsControllerAdapter::class.java.getDeclaredField("axes");f.isAccessible=true;return (f.get(host.input) as Map<Int,Int>).toMap()}
    private var downTime=0L
    private fun touch(host:PsFrontendHost,action:Int,points:List<Pair<Float,Float>>,index:Int=0) {
        if(action==MotionEvent.ACTION_DOWN)downTime=SystemClock.uptimeMillis()
        val props=Array(points.size){i->MotionEvent.PointerProperties().apply{id=i;toolType=MotionEvent.TOOL_TYPE_FINGER}}
        val coords=Array(points.size){i->MotionEvent.PointerCoords().apply{x=points[i].first;y=points[i].second;pressure=1f;size=1f}}
        val encoded=action or if(action==MotionEvent.ACTION_POINTER_DOWN||action==MotionEvent.ACTION_POINTER_UP)index shl MotionEvent.ACTION_POINTER_INDEX_SHIFT else 0
        val event=MotionEvent.obtain(downTime,SystemClock.uptimeMillis(),encoded,points.size,props,coords,0,0,1f,1f,0,0,InputDevice.SOURCE_TOUCHSCREEN,0)
        main {assertTrue(host.shell.dispatchTouchEvent(event))};event.recycle()
    }
    @Test fun simultaneousControlsCancelPauseAndBackground() {
        val context=instrumentation.targetContext
        context.startActivity(Intent().setClassName("com.gamepad.ps","com.armsx2.BootSplashActivity").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        waitFor{PsFrontendHost.current.get()!=null&&Runtime.nativeReady.value}
        val host=PsFrontendHost.current.get()!!
        waitFor {
            var hasCard=false
            main {hasCard=host.shell.findViewById<View>(R.id.card_game_grid)!=null}
            hasCard
        }
        main {assertTrue(host.shell.findViewById<View>(R.id.card_game_grid).performClick())}
        waitFor{Runtime.eState.value==EmuState.RUNNING}
        main {host.focusChanged(true)}
        assertEquals(2304,host.shell.width);assertEquals(1440,host.shell.height)
        val points=listOf(366.8f to 1083.1f,1937.2f to 1083.1f,597.2f to 854.1f,1706.8f to 854.1f,135.7f to 854.1f,2168.3f to 854.1f)
        touch(host,MotionEvent.ACTION_DOWN,points.take(1))
        for(i in 1 until points.size)touch(host,MotionEvent.ACTION_POINTER_DOWN,points.take(i+1),i)
        val moved=points.toMutableList().apply{this[0]=420f to 1040f;this[1]=1880f to 1130f}
        touch(host,MotionEvent.ACTION_MOVE,moved)
        main {
            assertTrue(held(host).containsAll(setOf(102,103,104,105)))
            val axis=axes(host);assertTrue(axis[111]!!>0&&axis[110]!!>0&&axis[123]!!>0&&axis[122]!!>0)
        }
        touch(host,MotionEvent.ACTION_CANCEL,moved)
        main {assertTrue(held(host).isEmpty());assertTrue(axes(host).values.all{it==0})}
        // Both stick-click dwell timers fire independently and release on lift.
        touch(host,MotionEvent.ACTION_DOWN,points.take(1));touch(host,MotionEvent.ACTION_POINTER_DOWN,points.take(2),1)
        SystemClock.sleep(750)
        main {assertTrue(held(host).containsAll(setOf(106,107)))}
        touch(host,MotionEvent.ACTION_CANCEL,points.take(2))
        // Sliding away from a face button and releasing must not leave it latched.
        touch(host,MotionEvent.ACTION_DOWN,listOf(1937.2f to 1263.1f));touch(host,MotionEvent.ACTION_MOVE,listOf(1500f to 700f));touch(host,MotionEvent.ACTION_UP,listOf(1500f to 700f))
        main {assertTrue(held(host).isEmpty())}
        // HOME while a digital control is held clears input before pausing.
        touch(host,MotionEvent.ACTION_DOWN,listOf(597.2f to 854.1f))
        main {host.home();assertTrue(held(host).isEmpty());assertFalse(host.input.enabled)}
        waitFor{Runtime.eState.value==EmuState.PAUSED}
        main{host.back()};waitFor{Runtime.eState.value==EmuState.RUNNING}
        touch(host,MotionEvent.ACTION_DOWN,listOf(135.7f to 854.1f))
        instrumentation.uiAutomation.executeShellCommand("input keyevent KEYCODE_HOME").close()
        SystemClock.sleep(600)
        main {assertTrue(held(host).isEmpty());assertFalse(host.input.enabled)}
        context.startActivity(Intent().setClassName("com.gamepad.ps","com.armsx2.BootSplashActivity").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        waitFor{host.shell.hasWindowFocus()}
        main{host.home();host.back()};waitFor{Runtime.eState.value==EmuState.RUNNING}
        touch(host,MotionEvent.ACTION_DOWN,listOf(2168.3f to 854.1f));main{assertTrue(held(host).contains(105))};touch(host,MotionEvent.ACTION_UP,listOf(2168.3f to 854.1f))
        main{assertTrue(held(host).isEmpty())}
        SystemClock.sleep(10500)
        main {
            val f=host.shell.javaClass.getDeclaredField("lowerControlsAlpha");f.isAccessible=true
            assertEquals(0f,f.getFloat(host.shell),0.01f)
        }
        touch(host,MotionEvent.ACTION_DOWN,listOf(1937.2f to 1263.1f))
        main{assertTrue(held(host).isEmpty())}
        touch(host,MotionEvent.ACTION_UP,listOf(1937.2f to 1263.1f))
        touch(host,MotionEvent.ACTION_DOWN,listOf(1937.2f to 1263.1f))
        main{assertTrue(held(host).contains(96))}
        touch(host,MotionEvent.ACTION_UP,listOf(1937.2f to 1263.1f))
        main{assertTrue(held(host).isEmpty());host.home()}
        File(context.getExternalFilesDir(null),"stage4-input-report.json").writeText(JSONObject(mapOf("sixFingerSticksShoulders" to true,"stickClicks" to true,"slideAndRelease" to true,"cancel" to true,"pauseResume" to true,"backgroundResume" to true,"tenSecondAutoHide" to true,"wakeTouchDoesNotPressGameButton" to true)).toString(2))
    }
}
