package com.example.hemaremobile.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.hemaremobile.BotaoLibrasFlutuante
import com.example.hemaremobile.ConfiguracaoScreen
import com.example.hemaremobile.ConfiguracaoViewModel
import com.example.hemaremobile.LocalVoltar
import com.example.hemaremobile.data.Estado
import com.example.hemaremobile.data.HemareStore
import com.example.hemaremobile.data.TipoConta
import com.example.hemaremobile.ui.DadosPortao
import com.example.hemaremobile.ui.Portoes
import com.example.hemaremobile.ui.Rotas
import com.example.hemaremobile.ui.telas.*

/** Uma aba da barra de baixo. `grupo` são as rotas que contam como "dentro" dessa aba. */
private data class Aba(val rota: String, val rotulo: String, val icone: ImageVector, val grupo: Set<String> = setOf(rota))

private val ROTAS_CONTA_VISITANTE = setOf(Rotas.CONTA, Rotas.LOGIN, Rotas.CADASTRO, Rotas.CADASTRO_HOSPITAL, Rotas.RECUPERAR_SENHA)

private val ABA_INICIO = Aba(Rotas.INICIO, "Início", Icons.Filled.Home)
private val ABA_LISTA = Aba(Rotas.LISTA, "Lista", Icons.AutoMirrored.Filled.List)
private val ABA_CONFIG = Aba(Rotas.CONFIGURACAO, "Configuração", Icons.Filled.Settings)

private fun abasDe(papel: TipoConta?): List<Aba> = when (papel) {
    null -> listOf(ABA_INICIO, ABA_LISTA, Aba(Rotas.CONTA, "Entrar", Icons.AutoMirrored.Filled.Login, ROTAS_CONTA_VISITANTE), ABA_CONFIG)
    TipoConta.DOADOR -> listOf(
        ABA_INICIO, ABA_LISTA,
        Aba(Rotas.AREA_DOADOR, "Minha área", Icons.Filled.Person, setOf(Rotas.AREA_DOADOR, Rotas.MEU_PERFIL, Rotas.MINHAS_DOACOES, Rotas.COMPLETAR_PERFIL)),
        ABA_CONFIG
    )
    TipoConta.HOSPITAL -> listOf(
        Aba(Rotas.PAINEL_HOSPITAL, "Minha área", Icons.Filled.LocalHospital), Aba(Rotas.PERFIL_HOSPITAL, "Perfil", Icons.Filled.Business), ABA_LISTA, ABA_CONFIG
    )
    TipoConta.ADMIN -> listOf(
        Aba(Rotas.ADMIN, "Admin", Icons.Filled.AdminPanelSettings), Aba(Rotas.ADMIN_STATUS, "Status", Icons.Filled.BarChart), ABA_LISTA, ABA_CONFIG
    )
}

private fun base(rota: String?): String = rota?.substringBefore("?")?.substringBefore("/{")?.substringBefore("/") ?: ""

/**
 * Raiz do app: cada tipo de conta (visitante, doador, hospital, admin) tem suas abas.
 * Trocar de conta recria a navegação inteira (`key(papel)`), como o site faz ao entrar e sair.
 */
@Composable
fun HemareRaiz(configuracaoViewModel: ConfiguracaoViewModel, store: HemareStore) {
    val estado by store.estado.collectAsState()
    val configuracao by configuracaoViewModel.uiState.collectAsState()
    var destinoPendente by remember { mutableStateOf<String?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        key(estado.papel) {
            AppDoPapel(
                papel = estado.papel, store = store, configuracaoViewModel = configuracaoViewModel,
                destinoPendente = destinoPendente, consumirDestino = { destinoPendente = null }, definirDestino = { destinoPendente = it }
            )
        }
        if (configuracao.librasAtivo) {
            BotaoLibrasFlutuante(modifier = Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = 88.dp))
        }
    }
}

