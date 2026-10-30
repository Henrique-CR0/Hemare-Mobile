package com.example.hemaremobile.data

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import com.example.hemaremobile.domain.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

/**
 * A "loja" do Hemare Mobile: guarda o Estado (contas, doações, agenda...) e aplica as regras do domínio a cada operação.
 * Ainda não há backend publicado, então tudo vive em memória a partir dos dados de exemplo (Seed). Trocar isto por chamadas
 * HTTP à API do site é o próximo passo (Repository/Data layer) e não muda as telas.
 */
class HemareStore : ViewModel() {
    private val _estado = MutableStateFlow(Seed.criar())
    val estado: StateFlow<Estado> = _estado.asStateFlow()

    private val codigosRecuperacao = mutableMapOf<String, Triple<String, Long, Int>>() // email -> (código, validade, tentativas erradas)

    fun atual(): Estado = _estado.value
    fun definir(e: Estado) { _estado.value = e }

    /** Aplica uma mudança no estado de uma vez só. */
    inline fun mudar(bloco: (Estado) -> Estado) { definir(bloco(atual())) }

    // =====================================================================
    // Sessão
    // =====================================================================

    fun entrar(email: String, senha: String): Res {
        val e = atual()
        val mail = Auth.normalizarEmail(email)
        if (mail.isEmpty() || senha.isEmpty()) return Res.erro("Informe e-mail e senha.")
        val espera = Auth.bloqueio(mail)
        if (espera > 0) return Res.erro(Auth.mensagemDeEspera(espera))
        val u = e.usuarios.firstOrNull { it.email == mail && it.senha == senha }
        if (u == null) {
            Auth.registrarFalha(mail)
            return Res.erro("E-mail ou senha incorretos.")
        }
        Auth.limparFalhas(mail)
        definir(e.copy(sessaoUsuarioId = u.id, avisoSessao = null))
        return Res.ok("Bem-vindo(a), ${u.nome.split(" ").first()}!")
    }

    fun sair() {
        definir(atual().copy(sessaoUsuarioId = null, avisoSessao = null))
    }

    // =====================================================================
    // Cadastro
    // =====================================================================

    fun cadastrarDoador(nome: String, email: String, senha: String, codigoConvite: String? = null): Res {
        val e = atual()
        val n = Auth.validarNomeCadastro(nome)
        if (n is Validacao.Erro) return Res.erro(n.mensagem)
        val nomeLimpo = (n as Validacao.Ok).valor
        val mail = Auth.normalizarEmail(email)
        if (!Auth.emailValido(mail)) return Res.erro("Informe um e-mail válido.")
        val s = Auth.validarSenha(senha, mail, nomeLimpo)
        if (s is Validacao.Erro) return Res.erro(s.mensagem)
        if (e.usuarios.any { it.email == mail }) return Res.erro("Este e-mail já tem uma conta. Tente entrar.")

        val id = e.proximoId
        val indicador = codigoConvite?.trim()?.uppercase()?.let { c -> e.doadores.values.firstOrNull { it.codigoIndicacao == c }?.id }
        definir(
            e.copy(
                usuarios = e.usuarios + Usuario(id, nomeLimpo, mail, senha, TipoConta.DOADOR, Datas.hoje()),
                proximoId = id + 1,
                indicacoes = if (indicador != null) e.indicacoes + (id to indicador) else e.indicacoes
            )
        )
        return Res.ok("Conta criada!", id)
    }

    data class DadosHospital(
        val nome: String, val email: String, val senha: String, val cnpj: String, val cnes: String, val cep: String,
        val endereco: String, val numero: String, val bairro: String, val complemento: String, val cidade: String, val estado: String
    )

