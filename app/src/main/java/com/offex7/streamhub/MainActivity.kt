package com.offex7.streamhub

import android.app.PictureInPictureParams
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color as AColor
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.LocalHapticFeedback
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlin.math.max
import kotlin.math.min

private val Red = Color(0xFFE53935)
private val Bg = Color(0xFF121212)
private val Panel = Color(0xFF1A1A1A)
private val PanelAlt = Color(0xFF232323)
private val Gray = Color(0xFF808080)
private const val TELEGRAM = "https://t.me/TvRadioOnline"
private const val WALLET = "TCo8GJ3F5WAAQLq1GTvi5BY3r5acBw6pbX"
private const val RESTORE_WINDOW = 10 * 60 * 1000L
private val zoomByChannel = mutableMapOf<String, Float>()

class MainActivity : ComponentActivity() {
    private lateinit var store: SettingsStore
    private lateinit var tv: PlayerController
    private lateinit var radio: RadioMediaController
    internal var pipEnabled = true
    internal var tvViewing = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = AColor.TRANSPARENT
        window.navigationBarColor = AColor.TRANSPARENT
        store = SettingsStore(applicationContext)
        tv = PlayerController(applicationContext)
        radio = RadioMediaController(applicationContext)
        setContent { AppTheme { App(store, tv, radio, this) } }
    }

    override fun onUserLeaveHint() {
        if (tvViewing && pipEnabled && Build.VERSION.SDK_INT >= 26 && !isInPictureInPictureMode) {
            enterPictureInPictureMode(PictureInPictureParams.Builder().setAspectRatio(Rational(16, 9)).build())
        }
        super.onUserLeaveHint()
    }

    override fun onStop() {
        lifecycleScope.launch { store.setLastExitTime(System.currentTimeMillis()) }
        super.onStop()
    }

    override fun onDestroy() {
        tv.release()
        radio.release()
        super.onDestroy()
    }
}

@Composable private fun AppTheme(content: @Composable () -> Unit) = MaterialTheme(colorScheme = androidx.compose.material3.darkColorScheme(primary = Red, secondary = Red, background = Bg, surface = Panel, surfaceVariant = PanelAlt), content = content)

@Composable
private fun App(store: SettingsStore, tv: PlayerController, radio: RadioMediaController, activity: MainActivity) {
    val scope = rememberCoroutineScope()
    val snack = remember { SnackbarHostState() }
    var section by rememberSaveable { mutableStateOf<Section?>(null) }
    var settings by rememberSaveable { mutableStateOf(false) }
    var disclaimer by rememberSaveable { mutableStateOf(false) }
    var exit by rememberSaveable { mutableStateOf(false) }
    var restore by remember { mutableStateOf<StreamItem?>(null) }
    var pip by remember { mutableStateOf(true) }
    var sleepUntil by remember { mutableLongStateOf(0L) }
    var sleepRemaining by remember { mutableLongStateOf(0L) }
    var sleepMinutes by remember { mutableLongStateOf(0L) }

    LaunchedEffect(Unit) {
        pip = store.pipEnabled(); activity.pipEnabled = pip
        val lastExit = store.lastExitTime(); val s = store.lastSection()
        if (s != null && lastExit > 0L && System.currentTimeMillis() - lastExit < RESTORE_WINDOW) { section = s; restore = store.lastStream(s) }
    }
    LaunchedEffect(sleepUntil) {
        if (sleepUntil <= 0L) { sleepRemaining = 0L; return@LaunchedEffect }
        while (true) {
            val left = sleepUntil - System.currentTimeMillis()
            if (left <= 0L) { sleepUntil = 0L; sleepRemaining = 0L; sleepMinutes = 0L; tv.stop(); radio.stop(); snack.showSnackbar("Таймер сна — отключён", duration = SnackbarDuration.Short); break }
            sleepRemaining = left; delay(1000L)
        }
    }

    fun leave() {
        section = null; settings = false; disclaimer = false; restore = null; activity.tvViewing = false
        tv.stop(); radio.stop(); scope.launch { store.clearSection() }
    }

    BackHandler {
        when { disclaimer -> disclaimer = false; settings -> settings = false; section != null -> leave(); else -> exit = true }
    }

    Scaffold(snackbarHost = { SnackbarHost(snack) }, containerColor = Bg, contentWindowInsets = WindowInsets(0,0,0,0)) { pad ->
        Surface(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.systemBars.union(WindowInsets.displayCutout)).padding(pad), color = Bg) {
            when {
                disclaimer -> DisclaimerScreen { disclaimer = false }
                settings -> SettingsScreen(
                    store, pip, sleepRemaining, sleepUntil, sleepMinutes,
                    onPip = { v -> pip = v; activity.pipEnabled = v; scope.launch { store.setPipEnabled(v) } },
                    onSleep = { m -> sleepMinutes = m; sleepUntil = System.currentTimeMillis() + m * 60_000L; scope.launch { snack.showSnackbar("Таймер сна — запущен", duration = SnackbarDuration.Short) } },
                    onCancelSleep = { sleepUntil = 0L; sleepRemaining = 0L; sleepMinutes = 0L; scope.launch { snack.showSnackbar("Таймер сна — отключён", duration = SnackbarDuration.Short) } },
                    onResetStats = { target -> scope.launch { runCatching { store.resetUsage(target) }.onSuccess { snack.showSnackbar(if (target == Section.TV) "Счётчик ТВ сброшен" else "Счётчик Радио сброшен", duration = SnackbarDuration.Short) }.onFailure { snack.showSnackbar("Не удалось сбросить счётчик", duration = SnackbarDuration.Short) } } },
                    onSource = { i -> scope.launch { store.setSourceIndex(i); snack.showSnackbar("Источник сохранён", duration = SnackbarDuration.Short) } },
                    onDisclaimer = { disclaimer = true },
                    onResetAll = { scope.launch { store.resetAll(); sleepUntil = 0L; sleepRemaining = 0L; sleepMinutes = 0L; pip = true; activity.pipEnabled = true; section = null; settings = false; disclaimer = false; restore = null; snack.showSnackbar("Настройки сброшены", duration = SnackbarDuration.Short) } }
                )
                section == null -> HomeScreen { if (it == null) settings = true else { section = it; scope.launch { store.setSection(it) } } }
                section == Section.TV -> TvScreen(tv, store, restore, { restore = null }, { item -> restore = null; scope.launch { store.saveLastStream(Section.TV, item) } }, ::leave) { activity.tvViewing = it }
                else -> RadioScreen(radio, store, restore, { restore = null }, { item -> restore = null; scope.launch { store.saveLastStream(Section.RADIO, item) } }, ::leave)
            }
        }
    }
    if (exit) AlertDialog(onDismissRequest = { exit = false }, title = { Text("Выйти из приложения?") }, text = { Text("Закрыть TV / Radio. Online?") }, confirmButton = { TextButton(onClick = { activity.finishAndRemoveTask() }) { Text("Выйти", color = Red) } }, dismissButton = { TextButton(onClick = { exit = false }) { Text("Отмена") } })
}

