package app.galaxybypass

/**
 * Pure Kotlin (no Android dependency) so it can be unit-tested on the JVM
 */
object Bypass {

    const val PASS_THROUGH_KEY = "pass_through"

    data class GamePackage(val id: String, val label: String)

    /** Same order and names as the .bat file. */
    val GAME_PACKAGES = listOf(
        GamePackage("com.samsung.android.game.gametools", "Game Tools"),
        GamePackage("com.samsung.android.game.gamehome", "Game Launcher"),
        GamePackage("com.samsung.android.game.gos", "Game Optimization Service (GOS)"),
    )

    //  Commands
    const val GET_PASS_THROUGH = "settings get system $PASS_THROUGH_KEY"
    const val LIST_ALL = "pm list packages com.samsung.android.game."
    const val LIST_DISABLED = "pm list packages -d com.samsung.android.game."

    fun setPassThrough(on: Boolean) = "settings put system $PASS_THROUGH_KEY ${if (on) 1 else 0}"

    fun setPackageEnabled(id: String, enabled: Boolean): String {
        require(GAME_PACKAGES.any { it.id == id }) { "Unknown package: $id" }
        return if (enabled) "pm enable $id" else "pm disable-user $id"
    }

    /** One line of the plan: the text shown in the log (like the .bat "[1/4] ...") and the command. */
    data class Step(val label: String, val command: String)

    /** Option [1] of the .bat: bypass ON, then Game Tools, Game Launcher and GOS OFF. */
    fun gamingModeSteps(packages: List<GamePackage> = GAME_PACKAGES): List<Step> =
        listOf(Step("Enabling Power Bypass", setPassThrough(true))) +
            packages.map { Step("Disabling ${it.label}", setPackageEnabled(it.id, false)) }

    /** Option [2] of the .bat: bypass OFF, then everything back ON. */
    fun restoreSteps(packages: List<GamePackage> = GAME_PACKAGES): List<Step> =
        listOf(Step("Disabling Power Bypass", setPassThrough(false))) +
            packages.map { Step("Enabling ${it.label}", setPackageEnabled(it.id, true)) }

    fun gamingModePlan(packages: List<GamePackage> = GAME_PACKAGES): List<String> =
        gamingModeSteps(packages).map { it.command }

    fun restorePlan(packages: List<GamePackage> = GAME_PACKAGES): List<String> =
        restoreSteps(packages).map { it.command }

    //  Allow-list
    // The privileged service refuses anything that is not one of the commands above.
    // Full-match regexes with no shell metacharacters: no chaining, no redirection.
    private val PACKAGE_IDS = GAME_PACKAGES.joinToString("|") { Regex.escape(it.id) }

    private val ALLOWED = listOf(
        Regex("""settings get system pass_through"""),
        Regex("""settings put system pass_through [01]"""),
        Regex("""pm list packages( -d)? com\.samsung\.android\.game\."""),
        Regex("""pm (disable-user|enable) ($PACKAGE_IDS)"""),
    )

    fun isAllowed(command: String): Boolean = ALLOWED.any { it.matches(command) }

    //  Results & status -

    data class ShellResult(val exitCode: Int, val output: String) {
        val ok: Boolean get() = exitCode == 0
        fun encode(): String = "$exitCode\n$output"

        companion object {
            fun decode(raw: String): ShellResult {
                val nl = raw.indexOf('\n')
                if (nl < 0) return ShellResult(raw.trim().toIntOrNull() ?: 1, "")
                val code = raw.substring(0, nl).trim().toIntOrNull() ?: 1
                return ShellResult(code, raw.substring(nl + 1))
            }
        }
    }

    enum class PassThrough { ON, OFF, UNSET, UNKNOWN }

    /** What the phone is really doing right now, read back from the system. */
    enum class Mode { GAMING, NORMAL, CUSTOM }

    data class PackageState(val pkg: GamePackage, val installed: Boolean, val enabled: Boolean)

    data class Status(val passThrough: PassThrough, val packages: List<PackageState>) {
        private val installed get() = packages.filter { it.installed }

        /** Gaming Performance Mode: bypass ON and every installed game package disabled. */
        val gamingModeActive: Boolean
            get() = passThrough == PassThrough.ON && installed.all { !it.enabled }

        /** Normal Mode (Default): bypass not ON and every installed game package enabled. */
        val isDefault: Boolean
            get() = passThrough != PassThrough.ON && installed.all { it.enabled }

        val mode: Mode
            get() = when {
                gamingModeActive -> Mode.GAMING
                isDefault -> Mode.NORMAL
                else -> Mode.CUSTOM
            }
    }

    fun parsePassThrough(output: String): PassThrough = when (output.trim()) {
        "1" -> PassThrough.ON
        "0" -> PassThrough.OFF
        "", "null" -> PassThrough.UNSET
        else -> PassThrough.UNKNOWN
    }

    /** Parses `pm list packages` output ("package:com.foo") into a set of package ids. */
    fun parsePackages(output: String): Set<String> = output.lineSequence()
        .map { it.trim() }
        .filter { it.startsWith("package:") }
        .map { it.removePrefix("package:").substringBefore('=').trim() }
        .toSet()

    fun buildStatus(passThroughOut: String, allOut: String, disabledOut: String): Status {
        val all = parsePackages(allOut)
        val disabled = parsePackages(disabledOut)
        return Status(
            passThrough = parsePassThrough(passThroughOut),
            packages = GAME_PACKAGES.map { p ->
                val installed = p.id in all
                PackageState(p, installed, enabled = installed && p.id !in disabled)
            },
        )
    }
}