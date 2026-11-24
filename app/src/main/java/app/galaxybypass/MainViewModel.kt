package app.galaxybypass

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class UiState(
        val shizuku: ShizukuState = ShizukuState.NotRunning,
        val status: Bypass.Status? = null,
        val busy: Boolean = false,
        val log: List<String> = emptyList(),
) {
    /** Gaming Performance Mode / Normal Mode (Default) / custom, as read back from the phone. */
    val mode: Bypass.Mode?
        get() = status?.mode
}

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val controller = ShizukuController(app)
    private val mutex = Mutex() // one batch of commands at a time

    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui.asStateFlow()

    init {
        controller.start()
        viewModelScope.launch {
            controller.state.collectLatest { s ->
                _ui.update {
                    it.copy(shizuku = s, status = if (s == ShizukuState.Ready) it.status else null)
                }
                when (s) {
                    ShizukuState.Ready -> refreshStatus()
                    ShizukuState.Connecting -> {
                        // cancelled by collectLatest as soon as the state changes
                        delay(CONNECT_TIMEOUT_MS)
                        controller.connectTimedOut()
                    }
                    else -> Unit
                }
            }
        }
    }

    override fun onCleared() {
        controller.stop()
    }

    // Called from the UI
    fun onResume() = controller.refresh()

    fun retry() {
        controller.retry()
        refreshStatus()
    }

    fun requestPermission() = controller.requestPermission()

    fun refreshStatus() {
        viewModelScope.launch { mutex.withLock { loadStatus() } }
    }

    /** Option [1] of the .bat: Enable Power Bypass & Disable GOS. */
    fun gamingMode() = run("Gaming Performance Mode", Bypass.gamingModeSteps(installedPackages()))

    /** Option [2] of the .bat: Disable Power Bypass & Restore Defaults. */
    fun restoreDefaults() = run("Normal Mode (Default)", Bypass.restoreSteps(installedPackages()))

    fun setBypass(on: Boolean) {
        val label = if (on) "Enabling Power Bypass" else "Disabling Power Bypass"
        run(label, listOf(Bypass.Step(label, Bypass.setPassThrough(on))))
    }

    fun setPackageEnabled(id: String, enabled: Boolean) {
        val pkg = Bypass.GAME_PACKAGES.first { it.id == id }
        val label = "${if (enabled) "Enabling" else "Disabling"} ${pkg.label}"
        run(label, listOf(Bypass.Step(label, Bypass.setPackageEnabled(id, enabled))))
    }

    // Internals

    private fun installedPackages(): List<Bypass.GamePackage> =
            _ui.value.status?.packages?.filter { it.installed }?.map { it.pkg }
                    ?: Bypass.GAME_PACKAGES

    private fun run(title: String, steps: List<Bypass.Step>) {
        viewModelScope.launch {
            mutex.withLock {
                _ui.update { it.copy(busy = true) }
                log("▶ $title")
                var allOk = true
                steps.forEachIndexed { i, step ->
                    val r = controller.exec(step.command)
                    val line = "[${i + 1}/${steps.size}] ${step.label}..."
                    if (r.ok) {
                        log("$line OK")
                    } else {
                        allOk = false
                        val detail =
                                r.output.lineSequence().firstOrNull { it.isNotBlank() }.orEmpty()
                        log("$line FAILED\n    ${step.command}\n    $detail".trimEnd())
                    }
                }
                // read the real state back, the .bat just prints DONE
                val st = loadStatus()
                log(
                        when {
                            !allOk -> "⚠ Finished with errors"
                            st == null -> "Done (could not verify)"
                            st.mode == Bypass.Mode.GAMING ->
                                    "✔ Verified: Gaming Performance Mode is active"
                            st.mode == Bypass.Mode.NORMAL ->
                                    "✔ Verified: Normal Mode (Default) is active"
                            else -> "Done (custom state)"
                        }
                )
                _ui.update { it.copy(busy = false) }
            }
        }
    }

    private suspend fun loadStatus(): Bypass.Status? {
        if (!controller.isReady) return null
        val pass = controller.exec(Bypass.GET_PASS_THROUGH)
        val all = controller.exec(Bypass.LIST_ALL)
        val disabled = controller.exec(Bypass.LIST_DISABLED)
        if (!all.ok || !disabled.ok) {
            log("✗ Could not read status: ${all.output.ifBlank { disabled.output }}")
            return null
        }
        val st = Bypass.buildStatus(pass.output, all.output, disabled.output)
        _ui.update { it.copy(status = st) }
        return st
    }

    private fun log(line: String) = _ui.update { it.copy(log = (it.log + line).takeLast(200)) }

    private companion object {
        const val CONNECT_TIMEOUT_MS = 10_000L
    }
}
