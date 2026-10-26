package com.armsx2.gamepad;

import android.app.Instrumentation;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.InputDevice;
import android.view.MotionEvent;
import android.view.View;
import java.io.File;
import java.io.FileOutputStream;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.Set;

/** Standalone platform instrumentation: no dependency on names of shrunk Kotlin/JUnit runner classes. */
public final class PsProbeInstrumentation extends Instrumentation {
    private Object host;
    private View shell;
    private Object adapter;
    private long downTime;
    private interface Checked { void run() throws Exception; }
    private void check(boolean value,String what) {if(!value)throw new AssertionError(what);}
    private Object field(Object o,String name) throws Exception {Field f=o.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(o);}
    private void call(String method) throws Exception {host.getClass().getMethod(method).invoke(host);}
    private void main(Checked action) throws Exception {
        Throwable[] failure={null};runOnMainSync(()->{try{action.run();}catch(Throwable t){failure[0]=t;}});
        if(failure[0]!=null)throw new RuntimeException(failure[0]);
    }
    private interface Condition {boolean get() throws Exception;}
    private void waitFor(Condition test,String what) throws Exception {
        android.util.Log.i("PsProbe","wait "+what);long until=SystemClock.uptimeMillis()+35000;
        while(SystemClock.uptimeMillis()<until){if(test.get())return;SystemClock.sleep(100);}
        throw new AssertionError("Timeout: "+what);
    }
    private Object current() throws Exception {
        Class<?> type=Class.forName("com.armsx2.gamepad.PsFrontendHost");Object companion=type.getField("Companion").get(null);
        return ((WeakReference<?>)companion.getClass().getMethod("getCurrent").invoke(companion)).get();
    }
    private String state() throws Exception {return ((Enum<?>)field(host,"state")).name();}
    private Set<?> held() throws Exception {return (Set<?>)field(adapter,"down");}
    private Map<?,?> axes() throws Exception {return (Map<?,?>)field(adapter,"axes");}
    private void empty() throws Exception {check(held().isEmpty(),"no latched digital buttons");for(Object v:axes().values())check(((Number)v).intValue()==0,"all half axes neutral");}
    private void launch() {android.util.Log.i("PsProbe","launch begin");getTargetContext().startActivity(new Intent().setClassName("com.gamepad.ps","com.armsx2.BootSplashActivity").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));android.util.Log.i("PsProbe","launch returned");}
    private void touch(int action,float[][] points,int count,int index) throws Exception {
        if(action==MotionEvent.ACTION_DOWN)downTime=SystemClock.uptimeMillis();
        MotionEvent.PointerProperties[] props=new MotionEvent.PointerProperties[count];MotionEvent.PointerCoords[] coords=new MotionEvent.PointerCoords[count];
        for(int i=0;i<count;i++){props[i]=new MotionEvent.PointerProperties();props[i].id=i;props[i].toolType=MotionEvent.TOOL_TYPE_FINGER;coords[i]=new MotionEvent.PointerCoords();coords[i].x=points[i][0];coords[i].y=points[i][1];coords[i].pressure=1;coords[i].size=1;}
        int encoded=action|((action==MotionEvent.ACTION_POINTER_DOWN||action==MotionEvent.ACTION_POINTER_UP)?index<<MotionEvent.ACTION_POINTER_INDEX_SHIFT:0);
        MotionEvent ev=MotionEvent.obtain(downTime,SystemClock.uptimeMillis(),encoded,count,props,coords,0,0,1,1,0,0,InputDevice.SOURCE_TOUCHSCREEN,0);
        main(()->check(shell.dispatchTouchEvent(ev),"shell consumed real MotionEvent"));ev.recycle();
    }
    @Override public void onCreate(Bundle arguments){super.onCreate(arguments);android.util.Log.i("PsProbe","onCreate");start();}
    @Override public void onStart(){
        android.util.Log.i("PsProbe","onStart");Bundle result=new Bundle();
        try {
            launch();waitFor(()->current()!=null,"PS host");host=current();shell=(View)host.getClass().getMethod("getShell").invoke(host);adapter=host.getClass().getMethod("getInput").invoke(host);
            int card=getTargetContext().getResources().getIdentifier("card_game_grid","id","com.gamepad.ps");
            waitFor(()->shell.findViewById(card)!=null,"scanned cartridge");
            main(()->check(shell.findViewById(card).performClick(),"launch from actual NS cartridge"));
            waitFor(()->state().equals("RUNNING")&&shell.hasWindowFocus(),"running VM and focused game");
            Class<?> nativeApp=Class.forName("kr.co.iefriends.pcsx2.NativeApp");
            waitFor(()->Boolean.TRUE.equals(nativeApp.getMethod("hasActiveVM").invoke(null)) && !String.valueOf(nativeApp.getMethod("getGameCRC").invoke(null)).equals("00000000"),"native VM initialized");
            // Wake the original NS overlay if booting consumed its first idle deadline.
            float[][] emptyScreen={{1100,500}};touch(MotionEvent.ACTION_DOWN,emptyScreen,1,0);touch(MotionEvent.ACTION_UP,emptyScreen,1,0);
            check(shell.getWidth()==2304&&shell.getHeight()==1440,"native Pad dimensions");
            float[][] p={{366.8f,1083.1f},{1937.2f,1083.1f},{597.2f,854.1f},{1706.8f,854.1f},{135.7f,854.1f},{2168.3f,854.1f}};
            touch(MotionEvent.ACTION_DOWN,p,1,0);for(int i=1;i<6;i++)touch(MotionEvent.ACTION_POINTER_DOWN,p,i+1,i);
            float[][] moved={{420,1040},{1880,1130},{597.2f,854.1f},{1706.8f,854.1f},{135.7f,854.1f},{2168.3f,854.1f}};
            touch(MotionEvent.ACTION_MOVE,moved,6,0);
            main(()->{for(int k:new int[]{102,103,104,105})check(held().contains(k),"simultaneous shoulder "+k);for(int k:new int[]{110,111,122,123})check(((Number)axes().get(k)).intValue()>0,"simultaneous stick axis "+k);});
            touch(MotionEvent.ACTION_CANCEL,moved,6,0);main(this::empty);
            touch(MotionEvent.ACTION_DOWN,p,1,0);touch(MotionEvent.ACTION_POINTER_DOWN,p,2,1);SystemClock.sleep(750);
            main(()->check(held().contains(106)&&held().contains(107),"independent L3/R3 dwell"));touch(MotionEvent.ACTION_CANCEL,p,2,0);
            float[][] cross={{1937.2f,1263.1f}},away={{1500,700}};
            touch(MotionEvent.ACTION_DOWN,cross,1,0);touch(MotionEvent.ACTION_MOVE,away,1,0);touch(MotionEvent.ACTION_UP,away,1,0);main(this::empty);
            float[][] l={{597.2f,854.1f}};touch(MotionEvent.ACTION_DOWN,l,1,0);main(()->{call("home");empty();});waitFor(()->state().equals("PAUSED"),"pause");
            main(()->call("back"));waitFor(()->state().equals("RUNNING"),"resume");
            float[][] zl={{135.7f,854.1f}};touch(MotionEvent.ACTION_DOWN,zl,1,0);
            getUiAutomation().executeShellCommand("input keyevent KEYCODE_HOME").close();SystemClock.sleep(700);main(this::empty);
            launch();waitFor(()->shell.hasWindowFocus(),"return from background");main(()->{call("home");call("back");});waitFor(()->state().equals("RUNNING"),"resume after background");
            float[][] zr={{2168.3f,854.1f}};touch(MotionEvent.ACTION_DOWN,zr,1,0);main(()->check(held().contains(105),"input recovered"));touch(MotionEvent.ACTION_UP,zr,1,0);
            SystemClock.sleep(10800);main(()->check(((Number)field(shell,"lowerControlsAlpha")).floatValue()<.01f,"10 second auto-hide"));
            touch(MotionEvent.ACTION_DOWN,cross,1,0);main(this::empty);touch(MotionEvent.ACTION_UP,cross,1,0);
            touch(MotionEvent.ACTION_DOWN,cross,1,0);main(()->check(held().contains(96),"second touch sends Cross"));touch(MotionEvent.ACTION_UP,cross,1,0);
            main(()->{empty();call("home");});
            check(getTargetContext().getSharedPreferences("ARMSX2",0).getString("ui.osdMode","").equals("Off"),"OSD Off persisted");
            String json="{\"sixFingerSticksShoulders\":true,\"stickClicks\":true,\"slideAndRelease\":true,\"cancel\":true,\"pauseResume\":true,\"backgroundResume\":true,\"tenSecondAutoHide\":true,\"wakeTouchDoesNotPressGameButton\":true,\"osdOff\":true}";
            try(FileOutputStream out=new FileOutputStream(new File(getTargetContext().getExternalFilesDir(null),"stage4-input-report.json"))){out.write(json.getBytes(java.nio.charset.StandardCharsets.UTF_8));}
            result.putString("stream","PASS: actual NS touch routing, six pointers, both sticks + all shoulders, L3/R3, cancel, slide release, pause/background, auto-hide/wake, OSD Off\n");finish(-1,result);
        }catch(Throwable t){result.putString("stream",android.util.Log.getStackTraceString(t));finish(0,result);}
    }
}
