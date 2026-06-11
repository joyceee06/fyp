package com.fyp.ekopantri

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import androidx.work.*
import com.fyp.ekopantri.ui.auth.AuthViewModel
import com.fyp.ekopantri.ui.auth.LoginScreen
import com.fyp.ekopantri.ui.auth.RegisterScreen
import com.fyp.ekopantri.ui.auth.ProfileScreen
import com.fyp.ekopantri.ui.dashboard.DashboardScreen
import com.fyp.ekopantri.ui.education.EducationDetailScreen
import com.fyp.ekopantri.ui.education.EducationScreen
import com.fyp.ekopantri.ui.education.EducationViewModel
import com.fyp.ekopantri.ui.insight.InsightScreen
import com.fyp.ekopantri.ui.insight.InsightViewModel
import com.fyp.ekopantri.ui.inventory.*
import com.fyp.ekopantri.ui.recipe.*
import com.fyp.ekopantri.ui.theme.EkoPantriTheme
import com.fyp.ekopantri.worker.ExpiryCheckWorker
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import java.util.concurrent.TimeUnit
import kotlin.String

// --- NAVIGATION CONFIGURATION ---

/**
 * Defines all possible screens and their navigation routes in the application.
 *
 * @property route The unique string identifier used for navigation.
 * @property title The display name used in the UI (e.g., in the TopBar or BottomBar).
 * @property iconRes The optional drawable resource ID for the screen's icon.
 */
sealed class Screen(val route: String, val title: String, val iconRes: Int = 0) {
    object Login : Screen("login", "Login")
    object Register : Screen("register", "Register")
    object Dashboard : Screen("dashboard", "Home", R.drawable.dashboard)
    object Inventory : Screen("inventory", "Inventory", R.drawable.inventory)
    object Recipes : Screen("recipes", "Recipes", R.drawable.recipe)
    object Education : Screen("education", "Guide", R.drawable.education)
    object Insights : Screen("insights", "Insights", R.drawable.insights)
    object Profile : Screen("profile", "Profile")
    object AddFood : Screen("add_food", "Add Food")
    object EditFood : Screen("edit_food/{foodId}", "Edit Food")
    object RecipeDetail : Screen("recipe_detail", "Recipe Details")
}

/**
 * A list of screens intended to be displayed in the Bottom Navigation Bar.
 */
val bottomNavItems = listOf(
    Screen.Dashboard,
    Screen.Inventory,
    Screen.Recipes,
    Screen.Education,
    Screen.Insights
)

/**
 * The entry point of the EkoPantri application.
 *
 * This Activity handles initial setup such as:
 * - Runtime permission requests for notifications.
 * - Configuring background workers for expiry checks.
 * - Hosting the main Compose-based UI.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestNotificationPermission()
        setupBackgroundWorkers()

        setContent {
            EkoPantriTheme {
                EkoPantriApp()
            }
        }
    }

    /**
     * Requests the [Manifest.permission.POST_NOTIFICATIONS] permission on Android 13 (Tiramisu) and above.
     */
    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
        }
    }

    /**
     * Configures and enqueues [ExpiryCheckWorker] with WorkManager.
     * Includes both a periodic 24-hour request and a one-time immediate test trigger.
     */
    private fun setupBackgroundWorkers() {
        val workConstraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val expiryWorkRequest = PeriodicWorkRequestBuilder<ExpiryCheckWorker>(24, TimeUnit.HOURS)
            .setConstraints(workConstraints)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "ExpiryCheck",
            ExistingPeriodicWorkPolicy.KEEP,
            expiryWorkRequest
        )

        // Immediate test trigger
        val testWorkRequest = OneTimeWorkRequestBuilder<ExpiryCheckWorker>().build()
        WorkManager.getInstance(this).enqueue(testWorkRequest)
    }
}

// --- MAIN APP COMPONENT ---

