package com.offex7.streamhub

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.media3.ui.StyledPlayerView
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val Red=Color(0xFFE53935); private val Background=Color(0xFF090909); private val Panel=Color(0xFF151515)

class MainActivity: ComponentActivity(){
 private lateinit var settings: SettingsStore; private lateinit var player: PlayerController
 override fun onCreate(savedInstanceState: Bundle?){ super.onCreate(savedInstanceState); settings=SettingsStore(applicationContext); player=PlayerController(applicationContext); setContent{ Theme{ App(settings,player) } } }
 override fun onStop(){ super.onStop(); player.pause() }
 override fun onDestroy(){ player.release(); super.onDestroy() }
}
@Composable private fun Theme(content:@Composable()->Unit){ MaterialTheme(colorScheme=darkColorScheme(primary=Red,onPrimary=Color.White,secondary=Red,background=Background,surface=Panel,surfaceVariant=Color(0xFF202020),onBackground=Color.White,onSurface=Color.White),content=content) }

@Composable private fun App(settings:SettingsStore, player:PlayerController){
 val scope=rememberCoroutineScope(); val context=LocalContext.current; var section by remember{mutableStateOf<Section?>(null)}; var settingsOpen by remember{mutableStateOf(false)}; var sleepEnd by remember{mutableLongStateOf(0L)}
 LaunchedEffect(Unit){ section=settings.lastSection() }
 val sleepText by produceState<String?>(null,sleepEnd){ while(sleepEnd>0){ val left=sleepEnd-System.currentTimeMillis(); if(left<=0){ value=null;sleepEnd=0;player.stop();(context as? ComponentActivity)?.finishAndRemoveTask();break };value=formatDuration(left);delay(1000) } }
 when{ settingsOpen->SettingsScreen(settings,sleepText,{settingsOpen=false},{m->sleepEnd=System.currentTimeMillis()+m*60000L}){scope.launch{settings.clearSection();section=null;settingsOpen=false;player.stop()}}
  section==null->Picker{chosen->section=chosen;scope.launch{settings.setSection(chosen)}}
  section==Section.RADIO->Radio(player,sleepText,{settingsOpen=true}){section=null;scope.launch{settings.clearSection();player.stop()}}
  else->TV(player,settings,sleepText,{settingsOpen=true}){section=null;scope.launch{settings.clearSection();player.stop()}}
 }
}
@Composable private fun Header(title:String,sleep:String?,back:(()->Unit)?,settings:()->Unit){Row(Modifier.fillMaxWidth().padding(12.dp),verticalAlignment=Alignment.CenterVertically){if(back!=null){IconButton(onClick=back){Icon(Icons.AutoMirrored.Filled.ArrowBack,null,tint=Red)}};Text(title,Modifier.weight(1f),style=MaterialTheme.typography.titleLarge);if(sleep!=null)Text("⏱ $sleep",color=Red);IconButton(onClick=settings){Icon(Icons.Default.Settings,null,tint=Red)}}}
@Composable private fun Picker(onSelect:(Section)->Unit){Surface(color=Background,modifier=Modifier.fillMaxSize()){Column(Modifier.fillMaxSize().padding(24.dp),verticalArrangement=Arrangement.Center,horizontalAlignment=Alignment.CenterHorizontally){Text("STREAMHUB",color=Red,style=MaterialTheme.typography.headlineMedium);Spacer(Modifier.height(28.dp));Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(16.dp)){Big("📺 TV",Modifier.weight(1f)){onSelect(Section.TV)};Big("📻 RADIO",Modifier.weight(1f)){onSelect(Section.RADIO)}}}}}
@Composable private fun Big(text:String,modifier:Modifier,onClick:()->Unit){Button(onClick,modifier.height(90.dp),shape=RoundedCornerShape(18.dp),colors=ButtonDefaults.buttonColors(containerColor=Red)){Text(text)}}

