package com.cpen321.usermanagement.ui.tests

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.cpen321.usermanagement.fakes.FakeAuthViewModel
import com.cpen321.usermanagement.fakes.FakeChallengesRepository
import com.cpen321.usermanagement.fakes.FakeNhlDataManager
import com.cpen321.usermanagement.fakes.FakeTicketsRepository
import com.cpen321.usermanagement.ui.navigation.NavigationStateManager
import com.cpen321.usermanagement.ui.screens.TicketsScreen
import com.cpen321.usermanagement.ui.screens.TicketsScreenActions
import com.cpen321.usermanagement.ui.viewmodels.TicketsViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TicketTests {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var fakeRepo: FakeTicketsRepository
    private lateinit var fakeChallengesRepo: FakeChallengesRepository
    private lateinit var fakeAuth: FakeAuthViewModel
    private lateinit var fakeNhl: FakeNhlDataManager
    private lateinit var navManager: NavigationStateManager
    private lateinit var viewModel: TicketsViewModel

    @Before
    fun setup() {
        fakeRepo = FakeTicketsRepository()
        fakeChallengesRepo = FakeChallengesRepository()
        fakeAuth = FakeAuthViewModel()
        fakeNhl = FakeNhlDataManager()
        navManager = NavigationStateManager()
        viewModel = TicketsViewModel(fakeRepo, fakeChallengesRepo, navManager, fakeNhl)
    }

    private fun launchTicketsScreen() {
        composeTestRule.setContent {
            TicketsScreen(
                authViewModel = fakeAuth,
                ticketsViewModel = viewModel,
                actions = TicketsScreenActions(
                    onBackClick = {},
                    onCreateTicketClick = { navManager.navigateToCreateTicket() }
                )
            )
        }
        viewModel.loadTickets(fakeAuth.uiState.value.user!!._id)
    }

    @Test
    fun viewTickets_displaysListCorrectly() {
        launchTicketsScreen()
        composeTestRule.waitForIdle() // Wait for tickets to load

        composeTestRule.onNodeWithText("Bingo Tickets").assertIsDisplayed()
        // Expand the upcoming section to see the ticket
        composeTestRule.onNodeWithText("Upcoming", useUnmergedTree = true).performClick()
        composeTestRule.onNodeWithText("My First Ticket").assertIsDisplayed()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun deleteTicket_removesFromList() {
        launchTicketsScreen()
        composeTestRule.waitForIdle() // Wait for tickets to load

        // Expand the upcoming section to see the ticket
        composeTestRule.onNodeWithText("Upcoming", useUnmergedTree = true).performClick()
        composeTestRule.onNodeWithText("My First Ticket").assertIsDisplayed()

        // The delete button is likely an icon inside the card now
        composeTestRule.onNodeWithContentDescription("Delete", useUnmergedTree = true).performClick()

        composeTestRule.waitForIdle() // Wait for deletion to process
        composeTestRule.waitUntilDoesNotExist(hasText("My First Ticket"))
    }

    @Test
    fun emptyState_showsNoTicketsText() {
        fakeRepo.emptyState = true
        launchTicketsScreen()
        composeTestRule.waitForIdle() // Wait for tickets to load

        composeTestRule.onNodeWithText("You have no tickets.").assertIsDisplayed()
        composeTestRule.onNodeWithText("Create one now!").assertIsDisplayed()
    }

    @Test
    fun createTicket_successfullyAddsToList() {
        // Navigate to Create Ticket screen
        navManager.navigateToCreateTicket()

        val userId = fakeAuth.uiState.value.user!!._id
        val game = fakeNhl.getGamesForTickets().first()
        val events = runBlocking { fakeNhl.getEventsForGame(game.id) }

        // Simulate user entering ticket name and selecting events
        viewModel.createTicket(
            userId = userId,
            name = "Test Ticket",
            game = game,
            events = events
        )
        composeTestRule.waitForIdle() // Wait for createTicket to update state

        // Navigate back to TicketsScreen to verify it was added
        composeTestRule.setContent {
            TicketsScreen(
                authViewModel = fakeAuth,
                ticketsViewModel = viewModel,
                actions = TicketsScreenActions(
                    onBackClick = {},
                    onCreateTicketClick = { navManager.navigateToCreateTicket() }
                )
            )
        }
        composeTestRule.waitForIdle() // wait for screen to reload and recompose
        // Expand the upcoming section to see the newly created ticket
        composeTestRule.onNodeWithText("Upcoming", useUnmergedTree = true).performClick()
        composeTestRule.onNodeWithText("Test Ticket").assertIsDisplayed()
    }
}
