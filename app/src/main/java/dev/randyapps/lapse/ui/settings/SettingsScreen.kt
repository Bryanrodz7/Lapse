package dev.randyapps.lapse.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.randyapps.lapse.BuildConfig
import dev.randyapps.lapse.R
import dev.randyapps.lapse.data.model.REMINDER_CHOICES
import dev.randyapps.lapse.data.settings.LapseSettings
import dev.randyapps.lapse.data.settings.ThemeMode
import dev.randyapps.lapse.ui.edit.FieldLabel

/** Three things and an about line. Stateless, so it previews in both themes. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    settings: LapseSettings,
    onToggleReminderDay: (Int) -> Unit,
    onThemeModeChange: (ThemeMode) -> Unit,
    /** Null when the consent framework does not require a way back into the form. */
    onPrivacyOptions: (() -> Unit)?,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.settings_title),
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                ),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            FieldLabel(stringResource(R.string.settings_default_reminders))
            Text(
                text = stringResource(R.string.settings_default_reminders_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 12.dp),
            )
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                REMINDER_CHOICES.forEach { days ->
                    ChoiceChip(
                        selected = days in settings.defaultReminderDays,
                        label = pluralStringResource(R.plurals.reminder_days_before, days, days),
                        onClick = { onToggleReminderDay(days) },
                    )
                }
            }

            Spacer(Modifier.height(36.dp))
            FieldLabel(stringResource(R.string.settings_theme))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ThemeMode.entries.forEach { mode ->
                    ChoiceChip(
                        selected = mode == settings.themeMode,
                        label = stringResource(mode.labelRes),
                        onClick = { onThemeModeChange(mode) },
                    )
                }
            }

            if (onPrivacyOptions != null) {
                Spacer(Modifier.height(36.dp))
                FieldLabel(stringResource(R.string.settings_privacy_options))
                Text(
                    text = stringResource(R.string.settings_privacy_options_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 12.dp),
                )
                OutlinedButton(onClick = onPrivacyOptions) {
                    Text(
                        text = stringResource(R.string.settings_privacy_options),
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }

            Spacer(Modifier.height(44.dp))
            FieldLabel(stringResource(R.string.settings_about))
            About()

            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
private fun ChoiceChip(selected: Boolean, label: String, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, style = MaterialTheme.typography.labelMedium) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primary,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
            labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
    )
}

@Composable
private fun About() {
    val body = MaterialTheme.typography.bodyMedium
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = stringResource(R.string.settings_version, BuildConfig.VERSION_NAME),
            style = body,
            color = muted,
        )
        // Stated plainly because it is the product's actual position, not a legal footnote.
        Text(stringResource(R.string.settings_privacy), style = body, color = muted)
        Text(stringResource(R.string.settings_ads), style = body, color = muted)
        Text(stringResource(R.string.settings_font_credit), style = body, color = muted)
    }
}

private val ThemeMode.labelRes: Int
    get() = when (this) {
        ThemeMode.SYSTEM -> R.string.theme_system
        ThemeMode.LIGHT -> R.string.theme_light
        ThemeMode.DARK -> R.string.theme_dark
    }
