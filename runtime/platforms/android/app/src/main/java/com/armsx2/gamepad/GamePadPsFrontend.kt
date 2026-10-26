package com.armsx2.gamepad

import android.content.Context
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.armsx2.EmuState
import com.armsx2.GameInfo
import com.armsx2.R
import com.armsx2.config.ConfigStore
import com.armsx2.config.SettingsScope
import com.armsx2.data.library.GameLibraryRepository
import com.armsx2.runtime.MainActivityRuntime as Runtime
import com.armsx2.ui.InGameOverlay
import com.armsx2.ui.WindowImpl
import com.armsx2.ui.settings.*
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kr.co.iefriends.pcsx2.NativeApp
import org.yuzu.yuzu_emu.views.*
import org.yuzu.yuzu_emu.utils.GamePadNsHomeGeometry
import org.yuzu.yuzu_emu.features.input.GameFoldUiNavigationBridge
import java.io.File
import java.lang.ref.WeakReference
import kotlin.math.roundToInt

/** Product host only: NS Views own artwork/geometry; ARMSX2 owns the VM, Surface and settings. */
@Composable
fun GamePadPsFrontend() {
    val activity = androidx.compose.ui.platform.LocalContext.current as ComponentActivity
    val state = Runtime.eState.value
    val ready = Runtime.nativeReady.value
    val overlay = WindowImpl.overlayVisible.value
    var games by remember { mutableStateOf<List<GameInfo>>(emptyList()) }
    val repository = remember { GameLibraryRepository(activity) }
    LaunchedEffect(ready) {
        if (ready && !Runtime.prefs.getBoolean("gamepad.ps.osd-off.v1",false)) {
            InGameOverlay.setOsdMode(InGameOverlay.OsdMode.Off)
            Runtime.prefs.edit().putBoolean("gamepad.ps.osd-off.v1",true).apply()
        }
        if (ready) games = repository.scan(Runtime.romsDirs.value)
    }
    val host = remember { PsFrontendHost(activity) {games=it} }
    DisposableEffect(host) { PsFrontendHost.current=WeakReference(host);onDispose { host.dispose();PsFrontendHost.current=WeakReference(null) } }
    val inGameScreen=WindowImpl.inGameScreen.value
    LaunchedEffect(inGameScreen) {if(inGameScreen!=null)host.inGameManager(inGameScreen)}
    val route=com.armsx2.navigation.UiNavigator.route.value
    LaunchedEffect(route) {host.routeChanged(route)}
    BackHandler { host.back() }
    androidx.compose.foundation.layout.Box(Modifier.fillMaxSize()) {
        AndroidView(factory={host.shell}, modifier=Modifier.fillMaxSize(),update={host.update(state,ready,overlay,games)})
        com.armsx2.ui.WelcomeBannerOverlay(this)
        com.armsx2.ui.common.GlobalConfirm.Host()
    }
}

