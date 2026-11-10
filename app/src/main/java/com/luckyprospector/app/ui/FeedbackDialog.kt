package com.luckyprospector.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.luckyprospector.app.R
import com.luckyprospector.app.data.remote.FeedbackCategory
import com.luckyprospector.app.data.remote.FeedbackLimitReachedException
import com.luckyprospector.app.data.remote.FeedbackRepository
import kotlinx.coroutines.launch

/**
 * Asks something specific for each kind of feedback: open "any comments?" boxes
 * mostly collect "nice app", while a concrete question gets concrete answers.
 */
@Composable
fun FeedbackDialog(
    signedIn: Boolean,
    onSignInRequested: () -> Unit,
    onSend: suspend (FeedbackCategory, String, String?) -> Result<Unit>,
    onDismiss: () -> Unit,
) {
    var category by rememberSaveable { mutableStateOf(FeedbackCategory.IDEA) }
    var message by rememberSaveable { mutableStateOf("") }
    var contact by rememberSaveable { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }
    var sent by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val genericError = stringResource(R.string.error_feedback)
    val limitError = stringResource(R.string.feedback_limit)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (sent) R.string.feedback_thanks_title else R.string.feedback_title)) },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                when {
                    sent -> Text(stringResource(R.string.feedback_thanks))
                    !signedIn -> Text(stringResource(R.string.feedback_sign_in))
                    else -> {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FeedbackCategory.entries.forEach { option ->
                                FilterChip(
                                    selected = category == option,
                                    onClick = { category = option },
                                    label = { Text(option.label()) },
                                )
                            }
                        }
                        OutlinedTextField(
                            value = message,
                            onValueChange = { message = it.take(FeedbackRepository.MAX_MESSAGE) },
                            label = { Text(category.question()) },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp),
                        )
                        OutlinedTextField(
                            value = contact,
                            onValueChange = { contact = it.take(FeedbackRepository.MAX_CONTACT) },
                            label = { Text(stringResource(R.string.feedback_contact)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Text(stringResource(R.string.feedback_privacy), style = MaterialTheme.typography.bodySmall)
                        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    }
                }
            }
        },
        confirmButton = {
            when {
                sent -> TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) }
                !signedIn -> Button(onClick = onSignInRequested) { Text(stringResource(R.string.sign_in_google)) }
                else -> Button(
                    enabled = !sending && message.isNotBlank(),
                    onClick = {
                        sending = true
                        error = null
                        scope.launch {
                            onSend(category, message, contact.ifBlank { null })
                                .onSuccess { sent = true }
                                .onFailure { error = if (it is FeedbackLimitReachedException) limitError else genericError }
                            sending = false
                        }
                    },
                ) { Text(stringResource(if (sending) R.string.feedback_sending else R.string.feedback_send)) }
            }
        },
        dismissButton = {
            if (!sent) TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}

@Composable
private fun FeedbackCategory.label() = stringResource(
    when (this) {
        FeedbackCategory.IDEA -> R.string.feedback_idea
        FeedbackCategory.PROBLEM -> R.string.feedback_problem
        FeedbackCategory.WRONG_DETECTION -> R.string.feedback_detection
    },
)

@Composable
private fun FeedbackCategory.question() = stringResource(
    when (this) {
        FeedbackCategory.IDEA -> R.string.feedback_question_idea
        FeedbackCategory.PROBLEM -> R.string.feedback_question_problem
        FeedbackCategory.WRONG_DETECTION -> R.string.feedback_question_detection
    },
)