@Composable private fun HomeScreen(onSelect: (Section?) -> Unit) {
    val c = LocalContext.current
    androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxSize().padding(18.dp)) {
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.End) {
                Text("TV_RADIO_ONLINE_V4.0", color = Color.Gray, fontSize = 10.sp)
                IconButton(onClick = { onSelect(null) }) { Icon(Icons.Default.Settings, "Настройки", tint = Red) }
            }
            if (maxWidth >= 560.dp) {
                Row(Modifier.fillMaxWidth().weight(1f).padding(vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    HomeCard("ТЕЛЕВИЗОР", "start_tv", Modifier.weight(1f)) { onSelect(Section.TV) }
                    HomeCard("РАДИО", "start_radio", Modifier.weight(1f)) { onSelect(Section.RADIO) }
                }
            } else {
                Column(Modifier.fillMaxWidth().weight(1f).padding(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    HomeCard("ТЕЛЕВИЗОР", "start_tv", Modifier.fillMaxWidth().weight(1f)) { onSelect(Section.TV) }
                    HomeCard("РАДИО", "start_radio", Modifier.fillMaxWidth().weight(1f)) { onSelect(Section.RADIO) }
                }
            }
            Spacer(Modifier.height(24.dp))
            Card(onClick = { openUrl(c, TELEGRAM) }, Modifier.fillMaxWidth().height(54.dp), colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(16.dp)) { Box(Modifier.fillMaxSize(), Alignment.Center) { Text("Порекомендовать проект друзьям", color = Red, fontSize = 13.sp) } }
        }
    }
}

@Composable private fun HomeCard(title: String, resourceName: String, modifier: Modifier, onClick: () -> Unit) {
    val ctx = LocalContext.current; val id = remember(resourceName) { ctx.resources.getIdentifier(resourceName, "drawable", ctx.packageName) }
    Card(onClick = onClick, modifier = modifier, colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.SpaceBetween) {
            Spacer(Modifier.height(4.dp)); if (id != 0) Image(painterResource(id), title, Modifier.fillMaxWidth(0.58f).aspectRatio(1.1f)) else Box(Modifier.fillMaxWidth(0.58f).aspectRatio(1.1f).background(PanelAlt, RoundedCornerShape(14.dp)))
            Text(title, fontSize = 22.sp, fontWeight = FontWeight.Bold); Spacer(Modifier.height(4.dp))
        }
    }
}

