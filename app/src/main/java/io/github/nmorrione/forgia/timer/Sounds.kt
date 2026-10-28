package io.github.nmorrione.forgia.timer

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import java.util.concurrent.Executors
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

enum class Cue { TICK, WORK, REST, FINISH, ALARM }

/** Durata di un giro del cicalino (quattro beep e una pausa), per ripeterlo a tempo. */
const val ALARM_CYCLE_MS = 1100L

/**
 * Beep sintetizzati: un'unica traccia audio aperta per tutta la durata del timer,
 * così ogni suono parte senza ritardo.
 */
class Beeper(alarmChannel: Boolean) {
    private val exec = Executors.newSingleThreadExecutor { r -> Thread(r, "forgia-beeper") }
    private val track: AudioTrack

    init {
        val attrs = AudioAttributes.Builder()
            .setUsage(if (alarmChannel) AudioAttributes.USAGE_ALARM else AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        val format = AudioFormat.Builder()
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setSampleRate(RATE)
            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
            .build()
        val minBuf = AudioTrack.getMinBufferSize(RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
        track = AudioTrack.Builder()
            .setAudioAttributes(attrs)
            .setAudioFormat(format)
            .setBufferSizeInBytes(max(minBuf, RATE / 10 * 2))
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
        track.play()
    }

    fun play(cue: Cue) {
        runCatching {
            exec.execute {
                val pcm = pcm(cue)
                runCatching { track.write(pcm, 0, pcm.size) }
            }
        }
    }

    fun release() {
        exec.execute {
            Thread.sleep(300) // lascia finire l'ultimo suono nel buffer
            runCatching {
                track.pause()
                track.flush()
                track.release()
            }
        }
        exec.shutdown()
    }

    companion object {
        private const val RATE = 44_100

        private class Note(val hz: Double, val ms: Int, val gapMs: Int = 0)

        private val cache = mutableMapOf<Cue, ShortArray>()

        @Synchronized
        private fun pcm(cue: Cue): ShortArray = cache.getOrPut(cue) {
            val notes = when (cue) {
                // Ultimi tre secondi
                Cue.TICK -> listOf(Note(880.0, 140))
                // Inizio lavoro: acuto e lungo
                Cue.WORK -> listOf(Note(1318.5, 550))
                // Inizio recupero: grave e lungo
                Cue.REST -> listOf(Note(659.3, 550))
                // Fine del circuito: arpeggio che sale, due volte, poi nota lunga
                Cue.FINISH -> listOf(
                    Note(659.3, 150, 40), Note(830.6, 150, 40), Note(987.8, 150, 40), Note(1318.5, 380, 160),
                    Note(659.3, 150, 40), Note(830.6, 150, 40), Note(987.8, 150, 40), Note(1318.5, 900),
                )
                // Cicalino da timer: quattro beep acuti e brevi, poi una pausa (in tutto ALARM_CYCLE_MS)
                Cue.ALARM -> List(4) { i -> Note(1975.5, 80, if (i < 3) 70 else 510) }
            }
            synth(notes)
        }

        private fun synth(notes: List<Note>): ShortArray {
            val total = notes.sumOf { (it.ms + it.gapMs) * RATE / 1000 }
            val out = ShortArray(total)
            var pos = 0
            val fade = RATE * 0.006
            for (n in notes) {
                val len = n.ms * RATE / 1000
                for (i in 0 until len) {
                    val t = i.toDouble() / RATE
                    val env = min(1.0, min(i / fade, (len - i) / fade))
                    // Fondamentale più seconda armonica: si sente meglio dall'altoparlante del telefono
                    val v = (sin(2 * PI * n.hz * t) + 0.35 * sin(4 * PI * n.hz * t)) / 1.35
                    out[pos++] = (v * env * 0.85 * Short.MAX_VALUE).toInt().toShort()
                }
                pos += n.gapMs * RATE / 1000
            }
            return out
        }
    }
}

/** Il suono della sveglia di sistema, in loop finché non viene fermato. */
class AlarmPlayer(private val ctx: Context) {
    private var mp: MediaPlayer? = null

    fun start(): Boolean = runCatching {
        val uri = RingtoneManager.getActualDefaultRingtoneUri(ctx, RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        mp = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
            setDataSource(ctx, uri)
            isLooping = true
            prepare()
            start()
        }
        true
    }.getOrElse {
        stop()
        false
    }

    fun stop() {
        mp?.let { runCatching { it.stop() }; it.release() }
        mp = null
    }
}

class Buzzer(ctx: Context) {
    private val vibrator: Vibrator? =
        if (Build.VERSION.SDK_INT >= 31) ctx.getSystemService(VibratorManager::class.java)?.defaultVibrator
        else @Suppress("DEPRECATION") ctx.getSystemService(Vibrator::class.java)

    fun once(ms: Long) {
        runCatching { vibrator?.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE)) }
    }

    fun pattern(timings: LongArray, repeat: Boolean) {
        runCatching { vibrator?.vibrate(VibrationEffect.createWaveform(timings, if (repeat) 0 else -1)) }
    }

    fun cancel() {
        runCatching { vibrator?.cancel() }
    }
}