/**
 * The high-level Composable representing the entire app's UI architecture.
 *
 * It manages:
 * - Shared ViewModels across the application lifecycle.
 * - Navigation state via [NavHostController].
 * - The global [Scaffold] layout including TopBar and BottomBar visibility.
 * - Dynamic start destination based on authentication state.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EkoPantriApp() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Create shared ViewModels at the top level to ensure a single source of truth
    val authViewModel: AuthViewModel = viewModel()
    val inventoryViewModel: InventoryViewModel = viewModel()
    val recipeViewModel: RecipeViewModel = viewModel()
    val educationViewModel: EducationViewModel = viewModel(factory = EducationViewModel.Factory)
    val insightViewModel: InsightViewModel = viewModel()

    val startDestination = if (Firebase.auth.currentUser != null) Screen.Dashboard.route else Screen.Login.route
    val isMainScreen = bottomNavItems.any { it.route == currentRoute }

    Scaffold(
        topBar = {
            if (isMainScreen) {
                EkoPantriTopBar(
                    onLogout = {
                        authViewModel.logout {
                        // Clear all user-specific data upon logout
                            inventoryViewModel.clearData()
                            insightViewModel.clearData()

                            navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                            }
                        } },
                    onProfileClick = {
                        navController.navigate(Screen.Profile.route)
                    }
                )
            }
        },
        bottomBar = {
            if (isMainScreen) {
                EkoPantriBottomBar(navController, currentRoute)
            }
        }
    ) { innerPadding ->
        AppNavHost(
            navController = navController,
            startDestination = startDestination,
            paddingValues = innerPadding,
            isMainScreen = isMainScreen,
            authViewModel = authViewModel,
            inventoryViewModel = inventoryViewModel,
            recipeViewModel = recipeViewModel,
            educationViewModel = educationViewModel,
            insightViewModel = insightViewModel
        )
    }
}

// --- NAVIGATION COMPONENTS ---

/**
 * A customized [TopAppBar] for the EkoPantri app.
 *
 * @param onLogout Callback triggered when the user clicks the logout icon.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EkoPantriTopBar(onLogout: () -> Unit, onProfileClick: () -> Unit) {
    TopAppBar(
        title = {
            Text(
                "EkoPantri",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-1).sp
            )
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.primary,
            titleContentColor = MaterialTheme.colorScheme.onPrimary
        ),
        actions = {
            IconButton(onClick = onProfileClick) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Profile",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
            IconButton(onClick = onLogout, modifier = Modifier.padding(end = 8.dp)) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                    contentDescription = "Logout",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    )
}

/**
 * The standard [NavigationBar] displayed on main screens.
 *
 * @param navController The controller used to perform tab switching.
 * @param currentRoute The current navigation path used to highlight the active tab.
 */
@Composable
fun EkoPantriBottomBar(navController: NavHostController, currentRoute: String?) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 8.dp
    ) {
        bottomNavItems.forEach { screen ->
            val selected = currentRoute == screen.route
            NavigationBarItem(
                icon = {
                    Icon(
                        painter = androidx.compose.ui.res.painterResource(id = screen.iconRes),
                        contentDescription = screen.title,
                        modifier = Modifier.size(if (selected) 26.dp else 24.dp)
                    )
                },
                label = {
                    Text(
                        screen.title,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                    )
                },
                selected = selected,
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                    indicatorColor = MaterialTheme.colorScheme.primary
                ),
                onClick = {
                    if (currentRoute != screen.route) {
                        navController.navigate(screen.route) {
                            popUpTo(Screen.Dashboard.route) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                }
            )
        }
    }
}

/**
 * The main Navigation Host that maps routes to specific screen Composables.
 *
 * @param navController The navigation controller.
 * @param startDestination The initial screen to display.
 * @param paddingValues Safe area padding provided by the Scaffold.
 * @param isMainScreen Whether the current screen is a primary tab (Dashboard, Inventory, etc.).
 * @param authViewModel Shared authentication state.
 * @param inventoryViewModel Shared inventory management state.
 * @param recipeViewModel Shared recipe suggestion state.
 * @param educationViewModel Shared educational content state.
 * @param insightViewModel Shared waste statistics state.
 */
