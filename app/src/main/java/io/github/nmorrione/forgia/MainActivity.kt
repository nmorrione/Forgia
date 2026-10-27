package io.github.nmorrione.forgia

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import io.github.nmorrione.forgia.ui.AppViewModel
import io.github.nmorrione.forgia.ui.ForgiaApp

class MainActivity : ComponentActivity() {
    private val vm: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { ForgiaApp(vm) }
    }

    override fun onPause() {
        super.onPause()
        vm.saveNow()
    }
}