class PsFrontendHost(private val activity: ComponentActivity, private val onGamesChanged:(List<GameInfo>)->Unit = {}) {
    private val context = ContextThemeWrapper(activity, R.style.Theme_GamePadPS_Assembly)
    private val inflater = LayoutInflater.from(context)
    val shell = GameFoldShellLayout(context)
    val stage = GameFoldViewportLayout(context).apply { id=R.id.gamefold_display_stage }
    private val frontend = FrameLayout(context)
    private var games = emptyList<GameInfo>()
    private var state = EmuState.STOPPED
    private var ready = false
    private var page = "library"
    private var rendered = ""
    private var scopedGame: GameInfo? = null
    private var hadVm = false
    private var disposed = false
    private var exitDialog:androidx.appcompat.app.AlertDialog?=null
    private var exitPromptOpen=false
    private var refreshRunning=false
    private var switchingTo:GameInfo?=null
    private var closingGame=false
    private val backStack=mutableListOf<String>()
    private var backendReturn="settings"
    private var backendRoute:com.armsx2.navigation.AppRoute?=null
    fun inGameManager(screen:com.armsx2.ui.InGameScreen) {
        release();scopedGame=Runtime.currentGame.value
        Runtime.userHeldPause.value=true;Runtime.pauseForOverlay();WindowImpl.overlayVisible.value=true
        WindowImpl.inGameScreen.value=null
        when(screen) {
            com.armsx2.ui.InGameScreen.Settings -> open("settings")
            com.armsx2.ui.InGameScreen.Controls -> open("controls")
            com.armsx2.ui.InGameScreen.Hotkeys -> settingsTab("hotkeys")
            com.armsx2.ui.InGameScreen.Patches -> com.armsx2.navigation.UiNavigator.navigate(com.armsx2.navigation.AppRoute.PatchManager)
            com.armsx2.ui.InGameScreen.Textures -> com.armsx2.navigation.UiNavigator.navigate(com.armsx2.navigation.AppRoute.TextureManager)
            com.armsx2.ui.InGameScreen.Memcard -> open("materials")
            else -> open("pause")
        }
    }
    fun routeChanged(route:com.armsx2.navigation.AppRoute) {
        if(route==com.armsx2.navigation.AppRoute.Home) {
            if(page=="backend") {backendRoute=null;open(backendReturn)}
            return
        }
        if(route is com.armsx2.navigation.AppRoute.Settings) {
            scopedGame=route.game
            open("settings");return
        }
        if(page!="backend") backendReturn=page
        backendRoute=route;open("backend")
    }
    private val nav = GameFoldUiNavigationBridge(activity, {back()}, {home()}, {page=="library"})
    // The copied NS shell emits these events after its original multi-pointer hit testing.
    val input = PsControllerAdapter({key,range,pressed ->
        NativeApp.setPadButton(key,range,pressed)
    }, {home()}, {showPauseMenu()})
    private val listener=object:GameFoldControllerListener {
        override fun onButtonChanged(button:GameFoldButton,pressed:Boolean) {
            if(page=="game") input.onButtonChanged(button,pressed) else navigateUi(button,pressed)
        }
        override fun onStickChanged(stick:GameFoldStick,x:Float,y:Float) {
            if(page=="game") input.onStickChanged(stick,x,y) else nav.onStickChanged(stick,x,y)
        }
    }
    init {
        shell.addView(stage,FrameLayout.LayoutParams(-2,-2))
        val surface=checkNotNull(Runtime.surface.value)
        check(surface.parent==null) { "Game surface already has a host" }
        stage.addView(surface,FrameLayout.LayoutParams(-1,-1))
        stage.addView(frontend,FrameLayout.LayoutParams(-1,-1))
        shell.setControllerInputListener(listener)
        shell.setControllerOverlayVisible(false)
        shell.setControllerAutoHideEnabled(false)
        stage.addOnLayoutChangeListener {_,left,top,right,bottom,oldLeft,oldTop,oldRight,oldBottom ->
            if(right-left!=oldRight-oldLeft || bottom-top!=oldBottom-oldTop) frontend.post {
                if(!disposed && page!="game") {rendered="";render()}
            }
        }
        render()
    }
    fun update(next:EmuState,nativeReady:Boolean,overlay:Boolean,allGames:List<GameInfo>) {
        val changed=games!=allGames;games=allGames;ready=nativeReady
        state=next
        val active=next==EmuState.RUNNING||next==EmuState.PAUSED
        Runtime.surface.value?.setGameActive(active)
        if(hadVm && !active) {
            closingGame=false;backStack.clear();scopedGame=null;WindowImpl.overlayVisible.value=false
            // The library is already visible during background save. Keep its bound covers.
            if(page!="library") {page="library";rendered=""}
            switchingTo?.let{game->switchingTo=null;activity.window.decorView.post{launch(game)}}
        }
        hadVm=active
        if(overlay && page=="game" && !exitPromptOpen) {release();page="pause"}
        input.enabled=page=="game" && !exitPromptOpen && next==EmuState.RUNNING && activity.hasWindowFocus()
        if(changed && page=="library") rendered=""
        render()
    }
    private fun navigateUi(button:GameFoldButton,pressed:Boolean) {
        // PS convention: Circle / virtual A confirms; Cross / virtual B goes back.
        val mapped=when(button) {GameFoldButton.A->GameFoldButton.B;GameFoldButton.B->GameFoldButton.A;else->button}
        nav.onButtonChanged(mapped,pressed)
    }
    fun handleMotion(event:android.view.MotionEvent):Boolean {
        if(page=="game")return false
        if(event.source and android.view.InputDevice.SOURCE_JOYSTICK==0)return false
        val x=event.getAxisValue(android.view.MotionEvent.AXIS_HAT_X).takeIf{kotlin.math.abs(it)>.3f}
            ?:event.getAxisValue(android.view.MotionEvent.AXIS_X)
        val y=event.getAxisValue(android.view.MotionEvent.AXIS_HAT_Y).takeIf{kotlin.math.abs(it)>.3f}
            ?:event.getAxisValue(android.view.MotionEvent.AXIS_Y)
        nav.onStickChanged(GameFoldStick.LEFT,x,y);return true
    }
    fun handleDialogKey(event: android.view.KeyEvent): Boolean {
        if(nav.dispatchingNativeKey)return false
        if(event.keyCode==android.view.KeyEvent.KEYCODE_BACK) {
            if(event.action==android.view.KeyEvent.ACTION_UP)
                org.yuzu.yuzu_emu.features.input.GameFoldDialogRegistry.activeDialog(activity)?.cancel()
            return true
        }
        if(event.keyCode in listOf(android.view.KeyEvent.KEYCODE_ENTER,android.view.KeyEvent.KEYCODE_DPAD_CENTER)) {
            if(event.repeatCount==0)nav.onButtonChanged(GameFoldButton.B,event.action==android.view.KeyEvent.ACTION_DOWN)
            return true
        }
        val button=PsControllerAdapter.KEYS.entries.firstOrNull{it.value==event.keyCode}?.key ?:return false
        if(event.repeatCount==0)navigateUi(button,event.action==android.view.KeyEvent.ACTION_DOWN)
        return true
    }
    fun handleKey(event: android.view.KeyEvent): Boolean {
        if(nav.dispatchingNativeKey)return false
        if(org.yuzu.yuzu_emu.features.input.GameFoldDialogRegistry.activeDialog(activity)!=null)
            return handleDialogKey(event)
        if(event.keyCode==android.view.KeyEvent.KEYCODE_BACK) {
            if(event.action==android.view.KeyEvent.ACTION_UP) back()
            return true
        }
        if(page=="game") {
            if(event.keyCode==106 || event.keyCode==107) {
                input.onButtonChanged(if(event.keyCode==106)GameFoldButton.L3 else GameFoldButton.R3,event.action==android.view.KeyEvent.ACTION_DOWN)
                return true
            }
            return false
        }
        if(event.keyCode in listOf(android.view.KeyEvent.KEYCODE_ENTER,android.view.KeyEvent.KEYCODE_DPAD_CENTER)) {
            nav.onButtonChanged(GameFoldButton.B,event.action==android.view.KeyEvent.ACTION_DOWN);return true
        }
        val button=PsControllerAdapter.KEYS.entries.firstOrNull{it.value==event.keyCode}?.key ?:return false
        navigateUi(button,event.action==android.view.KeyEvent.ACTION_DOWN)
        return true
    }
    fun release() {shell.releaseControllerInputs();input.enabled=false;nav.release()}
    fun focusChanged(focused:Boolean) {
        if(!focused) release()
        else input.enabled=page=="game" && !exitPromptOpen && state==EmuState.RUNNING
    }
    private fun pauseSession() {
        release(); Runtime.userHeldPause.value=true
        Runtime.pauseForOverlay();WindowImpl.overlayVisible.value=true
    }
    /** Product HOME keeps the same VM and Surface alive underneath the library. */
    fun home() {
        if(state==EmuState.RUNNING||state==EmuState.PAUSED) pauseSession() else release()
        backStack.clear();scopedGame=null;open("library")
    }
    fun showPauseMenu() {
        if(state!=EmuState.RUNNING&&state!=EmuState.PAUSED)return
        backStack.clear();pauseSession();open("pause")
    }
    fun exitSaveFailed() {closingGame=false;switchingTo=null;showPauseMenu()}
    fun back() {
        when(page) {
            "game" -> confirmExit()
            "pause" -> resume()
            "library" -> Unit
            else -> {
                if(page=="backend") {com.armsx2.navigation.UiNavigator.home();backendRoute=null}
                val previous=if(backStack.isNotEmpty())backStack.removeAt(backStack.lastIndex)else "library"
                if(previous=="library")scopedGame=null
                release();page=previous;rendered="";render()
            }
        }
    }
    private fun resume() {
        exitPromptOpen=false;backStack.clear()
        release();page="game";WindowImpl.overlayVisible.value=false;WindowImpl.inGameScreen.value=null
        Runtime.resume();rendered="";render();Runtime.surface.value?.requestFocus()
    }
    private fun open(target:String) {
        if(target!=page && target!="library" && page!="game")backStack.add(page)
        if(target=="library")backStack.clear()
        release();page=target;rendered="";render()
    }
    private fun render() {
        if(disposed||rendered==page)return
        rendered=page
        shell.setControllerOverlayVisible(page=="game")
        shell.setControllerAutoHideEnabled(page=="game")
        frontend.removeAllViews()
        frontend.visibility=if(page=="game")View.GONE else View.VISIBLE
        if(page=="game")return
        val view=when {
            page=="library" -> library()
            page=="pause" -> gameMenu(checkNotNull(Runtime.currentGame.value),"游戏已暂停", listOf(
                Entry("屏幕显示", "显示运行信息") {osdDialog()},
                Entry("游戏设置", "当前游戏的设置") {scopedGame=Runtime.currentGame.value;open("settings")},
                Entry("保存存档", "保存当前即时进度") {(activity as Runtime).saveState()},
                Entry("读取存档", "读取已保存的即时进度") {confirm("读取存档","使用当前槽位读取？") {(activity as Runtime).loadState {resume()}}},
                Entry("继续游戏", "返回当前进度") {resume()},
                Entry("关闭游戏", "保存进度并返回首页") {confirmExit()},
            ))
            page=="settings" -> menu(if(scopedGame==null)"全局设置" else "游戏设置", listOf(
                Entry("性能", "CPU、VU 多线程与速度控制") {settingsTab("performance")},
                Entry("图像", "分辨率、渲染器、纹理与 LGFS") {settingsTab("renderer")},
                Entry("音频", "音量与同步") {settingsTab("audio")},
                Entry("屏幕显示", "游戏 OSD 与显示参数") {settingsTab("overlay")},
                Entry("高级设置", "兼容性和游戏修正") {settingsTab("fixes")},
                Entry("网络", "PS2 网络与设备") {settingsTab("network")},
                Entry("热键", "物理手柄和键盘快捷操作") {settingsTab("hotkeys")},
                Entry("游戏补丁", "金手指与宽屏补丁") {com.armsx2.navigation.UiNavigator.navigate(com.armsx2.navigation.AppRoute.PatchManager)},
                Entry("纹理管理", "材质替换与纹理包") {com.armsx2.navigation.UiNavigator.navigate(com.armsx2.navigation.AppRoute.TextureManager)},
                Entry("手柄", "GamePad 触屏控制与 PS2 映射") {open("controls")},
                Entry("材料与存档", "BIOS、LGFS、游戏和记忆卡") {open("materials")},
                Entry("系统信息", "设备与运行环境") {open("system")},
                Entry("关于与开源许可", "GamePad PS / ARMSX2 / GamePad NS") {open("about")},
            ))
            page.startsWith("tab:") -> settingsBody(page.substringAfter(':'))
            page=="properties" -> properties()
            page=="backend" -> backendBody()
            page=="controls" -> menu("手柄",listOf(
                Entry("方向键与双摇杆", "按原 NS 几何处理多指输入；摇杆停留 600ms 触发 L3/R3") {},
                Entry("PS2 按键映射", "X→△　A→○　B→×　Y→□\nL/R→L1/R1　ZL/ZR→L2/R2\n−→SELECT　+→START　HOME→主页暂停；L3+R3→完整菜单") {},
                Entry("外接手柄", "沿用 ARMSX2 的物理手柄映射与热键") {com.armsx2.navigation.UiNavigator.navigate(com.armsx2.navigation.AppRoute.ControllerManager)},
            ))
            page=="materials" -> materials()
            page=="system" -> menu("系统信息",listOf(
                Entry(android.os.Build.MODEL,"Android ${android.os.Build.VERSION.RELEASE} / API ${android.os.Build.VERSION.SDK_INT}"){},
                Entry("显示尺寸", "${activity.resources.displayMetrics.widthPixels} × ${activity.resources.displayMetrics.heightPixels}"){},
                Entry("运行底座", "ARMSX2 2.8.4 / 25e7084；arm64 / 4KB 页"){},
            ))
            else -> menu("关于 GamePad PS",listOf(
                Entry("GamePad PS 0.4.1", "独立测试版本；参考 GamePad NS V1.8"){},
                Entry("开源许可", "GamePad NS 组件 GPL-3.0-or-later\nARMSX2 / PCSX2 GPL-3.0；保留源码内原始许可声明") {openLicense()},
            ))
        }
        frontend.addView(view,FrameLayout.LayoutParams(-1,-1))
        frontend.post {nav.requestDefaultFocus()}
    }
    private fun library():View {
        val view=inflater.inflate(R.layout.fragment_games,frontend,false)
        view.findViewById<View>(R.id.launch_qlaunch).visibility=View.GONE
        view.findViewById<View>(R.id.information_button).setOnClickListener {
            val result=org.yuzu.yuzu_emu.utils.GamePadNsDisplaySizeController.toggle(activity)
            if(result.contains("失败")||result.contains("授权"))com.armsx2.ui.WelcomeBanner.error(result)
        }
        view.findViewById<View>(R.id.settings_button).setOnClickListener {scopedGame=null;open("settings")}
        val density=context.resources.displayMetrics.density
        val dp=context.resources.configuration.screenWidthDp
        val inset=GamePadNsHomeGeometry.headerHorizontalInsetPx(context)
        view.findViewById<View>(R.id.header).setPadding(inset,(8*density).roundToInt(),inset,(8*density).roundToInt())
        val list=view.findViewById<RecyclerView>(R.id.grid_games)
        list.layoutManager=GridLayoutManager(context,4)
        // Same dynamic inset update as NS GamesFragment, including the 4K window change.
        list.setPadding((48*density).roundToInt(),(12*density).roundToInt(),(48*density).roundToInt(),(8*density).roundToInt())
        list.layoutParams=(list.layoutParams as RelativeLayout.LayoutParams).apply {
            width=(context.resources.displayMetrics.widthPixels*GamePadNsHomeGeometry.gridWidthFraction(dp)).roundToInt();addRule(RelativeLayout.CENTER_HORIZONTAL)
        }
        list.adapter=object:RecyclerView.Adapter<Holder>() {
            override fun getItemCount()=games.size+1
            override fun onCreateViewHolder(parent:ViewGroup,type:Int)=Holder(inflater.inflate(R.layout.card_game_grid,parent,false))
            override fun onBindViewHolder(holder:Holder,index:Int) {
                val item=holder.itemView
                if(index==games.size) {
                    item.findViewById<TextView>(R.id.text_game_title).text="添加游戏"
                    item.findViewById<AspectRatioFrameLayout>(R.id.card_game_grid).apply {
                        widthScale=GamePadNsHomeGeometry.cardScaleWithinTightenedGrid();contentDescription="添加游戏"
                        setOnClickListener{addGameDialog()};setOnLongClickListener(null)
                    }
                    item.findViewById<SquareImageView>(R.id.image_game_screen).apply {
                        enforceSquare=false;scaleType=ImageView.ScaleType.FIT_XY
                        layoutParams=FrameLayout.LayoutParams(-1,-1).apply {
                            gravity=android.view.Gravity.FILL
                            val outer=resources.getDimensionPixelSize(R.dimen.gamepad_ps_cartridge_artwork_inset)
                            setMargins(outer,outer,outer,outer)
                        }
                        setImageResource(R.drawable.gamefold_add_game_cartridge)
                    }
                    item.findViewById<View>(R.id.game_cartridge_frame).visibility=View.GONE
                    return
                }
                val game=games[index]
                item.findViewById<SquareImageView>(R.id.image_game_screen).apply {
                    enforceSquare=true;scaleType=ImageView.ScaleType.CENTER_CROP
                    val gap=(14*resources.displayMetrics.density).roundToInt()
                    layoutParams=FrameLayout.LayoutParams(-1,-2).apply{gravity=android.view.Gravity.TOP;setMargins(gap,(13*resources.displayMetrics.density).roundToInt(),gap,0)}
                }
                item.findViewById<View>(R.id.game_cartridge_frame).visibility=View.VISIBLE
                item.findViewById<TextView>(R.id.text_game_title).text=game.displayTitle(Runtime.prefs.getBoolean("library.englishTitles",false))
                item.findViewById<AspectRatioFrameLayout>(R.id.card_game_grid).apply {
                    widthScale=GamePadNsHomeGeometry.cardScaleWithinTightenedGrid()
                    contentDescription=game.displayTitle(false)
                    setOnClickListener {launch(game)};setOnLongClickListener {it.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS);scopedGame=game;open("properties");true}
                }
                val image=item.findViewById<ImageView>(R.id.image_game_screen)
                image.tag=game.uri.toString();PsCovers.show(image,game)
                if(PsCovers.existing(game)==null) activity.lifecycleScope.launch {
                    PsCovers.missing(game)
                    if(image.tag==game.uri.toString()) PsCovers.show(image,game)
                }
            }
        }
        view.findViewById<TextView>(R.id.notice_text).apply {
            visibility=View.GONE
            text=if(ready)"未发现游戏，请检查 GamePadPS/game 文件夹" else "正在读取游戏"
        }
        view.findViewById<androidx.swiperefreshlayout.widget.SwipeRefreshLayout>(R.id.swipe_refresh).apply {
            isEnabled=true;isRefreshing=refreshRunning
            setOnRefreshListener {refreshLibrary(this,list)}
        }
        return view
    }
    private fun refreshLibrary(refresh:androidx.swiperefreshlayout.widget.SwipeRefreshLayout,list:RecyclerView) {
        if(refreshRunning) return
        refreshRunning=true;refresh.isRefreshing=true
        activity.lifecycleScope.launch {
            try {
                val active=state==EmuState.RUNNING||state==EmuState.PAUSED
                // A live VM owns its folders. Re-scan metadata without rebinding it while paused.
                if(!active) withContext(Dispatchers.IO) {
                    val result=GamePadPsAssembler.assemble(activity,Runtime.prefs){_,_->}
                    check(result.ready){result.messages.joinToString("\n")}
                }
                games=GameLibraryRepository(activity).scan(Runtime.romsDirs.value)
                onGamesChanged(games)
                list.adapter?.notifyDataSetChanged()
                for(game in games) if(PsCovers.existing(game)==null) PsCovers.missing(game)
                list.adapter?.notifyDataSetChanged()
            }catch(e:Exception){com.armsx2.ui.WelcomeBanner.error("刷新失败：${e.message}")}
            finally {refreshRunning=false;refresh.isRefreshing=false}
        }
    }
    private fun addGameDialog() {
        // Same MaterialAlertDialog flow and button styling as NS GamesFragment.
        val dialog=MaterialAlertDialogBuilder(context,R.style.GameFoldPixel_DialogTheme)
            .setTitle("添加游戏")
            .setMessage("请将游戏文件存放至内部存储根目录 /GamePadPS/game，返回首页下拉刷新即可添加。")
            .setNegativeButton("确定",null)
            .setPositiveButton("下载游戏") {_,_->
                runCatching {activity.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW,android.net.Uri.parse("https://laojiku.com/97.html")))}
                    .onFailure {com.armsx2.ui.WelcomeBanner.error("无法打开下载页面")}
            }.create()
        dialog.setOnShowListener {
            dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).apply {
                setTextColor(context.getColor(R.color.pixel_text_light))
                backgroundTintList=android.content.res.ColorStateList.valueOf(context.getColor(R.color.pixel_action))
            }
        }
        NsSettingViews.show(dialog)
    }
    private fun launch(game:GameInfo) {
        if(!ready) return
        if(closingGame) {switchingTo=game;return}
        val current=Runtime.currentGame.value
        val active=state==EmuState.RUNNING||state==EmuState.PAUSED
        if(active && current?.uri==game.uri) {resume();return}
        if(active) {
            confirm("切换游戏", "将自动保存当前游戏，再开启所选游戏。") {
                switchingTo=game;beginClose()
            };return
        }
        release();backStack.clear();GameLibraryRepository(activity).markPlayed(game);page="game";rendered="";render()
        val path=if(game.uri.scheme=="file")game.uri.path ?:game.uri.toString() else game.uri.toString()
        Runtime.launchGame(path,game)
    }
    private data class Entry(val title:String,val description:String,val action:()->Unit)
    private class Holder(view:View):RecyclerView.ViewHolder(view)
    private fun entryIcon(title:String):Int=when {
        title.contains("音频") -> R.drawable.pixel_icon_audio
        title.contains("图像") -> R.drawable.pixel_icon_graphics
        title.contains("屏幕") -> R.drawable.pixel_icon_display
        title.contains("手柄")||title.contains("摇杆")||title.contains("按键")||title.contains("热键") -> R.drawable.pixel_icon_controller
        title.contains("保存")||title.contains("记忆卡") -> R.drawable.ic_save
        title.contains("读取")||title.contains("材料") -> R.drawable.pixel_icon_install
        title.contains("继续")||title.contains("开始") -> R.drawable.pixel_icon_play
        title.contains("关闭") -> R.drawable.ic_power
        title.contains("纹理") -> R.drawable.pixel_icon_palette
        title.contains("网络") -> R.drawable.ic_network
        title.contains("文件") -> R.drawable.pixel_icon_folder
        title.contains("信息")||title.contains("关于")||title.contains("许可") -> R.drawable.pixel_icon_info
        title.contains("性能")||title.contains("高级") -> R.drawable.pixel_icon_system
        else -> R.drawable.pixel_icon_settings
    }
    private fun bindMenu(list:RecyclerView,entries:List<Entry>,flushEdges:Boolean=false) {
        list.layoutManager=GridLayoutManager(context,2)
        list.adapter=object:RecyclerView.Adapter<Holder>() {
            override fun getItemCount()=entries.size
            override fun onCreateViewHolder(p:ViewGroup,t:Int)=Holder(inflater.inflate(R.layout.card_home_option,p,false))
            override fun onBindViewHolder(h:Holder,i:Int) {
                val e=entries[i]
                if(flushEdges) (h.itemView.layoutParams as ViewGroup.MarginLayoutParams).apply {
                    leftMargin=0;rightMargin=0;marginStart=0;marginEnd=0
                }
                h.itemView.findViewById<TextView>(R.id.option_title).text=e.title
                h.itemView.findViewById<TextView>(R.id.option_description).text=e.description
                h.itemView.findViewById<ImageView>(R.id.option_icon).apply {
                    setImageResource(entryIcon(e.title))
                    imageTintList=android.content.res.ColorStateList.valueOf(context.getColor(R.color.gamepad_ps_icon_ink))
                }
                h.itemView.setOnClickListener{e.action()}
            }
        }
        val gap=context.resources.getDimensionPixelSize(R.dimen.spacing_small)
        list.addItemDecoration(object:RecyclerView.ItemDecoration(){
            override fun getItemOffsets(out:android.graphics.Rect,v:View,parent:RecyclerView,state:RecyclerView.State){
                if(flushEdges) {
                    val col=parent.getChildAdapterPosition(v)%2
                    out.set(if(col==0)0 else gap,gap,if(col==1)0 else gap,gap)
                }else out.set(gap,gap,gap,gap)
            }
        })
    }
    private fun menu(title:String,entries:List<Entry>):View {
        val view=inflater.inflate(R.layout.fragment_home_settings,frontend,false)
        view.findViewById<MaterialToolbar>(R.id.toolbar_home_settings).apply {this.title=title;setNavigationOnClickListener{back()}}
        val content=view.findViewById<View>(R.id.linear_layout_settings)
        val inset=GamePadNsHomeGeometry.headerHorizontalInsetPx(context)
        content.setPadding(inset,content.paddingTop,inset,content.paddingBottom)
        bindMenu(view.findViewById(R.id.home_settings_list),entries)
        return view
    }
    private fun gameMenu(game:GameInfo,subtitle:String,entries:List<Entry>):View {
        val view=inflater.inflate(R.layout.fragment_game_properties,frontend,false)
        view.findViewById<View>(R.id.button_back).setOnClickListener{back()}
        view.findViewById<TextView>(R.id.title).text=game.displayTitle(false)
        view.findViewById<TextView>(R.id.playtime).text=subtitle
        val image=view.findViewById<ImageView>(R.id.image_game_screen);PsCovers.show(image,game)
        val content=view.findViewById<View>(R.id.layout_all)
        val inset=GamePadNsHomeGeometry.headerHorizontalInsetPx(context)
        content.setPadding(inset,content.paddingTop,inset,content.paddingBottom)
        view.findViewById<View>(R.id.button_start).visibility=View.GONE
        bindMenu(view.findViewById(R.id.list_properties),entries,true)
        return view
    }
    private fun settingsTab(name:String) {
        val serial=scopedGame?.settingsKey
        InGameOverlay.currentSerial.value=serial
        InGameOverlay.settingsScope.value=if(serial==null)SettingsScope.Global else SettingsScope.Game
        InGameOverlay.settingsState.value=if(serial==null)ConfigStore.loadGlobal() else ConfigStore.resolveForDisplay(serial)
        open("tab:$name")
    }
    private fun settingsBody(name:String):View {
        val view=inflater.inflate(R.layout.fragment_settings,frontend,false)
        view.findViewById<MaterialToolbar>(R.id.toolbar_settings).apply {title=mapOf("renderer" to "图像","performance" to "性能","audio" to "音频","overlay" to "屏幕显示","fixes" to "高级设置","network" to "网络","pad" to "外接手柄","hotkeys" to "热键")[name];setNavigationOnClickListener{back()}}
        val original=view.findViewById<RecyclerView>(R.id.list_settings)
        val parent=original.parent as ViewGroup
        // Same NS page, scrolling behavior and XML padding; replace only the Switch-specific adapter host.
        val content=ComposeView(context).apply {layoutParams=original.layoutParams}
        val left=GamePadNsHomeGeometry.headerHorizontalInsetPx(context)/context.resources.displayMetrics.density
        val top=original.paddingTop/context.resources.displayMetrics.density
        val bottom=original.paddingBottom/context.resources.displayMetrics.density
        parent.removeView(original);parent.addView(content)
        content.setContent {
            com.armsx2.ui.theme.Armsx2Theme {
                val scroll=rememberScrollState()
                CompositionLocalProvider(LocalSettingsScrollState provides scroll) {
                    Column(Modifier.fillMaxSize().verticalScroll(scroll).padding(start=left.dp,end=left.dp,top=top.dp,bottom=bottom.dp)) {
                        when(name) {
                            "renderer" -> RendererTab(InGameOverlay.settingsState)
                            "performance" -> PerformanceTab(InGameOverlay.settingsState)
                            "audio" -> AudioTab(InGameOverlay.settingsState)
                            "overlay" -> OverlayTab(InGameOverlay.settingsState)
                            "fixes" -> FixesTab(InGameOverlay.settingsState)
                            "network" -> NetworkTab(InGameOverlay.settingsState)
                            "pad" -> PadTab(InGameOverlay.settingsState)
                            "hotkeys" -> HotkeysTab(InGameOverlay.settingsState)
                        }
                    }
                }
            }
        };return view
    }
    private fun backendBody():View = ComposeView(context).apply {
        setContent {
            com.armsx2.ui.theme.Armsx2Theme {
                val dismiss={back()}
                when(backendRoute) {
                    com.armsx2.navigation.AppRoute.ControllerManager -> com.armsx2.ui.controls.ControllerManagerScreen(onBack=dismiss)
                    com.armsx2.navigation.AppRoute.TextureManager -> com.armsx2.ui.textures.TextureManagerScreen(onBack=dismiss)
                    com.armsx2.navigation.AppRoute.PatchManager -> com.armsx2.ui.patches.PatchManagerScreen(game=scopedGame,onBack=dismiss)
                    else -> androidx.compose.material3.Text("此功能尚未接入 GamePad PS")
                }
            }
        }
    }
    private fun properties():View {
        val g=checkNotNull(scopedGame)
        return gameMenu(g,g.serial ?:"PlayStation 2",listOf(
            Entry("开始游戏", "继续已保存的游戏进度") {launch(g)},
            Entry("游戏设置", "继承全局设置，可单独调整") {open("settings")},
            Entry("游戏文件", g.uri.path ?:g.uri.toString()){},
            Entry("记忆卡与材料", "查看当前游戏目录") {open("materials")},
        ))
    }
    private fun materials():View {
        val root=GamePadPsAssembler.root
        val game=scopedGame ?:Runtime.currentGame.value
        val local=game?.uri?.let {GamePadPsAssembler.gameForPath(if(it.scheme=="file")it.path.orEmpty() else it.toString())}
        val entries=mutableListOf(
            Entry("BIOS",File(root,"BIOS").listFiles()?.filter{it.extension.equals("bin",true)}?.joinToString{it.name} ?:"未发现"){},
            Entry("LGFS", if(GamePadPsAssembler.lsfgAvailable())"Lossless.dll 已就绪" else "未发现有效 DLL，运行时关闭插帧"){},
            Entry("游戏目录",File(root,"game").path){},
        )
        if(local!=null) entries+=listOf(Entry("记忆卡",File(local.directory,"Memcards").path){},Entry("即时存档",File(local.directory,"Sstates").path){})
        entries+=Entry("重新检查材料", "下次重新打开时重新执行装配检查；已有存档不会覆盖") {com.armsx2.gamepad.PsNotices.makeText(context,"关闭游戏并重新打开应用后自动检查",Toast.LENGTH_LONG).show()}
        return menu("材料与存档",entries)
    }
    private fun osdDialog() {
        val modes=listOf(InGameOverlay.OsdMode.Off,InGameOverlay.OsdMode.Min,InGameOverlay.OsdMode.Full,InGameOverlay.OsdMode.Custom)
        NsSettingViews.show(MaterialAlertDialogBuilder(context).setTitle("屏幕显示").setSingleChoiceItems(modes.map{it.name}.toTypedArray(),modes.indexOf(InGameOverlay.osdMode.value)) {d,index->InGameOverlay.setOsdMode(modes[index]);d.dismiss()}.setNegativeButton("取消",null).create())
    }
    private fun confirm(title:String,body:String,action:()->Unit) {
        NsSettingViews.show(MaterialAlertDialogBuilder(context).setTitle(title).setMessage(body).setPositiveButton("确定"){_,_->action()}.setNegativeButton("取消",null).create())
    }
    private fun beginClose() {
        if(closingGame)return
        pauseSession();closingGame=true
        scopedGame=null;open("library")
        // Let the library draw before scheduling the CPU-thread save/shutdown operation.
        frontend.post { Runtime.stop(saveAutosave=true) }
    }
    private fun confirmExit() {
        if(exitDialog?.isShowing==true)return
        val hasVm=state==EmuState.RUNNING||state==EmuState.PAUSED
        if(!hasVm)return
        val from=page
        exitPromptOpen=true;pauseSession()
        val body=inflater.inflate(R.layout.dialog_emulation_exit_pixel,null)
        val dialog=MaterialAlertDialogBuilder(context,R.style.GameFoldPixel_DialogTheme).setView(body).create()
        exitDialog=dialog
        var leaving=false
        body.findViewById<View>(R.id.continue_button).setOnClickListener{dialog.dismiss()}
        body.findViewById<View>(R.id.exit_button).setOnClickListener {
            leaving=true;dialog.dismiss();exitPromptOpen=false;beginClose()
        }
        NsSettingViews.show(dialog)
        dialog.setOnDismissListener {
            org.yuzu.yuzu_emu.features.input.GameFoldDialogRegistry.unregister(activity,dialog)
            exitDialog=null;exitPromptOpen=false
            if(!leaving) {if(from=="game")resume() else {page=from;rendered="";render()}}
        }
    }
    private fun openLicense() {
        val text=activity.assets.open("gamepad-ps-license-notices.txt").bufferedReader().use{it.readText()}
        NsSettingViews.show(MaterialAlertDialogBuilder(context).setTitle("开源许可").setMessage(text).setPositiveButton("关闭",null).create())
    }
    fun dispose() {disposed=true;release();nav.dispose();shell.setControllerInputListener(null)}
    companion object {var current=WeakReference<PsFrontendHost>(null)}
}
