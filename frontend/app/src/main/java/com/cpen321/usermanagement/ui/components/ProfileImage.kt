package com.cpen321.usermanagement.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.cpen321.usermanagement.R
import com.cpen321.usermanagement.data.remote.api.RetrofitClient

/**
 * A reusable profile image component that displays a user's profile picture
 * or a fallback icon if no picture is available.
 *
 * @param profilePicture The URL or path to the profile picture, or null if not available
 * @param size The size of the profile image (default: 40.dp)
 * @param backgroundColor The background color for the fallback icon (default: surfaceVariant)
 * @param contentDescription Optional content description for accessibility
 * @param modifier Optional modifier for customization
 */
@Composable
fun ProfileImage(
    profilePicture: String?,
    size: Dp = 40.dp,
    backgroundColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    contentDescription: String? = null,
    modifier: Modifier = Modifier
) {
    var isLoading by remember { mutableStateOf(false) }
    var hasError by remember { mutableStateOf(false) }

    if (profilePicture != null && profilePicture.isNotBlank() && !hasError) {
        Box(
            modifier = modifier.size(size),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = RetrofitClient.getPictureUri(profilePicture),
                contentDescription = contentDescription ?: stringResource(R.string.profile_picture),
                contentScale = ContentScale.Crop,
                onLoading = { isLoading = true },
                onSuccess = { isLoading = false },
                onError = { 
                    isLoading = false
                    hasError = true
                },
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
            )

            if (isLoading) {
                Surface(
                    shape = CircleShape,
                    color = backgroundColor.copy(alpha = 0.7f),
                    modifier = Modifier.fillMaxSize()
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(size / 2),
                            strokeWidth = 2.dp
                        )
                    }
                }
            }
        }
    } else {
        // Fallback to default icon
        Surface(
            shape = CircleShape,
            color = backgroundColor,
            modifier = modifier.size(size)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = contentDescription ?: stringResource(R.string.profile_picture),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(size * 0.6f)
                )
            }
        }
    }
}
