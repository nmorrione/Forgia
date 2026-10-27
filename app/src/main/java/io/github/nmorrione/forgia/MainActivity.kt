package io.github.nmorrione.forgia

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import io.github.nmorrione.forgia.ui.AppViewModel
import io.github.nmorrione.forgia.ui.ForgiaApp
import io.github.nmorrione.forgia.ui.Tab

class MainActivity : ComponentActivity() {
    companion object {
        const val EXTRA_OPEN_TIMER = "open_timer"
    }

    private val vm: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handle(intent)
        setContent { ForgiaApp(vm) }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handle(intent)
    }

    /** Dalla notifica del timer si apre direttamente la sezione Timer. */
    private fun handle(intent: Intent?) {
        if (intent?.getBooleanExtra(EXTRA_OPEN_TIMER, false) == true) {
            vm.stack.clear()
            vm.tab = Tab.TIMER
        }
    }

    override fun onPause() {
        super.onPause()
        vm.saveNow()
    }
}
