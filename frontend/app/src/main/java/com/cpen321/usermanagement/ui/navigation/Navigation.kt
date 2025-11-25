package com.cpen321.usermanagement.ui.navigation

import android.util.Log
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.cpen321.usermanagement.R
import com.cpen321.usermanagement.ui.screens.AuthScreen
import com.cpen321.usermanagement.ui.screens.FriendsScreen
import com.cpen321.usermanagement.ui.screens.ChallengesScreen
import com.cpen321.usermanagement.ui.screens.ChallengesScreenActions
import com.cpen321.usermanagement.ui.screens.CreateBingoTicketScreen
import com.cpen321.usermanagement.ui.screens.CreateChallengeScreen
import com.cpen321.usermanagement.ui.screens.ChallengeDetailsScreen
import com.cpen321.usermanagement.ui.screens.LoadingScreen
import com.cpen321.usermanagement.ui.screens.MainScreen
import com.cpen321.usermanagement.ui.screens.ManageProfileScreen
import com.cpen321.usermanagement.ui.screens.ProfileScreenActions
import com.cpen321.usermanagement.ui.screens.ProfileCompletionScreen
import com.cpen321.usermanagement.ui.screens.ProfileScreen
import com.cpen321.usermanagement.ui.screens.TicketDetailScreen
import com.cpen321.usermanagement.ui.screens.TicketsScreen
import com.cpen321.usermanagement.ui.screens.TicketsScreenActions
import com.cpen321.usermanagement.ui.viewmodels.AuthViewModel
import com.cpen321.usermanagement.ui.viewmodels.ChallengesViewModel
import com.cpen321.usermanagement.ui.viewmodels.MainViewModel
import com.cpen321.usermanagement.ui.viewmodels.NavigationViewModel
import com.cpen321.usermanagement.ui.viewmodels.ProfileViewModel
import com.cpen321.usermanagement.ui.viewmodels.TicketsViewModel
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.padding
import com.cpen321.usermanagement.ui.components.BottomNavigationBar

object NavRoutes {
    const val LOADING = "loading"
    const val AUTH = "auth"
    const val MAIN = "main"
    const val PROFILE = "profile"
    const val TICKETS = "tickets"
    const val TICKET_DETAIL = "ticket_detail"
    const val TICKET_DETAIL_ARG = "ticketId"
    const val TICKET_DETAIL_ROUTE = "$TICKET_DETAIL/{$TICKET_DETAIL_ARG}"
    const val CREATE_TICKET = "create_ticket"
    const val CREATE_TICKET_ARG = "gameId"
    const val CREATE_TICKET_WITH_ARG = "$CREATE_TICKET/{$CREATE_TICKET_ARG}"
    const val FRIENDS = "friends"
    const val CHALLENGES = "challenges"

    // Dynamic route format
    const val EDIT_CHALLENGE_ROUTE = "edit_challenge"
    const val EDIT_CHALLENGE_ARG = "challengeId"
    const val EDIT_CHALLENGE = "$EDIT_CHALLENGE_ROUTE/{$EDIT_CHALLENGE_ARG}"
    const val ADD_CHALLENGE = "$CHALLENGES/new"