@Composable private fun Header(title: String, session: String, back: () -> Unit, settings: (() -> Unit)?, searchOpen: Boolean, q: String, openSearch: () -> Unit, onQ: (String) -> Unit, closeSearch: () -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = back) { Icon(Icons.Default.ArrowBack, "Назад", tint = Red) }
            Text(title, Modifier.weight(1f), fontSize = 20.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            Text(session, color = Red, fontSize = 11.sp)
            if (!searchOpen) { IconButton(onClick = openSearch) { Icon(Icons.Default.Search, "Поиск", tint = Red) }; settings?.let { IconButton(onClick = it) { Icon(Icons.Default.Settings, "Настройки", tint = Red) } } }
            else IconButton(onClick = closeSearch) { Icon(Icons.Default.Close, "Закрыть", tint = Red) }
        }
        if (searchOpen) OutlinedTextField(q, onQ, Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 2.dp), singleLine = true, placeholder = { Text("Поиск…") }, trailingIcon = { if (q.isNotEmpty()) IconButton(onClick = closeSearch) { Icon(Icons.Default.Close, null) } })
    }
}

@Composable private fun SearchTimeout(open: Boolean, text: String, stamp: Long, close: () -> Unit) { LaunchedEffect(open, text, stamp) { if (open) { val s = stamp; delay(if (text.isBlank()) 5000L else 15000L); if (open && s == stamp) close() } } }

@Composable private fun RadioScreen(player: RadioMediaController, store: SettingsStore, restore: StreamItem?, dismissRestore: () -> Unit, save: (StreamItem) -> Unit, back: () -> Unit, settings: () -> Unit) {
    val scope = rememberCoroutineScope(); val currentIndex by player.currentIndex.collectAsState(); val playing by player.isPlaying.collectAsState(); val error by player.error.collectAsState(); val net by rememberNetworkState()
    val list = rememberLazyListState(); var q by rememberSaveable { mutableStateOf("") }; var search by rememberSaveable { mutableStateOf(false) }; var stamp by remember { mutableLongStateOf(System.currentTimeMillis()) }; var favs by remember { mutableStateOf(emptySet<String>()) }; var secs by remember { mutableLongStateOf(0L) }; var notice by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) { favs = store.favorites(Section.RADIO); val p = store.scrollPosition(Section.RADIO); list.scrollToItem(p.first.coerceAtMost(RADIO_STATIONS.lastIndex), p.second); val started = System.currentTimeMillis(); while (true) { delay(1000L); secs = (System.currentTimeMillis()-started)/1000L } }
    LaunchedEffect(Unit) { var n=0L; while(true){delay(1000); n++; if(n>=5){store.addUsageSeconds(Section.RADIO,n); n=0}} }
    LaunchedEffect(list) { snapshotFlow { list.firstVisibleItemIndex to list.firstVisibleItemScrollOffset }.collect { store.saveScrollPosition(Section.RADIO,it.first,it.second) } }
    LaunchedEffect(list) { val a=LocalContext.current as? ComponentActivity ?: return@LaunchedEffect; val ic=WindowInsetsControllerCompat(a.window,a.window.decorView); snapshotFlow{list.isScrollInProgress}.collect{if(it)ic.hide(WindowInsetsCompat.Type.navigationBars())} }
    SearchTimeout(search,q,stamp){q="";search=false}; BackHandler(enabled=search){q="";search=false;stamp=System.currentTimeMillis()}
    val current=RADIO_STATIONS.getOrNull(currentIndex); val filtered=favs.firstOrNull().let{fuzzyRadio(RADIO_STATIONS.sortedWith(compareByDescending<StreamItem>{favs.contains(it.key)}.thenBy{it.name}),q)}
    Surface(Modifier.fillMaxSize(), color=Bg){ Column(Modifier.fillMaxSize()){if(!net)NetworkBanner();Header("РАДИО",formatTime(secs),back,settings,search,q,{search=true;stamp=System.currentTimeMillis()},{q=it;stamp=System.currentTimeMillis()},{q="";search=false;stamp=System.currentTimeMillis()});current?.let{st->Card(Modifier.fillMaxWidth().padding(10.dp,4.dp),colors=CardDefaults.cardColors(containerColor=Panel),shape=RoundedCornerShape(14.dp)){Row(Modifier.fillMaxWidth().height(60.dp).padding(8.dp),verticalAlignment=Alignment.CenterVertically){LocalLogo(st,46.dp);Spacer(Modifier.width(8.dp));Column(Modifier.weight(1f)){Text(st.name.uppercase(Locale.ROOT),fontSize=18.sp,maxLines=1,fontWeight=FontWeight.SemiBold);Text(if(playing)"играет" else "пауза",fontSize=11.sp,color=Color.LightGray)};IconButton(onClick={player.previous()}){Icon(Icons.Default.SkipPrevious,null,tint=Red)};IconButton(onClick={player.toggle()}){Icon(if(playing)Icons.Default.Pause else Icons.Default.PlayArrow,null,tint=Red)};IconButton(onClick={player.next()}){Icon(Icons.Default.SkipNext,null,tint=Red)}}}}
        restore?.let{RestoreBanner(it,{val i=RADIO_STATIONS.indexOfFirst{s->s.url==it.url};if(i>=0){player.play(i);save(it)}},dismissRestore)}
        LazyColumn(state=list,Modifier.fillMaxSize(),contentPadding=PaddingValues(10.dp),verticalArrangement=Arrangement.spacedBy(7.dp)){items(filtered,key={it.key}){st->val f=favs.contains(st.key);ChannelRow(st,f,st.url==current?.url&&playing,false,{player.play(RADIO_STATIONS.indexOf(st));save(st)},{scope.launch{runCatching{store.setFavorite(Section.RADIO,st.key,!f);favs=store.favorites(Section.RADIO);notice=if(!f)"Добавлено в избранное" else "Удалено из избранного"}.onFailure{notice="Не удалось обновить избранное"}}})}};if(notice!=null){TextButton(onClick={notice=null}){Text(notice!!,color=Red,fontSize=11.sp)}}}}
    }
}

