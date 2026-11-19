package com.kalogsc.feedbil.ui.feeds

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kalogsc.feedbil.R
import com.kalogsc.feedbil.domain.FeedInputError
import com.kalogsc.feedbil.domain.FeedInputValidator
import com.kalogsc.feedbil.ui.theme.FeedbilTheme

@Composable
fun FeedFormRoute(
    viewModel: FeedFormViewModel,
    onSaved: () -> Unit,
    onBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state.saved) {
        if (state.saved) {
            onSaved()
        }
    }

    FeedFormScreen(
        state = state,
        onNameChange = viewModel::onNameChange,
        onUrlChange = viewModel::onUrlChange,
        onSave = viewModel::save,
        onBack = onBack,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedFormScreen(
    state: FeedFormUiState,
    onNameChange: (String) -> Unit,
    onUrlChange: (String) -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(
                            if (state.isEditing) {
                                R.string.feed_form_edit_title
                            } else {
                                R.string.feed_form_add_title
                            },
                        ),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        if (state.isLoadingFeed) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        } else {
            FeedFormFields(
                state = state,
                onNameChange = onNameChange,
                onUrlChange = onUrlChange,
                onSave = onSave,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .imePadding(),
            )
        }
    }
}

@Composable
private fun FeedFormFields(
    state: FeedFormUiState,
    onNameChange: (String) -> Unit,
    onUrlChange: (String) -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        OutlinedTextField(
            value = state.name,
            onValueChange = onNameChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.feed_form_name_label)) },
            singleLine = true,
            isError = state.nameError != null,
            supportingText = state.nameError?.let { error ->
                { Text(inputErrorText(error)) }
            },
            enabled = !state.isSaving,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
        )

        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = state.url,
            onValueChange = onUrlChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.feed_form_url_label)) },
            placeholder = { Text(stringResource(R.string.feed_form_url_placeholder)) },
            singleLine = true,
            isError = state.urlError != null,
            supportingText = state.urlError?.let { error ->
                { Text(inputErrorText(error)) }
            },
            enabled = !state.isSaving,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Uri,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(onDone = { onSave() }),
        )

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = onSave,
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.isSaving,
        ) {
            Text(stringResource(R.string.action_save))
        }

        if (state.isSaving) {
            Row(
                modifier = Modifier.padding(top = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = stringResource(R.string.feed_form_checking),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        state.submitError?.let { error ->
            Spacer(Modifier.height(16.dp))
            Text(
                text = submitErrorText(error),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun inputErrorText(error: FeedInputError): String = when (error) {
    FeedInputError.BLANK_NAME -> stringResource(R.string.error_name_blank)
    FeedInputError.NAME_TOO_LONG ->
        stringResource(R.string.error_name_too_long, FeedInputValidator.MAX_NAME_LENGTH)
    FeedInputError.BLANK_URL -> stringResource(R.string.error_url_blank)
    FeedInputError.INVALID_URL -> stringResource(R.string.error_url_invalid)
}

@Composable
private fun submitErrorText(error: FeedSubmitError): String = when (error) {
    FeedSubmitError.DuplicateUrl -> stringResource(R.string.error_duplicate_url)
    FeedSubmitError.NotAFeed -> stringResource(R.string.error_not_a_feed)
    is FeedSubmitError.HttpError -> stringResource(R.string.error_http, error.code)
    FeedSubmitError.Network -> stringResource(R.string.error_network)
    FeedSubmitError.Timeout -> stringResource(R.string.error_timeout)
    FeedSubmitError.InvalidUrl -> stringResource(R.string.error_url_invalid)
    FeedSubmitError.Unknown -> stringResource(R.string.error_unknown)
    FeedSubmitError.FeedMissing -> stringResource(R.string.error_feed_missing)
    FeedSubmitError.Storage -> stringResource(R.string.error_storage)
}

@Preview(showBackground = true)
@Composable
private fun FeedFormScreenPreview() {
    FeedbilTheme {
        FeedFormScreen(
            state = FeedFormUiState(
                name = "Kotlin Blog",
                url = "https://blog.example.com/rss",
                isEditing = true,
            ),
            onNameChange = {},
            onUrlChange = {},
            onSave = {},
            onBack = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun FeedFormScreenErrorPreview() {
    FeedbilTheme {
        FeedFormScreen(
            state = FeedFormUiState(
                name = "",
                url = "not a url",
                nameError = FeedInputError.BLANK_NAME,
                urlError = FeedInputError.INVALID_URL,
                submitError = FeedSubmitError.Network,
            ),
            onNameChange = {},
            onUrlChange = {},
            onSave = {},
            onBack = {},
        )
    }
}
