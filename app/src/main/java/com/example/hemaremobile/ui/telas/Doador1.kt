package com.example.hemaremobile.ui.telas

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hemaremobile.data.*
import com.example.hemaremobile.domain.*
import com.example.hemaremobile.ui.Rotas
import com.example.hemaremobile.ui.componentes.*
import com.example.hemaremobile.ui.compartilharTexto
import com.example.hemaremobile.ui.compartilharWhatsApp
import com.example.hemaremobile.ui.copiarTexto
import com.example.hemaremobile.ui.theme.HemareVerde
import androidx.compose.foundation.lazy.items
import java.time.LocalDate

/** As duas abas da conta: "Minha área | Meu perfil" (doador), "Minha área | Perfil do hospital", "Hospitais e notícias | Status". */
@Composable
fun AbasConta(tipo: TipoConta, rotaAtual: String, navegar: (String) -> Unit) {
    val abas = when (tipo) {
        TipoConta.DOADOR -> listOf(Rotas.AREA_DOADOR to "🏠 Minha área", Rotas.MEU_PERFIL to "👤 Meu perfil")
        TipoConta.HOSPITAL -> listOf(Rotas.PAINEL_HOSPITAL to "🏠 Minha área", Rotas.PERFIL_HOSPITAL to "🏥 Perfil do hospital")
        TipoConta.ADMIN -> listOf(Rotas.ADMIN to "🛡️ Hospitais e notícias", Rotas.ADMIN_STATUS to "📊 Status do desenvolvimento")
    }
    SubAbas(abas, rotaAtual) { if (it != rotaAtual) navegar(it) }
}

@Composable
fun FotoRedonda(doador: Doador?, tamanho: Int = 64) {
    val foto = doador?.foto
    Box(
        modifier = Modifier.size(tamanho.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)),
        contentAlignment = Alignment.Center
    ) {
        if (foto != null) Image(foto.asImageBitmap(), contentDescription = "Foto do perfil", contentScale = ContentScale.Crop, modifier = Modifier.size(tamanho.dp))
        else Text("🩸", fontSize = (tamanho / 2).sp)
    }
}

// ---------------------------------------------------------------------------------------------
// Minha área (doador)
// ---------------------------------------------------------------------------------------------