@Composable private fun TvScreen(player: PlayerController, store: SettingsStore, restore: StreamItem?, dismissRestore:()->Unit, save:(StreamItem)->Unit, back:()->Unit, settings:()->Unit, fullscreenChanged:(Boolean)->Unit) {
    val ctx=LocalContext.current; val scope=rememberCoroutineScope(); val repo=remember{PlaylistRepository(ctx.applicationContext)}; val list=rememberLazyListState(); val net by rememberNetworkState(); val error by player.error.collectAsState(); val waiting by player.waitingForNetwork.collectAsState(); var channels by remember{mutableStateOf(emptyList<StreamItem>())}; var favs by remember{mutableStateOf(emptySet<String>())}; var health by remember{mutableStateOf(emptyMap<String,AvailabilityStatus>())}; var full by rememberSaveable{mutableStateOf(false)}; var selected by rememberSaveable{mutableIntStateOf(-1)}; var q by rememberSaveable{mutableStateOf("")};var search by rememberSaveable{mutableStateOf(false)};var stamp by remember{mutableLongStateOf(System.currentTimeMillis())};var secs by remember{mutableLongStateOf(0L)};var loading by remember{mutableStateOf(true)};var refreshing by remember{mutableStateOf(false)};var notice by remember{mutableStateOf<String?>(null)};var source by remember{mutableIntStateOf(0)}
    LaunchedEffect(Unit){source=store.sourceIndex();favs=store.favorites(Section.TV);val p=store.scrollPosition(Section.TV);list.scrollToItem(p.first,p.second);repo.loadCached(source)?.let{channels=it};repo.loadSource(source).onSuccess{channels=it;loading=false;scope.launch(Dispatchers.IO){repo.warmFallbacks()}}.onFailure{loading=false;notice="Возникла проблема. Обсуждаем решения в Telegram."};val started=System.currentTimeMillis();while(true){delay(1000);secs=(System.currentTimeMillis()-started)/1000L}}
    LaunchedEffect(Unit){var n=0L;while(true){delay(1000);n++;if(n>=5){store.addUsageSeconds(Section.TV,n);n=0}}}
    LaunchedEffect(list){snapshotFlow{list.firstVisibleItemIndex to list.firstVisibleItemScrollOffset}.collect{store.saveScrollPosition(Section.TV,it.first,it.second)}}
    LaunchedEffect(channels){if(channels.isNotEmpty()){health=scanAvailability(channels.take(15));while(true){delay(30*60*1000L);health=scanAvailability(channels)}}}
    SearchTimeout(search,q,stamp){q="";search=false};BackHandler(enabled=search&&!full){q="";search=false;stamp=System.currentTimeMillis()};LaunchedEffect(full){fullscreenChanged(full)}
    fun play(idx:Int){if(idx in channels.indices){selected=idx;scope.launch{player.playWithFallback(listOf(channels[idx].url)+repo.fallbackUrlsFor(channels[idx].name))}}}
    LaunchedEffect(restore,channels){val it=restore?:return@LaunchedEffect;val idx=channels.indexOfFirst{c->c.url==it.url||c.name.equals(it.name,true)};if(idx>=0&&!full){selected=idx}}
    val filtered=favs.firstOrNull().let{fuzzyTV(channels.sortedWith(compareByDescending<StreamItem>{favs.contains(it.key)}.thenBy{it.name}),q)}
    if(full&&selected in channels.indices){TvPlayerScreen(player,channels[selected],error,waiting||!net,{full=false},{var i=selected-1;while(i>=0&&health[channels[i].url]==AvailabilityStatus.OFFLINE)i--;if(i<0)i=channels.lastIndex;play(i);save(channels[i])},{var i=selected+1;while(i<channels.size&&health[channels[i].url]==AvailabilityStatus.OFFLINE)i++;if(i>=channels.size)i=0;play(i);save(channels[i])},{zoomByChannel.remove(channels[selected].key)}, {full=false});return}
    Surface(Modifier.fillMaxSize(),color=Bg){Column(Modifier.fillMaxSize()){if(!net)NetworkBanner();Header("ТВ",formatTime(secs),back,settings,search,q,{search=true;stamp=System.currentTimeMillis()},{q=it;stamp=System.currentTimeMillis()},{q="";search=false;stamp=System.currentTimeMillis()});restore?.let{RestoreBanner(it,{val idx=channels.indexOfFirst{c->c.url==it.url||c.name.equals(it.name,true)};if(idx>=0){play(idx);full=true;save(channels[idx]);dismissRestore()}},dismissRestore)};when{loading->Skeleton();filtered.isEmpty()->Text(if(q.isBlank())"Плейлист пуст" else "Ничего не найдено",Modifier.fillMaxWidth().padding(40.dp),color=Color.Gray,textAlign=TextAlign.Center);else->PullList(refreshing,{scope.launch{refreshing=true;repo.loadSource(source).onSuccess{channels=it;health=emptyMap()}.onFailure{notice="Возникла проблема. Обсуждаем решения в Telegram."};refreshing=false}},{list,filtered,favs,health,{item->play(channels.indexOfFirst{it.url==item.url});full=true;save(item)},{item->val f=favs.contains(item.key);scope.launch{runCatching{store.setFavorite(Section.TV,item.key,!f);favs=store.favorites(Section.TV);notice=if(!f)"Добавлено в избранное" else "Удалено из избранного"}.onFailure{notice="Не удалось обновить избранное"}}})}}};if(notice!=null)TextButton(onClick={notice=null}){Text(notice!!,color=Red,fontSize=11.sp)}}}}
}

