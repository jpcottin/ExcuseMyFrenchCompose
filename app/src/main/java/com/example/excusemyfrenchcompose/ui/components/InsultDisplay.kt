package com.example.excusemyfrenchcompose.ui.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.excusemyfrenchcompose.ui.viewmodel.InsultViewModel
import com.example.excusemyfrenchcompose.R

@Composable
fun InsultDisplay(viewModel: InsultViewModel, modifier: Modifier = Modifier) {

    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Text (at least 15% of the screen height, centered vertically and horizontally)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = with(LocalDensity.current) { (0.15f * context.resources.displayMetrics.heightPixels).toDp() })
                .wrapContentHeight(Alignment.CenterVertically), // Center vertically within its allocated space
            contentAlignment = Alignment.Center // THIS IS THE KEY CHANGE

        ) {
            Text(
                text = uiState.insultText,
                style = MaterialTheme.typography.headlineLarge, // Use a larger text style
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(16.dp))

        if (uiState.isLoading) {
            CircularProgressIndicator()
        } else {
            val imageBitmap = uiState.imageBitmap
            // Image (constrained to 90% of remaining width/height)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {

                if (imageBitmap != null) {
                    Image(
                        bitmap = imageBitmap,
                        contentDescription = "Insult Image",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .aspectRatio(imageBitmap.width.toFloat() / imageBitmap.height.toFloat())
                            .fillMaxWidth(0.9f)
                            .fillMaxHeight(0.9f)
                    )
                } else {
                    Image(
                        painter = painterResource(id = R.drawable.ic_launcher_foreground),
                        contentDescription = "Placeholder Image",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .fillMaxHeight(0.9f)
                            .aspectRatio(1f)
                    )
                    Text("Error displaying image or decoding Base64 data.")
                }
            }
        }
    }
}