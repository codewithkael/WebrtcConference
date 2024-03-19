package com.codewithkael.webrtcconference.ui.components


import android.widget.FrameLayout
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import org.webrtc.SurfaceViewRenderer

@Composable
fun SurfaceViewRendererComposable(
    modifier: Modifier,
    onSurfaceReady: (SurfaceViewRenderer) -> Unit
) {
    // Create the SurfaceViewRenderer
    AndroidView(
        modifier = modifier
            .fillMaxWidth(),
        factory = { ctx ->
            FrameLayout(ctx).apply {
                addView(SurfaceViewRenderer(ctx).also {
                    onSurfaceReady.invoke(it)
                })
            }
        }
    )
}