@Composable private fun PullList(refreshing:Boolean,onRefresh:()->Unit,list:androidx.compose.foundation.lazy.LazyListState,itemsList:List<StreamItem>,favs:Set<String>,health:Map<String,AvailabilityStatus>,onPlay:(StreamItem)->Unit,onFav:(StreamItem)->Unit){androidx.compose.material3.pulltorefresh.PullToRefreshBox(isRefreshing=refreshing,onRefresh=onRefresh,Modifier.fillMaxSize()){LazyColumn(state=list,Modifier.fillMaxSize(),contentPadding=PaddingValues(10.dp),verticalArrangement=Arrangement.spacedBy(7.dp)){items(itemsList,key={it.key}){item->ChannelRow(item,favs.contains(item.key),false,health[item.url]==AvailabilityStatus.OFFLINE,onPlay,{onFav(item)})}}}}

@Composable private fun TvPlayerScreen(player:PlayerController,channel:StreamItem,error:String?,waiting:Boolean,onBack:()->Unit,onPrev:()->Unit,onNext:()->Unit,onReset:()->Unit,onClose:()->Unit){val cfg=LocalConfiguration.current;val portrait=cfg.screenWidthDp<cfg.screenHeightDp;val haptic=LocalHapticFeedback.current;var controls by remember(channel.key){mutableStateOf(true)};var taps by remember{mutableIntStateOf(0)};var zoom by remember(channel.key){mutableFloatStateOf(zoomByChannel[channel.key]?:1f)};val controller=player;LaunchedEffect(controls){if(controls){delay(5000);controls=false}};BackHandler{onClose()};Box(Modifier.fillMaxSize().background(Color.Black).pointerInput(channel.key){detectTransformGestures{_,_,z,_->zoom=(zoom*z).coerceIn(1f,3f);zoomByChannel[channel.key]=zoom;controls=true}}){AndroidView(Modifier.fillMaxSize().graphicsLayer(scaleX=zoom,scaleY=zoom),factory={ctx->PlayerView(ctx).apply{useController=false;this.player=controller.player;setShutterBackgroundColor(AColor.BLACK)}});AnimatedVisibility(controls,Modifier.align(Alignment.TopCenter).windowInsetsPadding(WindowInsets.displayCutout),enter=slideInVertically()+fadeIn(),exit=slideOutVertically()+fadeOut()){Card(Modifier.fillMaxWidth().padding(8.dp),colors=CardDefaults.cardColors(containerColor=Panel.copy(alpha=.95f)),shape=RoundedCornerShape(14.dp)){Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){IconButton(onClick=onClose){Icon(Icons.Default.ArrowBack,null,tint=Red)};Text(channel.name,Modifier.weight(1f),color=Color.White,fontSize=if(portrait)15.sp else 17.sp,maxLines=1);IconButton(onClick=onPrev){Icon(Icons.Default.SkipPrevious,null,tint=Red)};IconButton(onClick=onNext){Icon(Icons.Default.SkipNext,null,tint=Red)}}}};if(waiting)Text("Сигнал утерян, перепроверьте подключение к сети",Modifier.align(Alignment.BottomCenter).padding(12.dp),color=Color.White,fontSize=13.sp,textAlign=TextAlign.Center);if(error!=null)Text("Возникла проблема. Обсуждаем решения в Telegram.",Modifier.align(Alignment.Center).padding(20.dp),color=Color.White,textAlign=TextAlign.Center);Box(Modifier.matchParentSize().pointerInput(channel.key){androidx.compose.foundation.gestures.detectTapGestures(onTap={controls=true;taps++;if(taps>=3){zoom=1f;zoomByChannel.remove(channel.key);onReset();taps=0};haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)})})}}