    /** Cadastra o hospital (fica pendente até um administrador aprovar) e já entra na conta, como no site. */
    fun cadastrarHospital(d: DadosHospital): Res {
        val e = atual()
        val obrigatorios = listOf(d.nome, d.cnpj, d.cnes, d.email, d.senha, d.cep, d.endereco, d.numero, d.bairro, d.cidade, d.estado)
        if (obrigatorios.any { it.isBlank() }) return Res.erro("Preencha todos os campos obrigatórios.")
        if (!Documentos.cnpjValido(d.cnpj)) return Res.erro("CNPJ inválido.")
        if (Documentos.digitos(d.cnes).length != 7) return Res.erro("O CNES tem 7 números.")
        val n = Auth.validarNomeCadastro(d.nome)
        if (n is Validacao.Erro) return Res.erro(n.mensagem)
        val mail = Auth.normalizarEmail(d.email)
        if (!Auth.emailValido(mail)) return Res.erro("Informe um e-mail válido.")
        val s = Auth.validarSenha(d.senha, mail, d.nome)
        if (s is Validacao.Erro) return Res.erro(s.mensagem)
        if (e.usuarios.any { it.email == mail }) return Res.erro("Este e-mail já tem uma conta. Tente entrar.")
        if (e.hospitais.values.any { Documentos.digitos(it.cnpj) == Documentos.digitos(d.cnpj) }) return Res.erro("Já existe um hospital cadastrado com este CNPJ.")

        val id = e.proximoId
        definir(
            e.copy(
                usuarios = e.usuarios + Usuario(id, (n as Validacao.Ok).valor, mail, d.senha, TipoConta.HOSPITAL, Datas.hoje()),
                hospitais = e.hospitais + (id to Hospital(
                    id, d.cnpj, Documentos.digitos(d.cnes), d.cep, d.endereco.trim(), d.numero.trim(), d.bairro.trim(),
                    d.complemento.trim().ifEmpty { null }, d.cidade.trim(), d.estado, aprovado = false, motivoRecusa = null
                )),
                proximoId = id + 1,
                sessaoUsuarioId = id
            )
        )
        return Res.ok("Hospital cadastrado! Um administrador vai conferir o CNPJ e o CNES.", id)
    }

    // =====================================================================
    // Recuperação de senha (sem e-mail real: o código aparece na tela, só para demonstração)
    // =====================================================================

    /** Gera o código de 6 dígitos. Devolve-o só se o e-mail existe; a mensagem ao usuário é sempre a mesma. */
    fun pedirCodigoRecuperacao(email: String): String? {
        val mail = Auth.normalizarEmail(email)
        if (atual().usuarios.none { it.email == mail }) return null
        val codigo = (100000 + java.security.SecureRandom().nextInt(900000)).toString()
        codigosRecuperacao[mail] = Triple(codigo, System.currentTimeMillis() + 15 * 60 * 1000L, 0)
        return codigo
    }

    fun redefinirSenha(email: String, codigo: String, novaSenha: String): Res {
        val e = atual()
        val mail = Auth.normalizarEmail(email)
        val reg = codigosRecuperacao[mail] ?: return Res.erro("Código inválido ou vencido. Peça um novo.")
        if (System.currentTimeMillis() > reg.second || reg.third >= 5) {
            codigosRecuperacao.remove(mail)
            return Res.erro("Código inválido ou vencido. Peça um novo.")
        }
        if (reg.first != codigo.trim()) {
            codigosRecuperacao[mail] = reg.copy(third = reg.third + 1)
            return Res.erro("Código incorreto.")
        }
        val u = e.usuarios.firstOrNull { it.email == mail } ?: return Res.erro("Código inválido ou vencido. Peça um novo.")
        val s = Auth.validarSenha(novaSenha, mail, u.nome)
        if (s is Validacao.Erro) return Res.erro(s.mensagem)
        codigosRecuperacao.remove(mail)
        Auth.limparFalhas(mail)
        definir(e.copy(usuarios = e.usuarios.map { if (it.id == u.id) it.copy(senha = novaSenha) else it }))
        return Res.ok("Senha redefinida! Agora é só entrar.")
    }

    fun alterarSenha(atualSenha: String, nova: String, confirmacao: String): Res {
        val e = atual()
        val u = e.usuario ?: return Res.erro("Entre na sua conta.")
        if (nova != confirmacao) return Res.erro("A confirmação não é igual à nova senha.")
        if (u.senha != atualSenha) return Res.erro("A senha atual está incorreta.")
        val s = Auth.validarSenha(nova, u.email, u.nome)
        if (s is Validacao.Erro) return Res.erro(s.mensagem)
        definir(e.copy(usuarios = e.usuarios.map { if (it.id == u.id) it.copy(senha = nova) else it }))
        return Res.ok("Senha alterada. As outras sessões foram encerradas.")
    }

