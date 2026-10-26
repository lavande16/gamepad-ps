package com.armsx2.gamepad

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SwitchCompat
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.viewinterop.AndroidView
import com.armsx2.R
import androidx.compose.ui.unit.dp
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.slider.Slider
import org.yuzu.yuzu_emu.utils.GameFoldDialogSizing

/** Hosts the actual NS row XML, keeping ARMSX2's existing typed callbacks and scope logic. */
object NsSettingViews {
    const val active = com.armsx2.gamepad.PsProduct.active
    fun row(context: Context, title: String, description: String? = null, value: String? = null, action: (() -> Unit)? = null): View {
        val view = LayoutInflater.from(context).inflate(R.layout.list_item_setting, null)
        bind(view, title, description, value)
        view.setOnClickListener { action?.invoke() }
        return view
    }
    private fun bind(view: View, title: String, description: String?, value: String?) {
        view.findViewById<TextView>(R.id.text_setting_name).text = title
        view.findViewById<TextView>(R.id.text_setting_description).apply { text=description; visibility=if(description.isNullOrBlank()) View.GONE else View.VISIBLE }
        view.findViewById<TextView?>(R.id.text_setting_value)?.apply { text=value;visibility=if(value==null) View.GONE else View.VISIBLE }
    }
    fun show(dialog: AlertDialog) {
        var c:Context?=dialog.context
        while(c is android.content.ContextWrapper && c !is android.app.Activity)c=c.baseContext
        val activity=c as? android.app.Activity
        if(activity!=null) {
            org.yuzu.yuzu_emu.features.input.GameFoldDialogRegistry.register(activity,dialog)
            dialog.setOnDismissListener {org.yuzu.yuzu_emu.features.input.GameFoldDialogRegistry.unregister(activity,dialog)}
        }
        // Dialogs are separate Android windows: controller keys do not reach MainActivity.
        dialog.setOnKeyListener { _, _, event ->
            PsFrontendHost.current.get()?.handleDialogKey(event) == true
        }
        GameFoldDialogSizing.showWithinStageOnFirstFrame(dialog.context, dialog)
        activity?.let {
            org.yuzu.yuzu_emu.features.input.GameFoldDialogRegistry.requestDefaultFocus(dialog)
        }
    }
    @Composable fun Theme(content:@Composable ()->Unit) {
        val surface=androidx.compose.ui.res.colorResource(R.color.pixel_home_surface)
        val ink=androidx.compose.ui.res.colorResource(R.color.pixel_text_dark)
        val paper=androidx.compose.ui.res.colorResource(R.color.pixel_surface_slate)
        val muted=androidx.compose.ui.res.colorResource(R.color.pixel_text_secondary)
        androidx.compose.material3.MaterialTheme(colorScheme=androidx.compose.material3.lightColorScheme(
            primary=androidx.compose.ui.res.colorResource(R.color.pixel_action),
            primaryContainer=paper,onPrimaryContainer=ink,
            secondary=muted,onSecondary=paper,secondaryContainer=paper,onSecondaryContainer=ink,
            tertiary=muted,onTertiary=paper,tertiaryContainer=paper,onTertiaryContainer=ink,
            surfaceTint=paper,error=androidx.compose.ui.res.colorResource(R.color.pixel_danger),
            errorContainer=paper,onErrorContainer=ink,
            onPrimary=androidx.compose.ui.res.colorResource(R.color.pixel_text_light),
            background=surface,onBackground=ink,surface=paper,onSurface=ink,
            surfaceVariant=androidx.compose.ui.res.colorResource(R.color.pixel_surface_slate_dark),onSurfaceVariant=muted,
            outline=androidx.compose.ui.res.colorResource(R.color.pixel_border_ink),
        ),shapes=androidx.compose.material3.Shapes(
            extraSmall=androidx.compose.foundation.shape.RoundedCornerShape(0.dp),
            small=androidx.compose.foundation.shape.RoundedCornerShape(0.dp),
            medium=androidx.compose.foundation.shape.RoundedCornerShape(0.dp),
            large=androidx.compose.foundation.shape.RoundedCornerShape(0.dp),
            extraLarge=androidx.compose.foundation.shape.RoundedCornerShape(0.dp)),content=content)
    }
    @Composable fun BodyInset():androidx.compose.ui.unit.Dp {
        val c=androidx.compose.ui.platform.LocalContext.current
        val configuration=androidx.compose.ui.platform.LocalConfiguration.current
        return org.yuzu.yuzu_emu.utils.GamePadNsHomeGeometry.headerHorizontalInsetDp(configuration.screenWidthDp).dp
    }
    @Composable fun Toolbar(title:String,actions:@Composable androidx.compose.foundation.layout.RowScope.()->Unit) {
        androidx.compose.foundation.layout.Row(modifier=Modifier.fillMaxWidth()) {
            AndroidView(modifier=Modifier.weight(1f),factory={c->LayoutInflater.from(c).inflate(R.layout.gamepad_ps_toolbar,null)},update={v->
                (v as com.google.android.material.appbar.MaterialToolbar).apply {this.title=title;setNavigationOnClickListener {PsFrontendHost.current.get()?.back()} }
            })
            androidx.compose.foundation.layout.Row(content=actions)
        }
    }
    @Composable fun Action(label:String,description:String?=null,onClick:()->Unit) {
        AndroidView(modifier=Modifier.fillMaxWidth(),factory={c->row(c,label)},update={v->bind(v,label,description,null);v.setOnClickListener{onClick()}})
    }

