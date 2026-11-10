package com.luckyprospector.app.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalContext
import com.luckyprospector.app.ui.AppSettings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.luckyprospector.app.R
import kotlin.random.Random

/**
 * One curiosity about clovers, a different one on each visit. Facts and legends
 * are researched and sourced in docs/wiki/CLOVER_LORE.md; legends say so.
 */
@Composable
fun DidYouKnowCard(modifier: Modifier = Modifier) {
    val facts = stringArrayResource(R.array.clover_facts)
    var index by rememberSaveable { mutableIntStateOf(Random.nextInt(facts.size)) }
    val store = AppSettings.get(LocalContext.current)
    val settings by store.settings.collectAsState()
    val expanded = settings.factsExpanded
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = .9f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = .45f)),
        shadowElevation = 3.dp,
    ) {
        Column(Modifier.animateContentSize()) {
            // The whole title row folds and unfolds; the choice is remembered.
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { store.update { it.copy(factsExpanded = !expanded) } }
                    .padding(start = 16.dp, top = 12.dp, end = 12.dp, bottom = if (expanded) 0.dp else 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "☘ " + stringResource(R.string.did_you_know),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                )
                Icon(
                    if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                    contentDescription = stringResource(if (expanded) R.string.collapse else R.string.expand),
                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                )
            }
            if (expanded) {
                Text(
                    facts[index % facts.size],
                    modifier = Modifier.padding(start = 16.dp, top = 6.dp, end = 16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                )
                TextButton(
                    onClick = { index = (index + 1) % facts.size },
                    modifier = Modifier.align(Alignment.End).padding(end = 8.dp),
                ) { Text(stringResource(R.string.another_fact)) }
            }
        }
    }
}

/** How to press and keep a four-leaf clover; sources in docs/wiki/CLOVER_LORE.md. */
@Composable
fun PreservationDialog(onDismiss: () -> Unit) {
    val steps = stringArrayResource(R.array.preserve_steps)
    val tips = stringArrayResource(R.array.preserve_tips)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.preserve_title)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                steps.forEachIndexed { number, step ->
                    Text("${number + 1}. $step", style = MaterialTheme.typography.bodyMedium)
                }
                Text(
                    stringResource(R.string.preserve_tips_title),
                    modifier = Modifier.padding(top = 8.dp),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                tips.forEach { Text("• $it", style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) } },
    )
}
