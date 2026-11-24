package app.galaxybypass

import app.galaxybypass.Bypass.PassThrough
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BypassTest {

    @Test
    fun gamingModePlan_matchesOriginalBatOrder() {
        assertEquals(
            listOf(
                "settings put system pass_through 1",
                "pm disable-user com.samsung.android.game.gametools",
                "pm disable-user com.samsung.android.game.gamehome",
                "pm disable-user com.samsung.android.game.gos",
            ),
            Bypass.gamingModePlan(),
        )
    }

    @Test
    fun restorePlan_reEnablesEverything() {
        assertEquals(
            listOf(
                "settings put system pass_through 0",
                "pm enable com.samsung.android.game.gametools",
                "pm enable com.samsung.android.game.gamehome",
                "pm enable com.samsung.android.game.gos",
            ),
            Bypass.restorePlan(),
        )
    }

    @Test
    fun everyGeneratedCommandIsAllowed() {
        val all = Bypass.gamingModePlan() + Bypass.restorePlan() +
            listOf(Bypass.GET_PASS_THROUGH, Bypass.LIST_ALL, Bypass.LIST_DISABLED)
        all.forEach { assertTrue("should be allowed: $it", Bypass.isAllowed(it)) }
    }

    @Test
    fun allowList_rejectsEverythingElse() {
        listOf(
            "id",
            "rm -rf /sdcard",
            "settings put system pass_through 1; reboot",
            "settings put system pass_through 2",
            "settings put global adb_enabled 0",
            "pm disable-user com.android.systemui",
            "pm disable-user com.samsung.android.game.other",
            "pm disable-user --user 0 com.samsung.android.game.gos",
            "pm disable-user com.samsung.android.game.gos && id",
            "pm disable-user com.samsung.android.game.gos\nid",
            "pm uninstall com.samsung.android.game.gos",
            "pm list packages com.samsung.android.game. | sh",
            "",
        ).forEach { assertFalse("should be refused: $it", Bypass.isAllowed(it)) }
    }

    @Test
    fun setPackageEnabled_rejectsUnknownPackage() {
        var failed = false
        try {
            Bypass.setPackageEnabled("com.android.systemui", false)
        } catch (e: IllegalArgumentException) {
            failed = true
        }
        assertTrue(failed)
    }

    @Test
    fun steps_haveSameLabelsAsOriginalBat() {
        assertEquals(
            listOf(
                "Enabling Power Bypass",
                "Disabling Game Tools",
                "Disabling Game Launcher",
                "Disabling Game Optimization Service (GOS)",
            ),
            Bypass.gamingModeSteps().map { it.label },
        )
        assertEquals(
            listOf(
                "Disabling Power Bypass",
                "Enabling Game Tools",
                "Enabling Game Launcher",
                "Enabling Game Optimization Service (GOS)",
            ),
            Bypass.restoreSteps().map { it.label },
        )
    }

    @Test
    fun mode_gamingNormalCustom() {
        val all = "package:com.samsung.android.game.gametools\npackage:com.samsung.android.game.gamehome\npackage:com.samsung.android.game.gos"
        assertEquals(Bypass.Mode.GAMING, Bypass.buildStatus("1", all, all).mode)
        assertEquals(Bypass.Mode.NORMAL, Bypass.buildStatus("0", all, "").mode)
        assertEquals(Bypass.Mode.NORMAL, Bypass.buildStatus("null", all, "").mode)
        // bypass ON but GOS still enabled, or the reverse: neither of the two modes
        assertEquals(Bypass.Mode.CUSTOM, Bypass.buildStatus("1", all, "").mode)
        assertEquals(Bypass.Mode.CUSTOM, Bypass.buildStatus("0", all, all).mode)
    }

    @Test
    fun parsePassThrough() {
        assertEquals(PassThrough.ON, Bypass.parsePassThrough("1\n"))
        assertEquals(PassThrough.OFF, Bypass.parsePassThrough("0"))
        assertEquals(PassThrough.UNSET, Bypass.parsePassThrough("null\n"))
        assertEquals(PassThrough.UNSET, Bypass.parsePassThrough(""))
        assertEquals(PassThrough.UNKNOWN, Bypass.parsePassThrough("7"))
    }

    @Test
    fun parsePackages_handlesNoiseAndPaths() {
        val out = """
            package:com.samsung.android.game.gos
            package:com.samsung.android.game.gametools=/data/app/x/base.apk

            WARNING: something unrelated
        """.trimIndent()
        assertEquals(
            setOf("com.samsung.android.game.gos", "com.samsung.android.game.gametools"),
            Bypass.parsePackages(out),
        )
    }

    @Test
    fun buildStatus_gamingModeActive() {
        val all = "package:com.samsung.android.game.gametools\npackage:com.samsung.android.game.gamehome\npackage:com.samsung.android.game.gos"
        val st = Bypass.buildStatus("1", all, all)
        assertTrue(st.gamingModeActive)
        assertFalse(st.isDefault)
        assertTrue(st.packages.all { it.installed && !it.enabled })
    }

    @Test
    fun buildStatus_defaultAndMissingPackageIgnored() {
        // Game Launcher not installed on this device: must not prevent "default" detection.
        val all = "package:com.samsung.android.game.gametools\npackage:com.samsung.android.game.gos"
        val st = Bypass.buildStatus("0", all, "")
        assertTrue(st.isDefault)
        assertFalse(st.gamingModeActive)
        assertFalse(st.packages.first { it.pkg.id.endsWith("gamehome") }.installed)
    }

    @Test
    fun buildStatus_partialIsNeither() {
        val all = "package:com.samsung.android.game.gametools\npackage:com.samsung.android.game.gamehome\npackage:com.samsung.android.game.gos"
        val st = Bypass.buildStatus("1", all, "package:com.samsung.android.game.gos")
        assertFalse(st.gamingModeActive)
        assertFalse(st.isDefault)
    }

    @Test
    fun shellResult_roundTrip() {
        val r = Bypass.ShellResult(1, "Error: boom\nline2")
        assertEquals(r, Bypass.ShellResult.decode(r.encode()))
        assertTrue(Bypass.ShellResult.decode("0\nok").ok)
        assertEquals(1, Bypass.ShellResult.decode("garbage").exitCode)
    }
}