@OptIn(ExperimentalLayoutApi::class)
@Composable private fun SettingsScreen(store:SettingsStore,pip:Boolean,sleepRemaining:Long,sleepUntil:Long,sleepMinutes:Long,onPip:(Boolean)->Unit,onSleep:(Long)->Unit,onCancelSleep:()->Unit,onResetStats:(Section)->Unit,onSource:(Int)->Unit,onDisclaimer:()->Unit,onResetAll:()->Unit){val tvU by store.usageFlow(Section.TV).collectAsState(0L);val rU by store.usageFlow(Section.RADIO).collectAsState(0L);var src by remember{mutableIntStateOf(0)};var dlg by remember{mutableStateOf(false)};LaunchedEffect(Unit){src=store.sourceIndex()};LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(14.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){item{Text("Настройки",fontSize=22.sp,fontWeight=FontWeight.Bold)};item{Card(colors=CardDefaults.cardColors(containerColor=Panel),shape=RoundedCornerShape(16.dp)){Column(Modifier.padding(14.dp)){Text("Таймер сна",fontWeight=FontWeight.Bold);Spacer(Modifier.height(8.dp));val opts=listOf(15L to "15 мин",30L to "30 мин",60L to "1 ч",120L to "2 ч",240L to "4 ч",480L to "8 ч",600L to "10 ч",900L to "15 ч",1440L to "24 ч",2160L to "36 ч");FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){opts.forEach{(m,label)->val active=m==sleepMinutes&&sleepUntil>System.currentTimeMillis();val bg by animateColorAsState(if(active)Red else PanelAlt,label="sleep");Card(onClick={if(active)onCancelSleep() else onSleep(m)},Modifier.size(66.dp),colors=CardDefaults.cardColors(containerColor=bg),shape=CircleShape,border=BorderStroke(1.dp,if(active)Red else Gray)){Box(Modifier.fillMaxSize(),Alignment.Center){Text(if(active)formatTimerCircle(sleepRemaining) else label,color=Color.White,fontSize=if(active)11.sp else 12.sp,textAlign=TextAlign.Center)}}}}}}};item{DonationCard()};item{Row(Modifier.fillMaxWidth().background(Panel,RoundedCornerShape(14.dp)).padding(14.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text("PiP при сворачивании",fontSize=16.sp);Text("Сохраняется после перезапуска",fontSize=11.sp,color=Color.Gray)};Switch(checked=pip,onCheckedChange=onPip,colors=SwitchDefaults.colors(checkedThumbColor=Color.White,checkedTrackColor=Red,uncheckedThumbColor=Color.White,uncheckedTrackColor=Gray))}};item{Card(colors=CardDefaults.cardColors(containerColor=Panel),shape=RoundedCornerShape(16.dp)){Column(Modifier.padding(14.dp)){Text("Статистика",fontSize=17.sp,fontWeight=FontWeight.Bold);Text("Общее время использования ТВ: ${formatUsage(tvU)}",color=Color.LightGray);OutlinedButton(onClick={onResetStats(Section.TV)},Modifier.fillMaxWidth()){Text("Сбросить счётчик ТВ")};Text("Общее время использования Радио: ${formatUsage(rU)}",color=Color.LightGray);OutlinedButton(onClick={onResetStats(Section.RADIO)},Modifier.fillMaxWidth()){Text("Сбросить счётчик Радио")}}}};item{Card(colors=CardDefaults.cardColors(containerColor=Panel),shape=RoundedCornerShape(16.dp)){Column(Modifier.padding(14.dp)){Text("Источник ТВ-плейлиста",fontWeight=FontWeight.Bold);Text(TV_SOURCES.getOrNull(src)?.name?:"",color=Color.Gray,fontSize=12.sp);Button(onClick={dlg=true},Modifier.fillMaxWidth()){Text("Выбрать источник")}}}};item{OutlinedButton(onClick=onDisclaimer,Modifier.fillMaxWidth()){Text("Отказ от ответственности")}};item{Button(onClick=onResetAll,Modifier.fillMaxWidth(),colors=ButtonDefaults.buttonColors(containerColor=PanelAlt)){Text("Сбросить настройки до заводских")}}};if(dlg)AlertDialog(onDismissRequest={dlg=false},title={Text("Источник ТВ-плейлиста")},text={Column{TV_SOURCES.forEachIndexed{i,s->Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){RadioButton(src==i,onClick={src=i;dlg=false;onSource(i)});Text(s.name,Modifier.padding(6.dp))}}}},confirmButton={TextButton(onClick={dlg=false}){Text("Закрыть")}})}

