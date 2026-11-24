package app.galaxybypass

import android.content.Context
import androidx.annotation.Keep
import kotlin.system.exitProcess

/**
 * Runs inside the Shizuku "user service" process, with shell (ADB, uid 2000) privileges.
 * Only commands accepted by [Bypass.isAllowed] are executed, so even if the app were
 * compromised it could not use this service as a generic shell.
 */
class ShellService() : IShellService.Stub() {

    /** Used by Shizuku 13+ (tried first). */
    @Keep
    constructor(@Suppress("UNUSED_PARAMETER") context: Context) : this()

    override fun destroy() {
        exitProcess(0)
    }

    override fun exec(command: String): String {
        if (!Bypass.isAllowed(command)) {
            return Bypass.ShellResult(126, "Refused: command is not in the allow-list").encode()
        }
        return try {
            val pb = ProcessBuilder("sh", "-c", command).redirectErrorStream(true)
            pb.environment().putIfAbsent("PATH", "/system/bin:/system/xbin")
            val p = pb.start()
            val out = p.inputStream.bufferedReader().use { it.readText() }
            Bypass.ShellResult(p.waitFor(), out.trim()).encode()
        } catch (t: Throwable) {
            Bypass.ShellResult(1, t.toString()).encode()
        }
    }
}
