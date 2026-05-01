package com.fyp.ekopantri

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import androidx.work.*
import com.fyp.ekopantri.ui.auth.AuthViewModel
import com.fyp.ekopantri.ui.auth.LoginScreen
import com.fyp.ekopantri.ui.auth.RegisterScreen
import com.fyp.ekopantri.ui.education.EducationDetailScreen
import com.fyp.ekopantri.ui.education.EducationScreen
import com.fyp.ekopantri.ui.education.EducationViewModel
import com.fyp.ekopantri.ui.insight.InsightScreen
import com.fyp.ekopantri.ui.inventory.*
import com.fyp.ekopantri.ui.recipe.*
import com.fyp.ekopantri.ui.theme.EkoPantriTheme
import com.fyp.ekopantri.worker.ExpiryCheckWorker
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import java.util.concurrent.TimeUnit

sealed class Screen(val route: String, val title: String, val iconRes: Int = 0) {
    object Login : Screen("login", "Login")
    object Register : Screen("register", "Register")
    object Inventory : Screen("inventory", "Inventory", R.drawable.inventory)
    object Recipes : Screen("recipes", "Recipes", R.drawable.recipe)
    object Education : Screen("education", "Education", R.drawable.education)
    object Insights : Screen("insights", "Insights", R.drawable.insights)
    object AddFood : Screen("add_food", "Add Food")
    object EditFood : Screen("edit_food/{foodId}", "Edit Food")
    object RecipeDetail : Screen("recipe_detail", "Recipe Details")
    object EducationDetail : Screen("education_detail", "Education Details")
}

