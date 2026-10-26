package com.armsx2.gamepad

import android.content.Context
import android.widget.Toast

/** Suppress passive status toasts; actionable failures remain visible and logged. */
object PsNotices {
    private val actionable=Regex("fail|error|unable|cannot|can't|invalid|missing|denied|unavailable|not found|not supported|busy|hardcore|失败|错误|无法|不能|无效|缺少|缺失|未找到|未发现|未检测|不可用|不支持|拒绝|损坏|繁忙|忙碌|权限|授权",RegexOption.IGNORE_CASE)
    fun makeText(context:Context,text:CharSequence?,duration:Int):Toast {
        if(!PsProduct.active || actionable.containsMatchIn(text?.toString().orEmpty())) return Toast.makeText(context,text,duration)
        return object:Toast(context) { override fun show(){android.util.Log.i("GamePadPS-Notice",text.toString())} }
    }
    fun makeText(context:Context,resId:Int,duration:Int):Toast = makeText(context,context.getText(resId),duration)
}
