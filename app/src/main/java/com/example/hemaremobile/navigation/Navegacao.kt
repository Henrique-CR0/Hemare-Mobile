package com.example.hemaremobile.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import com.example.hemaremobile.ConfiguracaoScreen
import com.example.hemaremobile.ConfiguracaoViewModel
import com.example.hemaremobile.GuiaScreen
import com.example.hemaremobile.InicioScreen
import com.example.hemaremobile.ListaScreen
import com.example.hemaremobile.MitosScreen
import com.example.hemaremobile.OndeDoarScreen

/** As 3 abas principais do app (bottom navigation). */
sealed class Aba(val rota: String, val rotulo: String, val icone: ImageVector) {
    data object Inicio : Aba("inicio", "Início", Icons.Filled.Home)
    data object Lista : Aba("lista", "Lista", Icons.Filled.List)
    data object Configuracao : Aba("configuracao", "Configuração", Icons.Filled.Settings)
}

val abasPrincipais = listOf(Aba.Inicio, Aba.Lista, Aba.Configuracao)

/** Sub-rotas de conteúdo dentro da aba Lista. */
object RotasLista {
    const val HUB = "lista/hub"
    const val ONDE_DOAR = "lista/onde-doar"
    const val GUIA = "lista/guia"
    const val MITOS = "lista/mitos"
}

@Composable
fun HemareApp(configuracaoViewModel: ConfiguracaoViewModel) {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = { HemareBottomBar(navController) }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Aba.Inicio.rota,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Aba.Inicio.rota) {
                InicioScreen()
            }

            navigation(startDestination = RotasLista.HUB, route = Aba.Lista.rota) {
                composable(RotasLista.HUB) {
                    ListaScreen(onItemClick = { rota -> navController.navigate(rota) })
                }
                composable(RotasLista.ONDE_DOAR) { OndeDoarScreen() }
                composable(RotasLista.GUIA) { GuiaScreen() }
                composable(RotasLista.MITOS) { MitosScreen() }
            }

            composable(Aba.Configuracao.rota) {
                ConfiguracaoScreen(viewModel = configuracaoViewModel)
            }
        }
    }
}

@Composable
private fun HemareBottomBar(navController: NavHostController) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val destinoAtual = backStackEntry?.destination

    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
        abasPrincipais.forEach { aba ->
            val selecionado = destinoAtual?.hierarchy?.any { it.route == aba.rota } == true
            NavigationBarItem(
                selected = selecionado,
                onClick = {
                    navController.navigate(aba.rota) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = { Icon(aba.icone, contentDescription = aba.rotulo) },
                label = { Text(aba.rotulo) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    indicatorColor = Color.Transparent
                )
            )
        }
    }
}
