package com.cpen321.usermanagement.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cpen321.usermanagement.R
import com.cpen321.usermanagement.ui.viewmodels.AuthViewModelContract
import com.cpen321.usermanagement.ui.viewmodels.FriendsViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendsScreen(
    authViewModel: AuthViewModelContract,
    viewModel: FriendsViewModel,
) {

    // Screen state and authentication info
    val uiState by viewModel.uiState.collectAsState()
    val authState by authViewModel.uiState.collectAsState()

    if (authState.isCheckingAuth || authState.user == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    // Current user id and a friendly code to show. `friendCode` may be provided
    val currentUserId = authState.user?._id
    val displayFriendCode = authState.user?.friendCode

    LaunchedEffect(currentUserId) {
        currentUserId?.let { viewModel.setCurrentUser(it) }
    }

    var addCodeInput by rememberSaveable { mutableStateOf("") }
    val clipboardManager = LocalClipboardManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    
    // Capture string resources for use in non-composable contexts
    val friendCodeCopiedMsg = stringResource(R.string.friend_code_copied)
    val friendRequestSentMsg = stringResource(R.string.friend_request_sent)
    val enterValidCodeMsg = stringResource(R.string.enter_valid_code)

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.friends)) }) },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            // Friend code card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(stringResource(R.string.your_friend_code), fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = displayFriendCode ?: "",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.weight(1f)
                        )

                        TextButton(onClick = {
                            val code = displayFriendCode ?: ""
                            clipboardManager.setText(AnnotatedString(code))
                            coroutineScope.launch { snackbarHostState.showSnackbar(friendCodeCopiedMsg) }
                        }) { Text(stringResource(R.string.copy)) }

                        Spacer(Modifier.width(8.dp))

                    }
                }
            }

            // Add friend by code
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = addCodeInput,
                    onValueChange = { addCodeInput = it },
                    label = { Text(stringResource(R.string.enter_friend_code)) },
                    modifier = Modifier.weight(1f)
                )

                Button(
                    onClick = {
                        if (addCodeInput.isNotBlank()) {
                            viewModel.sendFriendRequest(addCodeInput.trim())
                            addCodeInput = ""
                            coroutineScope.launch { snackbarHostState.showSnackbar(friendRequestSentMsg) }
                        } else {
                            coroutineScope.launch { snackbarHostState.showSnackbar(enterValidCodeMsg) }
                        }
                    },
                    modifier = Modifier.align(Alignment.CenterVertically)
                ) {
                    Text(stringResource(R.string.send))
                }
            }

            uiState.statusMessage?.let { message ->
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Pending Requests
            if (uiState.pendingRequests.isNotEmpty()) {
                Text(stringResource(R.string.pending_requests), style = MaterialTheme.typography.titleMedium)
                LazyColumn {
                    items(uiState.pendingRequests) { req ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(req.sender.name)
                            Row {
                                TextButton(onClick = { viewModel.acceptFriendRequest(req._id) }) { Text(stringResource(R.string.accept)) }
                                TextButton(onClick = { viewModel.rejectFriendRequest(req._id) }) { Text(stringResource(R.string.reject)) }
                            }
                        }
                        HorizontalDivider(
                            Modifier,
                            DividerDefaults.Thickness,
                            DividerDefaults.color
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Friend List
            Text(stringResource(R.string.friends_list), style = MaterialTheme.typography.titleMedium, modifier = Modifier.testTag("friendsSectionHeader"))
            if (uiState.friends.isEmpty()) {
                Text(stringResource(R.string.no_friends_yet), modifier = Modifier.testTag("noFriendsText"))
            } else {
                LazyColumn(modifier = Modifier.testTag("friendsList")) {
                    items(uiState.friends) { friend ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(friend.name)
                            TextButton(onClick = { viewModel.removeFriend(friend.id) }) { Text(stringResource(R.string.remove)) }
                        }
                        HorizontalDivider(
                            Modifier,
                            DividerDefaults.Thickness,
                            DividerDefaults.color
                        )
                    }
                }
            }
        }
    }
}
