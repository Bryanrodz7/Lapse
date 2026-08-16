package dev.randyapps.lapse.ui.nav

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import dev.randyapps.lapse.data.model.ItemDraft
import dev.randyapps.lapse.data.model.QuickPick
import dev.randyapps.lapse.ui.edit.EditRoute
import dev.randyapps.lapse.ui.home.HomeRoute

object HomeDestination {
    const val ROUTE = "home"
}

object EditDestination {
    const val ARG_ITEM_ID = "itemId"
    const val ARG_QUICK_PICK = "quickPick"
    const val NO_QUICK_PICK = ""
    const val ROUTE = "edit?$ARG_ITEM_ID={$ARG_ITEM_ID}&$ARG_QUICK_PICK={$ARG_QUICK_PICK}"

    fun forNewItem(): String =
        "edit?$ARG_ITEM_ID=${ItemDraft.NEW_ITEM_ID}&$ARG_QUICK_PICK=$NO_QUICK_PICK"

    fun forItem(id: Long): String = "edit?$ARG_ITEM_ID=$id&$ARG_QUICK_PICK=$NO_QUICK_PICK"

    /** Opens the new-item form with a quick pick already applied. */
    fun forQuickPick(pick: QuickPick): String =
        "edit?$ARG_ITEM_ID=${ItemDraft.NEW_ITEM_ID}&$ARG_QUICK_PICK=${pick.name}"
}

// Shared-axis rather than a fade: the incoming screen slides in from the trailing edge while
// the outgoing one moves a shorter distance the same way, so the pair reads as one movement.
private const val TRANSITION_MS = 280
private const val OUTGOING_FRACTION = 4

@Composable
fun LapseNavHost(
    deepLinkItemId: Long? = null,
    onDeepLinkHandled: () -> Unit = {},
    navController: NavHostController = rememberNavController(),
) {
    // A tapped notification opens straight to that item's edit screen, with Home underneath so
    // Back behaves normally rather than dumping the user out of the app.
    LaunchedEffect(deepLinkItemId) {
        val id = deepLinkItemId ?: return@LaunchedEffect
        navController.navigate(EditDestination.forItem(id))
        onDeepLinkHandled()
    }

    NavHost(
        navController = navController,
        startDestination = HomeDestination.ROUTE,
        enterTransition = {
            slideInHorizontally(tween(TRANSITION_MS)) { it } + fadeIn(tween(TRANSITION_MS))
        },
        exitTransition = {
            slideOutHorizontally(tween(TRANSITION_MS)) { -it / OUTGOING_FRACTION } +
                fadeOut(tween(TRANSITION_MS))
        },
        popEnterTransition = {
            slideInHorizontally(tween(TRANSITION_MS)) { -it / OUTGOING_FRACTION } +
                fadeIn(tween(TRANSITION_MS))
        },
        popExitTransition = {
            slideOutHorizontally(tween(TRANSITION_MS)) { it } + fadeOut(tween(TRANSITION_MS))
        },
    ) {
        composable(HomeDestination.ROUTE) {
            HomeRoute(
                onAddClick = { navController.navigate(EditDestination.forNewItem()) },
                // Settings arrives in its own stage; deliberately inert rather than wired to a
                // placeholder destination.
                onSettingsClick = {},
                onItemClick = { item -> navController.navigate(EditDestination.forItem(item.id)) },
                onQuickPick = { pick ->
                    navController.navigate(EditDestination.forQuickPick(pick))
                },
            )
        }

        composable(
            route = EditDestination.ROUTE,
            arguments = listOf(
                navArgument(EditDestination.ARG_ITEM_ID) {
                    type = NavType.LongType
                    defaultValue = ItemDraft.NEW_ITEM_ID
                },
                navArgument(EditDestination.ARG_QUICK_PICK) {
                    type = NavType.StringType
                    defaultValue = EditDestination.NO_QUICK_PICK
                },
            ),
        ) {
            EditRoute(onClose = { navController.popBackStack() })
        }
    }
}
