package com.example.player

import android.app.Activity
import android.app.PictureInPictureParams
import android.util.Rational
import android.view.ViewGroup
import android.view.WindowManager
import androidx.annotation.OptIn
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.PictureInPicture
import androidx.compose.material.icons.filled.Headset
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.SlowMotionVideo
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.library.VideoItem
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayerView(
    video: VideoItem,
    viewModel: MainViewModel,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Initialize ExoPlayer
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            playWhenReady = true
        }
    }

    // Set source URI and repeat/speed modes
    LaunchedEffect(video) {
        exoPlayer.setMediaItem(MediaItem.fromUri(video.path))
        exoPlayer.prepare()
        exoPlayer.setPlaybackSpeed(viewModel.playbackSpeed)
        
        // Check if there is history for this video and auto resume
        val matchingHistory = viewModel.watchHistory.value.find { it.videoUrl == video.path }
        if (matchingHistory != null && viewModel.playbackSettingAutoResume && matchingHistory.playbackPosition > 5000) {
            exoPlayer.seekTo(matchingHistory.playbackPosition)
        }
    }

    // Control bar state
    var showControls by remember { mutableStateOf(true) }
    
    // Hide controls after a timeout
    LaunchedEffect(showControls, viewModel.isPlaying) {
        if (showControls && viewModel.isPlaying) {
            delay(4000)
            showControls = false
        }
    }

    // Capture lifecycle events to pause/release
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    if (!viewModel.isAudioOnlyMode) {
                        exoPlayer.pause()
                        viewModel.isPlaying = false
                    }
                }
                Lifecycle.Event.ON_RESUME -> {
                    if (viewModel.isPlaying) {
                        exoPlayer.play()
                    }
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            // Save history progress before releasing
            if (exoPlayer.currentPosition > 2000) {
                viewModel.updateHistoryProgress(video.path, exoPlayer.currentPosition)
            }
            lifecycleOwner.lifecycle.removeObserver(observer)
            exoPlayer.release()
        }
    }

    // Interactive slider gesture indicators
    var brightnessVal by remember { mutableFloatStateOf(0.7f) } // Default level
    var volumeVal by remember { mutableFloatStateOf(0.5f) }    // Default level
    
    // Show sliding overlay trigger flags
    var currentGestureText by remember { mutableStateOf<String?>(null) }
    var hideGestureIndicatorJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    val coroutineScope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("video_player_container")
    ) {
        // ACTUAL PLAYER RENDER LAYERING
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false // We use our own customized M3 controls
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            update = { playerView ->
                // Apply Aspect Ratio Fit/Fill/Zoom options
                playerView.resizeMode = when (viewModel.fitMode) {
                    0 -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                    1 -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                    2 -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    else -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    // Double Tap Left and Right to Seek back/forward
                    detectTapGestures(
                        onDoubleTap = { offset ->
                            val width = size.width
                            if (offset.x < width / 3) {
                                // Double tap Left -> Seek Back -10s
                                val seekTarget = (exoPlayer.currentPosition - 10000).coerceAtLeast(0)
                                exoPlayer.seekTo(seekTarget)
                                currentGestureText = "⏪ Replay 10s"
                            } else if (offset.x > 2 * width / 3) {
                                // Double tap Right -> Seek Forward +10s
                                val seekTarget = (exoPlayer.currentPosition + 10000).coerceAtMost(exoPlayer.duration)
                                exoPlayer.seekTo(seekTarget)
                                currentGestureText = "⏩ Forward 10s"
                            }
                            // Reset gesture display shortly
                            coroutineScope.launch {
                                delay(1200)
                                currentGestureText = null
                            }
                        },
                        onTap = {
                            if (!viewModel.isTouchLocked) {
                                showControls = !showControls
                            }
                        }
                    )
                }
                .pointerInput(Unit) {
                    // Drag vertically to volume and brightness gestures support
                    detectDragGestures(
                        onDragStart = { /* Reset overlays */ },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            if (viewModel.isTouchLocked) return@detectDragGestures
                            
                            val width = size.width
                            val dragYFraction = dragAmount.y / size.height
                            
                            if (change.position.x < width / 2) {
                                // Brightness gestures (Left Side)
                                brightnessVal = (brightnessVal - dragYFraction * 1.5f).coerceIn(0f, 1f)
                                currentGestureText = "☀️ Brightness: ${(brightnessVal * 100).toInt()}%"
                                
                                // Adjust window brightness if active activity is present
                                (context as? Activity)?.let { activity ->
                                    val lp = activity.window.attributes
                                    lp.screenBrightness = brightnessVal
                                    activity.window.attributes = lp
                                }
                            } else {
                                // Volume gestures (Right Side)
                                volumeVal = (volumeVal - dragYFraction * 1.5f).coerceIn(0f, 1f)
                                exoPlayer.volume = volumeVal
                                currentGestureText = "🔊 Volume: ${(volumeVal * 100).toInt()}%"
                            }

                            hideGestureIndicatorJob?.cancel()
                            hideGestureIndicatorJob = coroutineScope.launch {
                                delay(1200)
                                currentGestureText = null
                            }
                        }
                    )
                }
        )

        // GESTURE SLIDER OVERLAY POPUP
        currentGestureText?.let { text ->
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Text(
                    text = text,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }

        // TOUCH LOCK INDICATOR SECTION
        if (viewModel.isTouchLocked) {
            IconButton(
                onClick = { viewModel.isTouchLocked = false; showControls = true },
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 24.dp)
                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                    .size(54.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = "Unlock Controls",
                    tint = Color.Red,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        // CONTROL LAYOUTS BAR (Top and Bottom overlays)
        AnimatedVisibility(
            visible = showControls && !viewModel.isTouchLocked,
            enter = fadeIn() + slideInVertically { -it },
            exit = fadeOut() + slideOutVertically { -it },
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.55f))
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = video.title,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        maxLines = 1
                    )
                    Text(
                        text = if (video.isLocal) "Offline Library" else "Network Live Stream",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )
                }

                // PiP Button Trigger
                IconButton(onClick = {
                    (context as? Activity)?.let { activity ->
                        try {
                            val builder = PictureInPictureParams.Builder()
                                .setAspectRatio(Rational(16, 9))
                            activity.enterPictureInPictureMode(builder.build())
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }) {
                    Icon(
                        imageVector = Icons.Filled.PictureInPicture,
                        contentDescription = "Picture in Picture",
                        tint = Color.White
                    )
                }

                // Background audio toggle
                IconButton(onClick = {
                    viewModel.isAudioOnlyMode = !viewModel.isAudioOnlyMode
                    currentGestureText = if (viewModel.isAudioOnlyMode) "🎧 Background Audio On" else "📺 Video Render On"
                }) {
                    Icon(
                        imageVector = Icons.Filled.Headset,
                        contentDescription = "Audio Play",
                        tint = if (viewModel.isAudioOnlyMode) Color.Green else Color.White
                    )
                }
            }
        }

        // BOTTOM CONTROLS GRAPHICS
        AnimatedVisibility(
            visible = showControls && !viewModel.isTouchLocked,
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it },
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.55f))
                    .padding(bottom = 24.dp, start = 16.dp, end = 16.dp, top = 8.dp)
            ) {
                var position by remember { mutableStateOf(0L) }
                var duration by remember { mutableStateOf(0L) }

                // Periodic current timing check
                LaunchedEffect(exoPlayer) {
                    while (true) {
                        position = exoPlayer.currentPosition
                        duration = exoPlayer.duration.coerceAtLeast(0L)
                        delay(500)
                    }
                }

                // Media Progress slider bar control
                Slider(
                    value = if (duration > 0) position.toFloat() / duration else 0f,
                    onValueChange = { percent ->
                        val target = (percent * duration).toLong()
                        exoPlayer.seekTo(target)
                    },
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatTime(position),
                        color = Color.White,
                        fontSize = 12.sp
                    )
                    Text(
                        text = formatTime(duration),
                        color = Color.White,
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Lock button screen
                    IconButton(onClick = { viewModel.isTouchLocked = true }) {
                        Icon(
                            imageVector = Icons.Filled.LockOpen,
                            contentDescription = "Lock controls",
                            tint = Color.White
                        )
                    }

                    // Prev track in queue
                    IconButton(onClick = { viewModel.playPrev() }) {
                        Icon(
                            imageVector = Icons.Filled.SkipPrevious,
                            contentDescription = "Prev Video",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    // Play/Pause central control
                    IconButton(
                        onClick = {
                            if (exoPlayer.isPlaying) {
                                exoPlayer.pause()
                                viewModel.isPlaying = false
                            } else {
                                exoPlayer.play()
                                viewModel.isPlaying = true
                            }
                        },
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(100.dp))
                            .size(54.dp)
                    ) {
                        Icon(
                            imageVector = if (viewModel.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = "Play/Pause",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    // Next track in queue
                    IconButton(onClick = { viewModel.playNext() }) {
                        Icon(
                            imageVector = Icons.Filled.SkipNext,
                            contentDescription = "Next Video",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    // Resize layout option
                    IconButton(onClick = {
                        viewModel.fitMode = (viewModel.fitMode + 1) % 3
                        currentGestureText = when (viewModel.fitMode) {
                            0 -> "Aspect: Fit Mode"
                            1 -> "Aspect: Fill Mode"
                            2 -> "Aspect: Zoom Mode"
                            else -> "Aspect: Fit Mode"
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Filled.AspectRatio,
                            contentDescription = "Aspect ratio",
                            tint = Color.White
                        )
                    }

                    // Playback speed picker Dialog trigger
                    var showSpeedMenu by remember { mutableStateOf(false) }
                    Box {
                        IconButton(onClick = { showSpeedMenu = true }) {
                            Icon(
                                imageVector = Icons.Filled.SlowMotionVideo,
                                contentDescription = "Playback Speed",
                                tint = Color.White
                            )
                        }

                        DropdownMenu(
                            expanded = showSpeedMenu,
                            onDismissRequest = { showSpeedMenu = false }
                        ) {
                            listOf(0.5f, 1.0f, 1.5f, 2.0f, 3.0f).forEach { speed ->
                                DropdownMenuItem(
                                    text = { Text("${speed}x", fontWeight = FontWeight.Bold) },
                                    onClick = {
                                        viewModel.playbackSpeed = speed
                                        exoPlayer.setPlaybackSpeed(speed)
                                        showSpeedMenu = false
                                        currentGestureText = "Speed: ${speed}x"
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatTime(millis: Long): String {
    if (millis <= 0) return "00:00"
    val totalSeconds = millis / 1000
    val seconds = totalSeconds % 60
    val minutes = (totalSeconds / 60) % 60
    val hours = totalSeconds / 3600
    return if (hours > 0) {
        String.format("%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}
