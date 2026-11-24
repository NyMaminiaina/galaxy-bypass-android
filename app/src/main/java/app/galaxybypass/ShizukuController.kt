package app.galaxybypass

import android.app.Application
import android.content.ComponentName
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku

enum class ShizukuState {
    NotInstalled,
    NotRunning,
    Unsupported,
    NeedsPermission,
    PermissionDenied,
    Connecting,
    ConnectFailed,
    Ready
}

const val SHIZUKU_PACKAGE = "moe.shizuku.privileged.api"

/** Tracks Shizuku (installed / running / permission) and owns the privileged [ShellService]. */
class ShizukuController(private val app: Application) {

    private val _state = MutableStateFlow(ShizukuState.NotRunning)
    val state: StateFlow<ShizukuState> = _state.asStateFlow()

    @Volatile private var service: IShellService? = null
    @Volatile private var bound = false
    @Volatile private var failed = false

    private val userServiceArgs =
            Shizuku.UserServiceArgs(ComponentName(app.packageName, ShellService::class.java.name))
                    .tag("galaxy-bypass-shell")
                    .daemon(false)
                    .processNameSuffix("shell")
                    .debuggable(BuildConfig.DEBUG)
                    .version(BuildConfig.VERSION_CODE)

    private val connection =
            object : ServiceConnection {
                override fun onServiceConnected(name: ComponentName, binder: IBinder?) {
                    service =
                            if (binder != null && binder.pingBinder())
                                    IShellService.Stub.asInterface(binder)
                            else null
                    failed = service == null
                    refresh()
                }

                override fun onServiceDisconnected(name: ComponentName) {
                    service = null
                    bound = false
                    refresh()
                }
            }

    private val onBinderReceived = Shizuku.OnBinderReceivedListener { refresh() }
    private val onBinderDead =
            Shizuku.OnBinderDeadListener {
                service = null
                bound = false
                refresh()
            }
    private val onPermissionResult = Shizuku.OnRequestPermissionResultListener { _, _ -> refresh() }

    fun start() {
        Shizuku.addBinderReceivedListenerSticky(onBinderReceived)
        Shizuku.addBinderDeadListener(onBinderDead)
        Shizuku.addRequestPermissionResultListener(onPermissionResult)
        refresh()
    }

    fun stop() {
        Shizuku.removeBinderReceivedListener(onBinderReceived)
        Shizuku.removeBinderDeadListener(onBinderDead)
        Shizuku.removeRequestPermissionResultListener(onPermissionResult)
        try {
            service?.destroy() // ends the privileged process
        } catch (_: Throwable) {}
        try {
            if (Shizuku.pingBinder()) Shizuku.unbindUserService(userServiceArgs, connection, true)
        } catch (_: Throwable) {}
        service = null
        bound = false
    }

    val isReady: Boolean
        get() = service != null

    fun refresh() {
        _state.value = compute()
    }

    /** User pressed Refresh / Retry: forget a previous connection failure and try again. */
    fun retry() {
        failed = false
        refresh()
    }

    /** Called when the service never answered (stays in Connecting otherwise). */
    fun connectTimedOut() {
        if (service != null) return
        try {
            if (Shizuku.pingBinder()) Shizuku.unbindUserService(userServiceArgs, connection, false)
        } catch (_: Throwable) {}
        bound = false
        failed = true
        refresh()
    }

    fun requestPermission() {
        try {
            Shizuku.requestPermission(REQUEST_CODE)
        } catch (_: Throwable) {
            refresh()
        }
    }

    suspend fun exec(command: String): Bypass.ShellResult =
            withContext(Dispatchers.IO) {
                val s =
                        service
                                ?: return@withContext Bypass.ShellResult(
                                        1,
                                        "Shizuku service not connected"
                                )
                try {
                    Bypass.ShellResult.decode(s.exec(command))
                } catch (t: Throwable) {
                    Bypass.ShellResult(1, t.message ?: t.toString())
                }
            }

    private fun compute(): ShizukuState {
        if (!Shizuku.pingBinder()) {
            service = null
            bound = false
            return if (isShizukuInstalled()) ShizukuState.NotRunning else ShizukuState.NotInstalled
        }
        if (Shizuku.isPreV11()) return ShizukuState.Unsupported
        return try {
            // user services need Shizuku 10+
            if (Shizuku.getVersion() < 10) return ShizukuState.Unsupported
            when {
                Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED ->
                        when {
                            service != null -> ShizukuState.Ready
                            failed -> ShizukuState.ConnectFailed
                            else -> {
                                bindService()
                                if (failed) ShizukuState.ConnectFailed else ShizukuState.Connecting
                            }
                        }
                Shizuku.shouldShowRequestPermissionRationale() -> ShizukuState.PermissionDenied
                else -> ShizukuState.NeedsPermission
            }
        } catch (_: Throwable) {
            ShizukuState.NotRunning
        }
    }

    private fun bindService() {
        if (bound) return
        try {
            bound = true
            Shizuku.bindUserService(userServiceArgs, connection)
        } catch (_: Throwable) {
            bound = false
            failed = true
        }
    }

    private fun isShizukuInstalled(): Boolean =
            try {
                app.packageManager.getPackageInfo(SHIZUKU_PACKAGE, 0)
                true
            } catch (_: PackageManager.NameNotFoundException) {
                false
            }

    private companion object {
        const val REQUEST_CODE = 4201
    }
}
