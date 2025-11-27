package com.cpen321.usermanagement.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cpen321.usermanagement.R
import com.cpen321.usermanagement.ui.navigation.NavRoutes
import com.cpen321.usermanagement.ui.viewmodels.NavigationViewModel
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag

@Composable
fun BottomNavigationBar(
    navigationViewModel: NavigationViewModel = hiltViewModel()
) {
    val navManager = navigationViewModel.navigationStateManager
    val navState by navManager.navigationState.collectAsState()
    val currentRoute = navState.currentRoute

    NavigationBar {

        NavigationBarItem(
            modifier = Modifier.semantics { testTag = "nav_tickets" },
            selected = currentRoute == NavRoutes.TICKETS,
            onClick = { navManager.navigateToTickets() },
            icon = {
                Icon(
                    painter = androidx.compose.ui.res.painterResource(id = R.drawable.bingo_ticket),
                    contentDescription = stringResource(R.string.bingo_tickets),
                    modifier = Modifier.size(24.dp)
                )
            },
            label = { Text(stringResource(R.string.tickets)) }
        )

        NavigationBarItem(
            modifier = Modifier.semantics { testTag = "nav_challenges" },
            selected = currentRoute == NavRoutes.CHALLENGES,
            onClick = { navManager.navigateToChallenges() },
            icon = {
                Icon(
                    painter = androidx.compose.ui.res.painterResource(id = R.drawable.swords_icon),
                    contentDescription = stringResource(R.string.challenges),
                    modifier = Modifier.size(24.dp)
                )
            },
            label = { Text(stringResource(R.string.challenges)) }
        )

        NavigationBarItem(
            modifier = Modifier.semantics { testTag = "nav_home" },
            selected = currentRoute == NavRoutes.MAIN,
            onClick = { navManager.navigateToMain() },
            icon = {
                Icon(
                    painter = androidx.compose.ui.res.painterResource(id = R.drawable.hockey_outlined_icon),
                    contentDescription = stringResource(R.string.app_name),
                    modifier = Modifier.size(28.dp)
                )
            },
            label = { Text(stringResource(R.string.home)) }
        )

        NavigationBarItem(
            modifier = Modifier.semantics { testTag = "nav_friends" },
            selected = currentRoute == NavRoutes.FRIENDS,
            onClick = { navManager.navigateToFriends() },
            icon = {
                Icon(
                    painter = androidx.compose.ui.res.painterResource(id = R.drawable.group_outlined_icon),
                    contentDescription = stringResource(R.string.friends),
                    modifier = Modifier.size(24.dp)
                )
            },
            label = { Text(stringResource(R.string.friends)) }
        )

        NavigationBarItem(
            modifier = Modifier.semantics { testTag = "nav_profile" },
            selected = currentRoute == NavRoutes.PROFILE,
            onClick = { navManager.navigateToProfile() },
            icon = {
                Icon(
                    painter = androidx.compose.ui.res.painterResource(id = R.drawable.ic_account_circle),
                    contentDescription = stringResource(R.string.profile),
                    modifier = Modifier.size(24.dp)
                )
            },
            label = { Text(stringResource(R.string.profile)) }
        )
    }
}