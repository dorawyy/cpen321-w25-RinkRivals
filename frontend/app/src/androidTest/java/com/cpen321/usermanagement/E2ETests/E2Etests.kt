package com.cpen321.usermanagement.E2ETests

import android.util.Log
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiSelector
import com.cpen321.usermanagement.MainActivity
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Rule
import org.junit.Test
import java.lang.Thread.sleep

//Log in to EMAIL_1 and EMAIL_2 using password "rinkrivals321" if necessary
@HiltAndroidTest
class E2ETests {

    companion object {
        const val EMAIL_1 = "rinkrivalstests@gmail.com"
        const val EMAIL_2 = "rinkrivalstests2@gmail.com"
        const val EMAIL_TICKETS = "rinkrivalstests@gmail.com"   // same as EMAIL_1 unless you want separate accounts
    }

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    // --------------------------------------------------------------------
    // Helpers
    // --------------------------------------------------------------------

    private fun waitForVm(millis: Long) {
        composeRule.waitForIdle()
        sleep(millis)
        composeRule.waitForIdle()
    }

    private fun signIn(email: String) {
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("signinButton")
            .assertExists()
            .performClick()

        waitForVm(1500)

        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        val account = device.findObject(UiSelector().text(email))

        if (account.exists() && account.isEnabled) {
            account.click()
        }

        composeRule.waitUntilNodeExists("nav_friends")
        composeRule.waitForIdle()
    }

    private fun logout() {
        composeRule.onNodeWithTag("nav_profile").performClick()
        waitForVm(400)

        composeRule.waitUntilNodeExists("signOutButton")
        composeRule.onNodeWithTag("signOutButton").performClick()

        composeRule.waitUntilNodeExists("signinButton")
        composeRule.waitForIdle()
    }

    private fun navigateToFriends() {
        composeRule.onNodeWithTag("nav_friends").performClick()
        composeRule.waitUntilNodeExists("friendsSectionHeader")
    }

    private fun goToTickets() {
        composeRule.onNodeWithTag("nav_tickets").performClick()
        composeRule.waitUntilNodeExists("addTicketButton")
    }

    private fun fillAllBingoSquares() {
        repeat(9) { index ->
            composeRule.onNodeWithTag("bingoSquare_$index").performClick()

            // Wait for event picker to appear
            composeRule.waitUntilNodeExistsWithPrefix("eventPickerItem_")

            // Pick the first event
            composeRule.onAllNodes(hasTestTagPrefix("eventPickerItem_"))
                .onFirst()
                .performClick()

            // Return to Bingo grid before moving to the next square
            composeRule.waitUntilNodeExists("createTicketButton")
        }
    }

    private fun captureFriendCode(): String {
        return composeRule
            .onNodeWithTag("friendCodeText")
            .fetchSemanticsNode()
            .config[SemanticsProperties.Text]
            .first()
            .text
    }

    // --------------------------------------------------------------------
    // Extensions
    // --------------------------------------------------------------------

    private fun ComposeTestRule.waitUntilNodeExists(tag: String, timeout: Long = 10_000) {
        waitUntil(timeout) {
            onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun hasTestTagPrefix(prefix: String): SemanticsMatcher {
        return SemanticsMatcher("${SemanticsProperties.TestTag.name} startsWith $prefix") { node ->
            node.config.getOrNull(SemanticsProperties.TestTag)?.startsWith(prefix) == true
        }
    }

    private fun ComposeTestRule.waitUntilNodeExistsWithPrefix(prefix: String, timeout: Long = 10_000) {
        waitUntil(timeout) {
            onAllNodes(hasTestTagPrefix(prefix)).fetchSemanticsNodes().isNotEmpty()
        }
    }

    // --------------------------------------------------------------------
    // FRIENDS TEST
    // --------------------------------------------------------------------

    @Test
    fun testFriendsFlow() {
        Log.d("TEST_FRIENDS", "Injecting Hilt")
        hiltRule.inject()
        waitForVm(2000)

        // ----------------------------- USER 2 LOGIN -----------------------------
        signIn(EMAIL_2)
        waitForVm(1500)
        navigateToFriends()

        val codeUser2 = captureFriendCode()
        assert(codeUser2.isNotBlank())

        logout()
        waitForVm(1000)

        // ----------------------------- USER 1 LOGIN -----------------------------
        signIn(EMAIL_1)
        navigateToFriends()

        composeRule.waitUntilNodeExists("noFriendsText")

        composeRule.onNodeWithTag("addFriendInput").performTextInput(codeUser2)
        composeRule.onNodeWithTag("sendFriendRequestButton").performClick()
        waitForVm(1000)

        logout()
        waitForVm(1000)

        // ----------------------------- USER 2 ACCEPT -----------------------------
        signIn(EMAIL_2)
        navigateToFriends()

        composeRule.waitUntilNodeExists("pendingRequestRow")
        composeRule.onNodeWithTag("acceptFriendRequestButton").performClick()
        composeRule.waitUntilNodeExists("friendRow")

        logout()
        waitForVm(1000)

        // ----------------------------- USER 1 REMOVE -----------------------------
        signIn(EMAIL_1)
        navigateToFriends()

        composeRule.waitUntilNodeExists("friendRow")
        composeRule.onNodeWithTag("removeFriendButton", useUnmergedTree = true).performClick()
        composeRule.waitUntilNodeExists("noFriendsText")

        logout()
    }

    // --------------------------------------------------------------------
    // TICKETS TEST
    // --------------------------------------------------------------------

    @Test
    fun testTicketsFlow() {
        Log.d("TEST_TICKETS", "Injecting Hilt")
        hiltRule.inject()
        waitForVm(1800)

        signIn(EMAIL_TICKETS)
        goToTickets()

        // Empty state
        composeRule.waitUntilNodeExists("emptyAddTicketButton")

        // Create ticket
        composeRule.onNodeWithTag("addTicketButton").performClick()
        composeRule.waitUntilNodeExists("back_button")

        composeRule.onNodeWithTag("ticketNameTextField")
            .performTextInput("MyTestTicket")

        composeRule.onNodeWithTag("gameDropdown").performClick()
        waitForVm(400)

        composeRule.waitUntilNodeExistsWithPrefix("gameDropdownItem_")
        composeRule.onAllNodes(hasTestTagPrefix("gameDropdownItem_"))
            .onFirst()
            .performClick()

        // 🔥 Fill all 9 squares
        fillAllBingoSquares()

        composeRule.onNodeWithTag("createTicketButton").performClick()

        // Click the first available ticket section
        composeRule.waitUntilNodeExistsWithPrefix("tickets_section_header_")
        composeRule.onAllNodes(hasTestTagPrefix("tickets_section_header_"))
            .onFirst()
            .performClick()

        val createdNode = composeRule
            .onAllNodes(hasTestTagPrefix("ticket_card_"))
            .fetchSemanticsNodes()
            .firstOrNull()

        assert(createdNode != null)

        val ticketTag = createdNode!!.config[SemanticsProperties.TestTag]
        val ticketId = ticketTag.substringAfter("ticket_card_")

        composeRule.onNodeWithTag("ticket_delete_button_$ticketId").performClick()
        waitForVm(1000)

        composeRule.waitUntilNodeExists("emptyAddTicketButton")

        logout()
    }
}
