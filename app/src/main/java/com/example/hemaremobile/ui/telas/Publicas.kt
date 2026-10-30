package com.example.hemaremobile.ui.telas

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hemaremobile.data.Estado
import com.example.hemaremobile.data.TipoConta
import com.example.hemaremobile.domain.Estoque
import com.example.hemaremobile.data.climaPorCidade
import com.example.hemaremobile.data.estoquePublico
import com.example.hemaremobile.ui.Rotas
import com.example.hemaremobile.ui.SecaoMenu
import com.example.hemaremobile.ui.componentes.*
import com.example.hemaremobile.ui.menuDe
import com.example.hemaremobile.ui.theme.HemareRosaClaro
import com.example.hemaremobile.ui.theme.HemareVermelho
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.unit.IntOffset

// ---------------------------------------------------------------------------------------------
// Início (visitante e doador): o "landing" do site, com balões de tipo sanguíneo subindo
// ---------------------------------------------------------------------------------------------

private val TIPOS_BALAO = listOf("O+", "A+", "O-", "B+", "AB+", "A-", "B-", "AB-")

@Composable
private fun Baloes() {
    val transicao = rememberInfiniteTransition(label = "baloes")
    BoxWithConstraints(modifier = Modifier.fillMaxWidth().height(150.dp)) {
        val larguraPx = constraints.maxWidth
        val alturaPx = constraints.maxHeight
        TIPOS_BALAO.forEachIndexed { i, tipo ->
            val duracao = 6000 + i * 900
            val progresso by transicao.animateFloat(
                initialValue = 0f, targetValue = 1f,
                animationSpec = infiniteRepeatable(tween(duracao, easing = LinearEasing), RepeatMode.Restart), label = "balao$i"
            )
            val tamanho = 38 + (i % 3) * 8
            val x = ((i * 0.13f + 0.04f) % 0.92f) * larguraPx
            val y = (1f - ((progresso + i * 0.12f) % 1f)) * (alturaPx - 60)
            Box(
                modifier = Modifier.offset { IntOffset(x.toInt(), y.toInt()) }.size(tamanho.dp).clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) { Text(tipo, color = Color.White, fontWeight = FontWeight.Bold, fontSize = (tamanho * 0.3f).sp) }
        }
    }
}