@Composable private fun DonationCard(){val c=LocalContext.current;Card(colors=CardDefaults.cardColors(containerColor=Panel),shape=RoundedCornerShape(16.dp),border=BorderStroke(1.dp,Red.copy(alpha=.4f)),modifier=Modifier.fillMaxWidth()){Column(Modifier.fillMaxWidth().padding(14.dp),horizontalAlignment=Alignment.CenterHorizontally){Row(verticalAlignment=Alignment.CenterVertically){Icon(Icons.Default.Favorite,null,tint=Red);Spacer(Modifier.width(8.dp));Text("Поддержать проект",fontSize=18.sp,fontWeight=FontWeight.Bold)};Spacer(Modifier.height(8.dp));Text(WALLET,color=Color.LightGray,fontSize=12.sp,textAlign=TextAlign.Center);Button(onClick={val cb=c.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager;cb.setPrimaryClip(ClipData.newPlainText("Wallet",WALLET))},colors=ButtonDefaults.buttonColors(containerColor=Red)){Text("Копировать")};Spacer(Modifier.height(10.dp));Surface(Modifier.size(180.dp),color=Color.White,shape=RoundedCornerShape(8.dp)){Image(painterResource(com.offex7.streamhub.R.drawable.qr_donate),null,Modifier.fillMaxSize().padding(4.dp))}}}}

@Composable private fun DisclaimerScreen(back:()->Unit){val c=LocalContext.current;LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(16.dp)){item{Row(verticalAlignment=Alignment.CenterVertically){IconButton(onClick=back){Icon(Icons.Default.ArrowBack,null,tint=Red)};Text("Отказ от ответственности",fontSize=20.sp,fontWeight=FontWeight.Bold)}};item{Text("Приложение работает с открытых источников трансляции, которые находятся в свободном доступе. Приложение является бесплатным и работает на добровольных пожертвованиях. Все авторские права сохранены за авторами контента.\n\nПриложение не хранит, не распространяет и не модифицирует транслируемый контент. Все трансляции предоставляются третьими лицами. Разработчик не несёт ответственности за содержание транслируемого контента.\n\nЕсли вы являетесь правообладателем и считаете, что ваши права нарушаются — свяжитесь с нами через Telegram: $TELEGRAM",color=Color.LightGray,fontSize=14.sp,lineHeight=21.sp)};item{Button(onClick={openUrl(c,TELEGRAM),}){Text("Telegram")}}}}

