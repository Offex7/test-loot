package com.radiotv.tvremote.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Bundle
import android.os.SystemClock
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.radiotv.tvremote.core.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs

class TVRemoteFragment : Fragment() {
    private var remote: RemoteDevice? = null
    private var castController: CastController? = null
    private var mediaProvider: RadioTvMediaProvider? = null
    private var airMouse: AirMouseEngine? = null
    private var voiceJob: Job? = null
    private lateinit var status: TextView
    private lateinit var root: LinearLayout

    fun setCastController(controller: CastController?) { castController = controller }
    fun setMediaProvider(provider: RadioTvMediaProvider?) { mediaProvider = provider }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View {
        root = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(10), dp(6), dp(10), dp(6))
            setBackgroundColor(Color.rgb(29, 28, 40))
        }
        buildUi()

        val viewport = FrameLayout(requireContext()).apply { setBackgroundColor(Color.BLACK) }
        val preferredWidth = dp(420)
        val screenWidth = resources.displayMetrics.widthPixels
        val viewportWidth = minOf(preferredWidth, maxOf(screenWidth, dp(280)))

        val scroll = ScrollView(requireContext()).apply {
            isFillViewport = true
            overScrollMode = View.OVER_SCROLL_IF_CONTENT_SCROLLS
            addView(root, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }

        viewport.addView(
            scroll,
            FrameLayout.LayoutParams(viewportWidth, ViewGroup.LayoutParams.MATCH_PARENT).apply {
                gravity = Gravity.CENTER
            }
        )
        return viewport
    }

    private fun buildUi() {
        val header = LinearLayout(requireContext()).apply {
            gravity = Gravity.CENTER_VERTICAL
            orientation = LinearLayout.HORIZONTAL
        }
        val title = TextView(requireContext()).apply {
            text = "TV ПУЛЬТ"
            setTextColor(Color.WHITE)
            textSize = 18f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        status = TextView(requireContext()).apply {
            text = "  •  Не подключено"
            setTextColor(Color.LTGRAY)
            textSize = 12f
            gravity = Gravity.CENTER_VERTICAL
        }
        header.addView(title, LinearLayout.LayoutParams(0, dp(42), 1f))
        header.addView(status, LinearLayout.LayoutParams(0, dp(42), 1.6f))
        header.addView(button("▣", 48, null) { showDevices() })
        root.addView(header)

        root.addView(row(
            button("⏻", 70, TvKey.POWER), button("SOURCE", 105, TvKey.INPUT),
            button("MUTE", 80, TvKey.MUTE)
        ))

        val main = LinearLayout(requireContext()).apply { orientation = LinearLayout.HORIZONTAL }
        val sideLeft = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
        }
        sideLeft.addView(button("VOL +", 64, TvKey.VOLUME_UP))
        sideLeft.addView(space(6))
        sideLeft.addView(button("VOL −", 64, TvKey.VOLUME_DOWN))
        sideLeft.addView(space(16))
        sideLeft.addView(button("CH +", 64, TvKey.CHANNEL_UP))
        sideLeft.addView(space(6))
        sideLeft.addView(button("CH −", 64, TvKey.CHANNEL_DOWN))
        main.addView(sideLeft, LinearLayout.LayoutParams(dp(68), -2))
        main.addView(dpad(), LinearLayout.LayoutParams(0, dp(222), 1f))

        val sideRight = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
        }
        sideRight.addView(button("HOME", 64, TvKey.HOME))
        sideRight.addView(space(6))
        sideRight.addView(button("BACK", 64, TvKey.BACK))
        sideRight.addView(space(16))
        sideRight.addView(button("MENU", 64, TvKey.MENU))
        sideRight.addView(space(6))
        sideRight.addView(button("OK", 64, TvKey.OK))
        main.addView(sideRight, LinearLayout.LayoutParams(dp(68), -2))
        root.addView(main)

        root.addView(sectionLabel("ЦИФРЫ КАНАЛА"))
        root.addView(numericPad())
        root.addView(mediaRow())
        root.addView(space(6))
        root.addView(sectionLabel("ИНСТРУМЕНТЫ"))
        root.addView(toolRow())
        root.addView(space(5))
        root.addView(touchPad())
    }

    private fun dpad(): View {
        val grid = GridLayout(requireContext()).apply {
            rowCount = 3
            columnCount = 3
            setPadding(dp(4), dp(4), dp(4), dp(4))
        }
        fun cell(v: View, r: Int, c: Int, size: Int) {
            grid.addView(v, GridLayout.LayoutParams(
                GridLayout.spec(r), GridLayout.spec(c)
            ).apply {
                width = dp(size); height = dp(size)
                setMargins(dp(3), dp(3), dp(3), dp(3))
            })
        }
        cell(View(requireContext()), 0, 0, 62)
        cell(button("▲", 74, TvKey.UP), 0, 1, 62)
        cell(View(requireContext()), 0, 2, 62)
        cell(button("◀", 74, TvKey.LEFT), 1, 0, 62)
        cell(button("OK", 84, TvKey.OK), 1, 1, 76)
        cell(button("▶", 74, TvKey.RIGHT), 1, 2, 62)
        cell(View(requireContext()), 2, 0, 62)
        cell(button("▼", 74, TvKey.DOWN), 2, 1, 62)
        cell(View(requireContext()), 2, 2, 62)
        return FrameLayout(requireContext()).apply {
            addView(grid, FrameLayout.LayoutParams(-1, -1).apply { gravity = Gravity.CENTER })
        }
    }

    private fun numericPad(): View {
        val box = LinearLayout(requireContext()).apply { orientation = LinearLayout.VERTICAL }
        val labels = arrayOf("1","2","3","4","5","6","7","8","9","⌫","0","↵")
        repeat(4) { r ->
            val line = LinearLayout(requireContext())
            repeat(3) { c ->
                val label = labels[r * 3 + c]
                val key = when (label) {
                    "0" -> TvKey.DIGIT_0; "1" -> TvKey.DIGIT_1; "2" -> TvKey.DIGIT_2
                    "3" -> TvKey.DIGIT_3; "4" -> TvKey.DIGIT_4; "5" -> TvKey.DIGIT_5
                    "6" -> TvKey.DIGIT_6; "7" -> TvKey.DIGIT_7; "8" -> TvKey.DIGIT_8
                    "9" -> TvKey.DIGIT_9; "⌫" -> TvKey.DELETE; else -> TvKey.ENTER
                }
                val v = button(label, 0, key)
                line.addView(v, LinearLayout.LayoutParams(0, dp(38), 1f).apply {
                    setMargins(dp(2), dp(2), dp(2), dp(2))
                })
            }
            box.addView(line, LinearLayout.LayoutParams(-1, dp(43)))
        }
        return box
    }

    private fun mediaRow(): View = row(
        button("↶", 72, TvKey.REWIND),
        button("▶/Ⅱ", 88, TvKey.PLAY_PAUSE),
        button("■", 72, TvKey.STOP),
        button("↷", 72, TvKey.FAST_FORWARD)
    )

    private fun toolRow(): View {
        val line = LinearLayout(requireContext()).apply { orientation = LinearLayout.HORIZONTAL }
        val actions = listOf(
            "🎙" to { startVoice() },
            "⌨" to { showKeyboard() },
            "⌁" to { startTouchMode() },
            "◉" to { startAirMouse() },
            "⤴" to { startCast() }
        )
        actions.forEach { (label, action) ->
            line.addView(button(label, 0, null, action), LinearLayout.LayoutParams(0, dp(50), 1f).apply {
                setMargins(dp(2), 0, dp(2), 0)
            })
        }
        return line
    }

    private fun touchPad(): View = TouchPadView(requireContext()).also { pad ->
        pad.layoutParams = LinearLayout.LayoutParams(-1, dp(70)).apply { topMargin = dp(5) }
        pad.setBackground(round(Color.rgb(49, 47, 61), 18f))
        pad.onGesture = { dx, dy, type ->
            val r = remote
            when {
                r == null -> Toast.makeText(requireContext(), "Сначала подключите ТВ", Toast.LENGTH_SHORT).show()
                type == GestureType.MOVE && r.supports(Capability.POINTER) ->
                    send(RemoteCommand.PointerMove(dx, dy))
                type == GestureType.MOVE -> {
                    val key = if (abs(dx) >= abs(dy)) {
                        if (dx > 0) TvKey.RIGHT else TvKey.LEFT
                    } else if (dy > 0) TvKey.DOWN else TvKey.UP
                    send(RemoteCommand.Key(key))
                }
                type == GestureType.CLICK && r.supports(Capability.POINTER) ->
                    send(RemoteCommand.PointerClick)
                type == GestureType.CLICK ->
                    send(RemoteCommand.Key(TvKey.OK))
                type == GestureType.RIGHT_CLICK && r.supports(Capability.POINTER) ->
                    send(RemoteCommand.PointerRightClick)
                type == GestureType.RIGHT_CLICK ->
                    send(RemoteCommand.Key(TvKey.BACK))
            }
        }
    }

    private fun startTouchMode() {
        Toast.makeText(requireContext(), "Тач-панель активна внизу: свайп — курсор/навигация, тап — OK, долгий тап — контекст/Back.", Toast.LENGTH_SHORT).show()
    }

    private fun startAirMouse() {
        if (airMouse != null) {
            airMouse?.stop()
            airMouse = null
            Toast.makeText(requireContext(), "Air Mouse выключена.", Toast.LENGTH_SHORT).show()
            return
        }
        val r = remote ?: run {
            Toast.makeText(requireContext(), "Сначала подключите ТВ", Toast.LENGTH_SHORT).show(); return
        }
        airMouse?.stop()
        val engine = AirMouseEngine(requireContext()) { dx, dy ->
            if (r.supports(Capability.POINTER)) r.send(RemoteCommand.PointerMove(dx, dy))
        }
        if (!engine.hasGyroscope) {
            Toast.makeText(requireContext(), "На телефоне нет гироскопа — Air Mouse отключена.", Toast.LENGTH_LONG).show()
            return
        }
        if (!r.supports(Capability.POINTER)) {
            Toast.makeText(requireContext(), "Для этого ТВ реальный курсор по Wi‑Fi не заявлен; D‑Pad и тач-навигация остаются доступны.", Toast.LENGTH_LONG).show()
            return
        }
        airMouse = engine
        engine.start()
        Toast.makeText(requireContext(), "Air Mouse включена. Нажмите ещё раз для остановки.", Toast.LENGTH_SHORT).show()
    }

    private fun startVoice() {
        val r = remote
        if (r !is AndroidTvRemoteV2) {
            Toast.makeText(requireContext(), "Потоковый голос через Android TV Remote v2 доступен для Android TV / Google TV.", Toast.LENGTH_LONG).show()
            return
        }
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.RECORD_AUDIO), 900)
            Toast.makeText(requireContext(), "Разрешите микрофон и повторите.", Toast.LENGTH_SHORT).show()
            return
        }
        voiceJob?.cancel()
        voiceJob = viewLifecycleOwner.lifecycleScope.launch {
            try {
                val session = r.startVoice()
                withContext(Dispatchers.IO) {
                    val min = AudioRecord.getMinBufferSize(8000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
                    val record = AudioRecord(
                        MediaRecorder.AudioSource.MIC, 8000,
                        AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, maxOf(8192, min)
                    )
                    record.startRecording()
                    val buffer = ByteArray(8192)
                    val until = SystemClock.elapsedRealtime() + 10_000
                    while (SystemClock.elapsedRealtime() < until) {
                        val n = record.read(buffer, 0, buffer.size, AudioRecord.READ_BLOCKING)
                        if (n > 0) session.sendPcm(buffer.copyOf(n))
                    }
                    record.stop(); record.release()
                }
                session.finish()
                Toast.makeText(requireContext(), "Голос передан на ТВ", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), e.message ?: "Ошибка голосового режима", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun showKeyboard() {
        if (remote == null) {
            Toast.makeText(requireContext(), "Сначала подключите ТВ", Toast.LENGTH_SHORT).show(); return
        }
        val edit = EditText(requireContext()).apply {
            hint = "Поиск / логин / пароль"; setSingleLine(true)
        }
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Клавиатура TV").setView(edit)
            .setNegativeButton("Отмена", null)
            .setPositiveButton("Отправить") { _, _ -> send(RemoteCommand.Text(edit.text.toString())) }
            .show()
        edit.post {
            edit.requestFocus()
            (requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
                .showSoftInput(edit, InputMethodManager.SHOW_IMPLICIT)
        }
    }

    private fun startCast() {
        val cast = castController ?: run {
            Toast.makeText(requireContext(), "DLNA-модуль не подключён", Toast.LENGTH_LONG).show(); return
        }
        val media = mediaProvider?.currentMedia() ?: run {
            Toast.makeText(requireContext(), "Radio.TV не передал текущий медиапоток.", Toast.LENGTH_LONG).show(); return
        }
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val renderers = cast.discover()
                if (renderers.isEmpty()) {
                    Toast.makeText(requireContext(), "DLNA/UPnP-устройства не найдены.", Toast.LENGTH_LONG).show()
                    return@launch
                }
                val names = renderers.map { it.name }.toTypedArray()
                androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setTitle("Транслировать на ТВ").setItems(names) { _, which ->
                        viewLifecycleOwner.lifecycleScope.launch {
                            runCatching { cast.play(renderers[which], media) }
                                .onSuccess { Toast.makeText(requireContext(), "Запуск: " + renderers[which].name, Toast.LENGTH_SHORT).show() }
                                .onFailure { Toast.makeText(requireContext(), it.message ?: "DLNA ошибка", Toast.LENGTH_LONG).show() }
                        }
                    }.show()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), e.message ?: "Ошибка DLNA", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun showDevices() {
        viewLifecycleOwner.lifecycleScope.launch {
            val items = mutableListOf<RemoteDeviceInfo>()
            runCatching { items += AndroidTvDiscovery(requireContext()).scan() }
            runCatching { items += LanDiscovery.scan(requireContext()) }
            val unique = items.distinctBy { it.id }.toTypedArray()
            val labels = unique.map { it.name + "  [" + it.protocol.name.replace('_', ' ') + "]  " + it.host }.toTypedArray()
            if (unique.isEmpty()) {
                androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setTitle("Телевизоры и приставки")
                    .setMessage("Автопоиск ничего не нашёл. Можно добавить устройство по IP.")
                    .setNeutralButton("Ввести IP") { _, _ -> manualDevice() }
                    .setNegativeButton("Закрыть", null).show()
                return@launch
            }
            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Телевизоры и приставки").setItems(labels) { _, which -> connectTo(unique[which]) }
                .setNeutralButton("Ввести IP") { _, _ -> manualDevice() }
                .setNegativeButton("Закрыть", null).show()
        }
    }

    private fun manualDevice() {
        val box = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL; setPadding(dp(10), 0, dp(10), 0)
        }
        val ip = EditText(requireContext()).apply {
            hint = "192.168.1.50"; inputType = android.text.InputType.TYPE_CLASS_PHONE
        }
        val spinner = Spinner(requireContext())
        val protocols = DeviceProtocol.values()
        spinner.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, protocols.map { it.name })
        box.addView(ip); box.addView(spinner)
        androidx.appcompat.app.AlertDialog.Builder(requireContext()).setTitle("Добавить устройство")
            .setView(box).setNegativeButton("Отмена", null)
            .setPositiveButton("Подключить") { _, _ ->
                val p = protocols[spinner.selectedItemPosition]
                val port = when (p) {
                    DeviceProtocol.ANDROID_TV_V2 -> 6466
                    DeviceProtocol.SAMSUNG_TIZEN -> 8001
                    DeviceProtocol.LG_WEBOS -> 3000
                    DeviceProtocol.ROKU_ECP -> 8060
                }
                connectTo(RemoteDeviceInfo(p.name + ":" + ip.text, p.name, ip.text.toString().trim(), port, p))
            }.show()
    }

    private fun connectTo(info: RemoteDeviceInfo) {
        viewLifecycleOwner.lifecycleScope.launch {
            remote?.disconnect()
            val next = runCatching {
                RemoteDeviceFactory.create(requireContext(), info).also { it.connect() }
            }.getOrNull()
            if (next != null) {
                remote = next
                status.text = "  •  " + info.name + "  •  Подключено"
                Toast.makeText(requireContext(), info.name + " подключён", Toast.LENGTH_SHORT).show()
                return@launch
            }
            if (info.protocol == DeviceProtocol.ANDROID_TV_V2) {
                val code = EditText(requireContext()).apply { hint = "A1B2C3"; setSingleLine(true) }
                androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setTitle("Код с Android TV")
                    .setMessage("Введите 6-символьный HEX-код, показанный на экране ТВ.")
                    .setView(code).setNegativeButton("Отмена", null)
                    .setPositiveButton("Сопрячь") { _, _ ->
                        viewLifecycleOwner.lifecycleScope.launch {
                            val atv = RemoteDeviceFactory.create(requireContext(), info) as AndroidTvRemoteV2
                            runCatching { atv.pair(code.text.toString()); atv.connect() }
                                .onSuccess {
                                    remote = atv
                                    status.text = "  •  " + info.name + "  •  Подключено"
                                }
                                .onFailure { Toast.makeText(requireContext(), it.message ?: "Pairing failed", Toast.LENGTH_LONG).show() }
                        }
                    }.show()
            } else {
                Toast.makeText(requireContext(), "Не удалось подключиться к " + info.name, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun send(command: RemoteCommand) {
        val r = remote ?: run {
            Toast.makeText(requireContext(), "Сначала подключите ТВ", Toast.LENGTH_SHORT).show(); return
        }
        requireView().performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        viewLifecycleOwner.lifecycleScope.launch {
            runCatching { r.send(command) }
                .onFailure { Toast.makeText(requireContext(), it.message ?: "Команда недоступна", Toast.LENGTH_SHORT).show() }
        }
    }

    private fun button(text: String, width: Int, key: TvKey?, action: (() -> Unit)? = null): TextView =
        TextView(requireContext()).apply {
            this.text = text
            setTextColor(Color.WHITE)
            textSize = if (text.length > 5) 10.5f else 17f
            gravity = Gravity.CENTER
            background = round(Color.rgb(55, 53, 68), 16f)
            isClickable = true
            isFocusable = true
            setOnClickListener { action?.invoke() ?: key?.let { send(RemoteCommand.Key(it)) } }
            minHeight = dp(40)
            if (width > 0) layoutParams = LinearLayout.LayoutParams(dp(width), dp(40)).apply {
                setMargins(dp(2), dp(2), dp(2), dp(2))
            }
        }

    private fun row(vararg views: View): View = LinearLayout(requireContext()).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER
        views.forEach { addView(it) }
    }

    private fun sectionLabel(text: String): TextView = TextView(requireContext()).apply {
        this.text = text; setTextColor(Color.LTGRAY); textSize = 9f
        setPadding(dp(5), dp(3), 0, dp(1))
    }

    private fun space(h: Int): View = Space(requireContext()).apply { layoutParams = LinearLayout.LayoutParams(1, dp(h)) }
    private fun round(color: Int, radius: Float): GradientDrawable = GradientDrawable().apply {
        setColor(color); cornerRadius = dp(radius.toInt()).toFloat()
    }
    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    override fun onDestroyView() {
        voiceJob?.cancel()
        airMouse?.stop()
        airMouse = null
        super.onDestroyView()
    }

    companion object {
        fun newInstance() = TVRemoteFragment()
    }
}

private enum class GestureType { MOVE, CLICK, RIGHT_CLICK }

private class TouchPadView(context: Context) : View(context) {
    var onGesture: (dx: Int, dy: Int, type: GestureType) -> Unit = { _, _, _ -> }
    private var downX = 0f
    private var downY = 0f
    private var downTime = 0L

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.x; downY = event.y
                downTime = SystemClock.elapsedRealtime()
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = ((event.x - downX) * 1.4f).toInt()
                val dy = ((event.y - downY) * 1.4f).toInt()
                if (abs(dx) + abs(dy) > 6) {
                    onGesture(dx.coerceIn(-40, 40), dy.coerceIn(-40, 40), GestureType.MOVE)
                    downX = event.x; downY = event.y
                }
                return true
            }
            MotionEvent.ACTION_UP -> {
                val elapsed = SystemClock.elapsedRealtime() - downTime
                onGesture(0, 0, if (elapsed > 600) GestureType.RIGHT_CLICK else GestureType.CLICK)
                performClick()
                return true
            }
        }
        return true
    }

    override fun performClick(): Boolean = super.performClick()
}