@Composable
private fun AppDoPapel(
    papel: TipoConta?, store: HemareStore, configuracaoViewModel: ConfiguracaoViewModel,
    destinoPendente: String?, consumirDestino: () -> Unit, definirDestino: (String?) -> Unit
) {
    val estado by store.estado.collectAsState()
    val nav = rememberNavController()
    val abas = remember(papel) { abasDe(papel) }
    val rotasDeAba = remember(papel) { abas.flatMap { it.grupo }.toSet() }

    /** Abas trocam a pilha inteira; as outras telas empilham por cima. */
    fun ir(rota: String) {
        if (base(rota) in rotasDeAba) {
            nav.navigate(rota) { popUpTo(Rotas.inicialDe(papel)) { inclusive = false }; launchSingleTop = true }
        } else {
            nav.navigate(rota) { launchSingleTop = true }
        }
    }

    LaunchedEffect(Unit) {
        val alvo = destinoPendente
        if (alvo != null) { consumirDestino(); ir(alvo) }
    }

    /** Depois de entrar ou cadastrar: o destino de retorno (se for do app) vale na conta nova. */
    fun entrou(destino: String?, padrao: String? = null) = definirDestino(Rotas.destinoSeguro(destino, padrao))

    // Botão Voltar no cabeçalho de toda tela que não é a raiz de uma aba e foi aberta por cima de outra.
    val entradaAtual by nav.currentBackStackEntryAsState()
    val raizesDeAba = remember(papel) { abas.map { it.rota }.toSet() }
    val voltar: (() -> Unit)? =
        if (base(entradaAtual?.destination?.route) !in raizesDeAba && nav.previousBackStackEntry != null) ({ nav.popBackStack() }) else null

    Scaffold(bottomBar = { BarraDeAbas(nav, abas) }) { espaco ->
      CompositionLocalProvider(LocalVoltar provides voltar) {
        NavHost(nav, startDestination = Rotas.inicialDe(papel), modifier = Modifier.padding(espaco)) {
            // ---- públicas ----
            composable(Rotas.INICIO) { InicioScreen(estado, ::ir) }
            composable(Rotas.LISTA) { ListaScreen(estado, ::ir) }
            composable(Rotas.CONFIGURACAO) {
                ConfiguracaoScreen(configuracaoViewModel, store, estado, onSair = { store.sair() }, onEntrar = { ir(Rotas.CONTA) })
            }
            composable(Rotas.LOCAIS) { LocaisScreen(estado) }
            composable(Rotas.ESTOQUE) { EstoqueScreen(estado, ::ir) }
            composable(Rotas.NOTICIAS) { NoticiasScreen(store) }
            composable(Rotas.CLIMA) { ClimaScreen(estado, ::ir) }
            composable(Rotas.ORIENTACOES) { OrientacoesScreen() }
            composable(Rotas.MITOS) { MitosScreen() }
            composable(Rotas.DIREITOS) { DireitosScreen() }
            composable(Rotas.PLACAR) { PlacarScreen(estado, ::ir) }
            composable(Rotas.CAMPANHA_COM_CODIGO, listOf(navArgument("codigo") { type = NavType.StringType })) {
                CampanhaScreen(store, it.arguments?.getString("codigo") ?: "", ::ir)
            }

            // ---- só doador (visitante vê o portão) ----
            composable(Rotas.TRIAGEM) { Protegido(estado, nav, Rotas.TRIAGEM, setOf(TipoConta.DOADOR), Portoes.TRIAGEM) { TriagemScreen() } }
            composable(Rotas.APADRINHAR) { Protegido(estado, nav, Rotas.APADRINHAR, setOf(TipoConta.DOADOR), Portoes.APADRINHAR) { ApadrinharScreen(store, ::ir) } }
            composable(Rotas.AGENDAR) { Protegido(estado, nav, Rotas.AGENDAR, setOf(TipoConta.DOADOR), Portoes.GENERICO) { AgendarScreen(store, ::ir) } }
            composable(Rotas.AREA_DOADOR) { Protegido(estado, nav, Rotas.AREA_DOADOR, setOf(TipoConta.DOADOR), Portoes.GENERICO) { AreaDoadorScreen(store, ::ir) } }
            composable(Rotas.MEU_PERFIL) { Protegido(estado, nav, Rotas.MEU_PERFIL, setOf(TipoConta.DOADOR), Portoes.GENERICO) { MeuPerfilScreen(store, ::ir) } }
            composable(Rotas.MINHAS_DOACOES) { Protegido(estado, nav, Rotas.MINHAS_DOACOES, setOf(TipoConta.DOADOR), Portoes.GENERICO) { MinhasDoacoesScreen(store, ::ir) } }
            composable(Rotas.COMPLETAR_PERFIL) {
                Protegido(estado, nav, Rotas.COMPLETAR_PERFIL, setOf(TipoConta.DOADOR), Portoes.GENERICO) {
                    CompletarPerfilScreen(store, aoConcluir = { ir(Rotas.AREA_DOADOR) })
                }
            }

            // ---- hospital e admin ----
            composable(Rotas.PAINEL_HOSPITAL) { Protegido(estado, nav, Rotas.PAINEL_HOSPITAL, setOf(TipoConta.HOSPITAL), Portoes.GENERICO) { PainelHospitalScreen(store, ::ir) } }
            composable(Rotas.PERFIL_HOSPITAL) { Protegido(estado, nav, Rotas.PERFIL_HOSPITAL, setOf(TipoConta.HOSPITAL), Portoes.GENERICO) { PerfilHospitalScreen(store, ::ir) } }
            composable(Rotas.RADAR) { Protegido(estado, nav, Rotas.RADAR, setOf(TipoConta.HOSPITAL, TipoConta.ADMIN), Portoes.GENERICO) { RadarScreen(store) } }
            composable(Rotas.ADMIN) { Protegido(estado, nav, Rotas.ADMIN, setOf(TipoConta.ADMIN), Portoes.GENERICO) { AdminScreen(store, ::ir) } }
            composable(Rotas.ADMIN_STATUS) { Protegido(estado, nav, Rotas.ADMIN_STATUS, setOf(TipoConta.ADMIN), Portoes.GENERICO) { AdminStatusScreen(store, ::ir) } }

            // ---- entrada (visitante) ----
            composable(Rotas.CONTA) {
                ContaVisitanteScreen(
                    aoEntrar = { ir(Rotas.login()) }, aoCadastro = { ir(Rotas.cadastro()) }, aoCadastroHospital = { ir(Rotas.CADASTRO_HOSPITAL) }
                )
            }
            composable(Rotas.LOGIN_COM_DESTINO, listOf(navArgument("destino") { type = NavType.StringType; nullable = true; defaultValue = null })) {
                val destino = it.arguments?.getString("destino")
                LoginScreen(
                    store, destino, aoEntrou = { d -> entrou(d) },
                    aoCadastro = { ir(Rotas.cadastro(destino)) }, aoCadastroHospital = { ir(Rotas.CADASTRO_HOSPITAL) }, aoRecuperar = { ir(Rotas.RECUPERAR_SENHA) }
                )
            }
            composable(
                Rotas.CADASTRO_COM_DESTINO,
                listOf(
                    navArgument("destino") { type = NavType.StringType; nullable = true; defaultValue = null },
                    navArgument("ref") { type = NavType.StringType; nullable = true; defaultValue = null }
                )
            ) {
                val destino = it.arguments?.getString("destino")
                CadastroScreen(
                    store, destino, it.arguments?.getString("ref"),
                    aoCriada = { entrou(destino, Rotas.COMPLETAR_PERFIL) }, aoLogin = { ir(Rotas.login(destino)) }
                )
            }
            composable(Rotas.CADASTRO_HOSPITAL) {
                CadastroHospitalScreen(store, aoCadastrado = { definirDestino(null) }, aoLogin = { ir(Rotas.login()) })
            }
            composable(Rotas.RECUPERAR_SENHA) { RecuperarSenhaScreen(store, aoVoltarLogin = { ir(Rotas.login()) }) }
        }
      }
    }
}

