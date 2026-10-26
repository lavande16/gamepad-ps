package com.armsx2.gamepad

import org.junit.Assert.*
import org.junit.Test
import org.yuzu.yuzu_emu.views.*

class PsControllerAdapterTest {
    @Test fun allButtonsUsePhysicalPs2Positions() {
        val pressed=mutableSetOf<Int>()
        val a=PsControllerAdapter({k,_,p->if(p)pressed.add(k) else pressed.remove(k)},{})
        a.enabled=true
        GameFoldButton.entries.filter{it!=GameFoldButton.HOME}.forEach{
            a.onButtonChanged(it,true)
            assertTrue(pressed.contains(PsControllerAdapter.KEYS[it]))
            a.onButtonChanged(it,false)
        }
        PsControllerAdapter.KEYS.filterKeys {it!=GameFoldButton.R3}.forEach{(b,_)->a.onButtonChanged(b,true)}
        assertEquals(setOf(19,22,20,21,100,97,96,99,109,108,102,104,103,105,106),pressed)
        a.enabled=false
        assertTrue(pressed.isEmpty())
    }
    @Test fun stickChordOpensMenuAndClearsGameInputs() {
        val held=mutableSetOf<Int>();var menus=0
        val a=PsControllerAdapter({k,_,p->if(p)held.add(k) else held.remove(k)},{},{menus++})
        a.enabled=true;a.onButtonChanged(GameFoldButton.L3,true);assertEquals(0,menus)
        a.onButtonChanged(GameFoldButton.R3,true);assertEquals(1,menus);assertTrue(held.isEmpty())
        a.onButtonChanged(GameFoldButton.L3,false);a.onButtonChanged(GameFoldButton.R3,false)
        assertEquals(1,menus)
    }
    @Test fun bothSticksReverseAndClearAllHalfAxesOnFocusLoss() {
        val values=mutableMapOf<Int,Int>()
        val a=PsControllerAdapter({k,r,p->values[k]=if(p)r else 0},{})
        a.enabled=true
        a.onStickChanged(GameFoldStick.LEFT,1f,-0.5f)
        a.onStickChanged(GameFoldStick.RIGHT,-1f,1f)
        assertEquals(32767,values[111]);assertEquals(16384,values[110]);assertEquals(32767,values[123]);assertEquals(32767,values[122])
        a.onStickChanged(GameFoldStick.LEFT,-1f,0f)
        assertEquals(0,values[111]);assertEquals(32767,values[113]);assertEquals(0,values[110])
        a.enabled=false
        assertTrue(values.values.all{it==0})
    }
    @Test fun homeAndDisabledInputNeverLeaveButtonsHeld() {
        val held=mutableSetOf<Int>();var homes=0
        val a=PsControllerAdapter({k,_,p->if(p)held.add(k) else held.remove(k)},{homes++})
        a.onButtonChanged(GameFoldButton.B,true);assertTrue(held.isEmpty())
        a.enabled=true;a.onButtonChanged(GameFoldButton.B,true);a.onButtonChanged(GameFoldButton.L,true)
        a.onButtonChanged(GameFoldButton.HOME,true)
        assertEquals(1,homes);assertTrue(held.isEmpty())
    }
}
