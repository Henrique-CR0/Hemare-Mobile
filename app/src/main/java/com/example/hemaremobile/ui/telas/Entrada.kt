package com.example.hemaremobile.ui.telas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hemaremobile.data.HemareStore
import com.example.hemaremobile.data.TipoConta
import com.example.hemaremobile.domain.Auth
import com.example.hemaremobile.domain.Documentos
import com.example.hemaremobile.domain.Estados
import com.example.hemaremobile.ui.DadosPortao
import com.example.hemaremobile.ui.componentes.*
import com.example.hemaremobile.ui.theme.HemareVerde
import kotlinx.coroutines.delay
import com.example.hemaremobile.data.buscarCep

/** O resumo "o que cada acesso permite" mostrado no login e no cadastro (componentes/AcessoResumo.jsx). */
@Composable
fun AcessoResumo(destaque: TipoConta? = null) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Cartao {
            Titulo("🌐 Sem conta")
            Espaco(6)
            Paragrafo("• Ver onde doar e o termômetro de estoque\n• Ler notícias e a previsão do sangue\n• Guia da doação, mitos e seus direitos", suave = true)
        }
        Cartao {
            Titulo("🩸 Conta de doador" + if (destaque == TipoConta.DOADOR) "  ← você está aqui" else "")
            Espaco(6)
            Paragrafo("• Triagem \"Posso doar?\"\n• Agendar e acompanhar suas doações\n• Perfil, carteirinha e conquistas\n• Apadrinhar pacientes", suave = true)
        }
        Cartao {
            Titulo("🏥 Conta de hospital" + if (destaque == TipoConta.HOSPITAL) "  ← você está aqui" else "")
            Espaco(6)
            Paragrafo("• Estoque, necessidades e agenda\n• Campanhas e rede de sangue raro\n• Só depois de aprovada por um administrador", suave = true)
        }
    }
}