@Composable private fun Radio(player:PlayerController,sleep:String?,settings:()->Unit,back:()->Unit){val playing by player.isPlaying.collectAsState();val err by player.error.collectAsState();var current by remember{mutableStateOf<StreamItem?>(null)};Surface(color=Background,modifier=Modifier.fillMaxSize()){Column{Header("RADIO",sleep,back,settings);LazyColumn(contentPadding=PaddingValues(12.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){items(RADIO_STATIONS,key={it.url}){s->Row(Modifier.fillMaxWidth().background(Panel,RoundedCornerShape(14.dp)).clickable{current=s;player.play(s.url)}.padding(18.dp),verticalAlignment=Alignment.CenterVertically){Text(s.name,Modifier.weight(1f));if(current?.url==s.url&&playing)Text("PLAY",color=Red)}};if(current!=null){item{Text(current!!.name,style=MaterialTheme.typography.titleMedium);if(err!=null)Text(err!!,color=Red);Button({player.toggle()},colors=ButtonDefaults.buttonColors(containerColor=Red),modifier=Modifier.fillMaxWidth()){Text(if(playing)"Пауза" else "Воспроизвести")}}}}}}}

@Composable private fun TV(player:PlayerController,settings:SettingsStore,sleep:String?,settingsClick:()->Unit,back:()->Unit){val scope=rememberCoroutineScope();val repo=remember{PlaylistRepository(LocalContext.current.applicationContext)};var channels by remember{mutableStateOf(emptyList<StreamItem>())};var source by remember{mutableIntStateOf(0)};var selected by remember{mutableIntStateOf(-1)};var loading by remember{mutableStateOf(true)};var error by remember{mutableStateOf<String?>(null)};var full by remember{mutableStateOf(false)};val playError by player.error.collectAsState();LaunchedEffect(Unit){source=settings.sourceIndex();repo.loadCachedIfFresh()?.let{channels=it};loading=channels.isEmpty();repo.refreshInOrder(source).onSuccess{(i,list)->source=i;channels=list;settings.setSourceIndex(i);error=null}.onFailure{error=it.message?:"Не удалось загрузить плейлист"};loading=false}
 if(full&&selected in channels.indices){TVPlayer(player,channels,selected,playError,{full=false},{d->{selected=(selected+d+channels.size)%channels.size;player.play(channels[selected].url)}},{scope.launch{repo.refreshInOrder((source+1)%TV_SOURCES.size).onSuccess{(i,list)->source=i;channels=list;settings.setSourceIndex(i);selected=selected.coerceIn(0,(list.size-1).coerceAtLeast(0));if(selected in list.indices)player.play(list[selected].url)}}});return}
 Surface(color=Background,modifier=Modifier.fillMaxSize()){Column{Header("TV",sleep,back,settingsClick);when{loading->Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center){CircularProgressIndicator(color=Red)};error!=null&&channels.isEmpty()->Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center){Column(horizontalAlignment=Alignment.CenterHorizontally){Text(error!!,color=Red);Button({scope.launch{loading=true;repo.refreshInOrder(source).onSuccess{(i,l)->source=i;channels=l;settings.setSourceIndex(i);error=null}.also{loading=false}}},colors=ButtonDefaults.buttonColors(containerColor=Red)){Text("Другой плейлист")}}};else->LazyColumn(contentPadding=PaddingValues(12.dp),verticalArrangement=Arrangement.spacedBy(7.dp)){itemsIndexed(channels,key={_,it->it.url}){i,c->Text(c.name,Modifier.fillMaxWidth().background(Panel,RoundedCornerShape(12.dp)).clickable{selected=i;player.play(c.url);full=true}.padding(17.dp))}}}}}}

