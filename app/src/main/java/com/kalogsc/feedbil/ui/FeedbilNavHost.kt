package com.kalogsc.feedbil.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.kalogsc.feedbil.AppContainer
import com.kalogsc.feedbil.domain.model.Feed
import com.kalogsc.feedbil.ui.feeds.FeedFormRoute
import com.kalogsc.feedbil.ui.feeds.FeedFormViewModel
import com.kalogsc.feedbil.ui.feeds.FeedListRoute
import com.kalogsc.feedbil.ui.feeds.FeedListViewModel

/**
 * Plain string routes (see `DECISIONS.md`): two destinations today, and the
 * feed id travels as an optional argument so the form serves both add and edit.
 */
object FeedRoutes {
    const val FEED_LIST = "feeds"
    const val FEED_ID_ARG = "feedId"
    const val FEED_FORM = "feedForm?$FEED_ID_ARG={$FEED_ID_ARG}"

    fun feedForm(feedId: Long? = null): String =
        if (feedId == null) "feedForm" else "feedForm?$FEED_ID_ARG=$feedId"
}

@Composable
fun FeedbilNavHost(
    container: AppContainer,
    modifier: Modifier = Modifier,
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = FeedRoutes.FEED_LIST,
        modifier = modifier,
    ) {
        composable(FeedRoutes.FEED_LIST) {
            val viewModel: FeedListViewModel = viewModel {
                FeedListViewModel(container.feedRepository)
            }
            FeedListRoute(
                viewModel = viewModel,
                onAddFeed = { navController.navigate(FeedRoutes.feedForm()) },
                onEditFeed = { feedId -> navController.navigate(FeedRoutes.feedForm(feedId)) },
            )
        }

        composable(
            route = FeedRoutes.FEED_FORM,
            arguments = listOf(
                navArgument(FeedRoutes.FEED_ID_ARG) {
                    type = NavType.LongType
                    defaultValue = Feed.UNSAVED_ID
                },
            ),
        ) { entry ->
            val feedId = entry.arguments?.getLong(FeedRoutes.FEED_ID_ARG) ?: Feed.UNSAVED_ID
            val viewModel: FeedFormViewModel = viewModel {
                FeedFormViewModel(
                    feedRepository = container.feedRepository,
                    articleRepository = container.articleRepository,
                    feedFetcher = container.feedFetcher,
                    feedId = feedId,
                )
            }
            FeedFormRoute(
                viewModel = viewModel,
                onSaved = { navController.popBackStack() },
                onBack = { navController.popBackStack() },
            )
        }
    }
}