    // =====================================================================
    // Perfil do doador
    // =====================================================================

    private fun gerarCodigoIndicacao(e: Estado): String {
        while (true) {
            val c = Raros.gerarCodigo()
            if (e.doadores.values.none { it.codigoIndicacao == c }) return c
        }
    }

    /** Cria ou atualiza o perfil do doador logado (foto: o Bitmap já reduzido, ou null para remover; só vale se fotoAlterada). */
    fun salvarPerfil(form: FormPerfil, foto: Bitmap?, fotoAlterada: Boolean): Res {
        val e = atual()
        val u = e.usuario ?: return Res.erro("Entre na sua conta.")
        if (u.tipo != TipoConta.DOADOR) return Res.erro("Só contas de doador têm perfil de doador.")
        val v = Perfil.validar(form)
        if (v is Validacao.Erro) return Res.erro(v.mensagem)
        val p = (v as Validacao.Ok).valor
        val antigo = e.doadores[u.id]

        val novo = Doador(
            id = u.id, tipoSanguineo = p.tipoSanguineo, sexo = p.sexo, genero = p.genero, nomeSocial = p.nomeSocial, cidade = p.cidade,
            estado = p.estado, dataNascimento = p.dataNascimento, pesoKg = p.pesoKg, telefone = p.telefone,
            cpf = p.cpf ?: antigo?.cpf, visibilidade = p.visibilidade,
            contatoEmergenciaNome = p.contatoEmergenciaNome, contatoEmergenciaTelefone = p.contatoEmergenciaTelefone,
            querLembrete = antigo?.querLembrete ?: false, codigoIndicacao = antigo?.codigoIndicacao ?: gerarCodigoIndicacao(e),
            indicadoPor = antigo?.indicadoPor ?: e.indicacoes[u.id], ultimoAlerta = antigo?.ultimoAlerta, pcd = antigo?.pcd ?: PcdInfo(),
            raro = antigo?.raro ?: RaroInfo(),
            foto = if (fotoAlterada) foto else antigo?.foto
        )
        definir(
            e.copy(
                doadores = e.doadores + (u.id to novo),
                usuarios = e.usuarios.map { if (it.id == u.id) it.copy(nome = p.nome) else it }
            )
        )
        return Res.ok(if (antigo == null) "Perfil criado! Obrigado por ajudar." else "Perfil atualizado.")
    }

    fun alternarLembrete(): Res {
        val e = atual()
        val d = e.doadorAtual ?: return Res.erro("Complete seu perfil primeiro.")
        val novo = !d.querLembrete
        definir(e.copy(doadores = e.doadores + (d.id to d.copy(querLembrete = novo))))
        return Res.ok(if (novo) "Pronto! Você recebe um aviso quando puder doar de novo." else "Lembrete desligado.")
    }

