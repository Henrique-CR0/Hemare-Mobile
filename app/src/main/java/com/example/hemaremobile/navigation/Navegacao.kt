package com.example.hemaremobile.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import com.example.hemaremobile.AutenticacaoViewModel
import com.example.hemaremobile.CadastroDoadorScreen
import com.example.hemaremobile.CadastroHospitalScreen
import com.example.hemaremobile.ConfiguracaoScreen
import com.example.hemaremobile.ConfiguracaoViewModel
import com.example.hemaremobile.GuiaScreen
import com.example.hemaremobile.InicioScreen
import com.example.hemaremobile.ListaScreen
import com.example.hemaremobile.LoginScreen
import com.example.hemaremobile.MitosScreen
import com.example.hemaremobile.OndeDoarScreen
import com.example.hemaremobile.PainelHospitalScreen
import com.example.hemaremobile.PossoDoarScreen
import com.example.hemaremobile.TipoConta

/** As 3 abas principais do app do doador (bottom navigation). */
sealed class Aba(val rota: String, val rotulo: String, val icone: ImageVector) {
    data object Inicio : Aba("inicio", "Início", Icons.Filled.Home)
    data object Lista : Aba("lista", "Lista", Icons.Filled.List)
    data object Configuracao : Aba("configuracao", "Configuração", Icons.Filled.Settings)
}

val abasPrincipais = listOf(Aba.Inicio, Aba.Lista, Aba.Configuracao)

/** Sub-rotas de conteúdo dentro da aba Lista. */
object RotasLista {
    const val HUB = "lista/hub"
    const val POSSO_DOAR = "lista/posso-doar"
    const val ONDE_DOAR = "lista/onde-doar"
    const val GUIA = "lista/guia"
    const val MITOS = "lista/mitos"
}

/** Rotas da tela de login/cadastro, mostradas antes de entrar na área do doador ou do hospital. */
private object RotasAutenticacao {
    const val LOGIN = "auth/login"
    const val CADASTRO_DOADOR = "auth/cadastro-doador"
    const val CADASTRO_HOSPITAL = "auth/cadastro-hospital"
}

/**
 * Raiz do app: decide entre a tela de login/cadastro, a área do doador (3 abas)
 * ou a área do hospital (2 abas), de acordo com a conta logada na sessão.
 */
@Composable
fun HemareRaiz(
    configuracaoViewModel: ConfiguracaoViewModel,
    autenticacaoViewModel: AutenticacaoViewModel
) {
    val autState by autenticacaoViewModel.uiState.collectAsState()
    val conta = autState.contaLogada

    when (conta?.tipo) {
        null -> AutenticacaoApp(viewModel = autenticacaoViewModel)
        TipoConta.DOADOR -> HemareApp(
            configuracaoViewModel = configuracaoViewModel,
            nomeDoador = conta.nome,
            emailDoador = conta.email,
            onSair = autenticacaoViewModel::sair
        )
        TipoConta.HOSPITAL -> HospitalApp(
            configuracaoViewModel = configuracaoViewModel,
            nomeHospital = conta.nome,
            emailHospital = conta.email,
            onSair = autenticacaoViewModel::sair
        )
    }
}

@Composable
private fun AutenticacaoApp(viewModel: AutenticacaoViewModel) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = RotasAutenticacao.LOGIN) {
        composable(RotasAutenticacao.LOGIN) {
            LoginScreen(
                viewModel = viewModel,
                onIrParaCadastroDoador = { navController.navigate(RotasAutenticacao.CADASTRO_DOADOR) },
                onIrParaCadastroHospital = { navController.navigate(RotasAutenticacao.CADASTRO_HOSPITAL) }
            )
        }
        composable(RotasAutenticacao.CADASTRO_DOADOR) {
            CadastroDoadorScreen(
                viewModel = viewModel,
                onVoltarParaLogin = { navController.popBackStack() }
            )
        }
        composable(RotasAutenticacao.CADASTRO_HOSPITAL) {
            CadastroHospitalScreen(
                viewModel = viewModel,
                onVoltarParaLogin = { navController.popBackStack() }
            )
        }
    }
}

@Composable
fun HemareApp(
    configuracaoViewModel: ConfiguracaoViewModel,
    nomeDoador: String,
    emailDoador: String,
    onSair: () -> Unit
) {
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
                composable(RotasLista.POSSO_DOAR) { PossoDoarScreen() }
                composable(RotasLista.ONDE_DOAR) { OndeDoarScreen() }
                composable(RotasLista.GUIA) { GuiaScreen() }
                composable(RotasLista.MITOS) { MitosScreen() }
            }

            composable(Aba.Configuracao.rota) {
                ConfiguracaoScreen(
                    viewModel = configuracaoViewModel,
                    nomeConta = nomeDoador,
                    emailConta = emailDoador,
                    onSair = onSair
                )
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

/** As 2 abas do app do hospital (sem Início/Lista do doador). */
private sealed class AbaHospital(val rota: String, val rotulo: String, val icone: ImageVector) {
    data object Painel : AbaHospital("hospital/painel", "Hospital", Icons.Filled.LocalHospital)
    data object Configuracao : AbaHospital("hospital/configuracao", "Configuração", Icons.Filled.Settings)
}

private val abasHospital = listOf(AbaHospital.Painel, AbaHospital.Configuracao)

@Composable
private fun HospitalApp(
    configuracaoViewModel: ConfiguracaoViewModel,
    nomeHospital: String,
    emailHospital: String,
    onSair: () -> Unit
) {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = { HospitalBottomBar(navController) }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = AbaHospital.Painel.rota,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(AbaHospital.Painel.rota) {
                PainelHospitalScreen(nomeHospital = nomeHospital, onSair = onSair)
            }
            composable(AbaHospital.Configuracao.rota) {
                ConfiguracaoScreen(
                    viewModel = configuracaoViewModel,
                    nomeConta = nomeHospital,
                    emailConta = emailHospital,
                    onSair = onSair
                )
            }
        }
    }
}

@Composable
private fun HospitalBottomBar(navController: NavHostController) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val destinoAtual = backStackEntry?.destination

    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
        abasHospital.forEach { aba ->
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
