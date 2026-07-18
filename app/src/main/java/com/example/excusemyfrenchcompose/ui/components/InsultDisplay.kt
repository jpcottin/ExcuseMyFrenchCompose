package com.example.excusemyfrenchcompose.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.example.excusemyfrenchcompose.R
import com.example.excusemyfrenchcompose.ui.viewmodel.InsultUiState
import com.example.excusemyfrenchcompose.ui.viewmodel.InsultViewModelInterface

private val WIDE_LAYOUT_THRESHOLD = 600.dp
private const val IMAGE_MAX_FRACTION = 0.9f
private const val TEXT_MIN_HEIGHT_FRACTION = 0.15f

@Composable
fun InsultDisplay(viewModel: InsultViewModelInterface, modifier: Modifier = Modifier) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Drive the auto-refresh loop only while the screen is at least STARTED, so polling
    // (and TTS) pauses automatically when the app is in the background.
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(viewModel, lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.autoRefresh()
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        if (maxWidth < WIDE_LAYOUT_THRESHOLD) {
            PortraitLayout(
                uiState = uiState,
                minTextHeight = maxHeight * TEXT_MIN_HEIGHT_FRACTION,
                onRetry = viewModel::retryFetch,
                onToggleMute = viewModel::toggleMute,
                onTogglePause = viewModel::togglePause,
                onNext = viewModel::fetchNext,
                onSetLevel = viewModel::setInsultLevel
            )
        } else {
            WideLayout(
                uiState = uiState,
                onRetry = viewModel::retryFetch,
                onToggleMute = viewModel::toggleMute,
                onTogglePause = viewModel::togglePause,
                onNext = viewModel::fetchNext,
                onSetLevel = viewModel::setInsultLevel
            )
        }
    }
}

@Composable
private fun PortraitLayout(
    uiState: InsultUiState,
    minTextHeight: Dp,
    onRetry: () -> Unit,
    onToggleMute: () -> Unit,
    onTogglePause: () -> Unit,
    onNext: () -> Unit,
    onSetLevel: (Int) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            InsultTextSection(
                uiState = uiState,
                onRetry = onRetry,
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = minTextHeight)
                    .wrapContentHeight(Alignment.CenterVertically)
            )

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                InsultMediaSection(uiState = uiState)
            }
        }

        ControlBar(
            uiState = uiState,
            onToggleMute = onToggleMute,
            onTogglePause = onTogglePause,
            onNext = onNext,
            onSetLevel = onSetLevel,
            modifier = Modifier.align(Alignment.BottomEnd)
        )
    }
}

@Composable
private fun WideLayout(
    uiState: InsultUiState,
    onRetry: () -> Unit,
    onToggleMute: () -> Unit,
    onTogglePause: () -> Unit,
    onNext: () -> Unit,
    onSetLevel: (Int) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            InsultTextSection(
                uiState = uiState,
                onRetry = onRetry,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .wrapContentHeight(Alignment.CenterVertically)
                    .padding(end = 8.dp)
            )

            VerticalDivider()

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(start = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                InsultMediaSection(uiState = uiState)
            }
        }

        ControlBar(
            uiState = uiState,
            onToggleMute = onToggleMute,
            onTogglePause = onTogglePause,
            onNext = onNext,
            onSetLevel = onSetLevel,
            modifier = Modifier.align(Alignment.BottomEnd)
        )
    }
}

@Composable
private fun InsultTextSection(uiState: InsultUiState, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        if (uiState.error != null) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = uiState.error,
                    style = MaterialTheme.typography.headlineLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.testTag("errorText")
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onRetry,
                    modifier = Modifier.testTag("retryButton")
                ) {
                    Text(text = stringResource(R.string.retry))
                }
            }
        } else {
            Text(
                text = uiState.insultText,
                style = MaterialTheme.typography.headlineLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.testTag("insultText")
            )
        }
    }
}

@Composable
private fun InsultMediaSection(uiState: InsultUiState) {
    val loadingDescription = stringResource(R.string.loading)

    if (uiState.isLoading) {
        CircularProgressIndicator(
            modifier = Modifier
                .testTag("loadingIndicator")
                .semantics { contentDescription = loadingDescription }
        )
    } else {
        val imageBitmap = uiState.imageBitmap
        if (imageBitmap != null) {
            Image(
                bitmap = imageBitmap,
                contentDescription = stringResource(R.string.insult_image),
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .aspectRatio(imageBitmap.width.toFloat() / imageBitmap.height.toFloat())
                    .fillMaxWidth(IMAGE_MAX_FRACTION)
                    .fillMaxHeight(IMAGE_MAX_FRACTION)
            )
        } else if (uiState.error == null) {
            Image(
                painter = painterResource(id = R.drawable.ic_launcher_foreground),
                contentDescription = stringResource(R.string.placeholder_image),
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth(IMAGE_MAX_FRACTION)
                    .fillMaxHeight(IMAGE_MAX_FRACTION)
                    .aspectRatio(1f)
            )
        }
    }
}

@Composable
private fun ControlBar(
    uiState: InsultUiState,
    onToggleMute: () -> Unit,
    onTogglePause: () -> Unit,
    onNext: () -> Unit,
    onSetLevel: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LevelSelector(selectedLevel = uiState.insultLevel, onSetLevel = onSetLevel)

        IconButton(
            onClick = onTogglePause,
            modifier = Modifier.testTag("pauseButton")
        ) {
            Icon(
                imageVector = if (uiState.isPaused) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                contentDescription = if (uiState.isPaused) stringResource(R.string.resume) else stringResource(R.string.pause),
                tint = MaterialTheme.colorScheme.onSurface
            )
        }

        IconButton(
            onClick = onNext,
            modifier = Modifier.testTag("nextButton")
        ) {
            Icon(
                imageVector = Icons.Filled.SkipNext,
                contentDescription = stringResource(R.string.next),
                tint = MaterialTheme.colorScheme.onSurface
            )
        }

        MuteButton(isMuted = uiState.isMuted, onToggleMute = onToggleMute)
    }
}

@Composable
private fun LevelSelector(selectedLevel: Int, onSetLevel: (Int) -> Unit, modifier: Modifier = Modifier) {
    val descriptions = listOf(
        stringResource(R.string.level_1),
        stringResource(R.string.level_2),
        stringResource(R.string.level_3)
    )
    SingleChoiceSegmentedButtonRow(modifier = modifier) {
        (1..3).forEach { level ->
            val description = descriptions[level - 1]
            SegmentedButton(
                selected = selectedLevel == level,
                onClick = { onSetLevel(level) },
                shape = SegmentedButtonDefaults.itemShape(index = level - 1, count = 3),
                // Skip the default checkmark icon to keep the control compact.
                icon = {},
                modifier = Modifier
                    .testTag("levelButton$level")
                    .semantics { contentDescription = description }
            ) {
                Text(text = level.toString())
            }
        }
    }
}

@Composable
private fun MuteButton(isMuted: Boolean, onToggleMute: () -> Unit, modifier: Modifier = Modifier) {
    IconButton(
        onClick = onToggleMute,
        modifier = modifier.testTag("muteButton")
    ) {
        Icon(
            imageVector = if (isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
            contentDescription = if (isMuted) stringResource(R.string.unmute) else stringResource(R.string.mute),
            tint = MaterialTheme.colorScheme.onSurface
        )
    }
}
