package com.example.hemaremobile.ui.telas

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.hemaremobile.data.*
import com.example.hemaremobile.domain.*
import com.example.hemaremobile.ui.Rotas
import com.example.hemaremobile.ui.abrirNoMapa
import com.example.hemaremobile.ui.abrirUrl
import com.example.hemaremobile.ui.componentes.*
import com.example.hemaremobile.ui.compartilharTexto
import com.example.hemaremobile.ui.compartilharWhatsApp
import com.example.hemaremobile.ui.ligar
import com.example.hemaremobile.ui.ultimaLocalizacao
import com.example.hemaremobile.ui.theme.HemareVerde
import kotlinx.coroutines.launch
import java.time.LocalDate

// ---------------------------------------------------------------------------------------------
// Onde doar
// ---------------------------------------------------------------------------------------------

@Composable
fun LocaisScreen(estado: Estado) {
    val contexto = LocalContext.current
    var busca by remember { mutableStateOf("") }
    var posicao by remember { mutableStateOf<Pair<Double, Double>?>(null) }
    var aviso by remember { mutableStateOf("") }

    val pedirPermissao = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { resultado ->
        if (resultado.values.any { it }) {
            val loc = ultimaLocalizacao(contexto)
            if (loc != null) { posicao = loc.latitude to loc.longitude; aviso = "" }
            else aviso = "Não consegui obter sua localização agora. Ligue o GPS e tente de novo."
        } else {
            aviso = "Não foi possível obter sua localização (permissão negada)."
        }
    }

    val termo = Textos.normalizar(busca)
    val filtrados = estado.locais.filter {
        termo.isEmpty() || Textos.normalizar(it.cidade).contains(termo) || Textos.normalizar(it.estado) == termo ||
            Textos.normalizar(it.nome).contains(termo) || Textos.normalizar(it.cidade + ", " + it.estado).contains(termo)
    }
    val lista = if (posicao != null) {
        filtrados.filter { it.lat != null && it.lng != null }
            .map { it to Distancia.calcular(posicao!!.first, posicao!!.second, it.lat!!, it.lng!!) }.sortedBy { it.second }
    } else filtrados.map { it to null }

    TelaLista("Onde doar") {
        bloco { Paragrafo("Encontre um hemocentro perto de você. Busque pela cidade ou use sua localização.", suave = true) }
        bloco {
            Campo(busca, { busca = it }, "Cidade ou estado", placeholder = "Ex.: Recife, PE")
            Espaco(8)
            BotaoVazado("📍 Usar minha localização", {
                aviso = "Obtendo sua localização..."
                pedirPermissao.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
            })
            Mensagem(aviso)
        }
        if (lista.isEmpty()) bloco { Vazio("Nenhum local encontrado para essa busca.") }
        items(lista) { (local, distancia) ->
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                Cartao {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(local.nome, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.fillMaxWidth(0.78f))
                        Selo(local.estado, MaterialTheme.colorScheme.primary)
                    }
                    Espaco(4)
                    Paragrafo("📍 ${local.cidade}", suave = true)
                    if (!local.endereco.isNullOrBlank()) Dica(local.endereco)
                    if (!local.telefone.isNullOrBlank()) {
                        BotaoTexto("📞 ${local.telefone}", { ligar(contexto, local.telefone) })
                    }
                    if (distancia != null) Paragrafo("🚗 a aproximadamente $distancia km de você")
                    if (local.lat != null && local.lng != null) {
                        Espaco(4)
                        BotaoVazado("🗺️ Abrir no mapa", { abrirNoMapa(contexto, local.nome, local.lat, local.lng) })
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Triagem "Posso doar?"
// ---------------------------------------------------------------------------------------------

@Composable
fun TriagemScreen() {
    var respostas by remember { mutableStateOf(mapOf<String, Boolean>()) }
    var idadeTexto by remember { mutableStateOf("") }
    var pesoTexto by remember { mutableStateOf("") }
    var resultado by remember { mutableStateOf<ResultadoTriagem?>(null) }
    var erro by remember { mutableStateOf("") }

    val idade = idadeTexto.toIntOrNull()
    val peso = pesoTexto.toIntOrNull()

    @Composable
    fun Grupo(titulo: String, perguntas: List<com.example.hemaremobile.domain.PerguntaSimNao>) {
        Cartao {
            Titulo(titulo)
            perguntas.forEach { p ->
                com.example.hemaremobile.ui.componentes.PerguntaSimNao(p.texto, respostas[p.campo]) { respostas = respostas + (p.campo to it) }
            }
        }
    }

    TelaLista("Posso doar?") {
        bloco { Paragrafo("Triagem — será que você pode doar hoje? Você recebe uma orientação, não um diagnóstico.", suave = true) }
        bloco {
            Cartao {
                Titulo("Sobre você")
                Espaco(8)
                Campo(idadeTexto, { v -> idadeTexto = v.filter { it.isDigit() }.take(3).let { t -> if ((t.toIntOrNull() ?: 0) > 120) "120" else t } },
                    "Sua idade (anos)", teclado = KeyboardType.Number, aviso = Triagem.avisoIdade(idade))
                Espaco(4)
                Campo(pesoTexto, { v -> pesoTexto = v.filter { it.isDigit() }.take(3).let { t -> if ((t.toIntOrNull() ?: 0) > 300) "300" else t } },
                    "Seu peso (kg)", teclado = KeyboardType.Number, aviso = Triagem.avisoPeso(peso))
            }
        }
        bloco { Grupo("Situações recentes", Triagem.PERGUNTAS_RECENTES) }
        bloco { Grupo("Saúde", Triagem.PERGUNTAS_SAUDE) }
        bloco { Grupo("Condições a confirmar", Triagem.PERGUNTAS_ATENCAO) }
        bloco {
            Botao("Ver resultado", {
                if (idade == null || idade == 0 || peso == null || peso == 0) {
                    resultado = null
                    erro = "⚠️ Preencha pelo menos sua idade e seu peso para ver o resultado."
                } else {
                    erro = ""
                    resultado = Triagem.avaliar(idade, peso, respostas)
                }
            })
            Mensagem(erro)
        }
        resultado?.let { r ->
            val cor = when (r.nivel) { NivelTriagem.VERDE -> HemareVerde; NivelTriagem.AMARELO -> CorAviso; NivelTriagem.VERMELHO -> CorAlerta }
            bloco {
                Cartao {
                    Selo(when (r.nivel) { NivelTriagem.VERDE -> "🟢 Verde"; NivelTriagem.AMARELO -> "🟡 Amarelo"; NivelTriagem.VERMELHO -> "🔴 Vermelho" }, cor)
                    Espaco(8)
                    Text(r.titulo, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = cor)
                    r.motivos.forEach { Espaco(6); Paragrafo("• $it") }
                    Espaco(10)
                    Dica("⚠️ Esta é uma orientação informativa, não substitui a triagem clínica. A avaliação final é feita por um profissional no dia da doação.")
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Notícias (feed estilo Google Notícias)
// ---------------------------------------------------------------------------------------------

@Composable
private fun CartaoNoticia(n: Noticia, agora: Long, ehAdmin: Boolean, lista: Boolean, aoEsconder: () -> Unit) {
    val contexto = LocalContext.current
    Cartao(aoClicar = { abrirUrl(contexto, n.url) }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(28.dp).clip(CircleShape).background(Color.hsv(Noticias.matizDaFonte(n.fonte), 0.55f, 0.42f)),
                contentAlignment = Alignment.Center
            ) { Text(Noticias.iniciais(n.fonte), color = Color.White, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold) }
            Text(n.fonte, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 8.dp))
        }
        Espaco(8)
        Text(n.titulo, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        if (lista && n.resumo.isNotBlank()) { Espaco(4); Paragrafo(n.resumo, suave = true) }
        Espaco(8)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Dica(Datas.haQuanto(n.publicadaEm, agora))
            Selo("${Noticias.ICONES[n.categoria]} ${Noticias.CATEGORIAS.firstOrNull { it.first == n.categoria }?.second ?: n.categoria}", MaterialTheme.colorScheme.primary)
            if (n.uf != null) Selo(n.uf, MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (ehAdmin) { Espaco(4); BotaoTexto("Esconder esta notícia", aoEsconder) }
    }
}

@Composable
fun NoticiasScreen(store: HemareStore) {
    val estado by store.estado.collectAsState()
    val escopo = rememberCoroutineScope()
    var categoria by remember { mutableStateOf<String?>(null) }
    var uf by remember { mutableStateOf("") }
    var busca by remember { mutableStateOf("") }
    var visiveis by remember { mutableStateOf(Noticias.POR_PAGINA) }
    var carregando by remember { mutableStateOf(false) }
    var aviso by remember { mutableStateOf("") }

    suspend fun atualizar() {
        carregando = true
        val r = store.coletarNoticias()
        aviso = if (r.ok) "" else r.texto
        carregando = false
    }

    LaunchedEffect(Unit) { if (estado.coleta == null) atualizar() }

    val todas = estado.noticiasVisiveis()
    val filtradas = Noticias.filtrar(todas, categoria, uf.ifEmpty { null }, busca)
    val filtrando = categoria != null || uf.isNotEmpty() || busca.isNotBlank()
    val agora = System.currentTimeMillis()
    val ehAdmin = estado.papel == TipoConta.ADMIN
    val contagem = Noticias.CATEGORIAS.associate { c -> c.first to todas.count { it.categoria == c.first } }

    TelaLista("Notícias") {
        bloco { Paragrafo("Doação de sangue, campanhas, estoques e saúde, de fontes públicas e de hemocentros. Cada notícia abre no site de quem a publicou.", suave = true) }
        bloco {
            Campo(busca, { busca = it.take(60); visiveis = Noticias.POR_PAGINA }, "🔎 Buscar nas notícias")
            Espaco(4)
            CampoSelecao("Estado", uf, listOf("" to "Brasil todo") + Estados.UFS.map { it.first to it.second }, { uf = it; visiveis = Noticias.POR_PAGINA })
        }
        item { SubAbasNoticias(categoria, contagem, todas.size) { categoria = it; visiveis = Noticias.POR_PAGINA } }
        bloco {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Dica(
                    (estado.coleta?.let { "Atualizado " + Datas.haQuanto(it.terminouEmMs, agora) + " · " } ?: "") +
                        filtradas.size + (if (filtradas.size == 1) " notícia" else " notícias")
                )
            }
            BotaoTexto(if (carregando) "Atualizando..." else "↻ Atualizar", { if (!carregando) escopo.launch { atualizar() } })
            Mensagem(aviso)
        }
        if (filtradas.isEmpty()) {
            bloco {
                if (filtrando) {
                    Vazio("Nenhuma notícia com esses filtros.")
                    BotaoVazado("Limpar filtros", { categoria = null; uf = ""; busca = "" })
                } else Vazio("Ainda não há notícias coletadas. Volte em instantes.")
            }
        } else {
            bloco { Rotulo(if (filtrando) "RESULTADOS" else "PRINCIPAIS NOTÍCIAS") }
            items(filtradas.take(visiveis), key = { it.id }) { n ->
                Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                    CartaoNoticia(n, agora, ehAdmin, lista = filtradas.indexOf(n) >= 4) { store.ocultarNoticia(n.id) }
                }
            }
            if (filtradas.size > visiveis) bloco { BotaoVazado("Ver mais notícias", { visiveis += Noticias.POR_PAGINA }) }
        }
        bloco {
            Aviso(
                "O Hemare mostra só o título, um trecho curto e a imagem de divulgação que cada fonte oferece nos seus feeds públicos (RSS), sempre com o nome da fonte e o link da matéria original; os direitos pertencem a quem publicou. " +
                    "Uma notícia não substitui o comunicado oficial do hemocentro: antes de ir doar, confirme os horários e a necessidade com ele."
            )
        }
    }
}

@Composable
private fun SubAbasNoticias(categoria: String?, contagem: Map<String, Int>, total: Int, aoTrocar: (String?) -> Unit) {
    val abas = listOf("" to "Todas ($total)") + Noticias.CATEGORIAS.map { it.first to "${Noticias.ICONES[it.first]} ${it.second} (${contagem[it.first] ?: 0})" }
    SubAbas(abas, categoria ?: "") { aoTrocar(it.ifEmpty { null }) }
}

// ---------------------------------------------------------------------------------------------
// Apadrinhar
// ---------------------------------------------------------------------------------------------

@Composable
fun ApadrinharScreen(store: HemareStore, navegar: (String) -> Unit) {
    val estado by store.estado.collectAsState()
    var avisos by remember { mutableStateOf(mapOf<Int, String>()) }
    val casos = estado.casosPublicos()
    val doador = estado.doadorAtual
    val meus = if (doador != null) estado.afilhadosDe(doador.id).map { it.id } else emptyList()

    TelaLista("Apadrinhe um paciente") {
        bloco {
            Paragrafo(
                "Algumas pessoas precisam de sangue a cada poucas semanas, a vida toda (como quem tem anemia falciforme ou talassemia). Ao apadrinhar, você se compromete a voltar a doar por esse paciente sempre que puder, e o hospital avisa você quando chegar a hora.",
                suave = true
            )
            Espaco(8)
            Aviso("🔒 Para proteger os pacientes, aqui aparece só um apelido: nunca o nome verdadeiro. E você pode deixar de ser padrinho quando quiser.", HemareVerde)
        }
        if (casos.isEmpty()) bloco { Vazio("Nenhum paciente procurando padrinhos no momento. Volte em breve! 💛") }
        items(casos, key = { it.id }) { c ->
            val meta = Apadrinhamento.situacaoMeta(c.padrinhos.size, c.meta)
            val h = estado.hospitais[c.hospitalId]
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                Cartao {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface2(c.tipo)
                        Column(modifier = Modifier.padding(start = 12.dp)) {
                            Text(c.apelido, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            Dica("${estado.hospitalNome(c.hospitalId)} — ${h?.cidade ?: ""}${if (h != null) "/" + h.estado else ""}")
                        }
                    }
                    Espaco(8)
                    Paragrafo((if (c.condicao != "Não informada") "${c.condicao} · " else "") + "precisa de sangue a cada ${c.frequenciaDias} dias", suave = true)
                    Espaco(8)
                    BarraProgresso(meta.porcentagem)
                    Dica("${meta.padrinhos} de ${meta.meta} padrinhos" + if (meta.completo) " — meta atingida, mas sempre cabe mais um 💛" else "")
                    Espaco(8)
                    when {
                        c.id in meus -> Paragrafo("✅ Você é padrinho deste paciente. Veja em Minha área.")
                        doador != null -> BotaoVazado("💝 Quero apadrinhar", { avisos = avisos + (c.id to store.apadrinhar(c.id).texto) })
                        estado.papel != null -> Dica("Só contas de doador podem apadrinhar.")
                        else -> Dica("Entre ou crie sua conta de doador para apadrinhar.")
                    }
                    Mensagem(avisos[c.id])
                }
            }
        }
        bloco { Aviso("Apadrinhar não substitui a triagem: antes de cada doação você passa pela avaliação do hemocentro, como sempre.") }
    }
}

@Composable
private fun Surface2(tipo: String) {
    androidx.compose.material3.Surface(shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)) {
        Text(tipo, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp))
    }
}

// ---------------------------------------------------------------------------------------------
// Campanha pública: "Quem doa por mim"
// ---------------------------------------------------------------------------------------------

@Composable
fun CampanhaScreen(store: HemareStore, codigo: String, navegar: (String) -> Unit) {
    val estado by store.estado.collectAsState()
    val contexto = LocalContext.current
    val c = estado.campanhaPorCodigo(codigo)
    var data by remember { mutableStateOf<LocalDate?>(null) }
    var aviso by remember { mutableStateOf("") }

    if (c == null) {
        TelaLista("Campanha") {
            bloco {
                Titulo("Campanha não encontrada")
                Espaco(6)
                Paragrafo("Confira se o código está completo. Você também pode encontrar um hemocentro e doar.", suave = true)
                Espaco(8)
                BotaoVazado("Onde doar", { navegar(Rotas.LOCAIS) })
            }
        }
        return
    }

    val sit = estado.situacaoCampanha(c)
    val h = estado.hospitais[c.hospitalId]
    val hoje = Datas.hoje()
    val limite = minOf(c.expiraEm, hoje.plusDays(Campanha.JANELA_PROMESSA.toLong()))
    val dias = generateSequence(hoje) { it.plusDays(1) }.takeWhile { !it.isAfter(limite) }.toList()
    val texto = Campanha.mensagemCompartilhar(c.apelido, estado.hospitalNome(c.hospitalId), h?.cidade ?: "", sit.faltam, "Código da campanha no app Hemare: ${c.codigo}")

    TelaLista("Doe por ${c.apelido}") {
        bloco {
            Selo(Campanha.ROTULOS[sit.status] ?: sit.status, MaterialTheme.colorScheme.primary)
            Espaco(8)
            Paragrafo("O ${estado.hospitalNome(c.hospitalId)} (${h?.cidade ?: ""}${if (h != null) "/" + h.estado else ""}) pediu reposição de sangue para este paciente. Cada doação confirmada pelo hospital conta na meta abaixo.", suave = true)
        }
        bloco {
            Cartao {
                BarraProgresso(sit.porcentagem)
                Espaco(10)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    Numero("${sit.confirmadas}", "de ${sit.metaBolsas} doações confirmadas")
                    Numero("${sit.prometidas}", if (sit.prometidas == 1) "pessoa já prometeu" else "pessoas já prometeram")
                    Numero("${sit.diasRestantes}", if (sit.diasRestantes == 1) "dia restante" else "dias restantes")
                }
            }
        }
        if (sit.aceitaPromessas) {
            bloco {
                Cartao {
                    Titulo("Quero doar por ${c.apelido}")
                    Espaco(6)
                    if (estado.doadorAtual != null) {
                        Paragrafo("Em que dia você vai doar?", suave = true)
                        Espaco(6)
                        ChipsLinha { dias.forEach { d -> Chip(Datas.formatar(d), data == d) { data = d } } }
                        Espaco(10)
                        Botao("🤝 Prometer doar", { aviso = store.prometer(c.codigo, data).texto })
                    } else if (estado.papel != null) {
                        Paragrafo("Só contas de doador podem prometer doação. Você pode compartilhar esta campanha abaixo.", suave = true)
                    } else {
                        Paragrafo("Entre ou crie sua conta de doador (é rápido) para prometer sua doação. Depois, volte a esta campanha.", suave = true)
                        Espaco(8)
                        Botao("Entrar", { navegar(Rotas.login(Rotas.campanha(c.codigo))) })
                        Espaco(6)
                        BotaoVazado("Criar conta", { navegar(Rotas.cadastro(Rotas.campanha(c.codigo))) })
                    }
                    Mensagem(aviso)
                    Espaco(8)
                    Dica("🔒 O hospital vê seu nome e telefone só para confirmar a sua doação. Ninguém mais vê. Antes de doar, faça a triagem.")
                }
            }
        } else {
            bloco { Vazio("Esta campanha não está mais recebendo promessas. Você ainda pode doar em um hemocentro: todo sangue salva vidas.") }
        }
        bloco {
            Titulo("Compartilhe com quem pode ajudar")
            Espaco(8)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip("📤 Compartilhar", false) { compartilharTexto(contexto, texto) }
                Chip("💬 WhatsApp", false) { compartilharWhatsApp(contexto, texto) }
            }
        }
        bloco { Aviso("Para proteger o paciente, aqui aparece só um apelido. Prazo até ${Datas.dmy(c.expiraEm)}.") }
    }
}
