package com.example.presentation.component

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.example.presentation.theme.BorderSubtle

@Composable
fun SkeletonBox(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(8.dp)
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.75f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Box(
        modifier = modifier
            .clip(shape)
            .background(BorderSubtle.copy(alpha = alpha))
    )
}

@Composable
fun PlaylistCardSkeleton() {
    Column(
        modifier = Modifier.width(120.dp)
    ) {
        SkeletonBox(
            modifier = Modifier.size(120.dp),
            shape = RoundedCornerShape(8.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        SkeletonBox(
            modifier = Modifier
                .width(80.dp)
                .height(14.dp),
            shape = RoundedCornerShape(4.dp)
        )
    }
}

@Composable
fun RecentTrackCardSkeleton(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(BorderSubtle.copy(alpha = 0.15f)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SkeletonBox(
            modifier = Modifier.size(48.dp),
            shape = RoundedCornerShape(0.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column(
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxHeight().weight(1f)
        ) {
            SkeletonBox(
                modifier = Modifier
                    .width(100.dp)
                    .height(14.dp),
                shape = RoundedCornerShape(4.dp)
            )
        }
    }
}

@Composable
fun RecommendedTrackCardSkeleton() {
    Column(
        modifier = Modifier.width(160.dp)
    ) {
        SkeletonBox(
            modifier = Modifier.size(160.dp),
            shape = RoundedCornerShape(8.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        SkeletonBox(
            modifier = Modifier
                .width(120.dp)
                .height(16.dp),
            shape = RoundedCornerShape(4.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        SkeletonBox(
            modifier = Modifier
                .width(80.dp)
                .height(12.dp),
            shape = RoundedCornerShape(4.dp)
        )
    }
}

@Composable
fun TrackItemSkeleton() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SkeletonBox(
            modifier = Modifier.size(48.dp),
            shape = RoundedCornerShape(8.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            SkeletonBox(
                modifier = Modifier
                    .width(140.dp)
                    .height(16.dp),
                shape = RoundedCornerShape(4.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            SkeletonBox(
                modifier = Modifier
                    .width(90.dp)
                    .height(12.dp),
                shape = RoundedCornerShape(4.dp)
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        SkeletonBox(
            modifier = Modifier
                .width(40.dp)
                .height(12.dp),
            shape = RoundedCornerShape(4.dp)
        )
    }
}
