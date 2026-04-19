package com.example.excusemyfrenchcompose.ui.components

import androidx.compose.foundation.Image
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.excusemyfrenchcompose.R
import com.example.excusemyfrenchcompose.ui.viewmodel.InsultUiState
import com.example.excusemyfrenchcompose.ui.viewmodel.InsultViewModelInterface

private val WIDE_LAYOUT_THRESHOLD = 600.dp
private const val IMAGE_MAX_FRACTION = 0.9f

@Composable
fun InsultDisplay(viewModel: InsultViewModelInterface, modifier: Modifier = Modifier) {
    val uiState by viewModel.uiState.collectAsState()

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        if (maxWidth < WIDE_LAYOUT_THRESHOLD) {
            PortraitLayout(uiState = uiState, onToggleMute = viewModel::toggleMute)
        } else {
            WideLayout(uiState = uiState, onToggleMute = viewModel::toggleMute)
        }
    }
}

@Composable
private fun PortraitLayout(uiState: InsultUiState, onToggleMute: () -> Unit) {
    val context = LocalContext.current

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            InsultTextSection(
                uiState = uiState,
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = with(LocalDensity.current) { (0.15f * context.resources.displayMetrics.heightPixels).toDp() })
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

        MuteButton(
            isMuted = uiState.isMuted,
            onToggleMute = onToggleMute,
            modifier = Modifier.align(Alignment.BottomEnd)
        )
    }
}

@Composable
private fun WideLayout(uiState: InsultUiState, onToggleMute: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            InsultTextSection(
                uiState = uiState,
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

        MuteButton(
            isMuted = uiState.isMuted,
            onToggleMute = onToggleMute,
            modifier = Modifier.align(Alignment.BottomEnd)
        )
    }
}

@Composable
private fun InsultTextSection(uiState: InsultUiState, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        if (uiState.error != null) {
            Text(
                text = uiState.error,
                style = MaterialTheme.typography.headlineLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.testTag("errorText")
            )
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

