package com.offex7.streamhub
import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

object InteractionFeedback {
    private fun vibrator(context: Context): Vibrator? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) context.getSystemService(VibratorManager::class.java)?.defaultVibrator
        else { @Suppress("DEPRECATION") context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator }

    fun vibrate(context: Context, enabled:Boolean, durationMs:Long=45L, amplitude:Int=120):Boolean{
        if(!enabled)return false
        val v=vibrator(context)?:return false
        if(!v.hasVibrator())return false
        return runCatching{v.vibrate(VibrationEffect.createOneShot(durationMs.coerceIn(15L,250L),amplitude.coerceIn(1,255)));true}.getOrDefault(false)
    }
    fun threshold(context:Context,enabled:Boolean):Boolean=vibrate(context,enabled,34L,110)
    fun success(context:Context,enabled:Boolean):Boolean{
        if(!enabled)return false
        val v=vibrator(context)?:return false
        if(!v.hasVibrator())return false
        return runCatching{v.vibrate(VibrationEffect.createWaveform(longArrayOf(0L,38L,48L,55L),intArrayOf(0,105,0,125),-1));true}.getOrDefault(false)
    }
    fun error(context:Context,enabled:Boolean):Boolean{
        if(!enabled)return false
        val v=vibrator(context)?:return false
        if(!v.hasVibrator())return false
        return runCatching{v.vibrate(VibrationEffect.createWaveform(longArrayOf(0L,55L,55L,55L),intArrayOf(0,135,0,135),-1));true}.getOrDefault(false)
    }
    fun beep(context:Context,enabled:Boolean):Boolean=if(!enabled)false else runCatching{
        val tone=ToneGenerator(AudioManager.STREAM_MUSIC,62)
        try{tone.startTone(ToneGenerator.TONE_PROP_BEEP,35)}finally{tone.release()}
        true
    }.getOrDefault(false)
    fun click(context:Context,hapticsEnabled:Boolean,soundEnabled:Boolean,allowSound:Boolean=true){
        if(vibrate(context,hapticsEnabled,42L,105))return
        if(allowSound)beep(context,soundEnabled)
    }
}