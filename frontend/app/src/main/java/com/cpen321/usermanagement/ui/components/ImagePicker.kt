package com.cpen321.usermanagement.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.cpen321.usermanagement.ui.theme.LocalSpacing
import java.io.File

@Composable
fun ImagePicker(
    onDismiss: () -> Unit,
    onImageSelected: (Uri) -> Unit
) {
    val context = LocalContext.current
    var hasCameraPermission by remember { mutableStateOf(false) }
    var hasStoragePermission by remember { mutableStateOf(false) }
    var photoUri by remember { mutableStateOf<Uri?>(null) }

    val cameraLauncher = rememberCameraLauncher(
        photoUri = photoUri,
        onImageSelected = onImageSelected,
        onDismiss = onDismiss
    )

    val galleryLauncher = rememberGalleryLauncher(
        onImageSelected = onImageSelected,
        onDismiss = onDismiss
    )

    val cameraPermissionLauncher = rememberCameraPermissionLauncher(
        context = context,
        cameraLauncher = cameraLauncher,
        onPhotoUriSet = { photoUri = it },
        onPermissionGranted = { hasCameraPermission = it }
    )

    val storagePermissionLauncher = rememberStoragePermissionLauncher(
        galleryLauncher = galleryLauncher,
        onPermissionGranted = { hasStoragePermission = it }
    )

    LaunchedEffect(Unit) {
        checkInitialPermissions(context) { camera, storage ->
            hasCameraPermission = camera
            hasStoragePermission = storage
        }
    }

    ImagePickerDialog(
        onDismiss = onDismiss,
        onCameraClick = {
            handleCameraClick(
                context = context,
                hasCameraPermission = hasCameraPermission,
                cameraLauncher = cameraLauncher,
                permissionLauncher = cameraPermissionLauncher,
                onPhotoUriSet = { photoUri = it }
            )
        },
        onGalleryClick = {
            handleGalleryClick(
                hasStoragePermission = hasStoragePermission,
                galleryLauncher = galleryLauncher,
                permissionLauncher = storagePermissionLauncher
            )
        }
    )
}

@Composable
private fun rememberCameraLauncher(
    photoUri: Uri?,
    onImageSelected: (Uri) -> Unit,
    onDismiss: () -> Unit
) = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.TakePicture()
) { success ->
    if (success) {
        photoUri?.let { uri ->
            onImageSelected(uri)
            onDismiss()
        }
    }
}

@Composable
private fun rememberGalleryLauncher(
    onImageSelected: (Uri) -> Unit,
    onDismiss: () -> Unit
) = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia()
) { uri ->
    uri?.let { onImageSelected(it) }
    onDismiss()
}

@Composable
private fun rememberCameraPermissionLauncher(
    context: android.content.Context,
    cameraLauncher: androidx.activity.result.ActivityResultLauncher<Uri>,
    onPhotoUriSet: (Uri) -> Unit,
    onPermissionGranted: (Boolean) -> Unit
) = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission()
) { isGranted ->
    onPermissionGranted(isGranted)
    if (isGranted) {
        val photoFile = File.createTempFile(
            "profile_photo_${System.currentTimeMillis()}",
            ".jpg",
            context.cacheDir
        )
        val newPhotoUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileProvider",
            photoFile
        )
        onPhotoUriSet(newPhotoUri)
        launchCamera(cameraLauncher, newPhotoUri)
    }
}

@Composable
private fun rememberStoragePermissionLauncher(
    galleryLauncher: androidx.activity.result.ActivityResultLauncher<PickVisualMediaRequest>,
    onPermissionGranted: (Boolean) -> Unit
) = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission()
) { isGranted ->
    onPermissionGranted(isGranted)
    if (isGranted) {
        galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }
}

@Composable
private fun ImagePickerDialog(
    onDismiss: () -> Unit,
    onCameraClick: () -> Unit,
    onGalleryClick: () -> Unit
) {
    val spacing = LocalSpacing.current

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.medium),
            shape = MaterialTheme.shapes.medium
        ) {
            Column(
                modifier = Modifier.padding(spacing.large),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Select Image Source",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(spacing.large))

                Button(onClick = onCameraClick) {
                    Text("Take Photo")
                }

                Spacer(modifier = Modifier.height(spacing.small))

                Button(onClick = onGalleryClick) {
                    Text("Choose from Gallery")
                }

                Spacer(modifier = Modifier.height(spacing.small))

                Button(
                    type = "secondary",
                    onClick = onDismiss,
                ) {
                    Text("Cancel")
                }
            }
        }
    }
}

private fun checkInitialPermissions(
    context: android.content.Context,
    onPermissionsChecked: (Boolean, Boolean) -> Unit
) {
    val cameraPermission = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED

    val storagePermission =
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_MEDIA_IMAGES
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }

    onPermissionsChecked(cameraPermission, storagePermission)
}

private fun handleCameraClick(
    context: android.content.Context,
    hasCameraPermission: Boolean,
    cameraLauncher: androidx.activity.result.ActivityResultLauncher<Uri>,
    permissionLauncher: androidx.activity.result.ActivityResultLauncher<String>,
    onPhotoUriSet: (Uri) -> Unit
) {
    if (hasCameraPermission) {
        val photoFile = File.createTempFile(
            "profile_photo_${System.currentTimeMillis()}",
            ".jpg",
            context.cacheDir
        )
        val newPhotoUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileProvider",
            photoFile
        )
        onPhotoUriSet(newPhotoUri)
        launchCamera(cameraLauncher, newPhotoUri)
    } else {
        permissionLauncher.launch(Manifest.permission.CAMERA)
    }
}

private fun handleGalleryClick(
    hasStoragePermission: Boolean,
    galleryLauncher: androidx.activity.result.ActivityResultLauncher<PickVisualMediaRequest>,
    permissionLauncher: androidx.activity.result.ActivityResultLauncher<String>
) {
    // Photo Picker doesn't require permissions on Android 13+ (API 33+)
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
        // Use modern Photo Picker (no permission needed)
        galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    } else if (hasStoragePermission) {
        // Fallback for older Android versions with permission
        galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    } else {
        // Request permission for older Android versions
        permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
    }
}

private fun launchCamera(
    cameraLauncher: androidx.activity.result.ActivityResultLauncher<Uri>,
    photoUri: Uri
) {
    cameraLauncher.launch(photoUri)
}