@Composable
fun AreaDoadorScreen(store: HemareStore, navegar: (String) -> Unit) {
    val estado by store.estado.collectAsState()
    val usuario = estado.usuario ?: return
    val doador = estado.doadorAtual
    val contexto = LocalContext.current
    val primeiroNome = (doador?.nomeSocial?.takeIf { it.isNotBlank() } ?: usuario.nome).split(" ").first()
    var aviso by remember { mutableStateOf("") }
    var avisoAfilhado by remember { mutableStateOf("") }
    var avisoPromessa by remember { mutableStateOf("") }
    var copiado by remember { mutableStateOf("") }

    TelaLista("Minha área") {
        item { AbasConta(TipoConta.DOADOR, Rotas.AREA_DOADOR, navegar) }
        bloco {
            Row(verticalAlignment = Alignment.CenterVertically) {
                FotoRedonda(doador, 64)
                Column(modifier = Modifier.padding(start = 14.dp)) {
                    Text("Olá, $primeiroNome!", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                    Dica("Bem-vindo(a) à sua área. O que você quer fazer hoje?")
                }
            }
        }

        if (doador == null) {
            bloco {
                Aviso("Complete seu perfil (tipo sanguíneo, cidade, CPF) para ganhar emblemas, agendar doações e ser chamado quando precisarem do seu tipo.")
                Espaco(8)
                Botao("Completar meu perfil", { navegar(Rotas.COMPLETAR_PERFIL) })
            }
        } else {
            val c = estado.conquistas(doador.id)
            val el = Elegibilidade.verificar(estado.ultimaDoacaoDe(doador.id), doador.sexo)
            val textoCompartilhar = if (c.totalDoacoes == 0)
                "Estou me preparando para doar sangue pela primeira vez! 🩸 Uma doação pode salvar até 4 vidas. Veja se você pode doar também no app Hemare."
            else "Já doei sangue ${c.totalDoacoes} ${if (c.totalDoacoes == 1) "vez" else "vezes"} e posso ter ajudado a salvar até ${c.vidasSalvas} vidas! 🩸 Sou ${c.nivel.icone} \"${c.nivel.nome}\" no Hemare. Doe sangue você também!"

            bloco {
                Cartao {
                    Titulo("Minhas conquistas")
                    Espaco(8)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(c.nivel.icone, fontSize = 40.sp)
                        Column(modifier = Modifier.padding(start = 12.dp)) {
                            Dica("Nível ${c.nivel.numero}")
                            Text(c.nivel.nome, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                    Espaco(10)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        Numero("${c.totalDoacoes}", if (c.totalDoacoes == 1) "doação" else "doações")
                        Numero("${c.vidasSalvas}", "vidas que podem ter sido salvas")
                    }
                    Espaco(10)
                    if (c.proximoNivel != null) {
                        BarraProgresso(c.progresso)
                        Espaco(4)
                        Dica("Falta${if (c.proximoNivel.faltam == 1) "" else "m"} ${c.proximoNivel.faltam} ${if (c.proximoNivel.faltam == 1) "doação" else "doações"} para ${c.proximoNivel.icone} ${c.proximoNivel.nome}")
                    } else Dica("Você chegou ao nível máximo. Obrigado por tanto! 💛")
                    Espaco(10)
                    Aviso(
                        if (el.apto) "✅ Pelo intervalo entre doações, você já pode doar de novo." else "⏳ Você poderá doar de novo em ${el.diasRestantes} dias.",
                        if (el.apto) HemareVerde else CorAviso
                    )
                    Espaco(8)
                    BotaoVazado(if (doador.querLembrete) "🔔 Lembrete ligado" else "🔕 Me avise quando eu puder doar", { aviso = store.alternarLembrete().texto })
                    Dica(if (doador.querLembrete) "Você recebe um aviso quando o intervalo acabar. Toque para desligar." else "Um único aviso por doação, só se você pedir.")
                    Mensagem(aviso)
                }
            }
            bloco {
                Cartao {
                    Titulo("Emblemas (${c.emblemas.count { it.conquistado }} de ${c.emblemas.size})")
                    Espaco(8)
                    c.emblemas.forEach { e ->
                        Row(modifier = Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(if (e.conquistado) e.icone else "🔒", fontSize = 22.sp)
                            Column(modifier = Modifier.padding(start = 12.dp)) {
                                Text(e.nome, fontWeight = FontWeight.SemiBold, color = if (e.conquistado) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant)
                                Dica(e.descricao + if (e.conquistado) " — conquistado!" else "")
                            }
                        }
                    }
                    Espaco(10)
                    Paragrafo("Inspire mais gente a doar: compartilhe sua conquista.", suave = true)
                    Espaco(6)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Chip("📤 Compartilhar", false) { compartilharTexto(contexto, textoCompartilhar) }
                        Chip("💬 WhatsApp", false) { compartilharWhatsApp(contexto, textoCompartilhar) }
                        Chip("🏙️ Placar", false) { navegar(Rotas.PLACAR) }
                    }
                }
            }

            // promessas em campanhas
            val promessas = estado.promessasDe(doador.id)
            if (promessas.isNotEmpty()) {
                bloco {
                    Cartao {
                        Titulo("🤝 Minhas promessas de doação")
                        promessas.forEach { pr ->
                            Espaco(10)
                            Text(pr.campanha.apelido, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            Dica("${estado.hospitalNome(pr.campanha.hospitalId)} — ${estado.hospitais[pr.campanha.hospitalId]?.cidade ?: ""}")
                            Paragrafo(if (pr.promessa.confirmada) "✅ Doação confirmada — obrigado!" else "📅 Você prometeu doar em ${Datas.dmy(pr.promessa.data)}")
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Chip("Ver campanha", false) { navegar(Rotas.campanha(pr.campanha.codigo)) }
                                if (!pr.promessa.confirmada) Chip("Cancelar promessa", false) { avisoPromessa = store.cancelarPromessa(pr.campanha.codigo).texto }
                            }
                        }
                        Mensagem(avisoPromessa)
                    }
                }
            }

            // afilhados
            val afilhados = estado.afilhadosDe(doador.id)
            bloco {
                Cartao {
                    if (afilhados.isEmpty()) {
                        Titulo("💝 Apadrinhe um paciente")
                        Espaco(4)
                        Paragrafo("Alguns pacientes precisam de sangue a cada poucas semanas, a vida toda. Apadrinhar é se comprometer a voltar a doar por eles.", suave = true)
                        Espaco(8)
                        BotaoVazado("Ver pacientes que procuram padrinhos", { navegar(Rotas.APADRINHAR) })
                    } else {
                        Titulo("💝 Meus afilhados")
                        Espaco(4)
                        Paragrafo(if (el.apto) "✅ Você já pode doar de novo: seus afilhados agradecem! Se um hospital precisar, você é chamado." else "⏳ Você poderá doar de novo em ${el.diasRestantes} dias. Enquanto isso, eles contam com você.", suave = true)
                        afilhados.forEach { a ->
                            Espaco(10)
                            Text("${a.apelido} (${a.tipo})", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            Dica("${estado.hospitalNome(a.hospitalId)} · ${a.padrinhos.size} de ${a.meta} padrinhos · precisa de sangue a cada ${a.frequenciaDias} dias")
                            Chip("Deixar de apadrinhar", false) { avisoAfilhado = store.deixarDeApadrinhar(a.id).texto }
                        }
                        Mensagem(avisoAfilhado)
                        Espaco(4)
                        BotaoTexto("Ver outros pacientes →", { navegar(Rotas.APADRINHAR) })
                    }
                }
            }

            // traga um amigo
            bloco {
                val texto = "Doar sangue pode salvar até 4 vidas 🩸 Crie sua conta no Hemare e use o meu código de convite ${doador.codigoIndicacao} para se cadastrar."
                Cartao {
                    Titulo("🤝 Traga um amigo")
                    Espaco(4)
                    Paragrafo("Muita gente doa uma vez só e não volta. Convide alguém: quando o seu amigo fizer a primeira doação confirmada, você ganha o emblema Recrutador (e Multiplicador com três amigos).", suave = true)
                    Espaco(8)
                    Aviso("Seu código de convite: ${doador.codigoIndicacao}")
                    Espaco(8)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Chip("📋 Copiar", false) { copiarTexto(contexto, doador.codigoIndicacao); copiado = "Código copiado! Envie para um amigo." }
                        Chip("📤 Compartilhar", false) { compartilharTexto(contexto, texto) }
                        Chip("💬 WhatsApp", false) { compartilharWhatsApp(contexto, texto) }
                    }
                    Mensagem(copiado)
                    Espaco(8)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        val cad = estado.amigosCadastrados(doador.id)
                        val doaram = estado.amigosQueDoaram(doador.id)
                        Numero("$cad", if (cad == 1) "amigo criou conta" else "amigos criaram conta")
                        Numero("$doaram", if (doaram == 1) "amigo já doou" else "amigos já doaram")
                    }
                }
            }
        }

        bloco {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ItemDeLista("📅", "Agendar doação", "Escolha o hospital, o dia e o horário.") { navegar(Rotas.AGENDAR) }
                ItemDeLista("🩸", "Minhas doações", "Quantas vezes você doou, quando fica apto de novo e seus agendamentos.") { navegar(Rotas.MINHAS_DOACOES) }
                ItemDeLista("👤", "Meu perfil", "Foto, dados pessoais, privacidade e sua carteirinha.") { navegar(Rotas.MEU_PERFIL) }
                ItemDeLista("🩺", "Posso doar hoje?", "Faça a triagem rápida e veja se está apto.") { navegar(Rotas.TRIAGEM) }
                ItemDeLista("🗺️", "Onde doar", "Encontre um hemocentro perto de você.") { navegar(Rotas.LOCAIS) }
            }
            Espaco(14)
            BotaoVazado("Sair da conta", { store.sair() })
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Agendar doação
// ---------------------------------------------------------------------------------------------

@Composable
fun AgendarScreen(store: HemareStore, navegar: (String) -> Unit) {
    val estado by store.estado.collectAsState()
    val doador = estado.doadorAtual
    val ativoReg = doador?.let { estado.agendamentoAtivoDe(it.id) }
    val ativoVaga = ativoReg?.let { estado.vagaPorId(it.vagaId) }
    var reagendando by remember { mutableStateOf(false) }
    var hospitalId by remember { mutableStateOf<Int?>(null) }
    var dia by remember { mutableStateOf<LocalDate?>(null) }
    var vagaId by remember { mutableStateOf<Int?>(null) }
    var mensagem by remember { mutableStateOf("") }

    val hist = doador?.let { estado.historicoDe(it.id) }
    val aptoEm = hist?.resumo?.aptoEm
    val hospitais = estado.hospitaisComVagas()
    val dias = hospitalId?.let { estado.diasDoHospital(it) } ?: emptyList()
    val horarios = dias.firstOrNull { it.data == dia }?.horarios ?: emptyList()
    val escolhida = horarios.firstOrNull { it.vagaId == vagaId }
    val mostrarEscolha = ativoReg == null || reagendando

    TelaLista("Agendar doação") {
        bloco {
            Paragrafo("Escolha um hemocentro ou hospital, o dia e o horário. Só dá para marcar um horário em que você já estará apto a doar.", suave = true)
        }
        if (doador == null) {
            bloco {
                Aviso("Complete seu perfil (informe o sexo) para agendar.")
                Espaco(8)
                Botao("Completar meu perfil", { navegar(Rotas.COMPLETAR_PERFIL) })
            }
            return@TelaLista
        }

        if (ativoReg != null && ativoVaga != null) {
            bloco {
                Cartao {
                    Titulo("Seu agendamento")
                    Espaco(6)
                    Text("${Datas.formatar(ativoVaga.data)} às ${ativoVaga.hora}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    val h = estado.hospitais[ativoVaga.hospitalId]
                    Paragrafo("${estado.hospitalNome(ativoVaga.hospitalId)} · ${h?.cidade}/${h?.estado}" + (h?.endereco?.let { " · $it, ${h.numero}" } ?: ""), suave = true)
                    Espaco(6)
                    Dica("Leve um documento oficial com foto, esteja bem alimentado e hidratado e tenha dormido bem.")
                    if (!reagendando) {
                        Espaco(10)
                        Botao("Reagendar", { reagendando = true; mensagem = "" })
                        Espaco(6)
                        BotaoVazado("Cancelar agendamento", { mensagem = store.cancelarAgendamento(ativoReg.id).texto }, perigo = true)
                    } else Dica("Escolha o novo horário abaixo. Seu horário atual só é liberado quando você confirmar o novo.")
                }
            }
        }

        if (mostrarEscolha) {
            if (aptoEm != null && aptoEm.isAfter(Datas.hoje())) {
                bloco { Aviso("Você fica apto em ${Datas.dmy(aptoEm)}. Os dias antes disso aparecem desativados.") }
            }
            bloco { Titulo("1. Escolha o local") }
            if (hospitais.isEmpty()) bloco { Vazio("Nenhum hospital abriu horários ainda. Veja onde doar e ligue para combinar.") }
            hospitais.forEach { (h, livres) ->
                bloco {
                    Cartao(aoClicar = { hospitalId = h.id; dia = null; vagaId = null; mensagem = "" }) {
                        Text(estado.hospitalNome(h.id), fontWeight = FontWeight.Bold, color = if (hospitalId == h.id) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                        Dica("${h.cidade}/${h.estado} · $livres ${if (livres == 1) "vaga" else "vagas"}")
                    }
                }
            }
            if (hospitalId != null) {
                bloco {
                    Titulo("2. Escolha o dia")
                    Espaco(6)
                    ChipsLinha {
                        dias.forEach { d ->
                            val bloqueado = aptoEm != null && d.data.isBefore(aptoEm)
                            Chip(Datas.formatar(d.data), dia == d.data, habilitado = !bloqueado) { dia = d.data; vagaId = null }
                        }
                    }
                }
            }
            if (dia != null) {
                bloco {
                    Titulo("3. Escolha o horário")
                    Espaco(6)
                    ChipsLinha { horarios.forEach { hv -> Chip("${hv.hora} (${hv.livres} ${if (hv.livres == 1) "vaga" else "vagas"})", vagaId == hv.vagaId) { vagaId = hv.vagaId } } }
                }
            }
            if (escolhida != null && dia != null) {
                bloco {
                    Cartao {
                        Paragrafo("Confirmar ${Datas.formatar(dia!!)} às ${escolhida.hora} em ${estado.hospitalNome(hospitalId!!)}?")
                        Espaco(8)
                        Botao(if (reagendando) "Confirmar novo horário" else "Confirmar agendamento", {
                            val r = if (reagendando && ativoReg != null) store.reagendar(ativoReg.id, escolhida.vagaId) else store.agendar(escolhida.vagaId)
                            mensagem = r.texto
                            if (r.ok) { reagendando = false; hospitalId = null; dia = null; vagaId = null }
                        })
                        if (reagendando) { Espaco(6); BotaoVazado("Manter o horário atual", { reagendando = false }) }
                    }
                }
            }
        }
        bloco {
            Mensagem(mensagem)
            Espaco(6)
            Dica("Veja também o seu histórico de doações. A decisão final de aptidão é sempre da equipe de triagem.")
            BotaoTexto("Ver minhas doações", { navegar(Rotas.MINHAS_DOACOES) })
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Minhas doações
// ---------------------------------------------------------------------------------------------

@Composable
fun MinhasDoacoesScreen(store: HemareStore, navegar: (String) -> Unit) {
    val estado by store.estado.collectAsState()
    val doador = estado.doadorAtual
    TelaLista("Minhas doações") {
        if (doador == null) {
            bloco {
                Aviso("Complete seu perfil para ver o seu histórico.")
                Espaco(8)
                Botao("Completar meu perfil", { navegar(Rotas.COMPLETAR_PERFIL) })
            }
            return@TelaLista
        }
        val h = estado.historicoDe(doador.id)
        val r = h.resumo
        bloco {
            Paragrafo(Agendamento.proximoPasso(r))
            BotaoTexto("Agendar uma doação", { navegar(Rotas.AGENDAR) })
        }
        bloco {
            Cartao {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    Numero("${r.totalDoacoes}", if (r.totalDoacoes == 1) "doação" else "doações")
                    Numero("até ${r.vidasAte}", "vidas ajudadas")
                }
                Espaco(10)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    Numero(r.ultimaDoacao?.let { Datas.formatar(it) } ?: "—", "última doação")
                    Numero(if (r.aptoAgora) "Agora" else (r.aptoEm?.let { Datas.formatar(it) } ?: "—"), if (r.aptoAgora) "você já pode doar" else "você fica apto")
                }
            }
        }
        if (r.limiteAnual != null) {
            bloco {
                Cartao {
                    Paragrafo("Doações nos últimos 12 meses: ${r.nosUltimos12Meses} de ${r.limiteAnual} permitidas por ano.")
                    Espaco(6)
                    BarraProgresso(Math.round(r.nosUltimos12Meses * 100f / r.limiteAnual))
                }
            }
        }
        if (r.porAno.isNotEmpty()) {
            bloco {
                Cartao {
                    Titulo("Doações por ano")
                    val maior = maxOf(1, r.porAno.values.maxOrNull() ?: 1)
                    r.porAno.toSortedMap().forEach { (ano, qtd) ->
                        Espaco(8)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(ano, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("$qtd", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        }
                        BarraProgresso(qtd * 100 / maior)
                    }
                }
            }
        }
        if (r.taxaDeComparecimento != null) {
            bloco { Dica("Você compareceu a ${r.compareceu} de ${r.compareceu + r.faltou} horários marcados (${r.taxaDeComparecimento}%).") }
        }
        bloco { Titulo("Linha do tempo") }
        if (h.linhaDoTempo.isEmpty()) {
            bloco { Vazio("Nada por aqui ainda. Sua primeira doação aparece aqui assim que o hospital confirmar.") }
        } else {
            items(h.linhaDoTempo) { i ->
                Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                    Cartao {
                        val titulo = if (i.tipo == "doacao") "🩸 ${Datas.formatar(i.data)} · Doação confirmada"
                        else "${Agendamento.ICONES_STATUS[i.status]} ${Datas.formatar(i.data)} às ${i.hora} · ${Agendamento.NOMES_STATUS[i.status]}"
                        Text(titulo, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        Dica("${i.hospital} · ${i.cidade}" + (i.selo?.let { " · selo $it" } ?: ""))
                    }
                }
            }
        }
        bloco { Dica("O selo é o começo do código da cadeia de confiança: ele prova que o hospital confirmou a doação e que o registro não foi alterado.") }
    }
}