    const val MANAGE_PROFILE = "manage_profile"
    const val PROFILE_COMPLETION = "profile_completion"
}

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController()
) {
    val navigationViewModel: NavigationViewModel = hiltViewModel()
    val navigationStateManager = navigationViewModel.navigationStateManager
    val navigationEvent by navigationStateManager.navigationEvent.collectAsState()

    // Initialize view models required for navigation-level scope
    val authViewModel: AuthViewModel = hiltViewModel()
    val profileViewModel: ProfileViewModel = hiltViewModel()
    val mainViewModel: MainViewModel = hiltViewModel()
    val ticketsViewModel: TicketsViewModel = hiltViewModel()
    val challengesViewModel: ChallengesViewModel = hiltViewModel()


    // Handle navigation events from NavigationStateManager
    LaunchedEffect(navigationEvent) {
        handleNavigationEvent(
            navigationEvent,
            navController,
            navigationStateManager,
            authViewModel,
            mainViewModel,
        )
    }

    val navState by navigationStateManager.navigationState.collectAsState()
    // Show bottom bar on all app routes except auth and loading screens
    val showBottomBar = navState.currentRoute != NavRoutes.AUTH && navState.currentRoute != NavRoutes.LOADING

    androidx.compose.material3.Scaffold(
        bottomBar = {
            if (showBottomBar) {
                BottomNavigationBar(navigationViewModel)
            }
        }
    ) { innerPadding ->
        // Only apply the bottom portion of the Scaffold's innerPadding to the NavHost.
        // This preserves correct spacing for the bottom navigation while avoiding
        // adding extra top padding for screens that render their own TopAppBar.
        AppNavHost(
            navController = navController,
            authViewModel = authViewModel,
            profileViewModel = profileViewModel,
            mainViewModel = mainViewModel,
            ticketsViewModel = ticketsViewModel,
            challengesViewModel = challengesViewModel,
            navigationStateManager = navigationStateManager,
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding())
        )
    }
}

private fun handleNavigationEvent(
    navigationEvent: NavigationEvent,
    navController: NavHostController,
    navigationStateManager: NavigationStateManager,
    authViewModel: AuthViewModel,
    mainViewModel: MainViewModel,
) {
    when (navigationEvent) {
        is NavigationEvent.NavigateToAuth -> {
            navController.navigate(NavRoutes.AUTH) {
                popUpTo(0) { inclusive = true }
            }
            navigationStateManager.clearNavigationEvent()
        }

        is NavigationEvent.NavigateToAuthWithMessage -> {
            authViewModel.setSuccessMessage(navigationEvent.message)
            navController.navigate(NavRoutes.AUTH) {
                popUpTo(0) { inclusive = true }
            }
            navigationStateManager.clearNavigationEvent()
        }

        is NavigationEvent.NavigateToMain -> {
            navController.navigate(NavRoutes.MAIN) {
                popUpTo(0) { inclusive = true }
            }
            // Refresh main profile/tickets so UI reflects recent changes (e.g. newly created tickets)
            mainViewModel.loadProfile()
            navigationStateManager.clearNavigationEvent()
        }

        is NavigationEvent.NavigateToMainWithMessage -> {
            mainViewModel.setSuccessMessage(navigationEvent.message)
            navController.navigate(NavRoutes.MAIN) {
                popUpTo(0) { inclusive = true }
            }
            // Ensure main view reloads profile/tickets after navigating back
            mainViewModel.loadProfile()
            navigationStateManager.clearNavigationEvent()
        }

        is NavigationEvent.NavigateToProfileCompletion -> {
            navController.navigate(NavRoutes.PROFILE_COMPLETION) {
                popUpTo(0) { inclusive = true }
            }
            navigationStateManager.clearNavigationEvent()
        }

        is NavigationEvent.NavigateToProfile -> {
            navController.navigate(NavRoutes.PROFILE)
            navigationStateManager.clearNavigationEvent()
        }

        is NavigationEvent.NavigateToTickets -> {
            navController.navigate(NavRoutes.TICKETS) {
                popUpTo(NavRoutes.TICKETS) { inclusive = false }
                launchSingleTop = true
            }
            navigationStateManager.clearNavigationEvent()
        }

        is NavigationEvent.NavigateToCreateTicket -> {
            navController.navigate(NavRoutes.CREATE_TICKET)
            navigationStateManager.clearNavigationEvent()
        }

        is NavigationEvent.NavigateToTicketDetail -> {
            navController.navigate("${NavRoutes.TICKET_DETAIL}/${navigationEvent.ticketId}")
            navigationStateManager.clearNavigationEvent()
        }

        is NavigationEvent.NavigateToChallenges -> {
            navController.navigate(NavRoutes.CHALLENGES) {
                popUpTo(NavRoutes.CHALLENGES) { inclusive = false }
                launchSingleTop = true
            }
            navigationStateManager.clearNavigationEvent()
        }

        is NavigationEvent.NavigateToEditChallenge -> {
            navController.navigate("${NavRoutes.EDIT_CHALLENGE_ROUTE}/${navigationEvent.challengeId}")
            navigationStateManager.clearNavigationEvent()
        }

        is NavigationEvent.NavigateToAddChallenge -> {
            navController.navigate("${NavRoutes.CHALLENGES}/${"new"}")
            navigationStateManager.clearNavigationEvent()
        }


        is NavigationEvent.NavigateToFriends -> {
            navController.navigate(NavRoutes.FRIENDS)
            navigationStateManager.clearNavigationEvent()
        }

        is NavigationEvent.NavigateToManageProfile -> {
            navController.navigate(NavRoutes.MANAGE_PROFILE)
            navigationStateManager.clearNavigationEvent()
        }

        is NavigationEvent.NavigateBack -> {
            navController.popBackStack()
            navigationStateManager.clearNavigationEvent()
        }

        is NavigationEvent.ClearBackStack -> {
            navController.popBackStack(navController.graph.startDestinationId, false)
            navigationStateManager.clearNavigationEvent()
        }

        is NavigationEvent.NoNavigation -> {
            // Do nothing
        }
    }
}

