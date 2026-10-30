package com.example.hemaremobile.ui.telas

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.hemaremobile.data.*
import com.example.hemaremobile.domain.*
import com.example.hemaremobile.ui.Rotas
import com.example.hemaremobile.ui.componentes.*
import com.example.hemaremobile.ui.compartilharTexto
import com.example.hemaremobile.ui.desenharCarteirinha
import com.example.hemaremobile.ui.lerFoto
import com.example.hemaremobile.ui.salvarNaGaleria
import com.example.hemaremobile.ui.theme.HemareVerde
import kotlinx.coroutines.delay
import androidx.compose.runtime.LaunchedEffect

// ---------------------------------------------------------------------------------------------
// Segurança da conta (doador e hospital): trocar senha e sair de todos os aparelhos
// ---------------------------------------------------------------------------------------------

@Composable
fun SegurancaConta(store: HemareStore) {
    var aberto by remember { mutableStateOf(false) }
    var atual by remember { mutableStateOf("") }
    var nova by remember { mutableStateOf("") }
    var confirmacao by remember { mutableStateOf("") }
    var mensagem by remember { mutableStateOf("") }

    Cartao {
        Titulo("🔑 Segurança da conta")
        Espaco(4)
        Dica("Troque a senha quando quiser. Isso encerra as sessões abertas em outros aparelhos.")
        Espaco(8)
        if (!aberto) {
            BotaoVazado("Trocar minha senha", { aberto = true })
            Espaco(6)
            BotaoVazado("Sair de todos os aparelhos", { store.sair() })
        } else {
            Campo(atual, { atual = it }, "Senha atual", senha = true); Espaco(4)
            Campo(nova, { nova = it }, "Nova senha (mínimo de 8 caracteres)", senha = true); Espaco(4)
            Campo(confirmacao, { confirmacao = it }, "Repita a nova senha", senha = true)
            Espaco(8)
            Botao("Salvar nova senha", {
                val r = store.alterarSenha(atual, nova, confirmacao)
                mensagem = r.texto
                if (r.ok) { atual = ""; nova = ""; confirmacao = "" }
            })
            Espaco(6)
            BotaoVazado("Cancelar", { aberto = false; mensagem = "" })
        }
        Mensagem(mensagem)
    }
}

// ---------------------------------------------------------------------------------------------
// Completar perfil (primeiro acesso do doador)
// ---------------------------------------------------------------------------------------------

