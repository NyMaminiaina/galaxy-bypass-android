package app.galaxybypass

import android.os.Build
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class Actions(
    val refresh: () -> Unit,
    val getShizuku: () -> Unit,
    val openShizuku: () -> Unit,
    val grantPermission: () -> Unit,
    val gamingMode: () -> Unit,
    val restoreDefaults: () -> Unit,
    val setBypass: (Boolean) -> Unit,
    val setPackage: (String, Boolean) -> Unit,
)

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val ctx = LocalContext.current
    val scheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (dark) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        dark -> darkColorScheme()
        else -> lightColorScheme()
    }
    MaterialTheme(colorScheme = scheme, content = content)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(ui: UiState, actions: Actions) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    IconButton(onClick = actions.refresh) {
                        Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.refresh))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ShizukuCard(ui.shizuku, actions)
            DeviceCard()
            if (ui.shizuku == ShizukuState.Ready) {
                ModeCard(ui)
                StatusCard(ui, actions)
                Button(
                    onClick = actions.gamingMode,
                    enabled = !ui.busy,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(stringResource(R.string.btn_gaming_mode), style = MaterialTheme.typography.titleMedium)
                        Text(stringResource(R.string.btn_gaming_mode_sub), style = MaterialTheme.typography.labelSmall)
                    }
                }
                OutlinedButton(
                    onClick = actions.restoreDefaults,
                    enabled = !ui.busy,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(stringResource(R.string.btn_restore), style = MaterialTheme.typography.titleMedium)
                        Text(stringResource(R.string.btn_restore_sub), style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
            WarningCard()
            if (ui.log.isNotEmpty()) LogCard(ui.log)
        }
    }
}

@Composable
private fun ShizukuCard(state: ShizukuState, actions: Actions) {
    val message = when (state) {
        ShizukuState.NotInstalled -> R.string.shizuku_not_installed
        ShizukuState.NotRunning -> R.string.shizuku_not_running
        ShizukuState.Unsupported -> R.string.shizuku_unsupported
        ShizukuState.NeedsPermission -> R.string.shizuku_needs_permission
        ShizukuState.PermissionDenied -> R.string.shizuku_denied
        ShizukuState.Connecting -> R.string.shizuku_connecting
        ShizukuState.ConnectFailed -> R.string.shizuku_connect_failed
        ShizukuState.Ready -> R.string.shizuku_ready
    }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.shizuku_title), style = MaterialTheme.typography.titleMedium)
                if (state == ShizukuState.Connecting) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
            }
            Text(stringResource(message), style = MaterialTheme.typography.bodyMedium)
            when (state) {
                ShizukuState.NotInstalled, ShizukuState.Unsupported ->
                    Button(onClick = actions.getShizuku) { Text(stringResource(R.string.btn_get_shizuku)) }
                ShizukuState.NotRunning, ShizukuState.PermissionDenied ->
                    Button(onClick = actions.openShizuku) { Text(stringResource(R.string.btn_open_shizuku)) }
                ShizukuState.NeedsPermission ->
                    Button(onClick = actions.grantPermission) { Text(stringResource(R.string.btn_grant)) }
                ShizukuState.ConnectFailed ->
                    Button(onClick = actions.refresh) { Text(stringResource(R.string.btn_retry)) }
                else -> Unit
            }
        }
    }
}

@Composable
private fun DeviceCard() {
    val isSamsung = Build.MANUFACTURER.equals("samsung", ignoreCase = true)
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(R.string.device_title), style = MaterialTheme.typography.titleMedium)
            Text(
                "${Build.MANUFACTURER} ${Build.MODEL} · Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
                style = MaterialTheme.typography.bodyMedium,
            )
            if (!isSamsung) {
                Text(
                    stringResource(R.string.device_not_samsung),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun ModeCard(ui: UiState) {
    val mode = ui.mode
    val scheme = MaterialTheme.colorScheme
    val loading = ui.busy || mode == null

    val bgTarget = when {
        loading -> scheme.surfaceVariant
        mode == Bypass.Mode.GAMING -> scheme.primaryContainer
        mode == Bypass.Mode.NORMAL -> scheme.secondaryContainer
        else -> scheme.tertiaryContainer
    }
    val fgTarget = when {
        loading -> scheme.onSurfaceVariant
        mode == Bypass.Mode.GAMING -> scheme.onPrimaryContainer
        mode == Bypass.Mode.NORMAL -> scheme.onSecondaryContainer
        else -> scheme.onTertiaryContainer
    }
    val bg by animateColorAsState(bgTarget, label = "modeBg")
    val fg by animateColorAsState(fgTarget, label = "modeFg")

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = bg, contentColor = fg),
    ) {
        AnimatedContent(targetState = Pair(ui.busy, mode), label = "modeText") { (busy, m) ->
            val title = when {
                busy -> R.string.mode_applying
                m == Bypass.Mode.GAMING -> R.string.mode_gaming
                m == Bypass.Mode.NORMAL -> R.string.mode_normal
                m == Bypass.Mode.CUSTOM -> R.string.mode_custom
                else -> R.string.mode_reading
            }
            val detail = when {
                busy || m == null -> null
                m == Bypass.Mode.GAMING -> R.string.mode_gaming_detail
                m == Bypass.Mode.NORMAL -> R.string.mode_normal_detail
                else -> R.string.mode_custom_detail
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (busy || m == null) {
                    CircularProgressIndicator(Modifier.size(28.dp), strokeWidth = 3.dp, color = fg)
                } else {
                    Icon(
                        if (m == Bypass.Mode.CUSTOM) Icons.Default.Info else Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(stringResource(title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    if (detail != null) {
                        Text(stringResource(detail), style = MaterialTheme.typography.bodySmall)
                        Text(stringResource(R.string.mode_verified), style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusCard(ui: UiState, actions: Actions) {
    val st = ui.status ?: return
    val enabled = !ui.busy
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.status_title), style = MaterialTheme.typography.titleMedium)
            HorizontalDivider()
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.power_bypass), Modifier.weight(1f))
                Switch(
                    checked = st.passThrough == Bypass.PassThrough.ON,
                    onCheckedChange = actions.setBypass,
                    enabled = enabled,
                )
            }
            st.packages.forEach { p ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(p.pkg.label)
                        Text(
                            if (p.installed) p.pkg.id else stringResource(R.string.not_installed),
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = p.enabled,
                        onCheckedChange = { actions.setPackage(p.pkg.id, it) },
                        enabled = enabled && p.installed,
                    )
                }
            }
        }
    }
}

@Composable
private fun WarningCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Warning, contentDescription = null)
                Text(stringResource(R.string.warning_title), style = MaterialTheme.typography.titleSmall)
            }
            Text(stringResource(R.string.warning_body), style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun LogCard(lines: List<String>) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(stringResource(R.string.log_title), style = MaterialTheme.typography.titleSmall)
            Text(
                lines.takeLast(40).joinToString("\n"),
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                lineHeight = 15.sp,
            )
        }
    }
}