@Composable
private fun AppNavHost(
    navController: NavHostController,
    authViewModel: AuthViewModel,
    profileViewModel: ProfileViewModel,
    ticketsViewModel: TicketsViewModel,
    challengesViewModel: ChallengesViewModel,
    mainViewModel: MainViewModel,
    navigationStateManager: NavigationStateManager,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = NavRoutes.LOADING,
        modifier = modifier
    ) {
        composable(NavRoutes.LOADING) {
            LoadingScreen(message = stringResource(R.string.checking_authentication))
        }

        composable(NavRoutes.AUTH) {
            AuthScreen(authViewModel = authViewModel, profileViewModel = profileViewModel)
        }

        composable(NavRoutes.PROFILE_COMPLETION) {
            ProfileCompletionScreen(
                profileViewModel = profileViewModel,
                onProfileCompleted = { navigationStateManager.handleProfileCompletion() },
                onProfileCompletedWithMessage = { message ->
                    Log.d("AppNavigation", "Profile completed with message: $message")
                    navigationStateManager.handleProfileCompletionWithMessage(message)
                }
            )
        }

        composable(NavRoutes.MAIN) {
            MainScreen(
                mainViewModel = mainViewModel,
                onTicketClick = { navigationStateManager.navigateToTickets() },
            )
        }

        composable(NavRoutes.PROFILE) {
            ProfileScreen(
                authViewModel = authViewModel,
                profileViewModel = profileViewModel,
                actions = ProfileScreenActions(
                    onBackClick = { navigationStateManager.navigateBack() },
                    onManageProfileClick = { navigationStateManager.navigateToManageProfile() },
                    onAccountDeleted = { navigationStateManager.handleAccountDeletion() }
                )
            )
        }

        composable(NavRoutes.TICKETS) {
            TicketsScreen(
                authViewModel = authViewModel,
                ticketsViewModel = ticketsViewModel,
                actions = TicketsScreenActions(
                    onBackClick = { navigationStateManager.navigateBack() },
                    onCreateTicketClick = { navigationStateManager.navigateToCreateTicket() }
                )
            )
        }

        composable(NavRoutes.CREATE_TICKET) {
            CreateBingoTicketScreen(
                ticketsViewModel = ticketsViewModel,
                onBackClick = { navigationStateManager.navigateBack() },
                authViewModel = authViewModel,
                onTicketCreated = { navigationStateManager.navigateToTickets() },
                nhlDataManager = ticketsViewModel.nhlDataManager
            )
        }

        // Create ticket with a preselected game (gameId passed as path arg)
        composable(
            route = NavRoutes.CREATE_TICKET_WITH_ARG,
            arguments = listOf(navArgument(NavRoutes.CREATE_TICKET_ARG) { type = NavType.StringType })
        ) { backStackEntry ->
            val gameId = backStackEntry.arguments?.getString(NavRoutes.CREATE_TICKET_ARG)
            CreateBingoTicketScreen(
                ticketsViewModel = ticketsViewModel,
                onBackClick = { navigationStateManager.navigateBack() },
                authViewModel = authViewModel,
                onTicketCreated = { navigationStateManager.navigateToTickets() },
                nhlDataManager = ticketsViewModel.nhlDataManager,
                initialGameId = gameId
            )
        }

        composable(
            route = NavRoutes.TICKET_DETAIL_ROUTE,
            arguments = listOf(navArgument(NavRoutes.TICKET_DETAIL_ARG) { type = NavType.StringType })
        ) { backStackEntry ->
            val ticketId = backStackEntry.arguments?.getString(NavRoutes.TICKET_DETAIL_ARG)
            val ticket = ticketsViewModel.uiState.collectAsState().value.allTickets
                .find { it._id == ticketId }
            if (ticket != null) {
                TicketDetailScreen(ticket, onBackClick = { navigationStateManager.navigateBack() }, viewModel = ticketsViewModel)
            }
        }

        composable(NavRoutes.FRIENDS) {
            FriendsScreen(
                viewModel = hiltViewModel(),       // FriendsViewModel scoped to screen
                authViewModel = authViewModel,     // pass the shared AuthViewModel
            )
        }
        composable(NavRoutes.CHALLENGES) {
            ChallengesScreen(
                challengesViewModel = challengesViewModel,
                socketEventListener = mainViewModel.socketEventListener,
                actions = ChallengesScreenActions(
                    onBackClick = { navigationStateManager.navigateBack() },
                    onAddChallengeClick = { navigationStateManager.navigateToAddChallenge() },
                    onChallengeClick = { challengeId ->
                        navigationStateManager.navigateToEditChallenge(challengeId)
                    }
                )
            )

        }

        composable(
            route = NavRoutes.EDIT_CHALLENGE,
            arguments = listOf(navArgument(NavRoutes.EDIT_CHALLENGE_ARG) {type = NavType.StringType})
        ) { backStackEntry ->
            val challengeId = backStackEntry.arguments?.getString(NavRoutes.EDIT_CHALLENGE_ARG)

            if (challengeId != null) {
                ChallengeDetailsScreen(
                    challengeId = challengeId,
                    challengesViewModel = challengesViewModel,
                    socketManager = mainViewModel.socketManager,
                    socketEventListener = mainViewModel.socketEventListener,
                    onBackClick = { navigationStateManager.navigateBack() },
                    onCreateTicketClick = { gameId ->
                        // Navigate to the create ticket screen with the challenge's game preselected
                        navController.navigate("${NavRoutes.CREATE_TICKET}/$gameId")
                    },
                    nhlDataManager = challengesViewModel.nhlDataManager
                )
            } else {
                Log.e("AppNavigation", "Challenge ID is null, navigating back.")
                navigationStateManager.navigateBack()
            }
        }

        composable(NavRoutes.ADD_CHALLENGE) {
            CreateChallengeScreen(
                challengesViewModel = challengesViewModel,
                onBackClick = { navigationStateManager.navigateBack() },
                onChallengeCreated = { navigationStateManager.navigateToChallenges() }
            )

        }

        composable(NavRoutes.MANAGE_PROFILE) {
            ManageProfileScreen(
                profileViewModel = profileViewModel,
                onBackClick = { navigationStateManager.navigateBack() }
            )
        }
    }
}