@Composable
fun CompletarPerfilScreen(store: HemareStore, aoConcluir: () -> Unit) {
    val estado by store.estado.collectAsState()
    val usuario = estado.usuario ?: return
    var tipo by remember { mutableStateOf("") }
    var sexo by remember { mutableStateOf("") }
    var cidade by remember { mutableStateOf("") }
    var nomeSocial by remember { mutableStateOf("") }
    var cpf by remember { mutableStateOf("") }
    var cpfTocado by remember { mutableStateOf(false) }
    var telefone by remember { mutableStateOf("") }
    var visibilidade by remember { mutableStateOf("anonimo") }
    var mensagem by remember { mutableStateOf("") }
    var pronto by remember { mutableStateOf(false) }

    LaunchedEffect(pronto) { if (pronto) { delay(1200); aoConcluir() } }

    TelaLista("Complete seu perfil") {
        bloco {
            Paragrafo("Esses dados ajudam a te conectar a quem precisa.", suave = true)
            Espaco(12)
            CampoSelecao("Tipo sanguíneo *", tipo, Perfil.TIPOS_SANGUINEOS.map { it to (if (it == Perfil.RH_NULO) "🌟 $it — raríssimo" else it) }, { tipo = it })
            if (tipo == Perfil.RH_NULO) { Espaco(6); Aviso("🌟 Sangue dourado! É o tipo mais raro do mundo. Nosso time dará atenção especial.") }
            Espaco(8)
            CampoSelecao("Gênero (para o intervalo entre doações) *", sexo, listOf("F" to "Feminino", "M" to "Masculino"), { sexo = it })
            Espaco(8)
            Campo(cidade, { cidade = it }, "Cidade *", placeholder = "Sua cidade"); Espaco(4)
            Campo(nomeSocial, { nomeSocial = it.filterNot { c -> c.isDigit() } }, "Nome social (opcional)", dica = "Como você prefere ser chamado(a)"); Espaco(4)
            Campo(
                cpf, { cpf = Documentos.mascaraCpf(it); cpfTocado = true }, "CPF *", teclado = KeyboardType.Number, placeholder = "000.000.000-00",
                aviso = if (cpfTocado && cpf.isNotEmpty() && !Documentos.cpfValido(cpf)) "CPF inválido — confira os números." else ""
            ); Espaco(4)
            Campo(telefone, { telefone = Documentos.mascaraTelefone(it) }, "Telefone / WhatsApp", teclado = KeyboardType.Phone, placeholder = "(00) 00000-0000")
            Espaco(10)
            Rotulo("PRIVACIDADE *")
            OpcaoRadio("🔒 Anônimo — você ajuda sem que hospitais vejam seu nome ou contato.", visibilidade == "anonimo") { visibilidade = "anonimo" }
            OpcaoRadio("👋 Identificado — autorizo hospitais compatíveis a verem meu nome e contato para me convocar.", visibilidade == "identificado") { visibilidade = "identificado" }
            Espaco(12)
            Botao("Salvar perfil", {
                when {
                    tipo.isEmpty() || sexo.isEmpty() || cidade.isBlank() -> mensagem = "❌ Preencha tipo sanguíneo, gênero e cidade."
                    cpf.isEmpty() || !Documentos.cpfValido(cpf) -> mensagem = "❌ Informe um CPF válido."
                    visibilidade == "identificado" && telefone.isBlank() -> mensagem = "❌ Para aparecer aos hospitais, informe um telefone de contato."
                    else -> {
                        val r = store.salvarPerfil(
                            FormPerfil(usuario.nome, nomeSocial, tipo, sexo.firstOrNull(), "", cidade, "", "", "", telefone, cpf, visibilidade, "", ""), null, false
                        )
                        mensagem = r.texto
                        if (r.ok) pronto = true
                    }
                }
            })
            Mensagem(mensagem)
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Meu perfil (doador): dados e privacidade | saúde e doação | carteirinha | segurança
// ---------------------------------------------------------------------------------------------

@Composable
fun MeuPerfilScreen(store: HemareStore, navegar: (String) -> Unit) {
    val estado by store.estado.collectAsState()
    val usuario = estado.usuario ?: return
    val doador = estado.doadorAtual
    var aba by remember { mutableStateOf("dados") }

    if (doador == null) {
        TelaLista("Meu perfil") {
            item { AbasConta(TipoConta.DOADOR, Rotas.MEU_PERFIL, navegar) }
            bloco {
                Aviso("Você ainda não completou o perfil de doador. Leva 1 minuto.")
                Espaco(8)
                Botao("Completar meu perfil", { navegar(Rotas.COMPLETAR_PERFIL) })
                Espaco(14)
                SegurancaConta(store)
            }
        }
        return
    }

    val conquistas = estado.conquistas(doador.id)
    val completude = Perfil.completude(
        usuario.nome, doador.foto != null, doador.tipoSanguineo, doador.sexo, doador.cidade, doador.estado, doador.dataNascimento,
        doador.telefone, doador.cpf != null, doador.genero, doador.pesoKg, doador.contatoEmergenciaNome, doador.contatoEmergenciaTelefone
    )
    val nomeExibido = doador.nomeSocial?.takeIf { it.isNotBlank() } ?: usuario.nome

    TelaLista("Meu perfil") {
        item { AbasConta(TipoConta.DOADOR, Rotas.MEU_PERFIL, navegar) }
        bloco {
            Row(verticalAlignment = Alignment.CenterVertically) {
                FotoRedonda(doador, 72)
                Column(modifier = Modifier.padding(start = 14.dp)) {
                    Text(nomeExibido, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                    Dica(usuario.email)
                }
            }
            Espaco(8)
            ChipsLinha {
                Selo("🩸 " + if (doador.tipoSanguineo.startsWith("Rh nulo")) "Rh nulo" else doador.tipoSanguineo, MaterialTheme.colorScheme.primary)
                Selo("${conquistas.nivel.icone} ${conquistas.nivel.nome}", MaterialTheme.colorScheme.onSurfaceVariant)
                Selo("${conquistas.totalDoacoes} ${if (conquistas.totalDoacoes == 1) "doação" else "doações"}", MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Espaco(10)
            Cartao {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Titulo("Perfil ${completude.porcentagem}% completo")
                    if (completude.faltando.isEmpty()) Text("🎉", style = MaterialTheme.typography.titleMedium)
                }
                Espaco(6)
                BarraProgresso(completude.porcentagem)
                completude.faltando.take(3).forEach { Dica("• ${it.rotulo}") }
            }
        }
        item {
            SubAbas(
                listOf("dados" to "👤 Dados e privacidade", "saude" to "🩺 Saúde e doação", "carteirinha" to "🪪 Carteirinha", "seguranca" to "🔑 Conta e segurança"),
                aba
            ) { aba = it }
        }
        when (aba) {
            "dados" -> bloco { FormularioPerfil(store, estado, doador, usuario) }
            "saude" -> {
                bloco { RedeRaraDoador(store, estado, doador) }
                bloco { FichaPcd(store, doador) }
            }
            "carteirinha" -> bloco { CarteirinhaDoador(estado, doador, usuario, conquistas) }
            else -> bloco { LgpdESeguranca(store) }
        }
    }
}

@Composable
private fun FormularioPerfil(store: HemareStore, estado: Estado, d: Doador, u: Usuario) {
    val contexto = LocalContext.current
    var nome by remember(d.id) { mutableStateOf(u.nome) }
    var nomeSocial by remember(d.id) { mutableStateOf(d.nomeSocial ?: "") }
    var genero by remember(d.id) { mutableStateOf(d.genero ?: "") }
    var nascimento by remember(d.id) { mutableStateOf(d.dataNascimento?.let { Datas.dmy(it) } ?: "") }
    var telefone by remember(d.id) { mutableStateOf(d.telefone ?: "") }
    var cidade by remember(d.id) { mutableStateOf(d.cidade) }
    var uf by remember(d.id) { mutableStateOf(d.estado ?: "") }
    var peso by remember(d.id) { mutableStateOf(d.pesoKg?.toString() ?: "") }
    var alterandoCpf by remember(d.id) { mutableStateOf(false) }
    var novoCpf by remember(d.id) { mutableStateOf("") }
    var tipo by remember(d.id) { mutableStateOf(d.tipoSanguineo) }
    var sexo by remember(d.id) { mutableStateOf(d.sexo.toString()) }
    var visibilidade by remember(d.id) { mutableStateOf(d.visibilidade) }
    var emNome by remember(d.id) { mutableStateOf(d.contatoEmergenciaNome ?: "") }
    var emTel by remember(d.id) { mutableStateOf(d.contatoEmergenciaTelefone ?: "") }
    var foto by remember(d.id) { mutableStateOf(d.foto) }
    var fotoAlterada by remember(d.id) { mutableStateOf(false) }
    var avisoFoto by remember { mutableStateOf("") }
    var mensagem by remember { mutableStateOf("") }

    val escolherFoto = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            val b = lerFoto(contexto, uri)
            if (b != null) { foto = b; fotoAlterada = true; avisoFoto = "" } else avisoFoto = "❌ Não consegui abrir essa imagem."
        }
    }

    val visao = Perfil.visaoDoHospital(nome, nomeSocial, tipo, cidade, visibilidade, telefone)

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Cartao {
            Titulo("📷 Foto")
            Espaco(8)
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (foto != null) Image(foto!!.asImageBitmap(), "Foto do perfil", modifier = Modifier.padding(end = 12.dp).then(Modifier.fillMaxWidth(0.25f)))
                Column {
                    Chip(if (foto != null) "📷 Trocar foto" else "📷 Adicionar foto", false) { escolherFoto.launch("image/*") }
                    if (foto != null) { Espaco(4); Chip("Remover foto", false) { foto = null; fotoAlterada = true } }
                }
            }
            if (fotoAlterada) Dica("Foto nova: toque em “Salvar alterações” para guardar.")
            Mensagem(avisoFoto)
        }
        Cartao {
            Titulo("👤 Identidade")
            Espaco(8)
            Campo(nome, { nome = it.filterNot { c -> c.isDigit() } }, "Nome completo *"); Espaco(4)
            Campo(nomeSocial, { nomeSocial = it.filterNot { c -> c.isDigit() } }, "Nome social", dica = "Se preencher, é o nome que aparece no perfil, na carteirinha e para os hospitais."); Espaco(4)
            CampoSelecao("Gênero", genero, listOf("" to "Não informar") + Perfil.GENEROS, { genero = it })
        }
        Cartao {
            Titulo("📋 Dados pessoais")
            Espaco(8)
            Campo(nascimento, { nascimento = Datas.mascaraDmy(it) }, "Data de nascimento", teclado = KeyboardType.Number, placeholder = "dd/mm/aaaa"); Espaco(4)
            Campo(telefone, { telefone = Documentos.mascaraTelefone(it) }, "Telefone / WhatsApp", teclado = KeyboardType.Phone); Espaco(4)
            Campo(cidade, { cidade = it }, "Cidade *"); Espaco(4)
            CampoSelecao("Estado", uf, listOf("" to "—") + Estados.SIGLAS.map { it to it }, { uf = it }); Espaco(8)
            Campo(peso, { peso = it.filter { c -> c.isDigit() }.take(3) }, "Peso (kg)", teclado = KeyboardType.Number, dica = "Opcional. Quem doa precisa ter mais de 50 kg.")
            Espaco(8)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("CPF", color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (!alterandoCpf) {
                    Text(Documentos.mascararCpf(d.cpf) ?: "Não informado", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Chip(if (d.cpf != null) "Alterar" else "Informar", false) { alterandoCpf = true }
                }
            }
            if (alterandoCpf) {
                Campo(
                    novoCpf, { novoCpf = Documentos.mascaraCpf(it) }, "Novo CPF", teclado = KeyboardType.Number, placeholder = "000.000.000-00",
                    aviso = if (novoCpf.length == 14 && !Documentos.cpfValido(novoCpf)) "CPF inválido." else ""
                )
                Chip("Cancelar", false) { alterandoCpf = false; novoCpf = "" }
            }
        }
        Cartao {
            Titulo("🩸 Para doar")
            Espaco(8)
            CampoSelecao("Tipo sanguíneo *", tipo, Perfil.TIPOS_SANGUINEOS.map { it to (if (it == Perfil.RH_NULO) "🌟 $it" else it) }, { tipo = it }); Espaco(4)
            CampoSelecao("Sexo para o intervalo entre doações *", sexo, listOf("F" to "Feminino (intervalo de 90 dias)", "M" to "Masculino (intervalo de 60 dias)"), { sexo = it })
            Dica("Usado só para calcular quando você pode doar de novo. Quem decide a sua aptidão é sempre o hemocentro.")
        }
        Cartao {
            Titulo("🔒 Privacidade")
            OpcaoRadio("🔒 Anônimo — você ajuda sem que hospitais vejam seu nome ou contato.", visibilidade == "anonimo") { visibilidade = "anonimo" }
            OpcaoRadio("👋 Identificado — autorizo hospitais compatíveis a verem meu nome e telefone para me chamar.", visibilidade == "identificado") { visibilidade = "identificado" }
            Espaco(8)
            Aviso("👁️ Como o hospital te vê agora:\n" + visao.ve.joinToString("\n") { "• ${it.first}: ${it.second}" } +
                "\n\nNunca vistos por hospitais: " + visao.naoVe.joinToString(", ") + ".", HemareVerde)
            if (visibilidade == "identificado" && telefone.isBlank()) Mensagem("❌ Informe um telefone para aparecer aos hospitais.")
        }
        Cartao {
            Titulo("🚑 Contato de emergência")
            Dica("Opcional. Aparece só na sua carteirinha, que fica no seu aparelho. Nenhum hospital vê.")
            Espaco(8)
            Campo(emNome, { emNome = it.filterNot { c -> c.isDigit() } }, "Nome do contato"); Espaco(4)
            Campo(emTel, { emTel = Documentos.mascaraTelefone(it) }, "Telefone do contato", teclado = KeyboardType.Phone)
        }
        Botao("Salvar alterações", {
            val r = store.salvarPerfil(
                FormPerfil(nome, nomeSocial, tipo, sexo.firstOrNull(), genero, cidade, uf, nascimento, peso, telefone,
                    if (alterandoCpf && novoCpf.isNotBlank()) novoCpf else null, visibilidade, emNome, emTel),
                foto, fotoAlterada
            )
            mensagem = r.texto
            if (r.ok) { fotoAlterada = false; alterandoCpf = false; novoCpf = "" }
        })
        Mensagem(mensagem)
    }
}

// ---------------------------------------------------------------------------------------------
// Rede de sangue raro (doador)
// ---------------------------------------------------------------------------------------------

@Composable
private fun RedeRaraDoador(store: HemareStore, estado: Estado, d: Doador) {
    var fenotipo by remember(d.id) { mutableStateOf(d.raro.fenotipo ?: "") }
    var alcance by remember(d.id) { mutableStateOf(d.raro.alcance ?: "estado") }
    var consentimento by remember(d.id) { mutableStateOf(d.raro.participa) }
    var editando by remember { mutableStateOf(false) }
    var mensagem by remember { mutableStateOf("") }
    val chamados = estado.chamadosDo(d.id)

    Cartao {
        Titulo("💎 Rede de sangue raro")
        Espaco(4)
        Dica("Alguns fenótipos (como Rh nulo e Bombay) são tão raros que, numa emergência, encontrar uma bolsa compatível pode levar dias. Se você é um deles, a rede avisa você quando um hospital precisar. A decisão clínica é sempre da equipe médica.")
        if (chamados.isNotEmpty()) {
            Espaco(10)
            Aviso("🆘 Chamados para você", CorAlerta)
            chamados.forEach { c ->
                Espaco(8)
                Paragrafo("${c.hospital} (${c.cidade}/${c.estado}) procura ${Raros.rotuloFenotipo(c.pedido.fenotipo)}${c.pedido.apelidoPaciente?.let { " para $it" } ?: ""}. Restam cerca de ${c.horasRestantes} h.")
                when (c.resposta) {
                    "disponivel" -> Paragrafo("✅ Você disse que pode ajudar. O hospital vai falar com você pelo telefone do seu perfil.")
                    "indisponivel" -> Paragrafo("Você avisou que não pode desta vez. Obrigado!", suave = true)
                    else -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Chip("💛 Posso ajudar", false) { mensagem = store.responderChamado(c.pedido.id, "disponivel").texto }
                        Chip("Não posso agora", false) { mensagem = store.responderChamado(c.pedido.id, "indisponivel").texto }
                    }
                }
            }
            Dica("Só ao escolher “posso ajudar” o hospital passa a ver seu nome e telefone.")
        }
        Espaco(10)
        if (!d.raro.participa || editando) {
            if (d.telefone.isNullOrBlank()) Mensagem("❌ Para entrar, informe um telefone na aba “Dados e privacidade” e salve.")
            CampoSelecao("Meu fenótipo raro", fenotipo, listOf("" to "Escolha...") + Raros.FENOTIPOS, { fenotipo = it }); Espaco(4)
            CampoSelecao("Até onde posso ir", alcance, Raros.ALCANCES, { alcance = it }); Espaco(4)
            CaixaMarcar("Autorizo hospitais aprovados do Hemare a me chamar em emergências reais. Meu nome e telefone só aparecem para o hospital se eu responder “posso ajudar”.", consentimento, { consentimento = it })
            Espaco(6)
            Botao(if (d.raro.participa) "Salvar mudanças" else "💎 Entrar na rede", {
                val r = store.participarDaRede(fenotipo, alcance, consentimento)
                mensagem = r.texto
                if (r.ok) editando = false
            })
            if (editando) { Espaco(6); BotaoVazado("Cancelar", { editando = false }) }
        } else {
            ChipsLinha {
                Selo(Raros.rotuloFenotipo(d.raro.fenotipo ?: "") ?: "", MaterialTheme.colorScheme.primary)
                if (d.raro.status == "confirmado") Selo("✓ Confirmado por um hospital", HemareVerde) else Selo("⏳ Aguardando confirmação do laudo", CorAviso)
            }
            if (d.raro.status != "confirmado") {
                Espaco(8)
                Aviso("Seu código para o hospital: ${d.raro.codigo}\n\nLeve o laudo laboratorial a um hospital que use o Hemare e mostre este código. Quem confere o laudo pessoalmente confirma o seu fenótipo. Enquanto não for confirmado, você não é chamado.")
            }
            Espaco(6)
            Dica("Alcance: ${Raros.ALCANCES.firstOrNull { it.first == d.raro.alcance }?.second}. Você é chamado no máximo uma vez por semana.")
            Espaco(6)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip("Mudar fenótipo ou alcance", false) { editando = true }
                Chip("Sair da rede", false) { mensagem = store.sairDaRede().texto }
            }
        }
        Mensagem(mensagem)
    }
}