@Composable private fun TVPlayer(player:PlayerController,channels:List<StreamItem>,selected:Int,error:String?,back:()->Unit,switch:(Int)->Unit,other:()->Unit){val playing by player.isPlaying.collectAsState();val activity=LocalContext.current as? ComponentActivity;DisposableEffect(activity){activity?.let{WindowCompat.getInsetsController(it.window,it.window.decorView).hide(WindowInsetsCompat.Type.systemBars())};onDispose{activity?.let{WindowCompat.getInsetsController(it.window,it.window.decorView).show(WindowInsetsCompat.Type.systemBars())}}};Box(Modifier.fillMaxSize().background(Color.Black).pointerInput(selected,channels.size){detectHorizontalDragGestures{_,amount->if(amount>80)switch(-1)else if(amount< -80)switch(1)}}){AndroidView({ctx->StyledPlayerView(ctx).apply{useController=false;player=player.player}},Modifier.fillMaxSize());Column(Modifier.fillMaxWidth().align(Alignment.BottomCenter).padding(16.dp)){Row(Modifier.fillMaxWidth().background(Color(0xAA111111),RoundedCornerShape(14.dp)).padding(8.dp),verticalAlignment=Alignment.CenterVertically){IconButton(back){Icon(Icons.AutoMirrored.Filled.ArrowBack,null,tint=Red)};Text(channels[selected].name,Modifier.weight(1f),maxLines=1);Button({player.toggle()},colors=ButtonDefaults.buttonColors(containerColor=Red)){Text(if(playing)"❚❚" else "▶")}};if(error!=null)Row(Modifier.fillMaxWidth().background(Color(0xDD111111),RoundedCornerShape(12.dp)).padding(8.dp),verticalAlignment=Alignment.CenterVertically){Text(error,color=Red,Modifier.weight(1f));TextButton(other){Text("Другой плейлист",color=Red)}}}}}

@Composable private fun SettingsScreen(store:SettingsStore,sleep:String?,back:()->Unit,sleepSet:(Long)->Unit,reset:()->Unit){val scope=rememberCoroutineScope();var source by remember{mutableIntStateOf(0)};var custom by remember{mutableStateOf("")};LaunchedEffect(Unit){source=store.sourceIndex()};val choices=listOf("15 мин" to 15L,"30 мин" to 30L,"1 ч" to 60L,"2 ч" to 120L,"4 ч" to 240L,"8 ч" to 480L);Surface(color=Background,modifier=Modifier.fillMaxSize()){Column{Header("Настройки",sleep,back,{ });LazyColumn(contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){item{Text("Таймер сна",style=MaterialTheme.typography.titleMedium)};items(choices){(label,v)->Button({sleepSet(v)},Modifier.fillMaxWidth(),colors=ButtonDefaults.buttonColors(containerColor=Red)){Text(label)}};item{OutlinedTextField(custom,{if(it.all(Char::isDigit))custom=it},label={Text("Свои минуты (1–480)")},singleLine=true,modifier=Modifier.fillMaxWidth());Spacer(Modifier.height(8.dp));Button({custom.toLongOrNull()?.coerceIn(1,480)?.let(sleepSet)},colors=ButtonDefaults.buttonColors(containerColor=Red),modifier=Modifier.fillMaxWidth()){Text("Запустить свой таймер")}};item{Text("Источник ТВ-плейлиста",style=MaterialTheme.typography.titleMedium)};items(TV_SOURCES.indices.toList()){i->TextButton({source=i;scope.launch{store.setSourceIndex(i)}},Modifier.fillMaxWidth()){Text((if(source==i)"●  " else "○  ")+TV_SOURCES[i].name,color=if(source==i)Red else Color.White)}};item{Button(reset,colors=ButtonDefaults.buttonColors(containerColor=Red),modifier=Modifier.fillMaxWidth()){Text("Сбросить выбор раздела")}}}}}}
private fun formatDuration(ms:Long):String{val t=(ms/1000).coerceAtLeast(0);return if(t>=3600)"%d:%02d:%02d".format(t/3600,(t%3600)/60,t%60) else "%02d:%02d".format(t/60,t%60)}
