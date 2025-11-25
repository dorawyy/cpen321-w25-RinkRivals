package com.cpen321.usermanagement.ui.tests

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.cpen321.usermanagement.fakes.FakeAuthViewModel
import com.cpen321.usermanagement.fakes.FakeFriendsRepository
import com.cpen321.usermanagement.ui.screens.FriendsScreen
import com.cpen321.usermanagement.ui.viewmodels.FriendsViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FriendsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var fakeRepo: FakeFriendsRepository
    private lateinit var viewModel: FriendsViewModel
    private val fakeAuthViewModel = FakeAuthViewModel()

    private fun launchScreen() {
        composeTestRule.setContent {
            viewModel = FriendsViewModel(fakeRepo)
            FriendsScreen(
                authViewModel = fakeAuthViewModel,
                viewModel = viewModel,
            )
        }
    }

    @Test
    fun sendFriendRequest_successAndFailure() {
        fakeRepo = FakeFriendsRepository()
        launchScreen()

        // Type a valid code and send
        composeTestRule.onNodeWithText("Enter friend code").performTextInput("VALIDCODE")
        composeTestRule.onNodeWithText("Send").performClick()
        composeTestRule.onNodeWithText("Friend request sent!").assertIsDisplayed()

        // Type an invalid code and send
        composeTestRule.onNodeWithText("Enter friend code").performTextInput("INVALID")
        composeTestRule.onNodeWithText("Send").performClick()
        composeTestRule.onNodeWithText("Enter a valid code").assertIsDisplayed()

        // Sending an empty code
        composeTestRule.onNodeWithText("Enter friend code").performTextClearance()
        composeTestRule.onNodeWithText("Send").performClick()
        composeTestRule.onNodeWithText("enter a valid code").assertIsDisplayed()
    }

    @Test
    fun acceptFriendRequest_shouldMoveToFriendsList() {
        fakeRepo = FakeFriendsRepository()
        launchScreen()

        composeTestRule.onNodeWithText("Pending Requests").assertIsDisplayed()
        composeTestRule.onNodeWithText("Taylor").assertIsDisplayed()

        composeTestRule.onNodeWithText("Accept").performClick()

        composeTestRule.onNodeWithText("Taylor").assertIsDisplayed()
        composeTestRule.onNodeWithText("Friend request accepted!").assertIsDisplayed()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun rejectFriendRequest_removesFromPendingList() {
        fakeRepo = FakeFriendsRepository()
        launchScreen()

        composeTestRule.onNodeWithText("Taylor").assertIsDisplayed()
        composeTestRule.onNodeWithText("Reject").performClick()

        composeTestRule.onNodeWithText("Friend request rejected").assertIsDisplayed()
        composeTestRule.waitUntilDoesNotExist(hasText("Taylor"))
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun removeFriend_shouldUpdateList() {
        fakeRepo = FakeFriendsRepository()
        launchScreen()

        composeTestRule.onNodeWithText("Alex").assertIsDisplayed()

        composeTestRule.onAllNodesWithText("Remove")[0].performClick()

        composeTestRule.waitUntilDoesNotExist(hasText("Alex"))
        composeTestRule.onNodeWithText("Friend removed").assertIsDisplayed()
    }

    @Test
    fun viewFriends_displaysListCorrectly() {
        fakeRepo = FakeFriendsRepository()
        launchScreen()

        composeTestRule.onNodeWithText("Alex").assertIsDisplayed()
    }

    @Test
    fun viewFriends_noFriendsOrRequests_showsEmptyText() {
        fakeRepo = FakeFriendsRepository().apply { emptyState = true }
        launchScreen()

        composeTestRule.onNodeWithText("No friends yet.").assertIsDisplayed()
    }
}