// ---------------------------------------------------------------------------------------------
// Ficha PcD
// ---------------------------------------------------------------------------------------------

private fun alternar(lista: List<String>, valor: String): List<String> = if (lista.contains(valor)) lista - valor else lista + valor

@Composable
private fun FichaPcd(store: HemareStore, d: Doador) {
    val contexto = LocalContext.current
    var declarado by remember(d.id) { mutableStateOf(d.pcd.declarado) }
    var tipos by remember(d.id) { mutableStateOf(d.pcd.tipos) }
    var apoios by remember(d.id) { mutableStateOf(d.pcd.apoios) }
    var consentimento by remember(d.id) { mutableStateOf(d.pcd.declarado) }
    var mensagem by remember { mutableStateOf("") }

    var fichaAberta by remember { mutableStateOf(false) }
    var causas by remember { mutableStateOf(listOf<String>()) }
    var epilepsia by remember { mutableStateOf("") }
    var meses by remember { mutableStateOf("") }
    var medicamentos by remember { mutableStateOf(listOf<String>()) }
    var decisao by remember { mutableStateOf("sozinho") }
    var idade by remember { mutableStateOf(d.dataNascimento?.let { Perfil.idadeEm(it).toString() } ?: "") }
    var peso by remember { mutableStateOf(d.pesoKg?.toString() ?: "") }
    var resultado by remember { mutableStateOf<Pcd.ResultadoFicha?>(null) }
    var avisoFicha by remember { mutableStateOf("") }

    Cartao {
        Titulo("♿ Pessoa com deficiência (PcD)")
        Espaco(4)
        Dica(Pcd.INTRO)
        Espaco(8)
        CaixaMarcar("Sou pessoa com deficiência (PcD) e quero a Ficha PcD no meu perfil.", declarado, { declarado = it })
        if (declarado) {
            Rotulo("TIPO DE DEFICIÊNCIA (pode marcar mais de um)")
            Pcd.TIPOS.forEach { (v, r) -> CaixaMarcar(r, tipos.contains(v), { tipos = alternar(tipos, v) }) }
            Espaco(6)
            Rotulo("O QUE AJUDA NO ATENDIMENTO (opcional)")
            Pcd.APOIOS.forEach { (v, r) -> CaixaMarcar(r, apoios.contains(v), { apoios = alternar(apoios, v) }) }
            Espaco(6)
            CaixaMarcar("Entendo que a deficiência é uma informação sensível. Ela fica só no meu perfil, nenhum hospital vê, entra no “Baixar meus dados” e some se eu desmarcar aqui ou excluir a conta.", consentimento, { consentimento = it })
        }
        Espaco(6)
        Botao("Salvar", {
            val r = store.salvarPcd(declarado, tipos, apoios, consentimento)
            mensagem = r.texto
            if (!declarado && r.ok) { fichaAberta = false; resultado = null }
        })
        Mensagem(mensagem)

        if (d.pcd.declarado) {
            Espaco(12)
            BotaoVazado(if (fichaAberta) "Fechar a Ficha PcD" else "📋 Abrir a Ficha PcD (o que a norma diz para o meu caso)", { fichaAberta = !fichaAberta })
            if (fichaAberta) {
                Espaco(8)
                Dica("Suas respostas não são guardadas: o resultado aparece só aqui.")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column(modifier = Modifier.fillMaxWidth(0.48f)) { Campo(idade, { idade = it.filter { c -> c.isDigit() }.take(3) }, "Idade", teclado = KeyboardType.Number) }
                    Column(modifier = Modifier.fillMaxWidth()) { Campo(peso, { peso = it.filter { c -> c.isDigit() }.take(3) }, "Peso (kg)", teclado = KeyboardType.Number) }
                }
                Espaco(6)
                Rotulo("1. QUAL É A CAUSA DA DEFICIÊNCIA? (pode marcar mais de uma)")
                Pcd.CAUSAS.forEach { (v, r) -> CaixaMarcar(r, causas.contains(v), { causas = alternar(causas, v) }) }
                if (causas.contains("epilepsia")) {
                    Rotulo("SOBRE A EPILEPSIA")
                    Pcd.SITUACOES_EPILEPSIA.forEach { (v, r) -> OpcaoRadio(r, epilepsia == v) { epilepsia = v } }
                }
                if (causas.contains("lesao-trauma") || causas.contains("amputacao")) {
                    Campo(meses, { meses = it.filter { c -> c.isDigit() }.take(4) }, "Há quantos meses foi a última cirurgia ou o acidente? (opcional)", teclado = KeyboardType.Number)
                }
                Espaco(6)
                Rotulo("2. QUAIS REMÉDIOS DE USO CONTÍNUO VOCÊ TOMA?")
                Pcd.MEDICAMENTOS.forEach { (v, r) -> CaixaMarcar(r, medicamentos.contains(v), { medicamentos = alternar(medicamentos, v) }) }
                Dica("Nunca pare um remédio por conta própria para poder doar.")
                Espaco(6)
                Rotulo("3. COMO VOCÊ DECIDE SOBRE A SUA SAÚDE?")
                Pcd.DECISOES.forEach { (v, r) -> OpcaoRadio(r, decisao == v) { decisao = v } }
                Espaco(8)
                Botao("Gerar minha ficha", {
                    val v = Pcd.avaliarFicha(idade.toIntOrNull(), peso.toIntOrNull(), causas, epilepsia.ifEmpty { null }, meses.toIntOrNull(), medicamentos, decisao, d.pcd.apoios)
                    when (v) {
                        is Validacao.Erro -> { resultado = null; avisoFicha = "❌ ${v.mensagem}" }
                        is Validacao.Ok -> { resultado = v.valor; avisoFicha = "" }
                    }
                })
                Mensagem(avisoFicha)
            }
            resultado?.let { r ->
                val cor = when (r.nivel) { "verde" -> HemareVerde; "amarelo" -> CorAviso; else -> CorAlerta }
                Espaco(12)
                Selo(r.titulo, cor)
                Espaco(6)
                Paragrafo(r.resumo)
                Espaco(8)
                Titulo("O que a norma diz para o seu caso")
                r.avaliacao.forEach { i ->
                    val icone = when (i.nivel) { "ok" -> "✅"; "amarelo" -> "🟡"; else -> "🔴" }
                    Espaco(4); Paragrafo("$icone ${i.texto}" + (i.base?.let { " ($it)" } ?: ""))
                }
                Espaco(8); Titulo("O que levar"); r.levar.forEach { Paragrafo("• $it", suave = true) }
                if (r.apoios.isNotEmpty()) { Espaco(8); Titulo("Combine antes com o hemocentro"); r.apoios.forEach { Paragrafo("• $it", suave = true) } }
                Espaco(8); Titulo("Se a resposta for “não”"); r.depois.forEach { Paragrafo("• $it", suave = true) }
                Espaco(8)
                Dica(Pcd.NOTA_PRATICA)
                Espaco(4)
                Text(Pcd.AVISO, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Dica("Base: ${Pcd.FONTE}. Consultada em 05/10/2026. Os prazos e critérios podem mudar: confira com o hemocentro.")
                Espaco(8)
                BotaoVazado("📤 Compartilhar a ficha", {
                    compartilharTexto(
                        contexto,
                        "Ficha PcD do Hemare — ${r.titulo}\n\n${r.resumo}\n\n" + r.avaliacao.joinToString("\n") { "- ${it.texto}" } +
                            "\n\nO que levar:\n" + r.levar.joinToString("\n") { "- $it" } + "\n\n${Pcd.AVISO}"
                    )
                })
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Carteirinha digital
// ---------------------------------------------------------------------------------------------

@Composable
private fun CarteirinhaDoador(estado: Estado, d: Doador, u: Usuario, conquistas: Conquistas) {
    val contexto = LocalContext.current
    var emergencia by remember { mutableStateOf(true) }
    var mensagem by remember { mutableStateOf("") }
    val temContato = !d.contatoEmergenciaNome.isNullOrBlank() && !d.contatoEmergenciaTelefone.isNullOrBlank()
    val desde = estado.doacoesDe(d.id).firstOrNull()?.data
    val imagem = remember(d, u.nome, conquistas.totalDoacoes, emergencia) {
        desenharCarteirinha(u.nome, d.nomeSocial, d.tipoSanguineo, d.cidade, d.estado, conquistas, desde, d.foto, d.contatoEmergenciaNome, d.contatoEmergenciaTelefone, emergencia)
    }
    Cartao {
        Titulo("🪪 Minha carteirinha de doador")
        Espaco(4)
        Dica("Cartão com sua foto, tipo sanguíneo e nível, para guardar no celular. Ele se atualiza com o que você preenche na aba “Dados e privacidade”.")
        Espaco(10)
        Image(imagem.asImageBitmap(), "Carteirinha de doador de ${d.nomeSocial ?: u.nome}, tipo sanguíneo ${d.tipoSanguineo}", modifier = Modifier.fillMaxWidth())
        Espaco(10)
        Botao("⬇️ Salvar carteirinha na galeria", {
            mensagem = if (salvarNaGaleria(contexto, imagem, "carteirinha-hemare-${System.currentTimeMillis()}.png")) "✅ Salva em Imagens > Hemare." else "❌ Não consegui salvar a imagem neste aparelho."
        })
        if (temContato) { Espaco(4); CaixaMarcar("Mostrar o contato de emergência na carteirinha", emergencia, { emergencia = it }) }
        Mensagem(mensagem)
        Espaco(4)
        Dica("A carteirinha é montada no seu aparelho: nada é enviado a ninguém. Se for compartilhar a imagem, lembre que ela mostra seu nome${if (temContato && emergencia) " e o contato de emergência" else ""}.")
    }
}

// ---------------------------------------------------------------------------------------------
// Conta e segurança + LGPD
// ---------------------------------------------------------------------------------------------

@Composable
private fun LgpdESeguranca(store: HemareStore) {
    val contexto = LocalContext.current
    var excluindo by remember { mutableStateOf(false) }
    var senha by remember { mutableStateOf("") }
    var entendi by remember { mutableStateOf(false) }
    var aviso by remember { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SegurancaConta(store)
        Cartao {
            Titulo("🛡️ Meus dados (LGPD)")
            Espaco(4)
            Dica("Os dados são seus. Você pode levar uma cópia ou apagar tudo quando quiser.")
            Espaco(8)
            BotaoVazado("⬇️ Baixar meus dados", { compartilharTexto(contexto, store.exportarDados()); aviso = "✅ Arquivo gerado. Escolha onde salvar ou enviar." })
            Espaco(6)
            BotaoVazado("🗑️ Excluir minha conta", { excluindo = !excluindo; aviso = "" }, perigo = true)
            if (excluindo) {
                Espaco(8)
                Paragrafo("Seu nome, CPF, telefone, foto e demais dados pessoais serão apagados e você não poderá mais entrar. As doações já confirmadas ficam no registro de auditoria sem nenhum dado seu, porque não podem ser alteradas. Isso não pode ser desfeito.", suave = true)
                Espaco(6)
                Campo(senha, { senha = it }, "Digite sua senha para confirmar", senha = true)
                CaixaMarcar("Entendo que a exclusão é definitiva.", entendi, { entendi = it })
                BotaoVazado("Excluir definitivamente", { aviso = store.excluirConta(senha).texto }, habilitado = entendi && senha.isNotEmpty(), perigo = true)
            }
            Mensagem(aviso)
        }
    }
}