@Composable
fun InicioScreen(estado: Estado, navegar: (String) -> Unit) {
    val logado = estado.papel == TipoConta.DOADOR
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column(modifier = Modifier.fillMaxWidth().background(HemareVermelho).statusBarsPadding().padding(24.dp)) {
                Text("Hemare", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(18.dp))
                Surface(shape = RoundedCornerShape(20.dp), color = Color.White.copy(alpha = 0.18f)) {
                    Text("❤️ Doar sangue é doar vida", color = Color.White, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
                }
                Spacer(modifier = Modifier.height(14.dp))
                Text("Sua doação pode salvar até 4 vidas", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold, lineHeight = 34.sp)
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "O Hemare conecta você a hospitais e hemocentros que precisam de sangue. Descubra se pode doar e encontre onde doar pertinho de você.",
                    color = HemareRosaClaro, fontSize = 15.sp
                )
                Spacer(modifier = Modifier.height(18.dp))
                if (!logado) {
                    Surface(onClick = { navegar(Rotas.cadastro()) }, shape = RoundedCornerShape(14.dp), color = Color.White, modifier = Modifier.fillMaxWidth()) {
                        Text("Quero doar sangue", color = HemareVermelho, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.padding(14.dp))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
                Surface(
                    onClick = { navegar(Rotas.TRIAGEM) }, shape = RoundedCornerShape(14.dp), color = Color.Transparent,
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, Color.White), modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Será que posso doar?", color = Color.White, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.padding(14.dp))
                }
                Baloes()
            }
        }
        bloco {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                Numero("4 vidas", "por doação")
                Numero("10 min", "de doação")
                Numero("1,6%", "da população doa")
            }
        }
        bloco {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Cartao { Titulo("🩸 Compatibilidade"); Espaco(4); Paragrafo("Achamos os doadores certos para cada tipo sanguíneo, na hora que mais importa.", suave = true) }
                Cartao(aoClicar = { navegar(Rotas.LOCAIS) }) { Titulo("🏥 Onde doar"); Espaco(4); Paragrafo("Encontre hemocentros e hospitais perto de você, em todo o Brasil.", suave = true) }
                Cartao { Titulo("💛 Vire um herói"); Espaco(4); Paragrafo("Acompanhe suas doações, ganhe emblemas e inspire mais gente a doar.", suave = true) }
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Lista: o menu do site, organizado por seções (e por tipo de conta)
// ---------------------------------------------------------------------------------------------

@Composable
fun ListaScreen(estado: Estado, navegar: (String) -> Unit) {
    val secoes: List<SecaoMenu> = menuDe(estado.papel)
    TelaLista("Menu") {
        secoes.forEach { secao ->
            bloco { Rotulo(secao.titulo.uppercase()) }
            secao.itens.forEach { item ->
                bloco { ItemDeLista(item.emoji, item.titulo, item.descricao, item.exigeConta) { navegar(item.rota) } }
            }
        }
        if (estado.papel == null) {
            bloco { Dica("🔒 = exige conta gratuita. Você vê o que a conta libera antes de criar uma.") }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Conteúdo educativo
// ---------------------------------------------------------------------------------------------

@Composable
private fun Bloco(titulo: String, itens: List<String>) {
    Cartao {
        Titulo(titulo)
        Espaco(6)
        itens.forEach { Paragrafo("• $it", suave = true); Espaco(4) }
    }
}

@Composable
fun OrientacoesScreen() {
    TelaLista("Guia da doação") {
        bloco { Paragrafo("Tudo o que você precisa saber para doar com tranquilidade — do preparo até os cuidados depois.", suave = true) }
        bloco { Rotulo("ANTES DE DOAR") }
        bloco {
            Bloco("🍽️ Alimentação e preparo", listOf(
                "Alimente-se bem — nunca vá em jejum. Faça uma refeição leve algumas horas antes (pão, tapioca, arroz, frutas, ovos, queijos leves).",
                "Reforce o ferro nos dias anteriores — carnes magras, feijão, lentilha, ovos e folhas verde-escuras. Combine com vitamina C (laranja, acerola).",
                "Hidrate-se — beba bastante água no dia anterior e um copo pouco antes de doar.",
                "Descanse — tenha dormido bem na última noite."
            ))
        }
        bloco { Bloco("🚫 Evite no dia", listOf("Alimentos gordurosos, frituras e fast-food (atrapalham os exames do sangue)", "Bebida alcoólica nas 12 horas anteriores", "Excesso de café (ajuda a desidratar)")) }
        bloco { Rotulo("NO DIA DA DOAÇÃO") }
        bloco {
            Bloco("⏱️ Como funciona", listOf(
                "O processo completo (cadastro, triagem, coleta e lanche) leva em média 40 minutos.",
                "A coleta em si dura só 5 a 15 minutos.", "Leve um documento oficial com foto."
            ))
        }
        bloco { Rotulo("DEPOIS DE DOAR") }
        bloco {
            Bloco("✅ Cuidados imediatos", listOf(
                "Permaneça no hemocentro por 15 minutos e aceite o lanche oferecido.",
                "Mantenha o curativo por pelo menos 4 horas. Se voltar a sangrar, comprima o local sem dobrar o braço.",
                "Hidrate-se bem, especialmente nas primeiras 4 horas.", "Não fume por cerca de 2 horas e evite álcool por 12 horas."
            ))
        }
        bloco {
            Bloco("🚗 Dirigir e esforço físico", listOf(
                "Dirigir (carro): aguarde pelo menos 1 hora. Se sentir mal-estar, pare o veículo.",
                "Esforço físico e academia: evite por 12 horas (idealmente 24h), principalmente com o braço da punção."
            ))
        }
        bloco {
            Bloco("👷 Atenção a algumas profissões e esportes", listOf(
                "Motoristas de ônibus/caminhão, operadores de máquinas: aguardar 12 horas.",
                "Atletas (ciclismo, natação, mergulho, competição): aguardar 24 horas.",
                "Trabalho em altura, paraquedismo e mergulho também exigem espera."
            ))
        }
        bloco {
            Bloco("🚨 Nos dias seguintes", listOf(
                "Se tiver febre, diarreia ou sintomas de infecção em até 7 a 14 dias, comunique o hemocentro.",
                "A cada doação são feitos exames (Hepatite B e C, Sífilis, HIV, HTLV, Chagas). Se algo for detectado, você é convocado."
            ))
        }
        bloco { Aviso("⚠️ Informações orientativas, baseadas no Ministério da Saúde e em hemocentros oficiais (Pró-Sangue, Hemominas, Santa Casa). A avaliação final é feita por um profissional no dia da doação.") }
    }
}

@Composable
fun DireitosScreen() {
    TelaLista("Seus direitos") {
        bloco { Paragrafo("Doar sangue dá direitos que muita gente não conhece. Saber deles é também uma forma de incentivar a doação.", suave = true) }
        bloco {
            Bloco("💼 Folga no trabalho", listOf(
                "Pela CLT (art. 473), o trabalhador pode faltar 1 dia a cada 12 meses, sem prejuízo do salário, em caso de doação voluntária comprovada.",
                "Essa folga não pode ser descontada em folha nem usada para compensar horas.",
                "Para servidores públicos e militares, o direito vem da Lei nº 1.075/1950 (dispensa do ponto no dia da doação).",
                "Atenção: a folga só vale se a doação for efetivada. Se você foi mas não estava apto, não gera o direito.",
                "Para garantir, apresente ao RH o comprovante com data e horário da doação."
            ))
        }
        bloco {
            Bloco("🎟️ Outros benefícios (variam por estado)", listOf(
                "Isenção de taxa de inscrição em concursos públicos estaduais, em vários estados.",
                "Meia-entrada e atendimento preferencial em eventos culturais e esportivos, conforme a lei de cada estado.",
                "Os benefícios estaduais variam — vale conferir a legislação do seu estado."
            ))
        }
        bloco { Aviso("⚖️ Direitos baseados na CLT (art. 473) e na Lei nº 1.075/1950. Regras estaduais podem complementar esses direitos.") }
    }
}

private data class Mito(val mito: String, val veredito: String, val texto: String)

private val MITOS = listOf(
    Mito("Doar sangue engorda ou emagrece.", "Mito", "Não faz nem uma coisa nem outra. O líquido é reposto pelo corpo em cerca de 24 horas."),
    Mito("Mulher menstruada não pode doar.", "Mito", "Pode, sim! A perda menstrual já é prevista pelo corpo. Quem usa anticoncepcional ou DIU também está liberada."),
    Mito("Quem fez tatuagem recente pode doar normalmente.", "Depende", "Desde setembro de 2026 a espera é de 4 meses (antes eram 12) para tatuagem, piercing, maquiagem definitiva e botox. Se o local tem alvará sanitário e você leva o comprovante, são só 7 dias. Piercing na boca ou na região genital: 4 meses depois de retirar."),
    Mito("Doar sangue vicia.", "Mito", "Não existe nenhuma dependência ligada ao ato de doar."),
    Mito("Preciso estar em jejum para doar.", "Mito", "É o contrário! Você deve estar alimentado. Doar em jejum aumenta o risco de passar mal."),
    Mito("Doar enfraquece o organismo ou deixa o sangue \"mais fraco\".", "Mito", "O corpo repõe o volume rapidamente — a coleta é menos de 10% do seu sangue. Não há enfraquecimento."),
    Mito("Quem toma remédio nunca pode doar.", "Mito", "Depende do remédio. Muitos não impedem (como anticoncepcional). Antibióticos e anti-inflamatórios pedem um tempo de espera. Informe sempre na triagem e nunca pare um remédio por conta própria."),
    Mito("Quem tem pressão alta ou diabetes não pode doar.", "Depende", "Se a hipertensão ou o diabetes estiverem controlados e sem complicações, geralmente é possível doar. A avaliação é feita na triagem."),
    Mito("Já tive dengue, nunca mais posso doar.", "Mito", "Falso. Após a recuperação e um período de espera, você volta a poder doar."),
    Mito("Quem já teve hepatite depois dos 11 anos não pode doar.", "Verdade", "Segundo a legislação, quem teve hepatite viral após os 11 anos de idade fica impedido de doar."),
    Mito("Gripe ou febre não atrapalham a doação.", "Mito", "Quem está com gripe, resfriado ou febre deve aguardar a recuperação antes de doar."),
    Mito("Uma doação ajuda várias pessoas.", "Verdade", "O sangue é separado em componentes — uma doação pode salvar até 4 vidas."),
    Mito("Todo tipo sanguíneo é bem-vindo.", "Verdade", "Todos são importantes! O O– é o doador universal e o mais requisitado em emergências, mas os tipos mais comuns (O+ e A+) também são muito usados."),
    Mito("Pessoas com tatuagem antiga não podem doar.", "Mito", "Podem. A espera é só para procedimentos recentes (4 meses, ou 7 dias com alvará sanitário), não importa a quantidade de tatuagens."),
    Mito("Quem fez endoscopia ou colonoscopia precisa esperar 6 meses.", "Mito", "Mudou: desde setembro de 2026 a espera é de 4 meses depois do exame."),
    Mito("Idosos não podem doar sangue.", "Mito", "Podem. A primeira doação vai até os 60 anos, e quem já é doador regular pode continuar depois dos 70, após avaliação da triagem clínica. No Hemope, por exemplo, é preciso ter doado ao menos uma vez nos últimos 12 meses, com intervalo de 6 meses entre as doações."),
    Mito("Posso doar quantas vezes eu quiser no ano.", "Mito", "Há um intervalo: homens a cada 60 dias (até 4x/ano) e mulheres a cada 90 dias (até 3x/ano), para o corpo repor o ferro.")
)

@Composable
fun MitosScreen() {
    TelaLista("Mitos e verdades") {
        bloco { Paragrafo("Muita gente deixa de doar por causa de informações erradas. Veja o que é mito e o que é verdade sobre doar sangue, com base em fontes oficiais.", suave = true) }
        items(MITOS) { m ->
            val (rotulo, cor) = when (m.veredito) {
                "Verdade" -> "✅ Verdade" to com.example.hemaremobile.ui.theme.HemareVerde
                "Depende" -> "⚠️ Depende" to CorAviso
                else -> "❌ Mito" to CorAlerta
            }
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                Cartao {
                    Text("\"${m.mito}\"", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                    Espaco(8)
                    Selo(rotulo, cor)
                    Espaco(8)
                    Paragrafo(m.texto, suave = true)
                }
            }
        }
        bloco { Aviso("⚠️ Conteúdo baseado em informações do Ministério da Saúde e de hemocentros oficiais (Hemominas, Hemoce, Pró-Sangue). Em caso de dúvida sobre seu caso, consulte o hemocentro antes de doar.") }
    }
}

// ---------------------------------------------------------------------------------------------
// Estoque, previsão e placar (públicos)
// ---------------------------------------------------------------------------------------------

private fun corDoNivel(nivel: String): Color = when (nivel) {
    "estavel" -> com.example.hemaremobile.ui.theme.HemareVerde
    "alerta" -> CorAviso
    "critico" -> CorAlerta
    else -> Color(0xFF7C4DFF)
}

@Composable
fun EstoqueScreen(estado: Estado, navegar: (String) -> Unit) {
    val itens = estado.estoquePublico()
    val urgentes = itens.count { it.nivel == "critico" || it.nivel == "emergencia" }
    TelaLista("Termômetro de estoque") {
        bloco { Paragrafo("Veja onde há falta de sangue agora. Se o seu tipo está em alerta, sua doação pode salvar vidas hoje.", suave = true) }
        if (urgentes > 0) {
            bloco {
                Aviso("🚨 $urgentes ${if (urgentes == 1) "tipo precisa" else "tipos precisam"} de doação com urgência!", CorAlerta)
                if (estado.papel == null || estado.papel == TipoConta.DOADOR) {
                    Espaco(6)
                    BotaoVazado("Posso doar?", { navegar(Rotas.TRIAGEM) })
                }
            }
        }
        if (itens.isEmpty()) bloco { Vazio("Nenhum hospital publicou seu estoque ainda.") }
        items(itens) { i ->
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                Cartao {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(i.tipo, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = corDoNivel(i.nivel))
                        Selo(Estoque.rotulo(i.nivel), corDoNivel(i.nivel))
                    }
                    Espaco(4)
                    Text(i.hospital, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                    Dica("${i.cidade} - ${i.estado}")
                }
            }
        }
    }
}

@Composable
fun ClimaScreen(estado: Estado, navegar: (String) -> Unit) {
    val cidades = estado.climaPorCidade()
    TelaLista("Previsão do sangue") {
        bloco { Paragrafo("Assim como uma previsão do tempo, veja a \"situação\" da doação de sangue nas cidades cadastradas.", suave = true) }
        if (cidades.isEmpty()) bloco { Vazio("Nenhuma cidade com estoque publicado ainda.") }
        items(cidades) { c ->
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                Cartao {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(c.clima.icone, fontSize = 34.sp)
                        Column(modifier = Modifier.padding(start = 14.dp)) {
                            Text("${c.cidade} - ${c.estado}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            Selo(c.clima.rotulo, MaterialTheme.colorScheme.primary)
                        }
                    }
                    Espaco(8)
                    ChipsLinha { c.tipos.forEach { Chip(it, false) {} } }
                }
            }
        }
        bloco { BotaoVazado("🌡️ Ver termômetro de estoque detalhado", { navegar(Rotas.ESTOQUE) }) }
    }
}

@Composable
fun PlacarScreen(estado: Estado, navegar: (String) -> Unit) {
    val p = estado.placar()
    fun f(n: Int) = "%,d".format(java.util.Locale("pt", "BR"), n)
    TelaLista("Placar das cidades") {
        bloco { Paragrafo("As cidades onde os doadores do Hemare mais doaram sangue. Leve a sua cidade para o topo!", suave = true) }
        bloco {
            Cartao {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    Numero(f(p.totalDoacoes), "doações confirmadas")
                    Numero(f(p.totalVidas), "vidas que podem ter sido salvas")
                    Numero(f(p.totalDoadores), "doadores cadastrados")
                }
            }
        }
        if (p.cidades.isEmpty()) bloco { Vazio("Ainda não há cidades suficientes no placar. Cada doação confirmada conta!") }
        items(p.cidades) { c ->
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                Cartao {
                    val medalha = mapOf(1 to "🥇", 2 to "🥈", 3 to "🥉")[c.posicao] ?: "${c.posicao}º"
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(medalha, fontSize = 26.sp)
                        Column(modifier = Modifier.padding(start = 14.dp)) {
                            Text(c.cidade, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            Dica("${f(c.doadores)} doadores · ${f(c.doacoes)} ${if (c.doacoes == 1) "doação" else "doações"} · ❤️ ${f(c.vidasSalvas)} vidas")
                        }
                    }
                }
            }
        }
        bloco { Aviso("🔒 Por privacidade, só aparecem cidades com pelo menos ${Gamificacao_MIN} doadores cadastrados, e nenhum doador é identificado.") }
        if (estado.papel == null || estado.papel == TipoConta.DOADOR) {
            bloco {
                Botao("Posso doar?", { navegar(Rotas.TRIAGEM) })
                Espaco(8)
                BotaoVazado("Onde doar", { navegar(Rotas.LOCAIS) })
            }
        }
    }
}

private const val Gamificacao_MIN = 3