    /** "Baixar meus dados" (LGPD): tudo o que o Hemare guarda sobre o doador, em JSON. */
    fun exportarDados(): String {
        val e = atual()
        val u = e.usuario ?: return "{}"
        val d = e.doadores[u.id]
        val raiz = JSONObject()
        raiz.put("conta", JSONObject().put("nome", u.nome).put("email", u.email).put("criadoEm", u.criadoEm.toString()))
        if (d != null) {
            raiz.put(
                "perfil", JSONObject()
                    .put("nomeSocial", d.nomeSocial ?: JSONObject.NULL).put("tipoSanguineo", d.tipoSanguineo).put("sexo", d.sexo.toString())
                    .put("genero", d.genero ?: JSONObject.NULL).put("cidade", d.cidade).put("estado", d.estado ?: JSONObject.NULL)
                    .put("dataNascimento", d.dataNascimento?.toString() ?: JSONObject.NULL).put("pesoKg", d.pesoKg ?: JSONObject.NULL)
                    .put("telefone", d.telefone ?: JSONObject.NULL).put("cpf", d.cpf ?: JSONObject.NULL).put("visibilidade", d.visibilidade)
                    .put("contatoEmergenciaNome", d.contatoEmergenciaNome ?: JSONObject.NULL)
                    .put("contatoEmergenciaTelefone", d.contatoEmergenciaTelefone ?: JSONObject.NULL)
                    .put("querLembrete", d.querLembrete).put("codigoIndicacao", d.codigoIndicacao)
            )
            raiz.put("pcd", JSONObject().put("declarado", d.pcd.declarado).put("tipos", JSONArray(d.pcd.tipos)).put("apoios", JSONArray(d.pcd.apoios)))
            raiz.put("redeSangueRaro", JSONObject().put("fenotipo", d.raro.fenotipo ?: JSONObject.NULL).put("alcance", d.raro.alcance ?: JSONObject.NULL).put("status", d.raro.status ?: JSONObject.NULL))
        }
        val doacoes = JSONArray()
        e.doacoesDe(u.id).forEach { doacoes.put(JSONObject().put("data", it.data.toString()).put("hospital", e.hospitalNome(it.hospitalId)).put("selo", it.hash.take(12))) }
        raiz.put("doacoes", doacoes)
        val ags = JSONArray()
        e.agendamentos.filter { it.doadorId == u.id }.forEach { ag ->
            e.vagaPorId(ag.vagaId)?.let { ags.put(JSONObject().put("data", it.data.toString()).put("hora", it.hora).put("status", ag.status).put("hospital", e.hospitalNome(it.hospitalId))) }
        }
        raiz.put("agendamentos", ags)
        return raiz.toString(2)
    }

    /** Exclui a conta do doador: apaga os dados pessoais; as doações ficam na cadeia, sem nenhum dado da pessoa. */
    fun excluirConta(senha: String): Res {
        val e = atual()
        val u = e.usuario ?: return Res.erro("Entre na sua conta.")
        if (u.tipo != TipoConta.DOADOR) return Res.erro("Esta ação é só para contas de doador.")
        if (u.senha != senha) return Res.erro("Senha incorreta.")
        definir(
            e.copy(
                usuarios = e.usuarios.filter { it.id != u.id },
                doadores = e.doadores - u.id,
                doacoes = e.doacoes.map { if (it.doadorId == u.id) it.copy(doadorId = 0) else it },
                agendamentos = e.agendamentos.filter { it.doadorId != u.id || it.status != "agendado" }.map { if (it.doadorId == u.id) it.copy(doadorId = 0) else it },
                casos = e.casos.map { it.copy(padrinhos = it.padrinhos - u.id) },
                campanhas = e.campanhas.map { c -> c.copy(promessas = c.promessas.filter { it.doadorId != u.id || it.confirmada }.map { if (it.doadorId == u.id) it.copy(doadorId = 0) else it }) },
                sessaoUsuarioId = null
            )
        )
        return Res.ok("Conta excluída. Seus dados pessoais foram apagados.")
    }

    /** Declara (ou apaga) a condição de PcD no perfil: só o próprio doador vê. */
    fun salvarPcd(declarado: Boolean, tipos: List<String>, apoios: List<String>, consentimento: Boolean): Res {
        val e = atual()
        val d = e.doadorAtual ?: return Res.erro("Complete seu perfil primeiro.")
        val erro = Pcd.validarDeclaracao(declarado, tipos, consentimento)
        if (erro != null) return Res.erro(erro)
        val info = if (declarado) PcdInfo(true, tipos, apoios) else PcdInfo()
        definir(e.copy(doadores = e.doadores + (d.id to d.copy(pcd = info))))
        return Res.ok(if (declarado) "Salvo. Só você vê esta informação." else "Pronto: a informação foi apagada do seu perfil.")
    }

    // =====================================================================
    // Aviso de sessão (usado pelo login quando a sessão acaba)
    // =====================================================================

    fun limparAviso() {
        if (atual().avisoSessao != null) definir(atual().copy(avisoSessao = null))
    }
}

/** Atalho: o dia de hoje, para as telas. */
fun hoje(): LocalDate = Datas.hoje()
