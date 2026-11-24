package app.galaxybypass

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle

class MainActivity : ComponentActivity() {

    private val vm: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val actions =
                Actions(
                        refresh = vm::retry,
                        getShizuku = {
                            startActivity(
                                    Intent(
                                            Intent.ACTION_VIEW,
                                            Uri.parse("https://shizuku.rikka.app/download/")
                                    )
                            )
                        },
                        openShizuku = {
                            packageManager.getLaunchIntentForPackage(SHIZUKU_PACKAGE)?.let {
                                startActivity(it)
                            }
                        },
                        grantPermission = vm::requestPermission,
                        gamingMode = vm::gamingMode,
                        restoreDefaults = vm::restoreDefaults,
                        setBypass = vm::setBypass,
                        setPackage = vm::setPackageEnabled,
                )

        setContent {
            AppTheme {
                val ui = vm.ui.collectAsStateWithLifecycle().value
                MainScreen(ui, actions)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        vm.onResume() // re-check Shizuku after the user comes back from its app
    }
}