    @Composable fun Panel(modifier:Modifier,content:@Composable ()->Unit) {
        val current=androidx.compose.runtime.rememberUpdatedState(content)
        AndroidView(modifier=modifier,factory={raw ->
            val c=android.view.ContextThemeWrapper(raw,R.style.Theme_GamePadPS_Assembly)
            android.widget.FrameLayout(c).apply {
                setBackgroundResource(R.drawable.pixel_panel_background)
                val gap=resources.getDimensionPixelSize(R.dimen.pixel_space_md)
                setPadding(gap,gap,gap,gap)
                addView(androidx.compose.ui.platform.ComposeView(c).apply {setContent{Theme{current.value()}}},android.widget.FrameLayout.LayoutParams(-1,-2))
            }
        })
    }
    @Composable fun Button(glyph:String,description:String,onClick:()->Unit,selected:Boolean) {
        AndroidView(factory={raw->
            val c=android.view.ContextThemeWrapper(raw,R.style.Theme_GamePadPS_Assembly)
            android.widget.Button(c,null,0,R.style.GameFoldPixel_Button_Compact).apply {
                setBackgroundResource(R.drawable.pixel_button_background)
                val size=resources.getDimensionPixelSize(R.dimen.pixel_touch_target)
                layoutParams=android.view.ViewGroup.LayoutParams(size,size)
            }
        },update={v->v.text=if(glyph=="🗀")"▣" else glyph;v.setTextColor(v.context.getColor(R.color.gamepad_ps_icon_ink));v.contentDescription=description;v.isSelected=selected;v.setOnClickListener{onClick()}})
    }
    @Composable fun Toggle(label: String, value: Boolean, description: String?, onChange:(Boolean)->Unit) {
        AndroidView(modifier=Modifier.fillMaxWidth(), factory={ c -> LayoutInflater.from(c).inflate(R.layout.list_item_setting_switch,null) }, update={ v ->
            bind(v,label,description,null)
            v.findViewById<SwitchCompat>(R.id.switch_widget).apply {
                setOnCheckedChangeListener(null)
                thumbDrawable=androidx.core.content.ContextCompat.getDrawable(context,R.drawable.pixel_switch_thumb)
                trackDrawable=androidx.core.content.ContextCompat.getDrawable(context,R.drawable.pixel_switch_track)
                thumbTintList=null;trackTintList=null;splitTrack=false
                isChecked=value;setOnCheckedChangeListener { _, checked -> onChange(checked) }
            }
            v.setOnClickListener { onChange(!value) }
        })
    }
    @Composable fun Choice(label: String, options:List<String>, selected:Int, description:String?, onChange:(Int)->Unit) {
        AndroidView(modifier=Modifier.fillMaxWidth(),factory={ c -> row(c,label) },update={v ->
            bind(v,label,description,options.getOrNull(selected) ?: "—")
            v.setOnClickListener {
                show(MaterialAlertDialogBuilder(v.context).setTitle(label).setSingleChoiceItems(options.toTypedArray(),selected) { d,which -> onChange(which);d.dismiss() }.setNegativeButton("取消",null).create())
            }
        })
    }
    @Composable fun Integer(label:String,value:Int,min:Int,max:Int,description:String?,format:(Int)->String,onChange:(Int)->Unit) {
        AndroidView(modifier=Modifier.fillMaxWidth(),factory={ c -> row(c,label) },update={v ->
            bind(v,label,description,format(value));v.setOnClickListener {
                val body=LayoutInflater.from(v.context).inflate(R.layout.dialog_slider,null)
                val number=body.findViewById<TextView>(R.id.text_value);number.text=format(value)
                var chosen=value.coerceIn(min,max)
                body.findViewById<Slider>(R.id.slider).apply { valueFrom=min.toFloat();valueTo=max.coerceAtLeast(min+1).toFloat();stepSize=1f;this.value=chosen.toFloat();addOnChangeListener { _, f, _ -> chosen=f.toInt();number.text=format(chosen) } }
                show(MaterialAlertDialogBuilder(v.context).setTitle(label).setView(body).setPositiveButton("确定") { _,_->onChange(chosen) }.setNegativeButton("取消",null).create())
            }
        })
    }
    @Composable fun Section(title:String,expanded:Boolean,toggle:()->Unit) {
        AndroidView(modifier=Modifier.fillMaxWidth(),factory={c->LayoutInflater.from(c).inflate(R.layout.list_item_settings_header,null)},update={v->(v as TextView).text=(if(expanded)"▾ " else "▸ ")+title;v.isFocusable=true;v.isClickable=true;v.setOnClickListener{toggle()} })
    }
}
