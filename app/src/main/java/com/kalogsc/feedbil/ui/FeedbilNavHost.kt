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
import com.kalogsc.feedbil.ui.articles.ArticleDetailRoute
import com.kalogsc.feedbil.ui.articles.ArticleDetailViewModel
import com.kalogsc.feedbil.ui.articles.ArticleListRoute
import com.kalogsc.feedbil.ui.articles.ArticleListViewModel
import com.kalogsc.feedbil.ui.feeds.FeedFormRoute
import com.kalogsc.feedbil.ui.feeds.FeedFormViewModel
import com.kalogsc.feedbil.ui.feeds.FeedListRoute
import com.kalogsc.feedbil.ui.feeds.FeedListViewModel

/**
 * Plain string routes (see `DECISIONS.md`). The combined article list is the
 * start destination; the feed list is reachable from its top bar, and opening
 * a row pushes the article detail with its id. The feed id travels as an
 * optional argument so the form serves both add and edit.
 */
object FeedRoutes {
    const val ARTICLE_LIST = "articles"
    const val FEED_LIST = "feeds"
    const val FEED_ID_ARG = "feedId"
    const val FEED_FORM = "feedForm?$FEED_ID_ARG={$FEED_ID_ARG}"
    const val ARTICLE_ID_ARG = "articleId"
    const val ARTICLE_DETAIL = "articleDetail?$ARTICLE_ID_ARG={$ARTICLE_ID_ARG}"

    fun feedForm(feedId: Long? = null): String =
        if (feedId == null) "feedForm" else "feedForm?$FEED_ID_ARG=$feedId"

    fun articleDetail(articleId: Long): String = "articleDetail?$ARTICLE_ID_ARG=$articleId"
}

@Composable
fun FeedbilNavHost(
    container: AppContainer,
    modifier: Modifier = Modifier,
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = FeedRoutes.ARTICLE_LIST,
        modifier = modifier,
    ) {
        composable(FeedRoutes.ARTICLE_LIST) {
            val viewModel: ArticleListViewModel = viewModel {
                ArticleListViewModel(
                    articleRepository = container.articleRepository,
                    feedRepository = container.feedRepository,
                    refreshAll = container.feedRefreshService::refreshAll,
                )
            }
            ArticleListRoute(
                viewModel = viewModel,
                onOpenFeeds = {
                    navController.navigate(FeedRoutes.FEED_LIST) {
                        launchSingleTop = true
                    }
                },
                onAddFeed = { navController.navigate(FeedRoutes.feedForm()) },
                onOpenArticle = { articleId ->
                    navController.navigate(FeedRoutes.articleDetail(articleId))
                },
            )
        }

        composable(
            route = FeedRoutes.ARTICLE_DETAIL,
            arguments = listOf(navArgument(FeedRoutes.ARTICLE_ID_ARG) { type = NavType.LongType }),
        ) { entry ->
            val articleId = entry.arguments?.getLong(FeedRoutes.ARTICLE_ID_ARG)
                ?: error("Article detail requires an article id")
            val viewModel: ArticleDetailViewModel = viewModel {
                ArticleDetailViewModel(
                    loadArticle = container.articleRepository::getById,
                    markRead = container.articleRepository::markRead,
                    articleId = articleId,
                )
            }
            ArticleDetailRoute(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
            )
        }

        composable(FeedRoutes.FEED_LIST) {
            val viewModel: FeedListViewModel = viewModel {
                FeedListViewModel(container.feedRepository)
            }
            FeedListRoute(
                viewModel = viewModel,
                onAddFeed = { navController.navigate(FeedRoutes.feedForm()) },
                onEditFeed = { feedId -> navController.navigate(FeedRoutes.feedForm(feedId)) },
                onBack = { navController.popBackStack() },
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