@Composable private fun RestoreBanner(it:StreamItem,onGo:()->Unit,onClose:()->Unit){Card(Modifier.fillMaxWidth().padding(10.dp),colors=CardDefaults.cardColors(containerColor=PanelAlt),shape=RoundedCornerShape(12.dp)){Column(Modifier.padding(12.dp)){Text("Продолжить?",fontWeight=FontWeight.Bold);Text(it.name,color=Red,maxLines=2);Row(Modifier.fillMaxWidth().padding(top=8.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)){Button(onClick=onGo,Modifier.weight(1f)){Text("Продолжить")};OutlinedButton(onClick=onClose,Modifier.weight(1f)){Text("Закрыть")}}}}}
@Composable private fun NetworkBanner(){Row(Modifier.fillMaxWidth().background(Color(0xFF2B1B1B)).padding(8.dp),verticalAlignment=Alignment.CenterVertically){Icon(Icons.Default.WifiOff,null,tint=Color.LightGray,modifier=Modifier.size(16.dp));Spacer(Modifier.width(6.dp));Text("Сигнал утерян, перепроверьте подключение к сети",color=Color.LightGray,fontSize=12.sp)}}
@Composable private fun Skeleton(){LazyColumn(contentPadding=PaddingValues(10.dp),verticalArrangement=Arrangement.spacedBy(7.dp)){items(8){Box(Modifier.fillMaxWidth().height(70.dp).background(PanelAlt,RoundedCornerShape(12.dp)))}}}
@Composable private fun ChannelRow(it:StreamItem,fav:Boolean,playing:Boolean,offline:Boolean,onPlay:()->Unit,onFav:()->Unit){val col by animateColorAsState(if(fav)Red else Color.White,label="favcolor");val scale by animateFloatAsState(if(fav)1.16f else 1f,animationSpec=spring(),label="favscale");Card(onClick=onPlay,Modifier.fillMaxWidth(),colors=CardDefaults.cardColors(containerColor=if(fav)Red.copy(alpha=.08f) else Panel),shape=RoundedCornerShape(12.dp)){Row(Modifier.padding(10.dp),verticalAlignment=Alignment.CenterVertically){LocalLogo(it,56.dp,offline);Spacer(Modifier.width(10.dp));Column(Modifier.weight(1f)){Text(it.name.uppercase(Locale.ROOT),color=if(offline)Gray else Color.White,fontWeight=if(playing)FontWeight.Bold else FontWeight.Medium,maxLines=1,fontSize=14.sp);if(offline)Text("• offline",color=Gray,fontSize=10.sp)};IconButton(onClick=onFav,Modifier.graphicsLayer(scaleX=scale,scaleY=scale)){Icon(if(fav)Icons.Default.Favorite else Icons.Default.FavoriteBorder,null,tint=col)}}}}
@Composable private fun LocalLogo(it:StreamItem,size:Dp,dim:Boolean=false){val ctx=LocalContext.current;val name=logoName(it.name);val id=remember(name){ctx.resources.getIdentifier(name,"drawable",ctx.packageName)};if(id!=0)Image(painterResource(id),it.name,Modifier.size(size).alpha(if(dim).4f else 1f))else Box(Modifier.size(size).background(Color(0xFF2A2A2A),RoundedCornerShape(12.dp)).alpha(if(dim).4f else 1f),Alignment.Center){Text("NO Image",color=Color.Gray,fontSize=9.sp,textAlign=TextAlign.Center)}}
private fun logoName(n:String)=when(n.uppercase(Locale.ROOT)){"RECORD"->"logo_record";"CHOCOLATE"->"logo_chocolate";"ЭНЕРДЖИ"->"logo_energy";"ULTRA"->"logo_ultra";"КАЛЬЯН РЭП"->"logo_kalyan";"PIRATE STATION"->"logo_pirate";"VOCAL DRUM"->"logo_vocal";"CHILL HOUSE"->"logo_chill";"PSY TRANCE"->"logo_psy";"METALCORE"->"logo_metalcore";"ЮГ МОЛОДОЙ"->"logo_yug";"RELAX"->"logo_relax";else->"logo_fallback"}
private suspend fun scanAvailability(items:List<StreamItem>):Map<String,AvailabilityStatus>{val client=OkHttpClient.Builder().connectTimeout(3,TimeUnit.SECONDS).readTimeout(3,TimeUnit.SECONDS).callTimeout(3,TimeUnit.SECONDS).build();val sem=Semaphore(2);val out=ConcurrentHashMap<String,AvailabilityStatus>();kotlinx.coroutines.coroutineScope{items.map{item->launch(Dispatchers.IO){sem.acquire();try{out[item.url]=runCatching{client.newCall(Request.Builder().url(item.url).head().build()).execute().use{if(it.isSuccessful||it.code in 300..399)AvailabilityStatus.ONLINE else AvailabilityStatus.OFFLINE}}.getOrDefault(AvailabilityStatus.OFFLINE)}finally{sem.release()}}}.forEach{it.join()}};return out}
private fun fuzzyTV(items:List<StreamItem>,q:String)=if(q.isBlank())items else items.filter{it.name.contains(q.trim(),true)}
private fun fuzzyRadio(items:List<StreamItem>,q:String)=if(q.isBlank())items else items.filter{it.name.contains(q.trim(),true)}
private fun formatTime(s:Long)=if(s<3600)"%02d:%02d".format(s/60,s%60) else "%02d:%02d:%02d".format(s/3600,(s/60)%60,s%60)
private fun formatUsage(s:Long)="${s/3600} ч ${(s/60)%60} мин"
private fun formatTimerCircle(ms:Long):String{val t=max(0L,ms/1000);val h=t/3600;val m=(t/60)%60;val sec=t%60;return if(h>0)"%02d:%02d".format(h,m) else "%02d:%02d".format(m,sec)}
private fun openUrl(c:Context,u:String){runCatching{c.startActivity(Intent(Intent.ACTION_VIEW,android.net.Uri.parse(u)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))}}

@Composable private fun rememberNetworkState():androidx.compose.runtime.State<Boolean>{val ctx=LocalContext.current;return androidx.compose.runtime.produceState(initialValue=networkNow(ctx)){val cm=ctx.getSystemService(ConnectivityManager::class.java);val cb=object:ConnectivityManager.NetworkCallback(){override fun onAvailable(n:Network){value=true};override fun onLost(n:Network){value=networkNow(ctx)}};runCatching{cm.registerDefaultNetworkCallback(cb)};awaitDispose{runCatching{cm.unregisterNetworkCallback(cb)}}}}
private fun networkNow(c:Context):Boolean{val cm=c.getSystemService(ConnectivityManager::class.java);return cm.allNetworks.any{cm.getNetworkCapabilities(it)?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)==true}}