/**
 * Guarda de rota: visitante vê o portão de acesso (e volta para cá depois de entrar);
 * quem está logado com outro tipo de conta vai para a própria área.
 */
@Composable
private fun Protegido(
    estado: Estado, nav: NavHostController, rota: String, permitidos: Set<TipoConta>, portao: DadosPortao, conteudo: @Composable () -> Unit
) {
    val papel = estado.papel
    when {
        papel != null && papel in permitidos -> conteudo()
        papel == null -> PortaoDeAcesso(
            p = portao,
            aoCriarConta = { nav.navigate(Rotas.cadastro(rota)) },
            aoEntrar = { nav.navigate(Rotas.login(rota)) },
            aoCadastroHospital = { nav.navigate(Rotas.CADASTRO_HOSPITAL) },
            aoAbrir = { nav.navigate(it) }
        )
        else -> LaunchedEffect(papel) {
            nav.navigate(Rotas.inicialDe(papel)) { popUpTo(Rotas.inicialDe(papel)) { inclusive = false }; launchSingleTop = true }
        }
    }
}

@Composable
private fun BarraDeAbas(nav: NavHostController, abas: List<Aba>) {
    val entrada by nav.currentBackStackEntryAsState()
    val atual = base(entrada?.destination?.route)
    // "Lista" é a aba das telas que não pertencem a nenhuma outra aba
    val deOutras = abas.filter { it.rota != Rotas.LISTA }.flatMap { it.grupo }.toSet()

    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
        abas.forEach { aba ->
            val selecionada = if (aba.rota == Rotas.LISTA) atual == Rotas.LISTA || atual !in deOutras else atual in aba.grupo
            NavigationBarItem(
                selected = selecionada,
                onClick = {
                    nav.navigate(aba.rota) {
                        popUpTo(abas.first().rota) { inclusive = false }
                        launchSingleTop = true
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