@Composable
fun AppNavHost(
    navController: NavHostController,
    startDestination: String,
    paddingValues: PaddingValues,
    isMainScreen: Boolean,
    authViewModel: AuthViewModel,
    inventoryViewModel: InventoryViewModel,
    recipeViewModel: RecipeViewModel,
    educationViewModel: EducationViewModel,
    insightViewModel: InsightViewModel
) {
    val topPadding = if (isMainScreen) paddingValues.calculateTopPadding() else 0.dp
    val bottomPadding = paddingValues.calculateBottomPadding()

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = Modifier.padding(top = topPadding, bottom = bottomPadding)
    ) {
        // --- AUTH SCREENS ---
        composable(Screen.Login.route) {
            LoginScreen(
                viewModel = authViewModel,
                onLoginSuccess = {
                    inventoryViewModel.startListening()
                    insightViewModel.startListening()
                    navController.navigate(Screen.Dashboard.route)
                },
                onNavigateToRegister = { navController.navigate(Screen.Register.route) }
            )
        }

        composable(Screen.Register.route) {
            RegisterScreen(
                viewModel = authViewModel,
                onRegisterSuccess = {
                    inventoryViewModel.startListening()
                    insightViewModel.startListening()
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNavigateToLogin = { navController.popBackStack() }
            )
        }

        composable(Screen.Profile.route) {
            ProfileScreen(
                viewModel = authViewModel,
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        // --- CORE SCREENS ---
        composable(Screen.Dashboard.route) {
            DashboardScreen(
                inventoryViewModel = inventoryViewModel,
                onNavigateToInventory = {
                    navController.navigate(Screen.Inventory.route) {
                        popUpTo(Screen.Dashboard.route) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onNavigateToRecipes = {
                    navController.navigate(Screen.Recipes.route) {
                        popUpTo(Screen.Dashboard.route) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onNavigateToEducation = {
                    navController.navigate(Screen.Education.route) {
                        popUpTo(Screen.Dashboard.route) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onEditFood = { id -> navController.navigate("edit_food/$id") }
            )
        }

        composable(Screen.Inventory.route) {
            InventoryScreen(
                onNavigateToAddFood = { navController.navigate(Screen.AddFood.route) },
                onNavigateToEditFood = { id -> navController.navigate("edit_food/$id") },
                onNavigateToReview = { navController.navigate("scan_review") },
                viewModel = inventoryViewModel
            )
        }

        composable(Screen.AddFood.route) {
            AddFoodScreen(
                onNavigateBack = { navController.popBackStack() },
                viewModel = inventoryViewModel
            )
        }

        composable(
            route = Screen.EditFood.route,
            arguments = listOf(navArgument("foodId") { type = NavType.StringType })
        ) { backStackEntry ->
            val foodId = backStackEntry.arguments?.getString("foodId")
            AddFoodScreen(
                foodId = foodId,
                onNavigateBack = { navController.popBackStack() },
                viewModel = inventoryViewModel
            )
        }

        composable("scan_review") {
            val scannedItems by inventoryViewModel.scannedItems.collectAsState()
            ScanReviewScreen(
                scannedItems = scannedItems,
                onSaveAll = { items ->
                    inventoryViewModel.batchAddItems(items)
                    navController.popBackStack()
                },
                onCancel = { navController.popBackStack() },
                viewModel = inventoryViewModel
            )
        }

        composable(Screen.Recipes.route) {
            RecipeScreen(
                navController = navController,
                inventoryViewModel = inventoryViewModel,
                recipeViewModel = recipeViewModel
            )
        }

        composable(Screen.RecipeDetail.route) {
            RecipeDetailScreen(
                viewModel = recipeViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Education.route) {
            val pantryItems by inventoryViewModel.foodList.collectAsState()
            EducationScreen(
                navController = navController,
                inventoryItems = pantryItems,
                viewModel = educationViewModel
            )
        }

        composable(route = "article_detail/{documentId}") { backStackEntry ->
            val documentId = backStackEntry.arguments?.getString("documentId") ?: ""
            EducationDetailScreen(
                viewModel = educationViewModel,
                onBack = { navController.popBackStack() },
                documentId = documentId
            )
        }

        composable(Screen.Insights.route) {
            InsightScreen(viewModel = insightViewModel)
        }
    }
}
