package com.armsx2.gamepad
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
object PsPixel {
    fun shape(size:Dp)=RoundedCornerShape(if(PsProduct.active)0.dp else size)
    fun shape(percent:Int)=RoundedCornerShape(if(PsProduct.active)0 else percent)
}