/** "Portão de acesso": o que o visitante vê ao abrir uma tela que exige conta (componentes/PortaoDeAcesso.jsx). */
@Composable
fun PortaoDeAcesso(
    p: DadosPortao, aoCriarConta: () -> Unit, aoEntrar: () -> Unit, aoCadastroHospital: () -> Unit, aoAbrir: (String) -> Unit
) {
    TelaLista("Conta necessária") {
        bloco {
            Text(p.icone, fontSize = 44.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            Espaco(8)
            Text(p.titulo, style = MaterialTheme.typography.titleLarge, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
            Espaco(8)
            Paragrafo(p.porque, suave = true)
            Espaco(16)
            Titulo("Com a sua conta gratuita você pode:")
            Espaco(6)
            p.beneficios.forEach { Paragrafo("✅ $it"); Espaco(4) }
            Espaco(14)
            Botao("Criar minha conta", aoCriarConta)
            Espaco(8)
            BotaoVazado("Já tenho conta", aoEntrar)
            Espaco(4)
            BotaoTexto("É um hospital ou hemocentro? Cadastre sua instituição", aoCadastroHospital, Modifier.fillMaxWidth())
            if (p.abertas.isNotEmpty()) {
                Espaco(10)
                Titulo("Sem conta você já pode ver:")
                Espaco(6)
                ChipsLinha { p.abertas.forEach { (rotulo, rota) -> Chip(rotulo, false) { aoAbrir(rota) } } }
            }
        }
    }
}

/** Aba "Entrar" do visitante. */
@Composable
fun ContaVisitanteScreen(aoEntrar: () -> Unit, aoCadastro: () -> Unit, aoCadastroHospital: () -> Unit) {
    TelaLista("Entrar") {
        bloco {
            Text("🩸", fontSize = 44.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            Text("Entre no Hemare", style = MaterialTheme.typography.titleLarge, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            Espaco(4)
            Paragrafo("Sem conta você já vê o site público. Com conta, agenda doações, faz a triagem e acompanha suas conquistas.", suave = true, centro = true)
            Espaco(16)
            Botao("Entrar", aoEntrar)
            Espaco(8)
            BotaoVazado("Criar conta de doador", aoCadastro)
            Espaco(4)
            BotaoTexto("É um hospital ou hemocentro? Cadastre sua instituição", aoCadastroHospital, Modifier.fillMaxWidth())
            Espaco(10)
            AcessoResumo()
        }
    }
}

@Composable
fun LoginScreen(
    store: HemareStore, destino: String?, aoEntrou: (String?) -> Unit, aoCadastro: () -> Unit, aoCadastroHospital: () -> Unit, aoRecuperar: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var senha by remember { mutableStateOf("") }
    var mensagem by remember { mutableStateOf("") }

    TelaLista("Entrar no Hemare") {
        bloco {
            Text("🩸", fontSize = 44.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            Paragrafo("Bem-vindo(a) de volta! Acesse sua conta.", suave = true, centro = true)
            Espaco(16)
            Campo(email, { email = it }, "Email", teclado = KeyboardType.Email)
            Espaco(8)
            Campo(senha, { senha = it }, "Senha", senha = true)
            Espaco(12)
            Botao("Entrar", {
                val r = store.entrar(email, senha)
                if (r.ok) aoEntrou(destino) else mensagem = r.texto
            })
            Mensagem(mensagem)
            BotaoTexto("Esqueceu a senha?", aoRecuperar, Modifier.fillMaxWidth())
            BotaoTexto("Não tem conta? Cadastre-se", aoCadastro, Modifier.fillMaxWidth())
            BotaoTexto("É um hospital ou hemocentro? Cadastre sua instituição", aoCadastroHospital, Modifier.fillMaxWidth())
            Espaco(8)
            Dica("Contas de teste — doador: doador@hemare.com / doador123 · hospital: hospital@hemare.com / hospital123 · pendente: pendente@hemare.com / hospital123 · admin: admin@hemare.com / admin1234")
            Espaco(12)
            AcessoResumo()
        }
    }
}

@Composable
fun CadastroScreen(store: HemareStore, destino: String?, convite: String?, aoCriada: () -> Unit, aoLogin: () -> Unit) {
    var nome by remember { mutableStateOf("") }
    var avisoNome by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var emailTocado by remember { mutableStateOf(false) }
    var senha by remember { mutableStateOf("") }
    var mensagem by remember { mutableStateOf("") }
    var criada by remember { mutableStateOf(false) }
    var codigoConvite by remember { mutableStateOf(convite ?: "") }

    val emailValido = Auth.emailValido(Auth.normalizarEmail(email))
    val forca = Auth.forcaSenha(senha)

    LaunchedEffect(criada) {
        if (criada) { delay(1400); aoCriada() }
    }

    TelaLista("Criar sua conta") {
        bloco {
            Text("🩸", fontSize = 44.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            Paragrafo("Junte-se ao Hemare e ajude a salvar vidas.", suave = true, centro = true)
            if (codigoConvite.isNotBlank()) {
                Espaco(8)
                Aviso("💛 Você foi convidado(a) por um amigo. Que bom ter você aqui!", HemareVerde)
            }
            Espaco(16)
            Campo(
                nome, { v ->
                    avisoNome = if (v.any { it.isDigit() }) "O nome não pode conter números." else ""
                    nome = v.filterNot { it.isDigit() }
                }, "Nome completo", aviso = avisoNome
            )
            Espaco(8)
            Campo(
                email, { email = it; emailTocado = true }, "Email", teclado = KeyboardType.Email,
                aviso = if (emailTocado && email.isNotEmpty() && !emailValido) "Email inválido — verifique o \"@\" e o ponto." else ""
            )
            Espaco(8)
            Campo(senha, { senha = it }, "Senha (mín. 8 caracteres)", senha = true)
            if (senha.isNotEmpty()) {
                Espaco(6)
                BarraForca(forca)
            }
            Espaco(8)
            Campo(codigoConvite, { codigoConvite = it.uppercase().take(12) }, "Código de convite (opcional)", dica = "Se um amigo chamou você, digite o código dele.")
            Espaco(12)
            Botao("Cadastrar", {
                emailTocado = true
                if (nome.isBlank() || email.isBlank() || senha.isBlank()) { mensagem = "❌ Preencha todos os campos."; return@Botao }
                if (!emailValido) { mensagem = "❌ Digite um email válido (ex: nome@email.com)."; return@Botao }
                if (senha.length < 8) { mensagem = "❌ A senha deve ter pelo menos 8 caracteres."; return@Botao }
                val r = store.cadastrarDoador(nome, email, senha, codigoConvite.ifBlank { null })
                mensagem = if (r.ok) "✅ Conta criada! Redirecionando para o login..." else r.texto
                if (r.ok) criada = true
            })
            Mensagem(mensagem)
            BotaoTexto("Já tem conta? Entrar", aoLogin, Modifier.fillMaxWidth())
            Espaco(8)
            AcessoResumo(TipoConta.DOADOR)
        }
    }
}

@Composable
fun BarraForca(forca: Int) {
    val cores = listOf(CorAlerta, CorAlerta, CorAviso, HemareVerde)
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(3) { i ->
            Box(
                Modifier.size(width = 44.dp, height = 6.dp).clip(RoundedCornerShape(3.dp))
                    .background(if (i < forca) cores[forca] else MaterialTheme.colorScheme.surfaceVariant)
            )
        }
        Text(Auth.ROTULOS_FORCA[forca], style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun CadastroHospitalScreen(store: HemareStore, aoCadastrado: () -> Unit, aoLogin: () -> Unit) {
    var nome by remember { mutableStateOf("") }
    var cnpj by remember { mutableStateOf("") }
    var cnes by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var senha by remember { mutableStateOf("") }
    var cep by remember { mutableStateOf("") }
    var endereco by remember { mutableStateOf("") }
    var numero by remember { mutableStateOf("") }
    var bairro by remember { mutableStateOf("") }
    var complemento by remember { mutableStateOf("") }
    var cidade by remember { mutableStateOf("") }
    var estado by remember { mutableStateOf("") }
    var mensagem by remember { mutableStateOf("") }
    var buscando by remember { mutableStateOf(false) }

    // Busca o endereço pelo CEP (ViaCEP), como o site faz ao sair do campo.
    LaunchedEffect(cep) {
        if (Documentos.digitos(cep).length == 8) {
            buscando = true
            val achado = buscarCep(cep)
            buscando = false
            if (achado != null) {
                if (achado.logradouro.isNotBlank()) endereco = achado.logradouro
                if (achado.bairro.isNotBlank()) bairro = achado.bairro
                if (achado.cidade.isNotBlank()) cidade = achado.cidade
                if (achado.uf.isNotBlank()) estado = achado.uf
            }
        }
    }

    TelaLista("Cadastrar hospital") {
        bloco {
            Text("🏥", fontSize = 44.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            Paragrafo("Cadastre sua instituição e encontre doadores.", suave = true, centro = true)
            Espaco(16)
            Campo(nome, { nome = it }, "Nome do hospital *"); Espaco(8)
            Campo(cnpj, { cnpj = Documentos.mascaraCnpj(it) }, "CNPJ *", teclado = KeyboardType.Number, placeholder = "00.000.000/0000-00"); Espaco(8)
            Campo(cnes, { cnes = Documentos.digitos(it).take(7) }, "CNES *", teclado = KeyboardType.Number); Espaco(8)
            Campo(email, { email = it }, "Email institucional *", teclado = KeyboardType.Email); Espaco(8)
            Campo(senha, { senha = it }, "Senha (mín. 8 caracteres) *", senha = true); Espaco(8)
            Campo(cep, { cep = Documentos.mascaraCep(it) }, "CEP *", teclado = KeyboardType.Number, placeholder = "00000-000", dica = if (buscando) "Buscando o endereço..." else ""); Espaco(8)
            Campo(endereco, { endereco = it }, "Endereço (rua) *"); Espaco(8)
            Campo(numero, { numero = it }, "Número *"); Espaco(8)
            Campo(bairro, { bairro = it }, "Bairro *"); Espaco(8)
            Campo(complemento, { complemento = it }, "Complemento (opcional)"); Espaco(8)
            Campo(cidade, { cidade = it }, "Cidade *"); Espaco(8)
            CampoSelecao("Estado (UF) *", estado, Estados.SIGLAS.map { it to it }, { estado = it })
            Espaco(14)
            Botao("Cadastrar hospital", {
                val r = store.cadastrarHospital(HemareStore.DadosHospital(nome, email, senha, cnpj, cnes, cep, endereco, numero, bairro, complemento, cidade, estado))
                mensagem = if (r.ok) "✅ Hospital cadastrado! Redirecionando..." else r.texto
                if (r.ok) aoCadastrado()
            })
            Mensagem(mensagem)
            BotaoTexto("É doador? Cadastre-se aqui", aoLogin, Modifier.fillMaxWidth())
            Espaco(8)
            AcessoResumo(TipoConta.HOSPITAL)
        }
    }
}

@Composable
fun RecuperarSenhaScreen(store: HemareStore, aoVoltarLogin: () -> Unit) {
    var etapa by remember { mutableStateOf(1) }
    var email by remember { mutableStateOf("") }
    var codigo by remember { mutableStateOf("") }
    var novaSenha by remember { mutableStateOf("") }
    var mensagem by remember { mutableStateOf("") }
    var codigoDemo by remember { mutableStateOf<String?>(null) }
    var pronto by remember { mutableStateOf(false) }

    LaunchedEffect(pronto) { if (pronto) { delay(1600); aoVoltarLogin() } }

    TelaLista("Recuperar senha") {
        bloco {
            Text("🔑", fontSize = 44.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            Espaco(8)
            if (etapa == 1) {
                Paragrafo("Digite seu email para receber um código de recuperação.", suave = true, centro = true)
                Espaco(12)
                Campo(email, { email = it }, "Seu email", teclado = KeyboardType.Email)
                Espaco(12)
                Botao("Enviar código", {
                    if (email.isBlank()) { mensagem = "❌ Informe seu email."; return@Botao }
                    codigoDemo = store.pedirCodigoRecuperacao(email)
                    mensagem = "✅ Se esse e-mail estiver cadastrado, enviamos um código de 6 dígitos."
                    etapa = 2
                })
            } else {
                Paragrafo("Digite o código que enviamos para $email e sua nova senha.", suave = true, centro = true)
                if (codigoDemo != null) {
                    Espaco(8)
                    Aviso("🧪 Modo demonstração: o app ainda não envia e-mails reais. Seu código é $codigoDemo.")
                }
                Espaco(12)
                Campo(codigo, { codigo = Documentos.digitos(it).take(6) }, "Código de 6 dígitos", teclado = KeyboardType.Number); Espaco(8)
                Campo(novaSenha, { novaSenha = it }, "Nova senha (mín. 8 caracteres)", senha = true)
                Espaco(12)
                Botao("Redefinir senha", {
                    if (codigo.isBlank() || novaSenha.isBlank()) { mensagem = "❌ Preencha o código e a nova senha."; return@Botao }
                    val r = store.redefinirSenha(email, codigo, novaSenha)
                    mensagem = r.texto
                    if (r.ok) pronto = true
                })
                BotaoTexto("← Não recebi o código", { etapa = 1; mensagem = "" }, Modifier.fillMaxWidth())
            }
            Mensagem(mensagem)
            BotaoTexto("Lembrou a senha? Entrar", aoVoltarLogin, Modifier.fillMaxWidth())
        }
    }
}
