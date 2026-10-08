package com.radiotv.tvremote

import android.graphics.Color
import android.os.Bundle
import android.widget.FrameLayout
import androidx.fragment.app.FragmentActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.radiotv.tvremote.cast.DlnaCastController
import com.radiotv.tvremote.ui.TVRemoteFragment

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.BLACK
        window.navigationBarColor = Color.BLACK

        val frame = FrameLayout(this).apply {
            id = android.view.View.generateViewId()
            setBackgroundColor(Color.BLACK)
        }
        setContentView(frame)

        ViewCompat.setOnApplyWindowInsetsListener(frame) { view, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        if (savedInstanceState == null) {
            val remote = TVRemoteFragment.newInstance().apply {
                setCastController(DlnaCastController())
            }
            supportFragmentManager.beginTransaction()
                .replace(frame.id, remote)
                .commit()
        }
    }
}