val bottomNavItems = listOf(
    Screen.Inventory,
    Screen.Recipes,
    Screen.Education,
    Screen.Insights
)

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Request Notification Permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
        }

        // 1. Define background constraints
        val workConstraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        // 2. Build the request using the constraints above
        val expiryWorkRequest = PeriodicWorkRequestBuilder<ExpiryCheckWorker>(24, TimeUnit.HOURS)
            .setConstraints(workConstraints)
            .build()

        // 3. Enqueue the work
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "ExpiryCheck",
            ExistingPeriodicWorkPolicy.KEEP,
            expiryWorkRequest
        )

        // Add this below your PeriodicWorkRequest to trigger an immediate test
        val testWorkRequest = OneTimeWorkRequestBuilder<ExpiryCheckWorker>().build()
        WorkManager.getInstance(this).enqueue(testWorkRequest)

        setContent {
            EkoPantriTheme {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route
                val authViewModel: AuthViewModel = viewModel()

                val startDestination = if (Firebase.auth.currentUser != null) {
                    Screen.Inventory.route
                } else {
                    Screen.Login.route
                }

                val isMainScreen = bottomNavItems.any { it.route == currentRoute }

                Scaffold(
                    topBar = {
                        if (isMainScreen) {
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
                                    IconButton(
                                        onClick = {
                                            authViewModel.logout {
                                                navController.navigate(Screen.Login.route) {
                                                    popUpTo(0) { inclusive = true }
                                                }
                                            }
                                        },
                                        modifier = Modifier.padding(end = 8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                                            contentDescription = "Logout",
                                            tint = MaterialTheme.colorScheme.onPrimary
                                        )
                                    }
                                },
                                scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
                            )
                        }
                    },
                    bottomBar = {
                        if (isMainScreen) {
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
                                            navController.navigate(screen.route) {
                                                popUpTo(navController.graph.startDestinationId) {
                                                    saveState = true
                                                }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                ) { innerPadding ->
                    val topPadding = if (isMainScreen) innerPadding.calculateTopPadding() else 0.dp
                    val bottomPadding = innerPadding.calculateBottomPadding()

                    NavHost(
                        navController = navController,
                        startDestination = startDestination,
                        modifier = Modifier.padding(top = topPadding, bottom = bottomPadding)
                    ) {
                        composable(Screen.Login.route) {
                            val invViewModel: InventoryViewModel =
                                viewModel(LocalContext.current as ComponentActivity)
                            LoginScreen(
                                onLoginSuccess = {
                                    invViewModel.startListening()
                                    navController.navigate(Screen.Inventory.route) {
                                        popUpTo(Screen.Login.route) { inclusive = true }
                                    }
                                },
                                onNavigateToRegister = {
                                    navController.navigate(Screen.Register.route)
                                }
                            )
                        }

                        composable(Screen.Register.route) {
                            val invViewModel: InventoryViewModel =
                                viewModel(LocalContext.current as ComponentActivity)
                            RegisterScreen(
                                onRegisterSuccess = {
                                    invViewModel.startListening()
                                    navController.navigate(Screen.Login.route) {
                                        popUpTo(Screen.Login.route) { inclusive = true }
                                    }
                                },
                                onNavigateToLogin = {
                                    navController.popBackStack()
                                }
                            )
                        }

                        // --- MAIN APP SCREENS ---
                        composable(Screen.Inventory.route) {
                            val inventoryViewModel: InventoryViewModel = viewModel(LocalContext.current as ComponentActivity)
                            InventoryScreen(
                                onNavigateToAddFood = { navController.navigate(Screen.AddFood.route) },
                                onNavigateToEditFood = { id -> navController.navigate("edit_food/$id") },
                                onNavigateToReview = { navController.navigate("scan_review") },
                                viewModel = inventoryViewModel
                            )
                        }

                        composable(Screen.AddFood.route) {
                            val inventoryViewModel: InventoryViewModel = viewModel(LocalContext.current as ComponentActivity)
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
                            val inventoryViewModel: InventoryViewModel = viewModel(LocalContext.current as ComponentActivity)
                            AddFoodScreen(
                                foodId = foodId,
                                onNavigateBack = { navController.popBackStack() },
                                viewModel = inventoryViewModel
                            )
                        }

                        composable("scan_review") {
                            val viewModel: InventoryViewModel =
                                viewModel(LocalContext.current as ComponentActivity)
                            val scannedItems by viewModel.scannedItems.collectAsState()

                            ScanReviewScreen(
                                scannedItems = scannedItems,
                                onSaveAll = { items ->
                                    viewModel.batchAddItems(items)
                                    navController.popBackStack()
                                },
                                onCancel = { navController.popBackStack() }
                            )
                        }

                        composable(Screen.Recipes.route) {
                            val context = LocalContext.current
                            val inventoryViewModel: InventoryViewModel = viewModel(context as ComponentActivity)
                            val recipeViewModel: RecipeViewModel = viewModel(context as ComponentActivity)

                            RecipeScreen(
                                navController = navController,
                                inventoryViewModel = inventoryViewModel,
                                recipeViewModel = recipeViewModel
                            )
                        }

                        composable(Screen.RecipeDetail.route) {
                            val context = LocalContext.current
                            val recipeViewModel: RecipeViewModel = viewModel(context as ComponentActivity)

                            RecipeDetailScreen(
                                viewModel = recipeViewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable(Screen.Education.route) {
                            val invViewModel: InventoryViewModel = viewModel(LocalContext.current as ComponentActivity)
                            val pantryItems by invViewModel.foodList.collectAsState()
                            val currentCategories = pantryItems.map { it.category }.distinct()
                            val eduViewModel: EducationViewModel = viewModel(factory = EducationViewModel.Factory)

                            EducationScreen(
                                navController = navController,
                                inventoryCategories = currentCategories,
                                viewModel = eduViewModel
                            )
                        }

                        composable(route = "article_detail/{documentId}") { backStackEntry ->
                            val documentId = backStackEntry.arguments?.getString("documentId") ?: ""

                            val eduViewModel: EducationViewModel = viewModel(factory = EducationViewModel.Factory)

                            EducationDetailScreen(
                                viewModel = eduViewModel,
                                navController = navController,
                                documentId = documentId
                            )
                        }


                        composable(Screen.Insights.route) {
                            val viewModel: InventoryViewModel =
                                viewModel(LocalContext.current as ComponentActivity)
                            InsightScreen(
                                viewModel = viewModel
                            )
                        }
                    }
                }
            }
        }
    }

}

