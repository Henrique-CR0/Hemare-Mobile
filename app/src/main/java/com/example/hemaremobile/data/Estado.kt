package com.example.hemaremobile.data

import android.graphics.Bitmap
import com.example.hemaremobile.domain.*
import java.time.LocalDate

enum class TipoConta { DOADOR, HOSPITAL, ADMIN }

/** Resultado de uma operação da loja: ok + a frase que a tela mostra. */
data class Res(val ok: Boolean, val msg: String, val id: Int = 0) {
    /** A frase como o site mostra: ✅ para sucesso, ❌ para erro. */
    val texto: String get() = (if (ok) "✅ " else "❌ ") + msg

    companion object {
        fun ok(msg: String = "", id: Int = 0) = Res(true, msg, id)
        fun erro(msg: String) = Res(false, msg)
    }
}

data class Usuario(val id: Int, val nome: String, val email: String, val senha: String, val tipo: TipoConta, val criadoEm: LocalDate)

data class PcdInfo(val declarado: Boolean = false, val tipos: List<String> = emptyList(), val apoios: List<String> = emptyList())

data class RaroInfo(
    val fenotipo: String? = null, val alcance: String? = null, val consentimento: Boolean = false,
    val status: String? = null, // declarado | confirmado
    val codigo: String? = null, val ultimaNotificacao: LocalDate? = null
) {
    val participa: Boolean get() = fenotipo != null && consentimento
}

data class Doador(
    val id: Int, // igual ao id do usuário
    val tipoSanguineo: String, val sexo: Char, val genero: String?, val nomeSocial: String?, val cidade: String, val estado: String?,
    val dataNascimento: LocalDate?, val pesoKg: Int?, val telefone: String?, val cpf: String?, val visibilidade: String,
    val contatoEmergenciaNome: String?, val contatoEmergenciaTelefone: String?, val querLembrete: Boolean, val codigoIndicacao: String,
    val indicadoPor: Int?, val ultimoAlerta: LocalDate?, val pcd: PcdInfo, val raro: RaroInfo, val foto: Bitmap?
)

data class Hospital(
    val id: Int, // igual ao id do usuário
    val cnpj: String, val cnes: String, val cep: String, val endereco: String, val numero: String, val bairro: String,
    val complemento: String?, val cidade: String, val estado: String, val aprovado: Boolean, val motivoRecusa: String?,
    val lat: Double? = null, val lng: Double? = null
) {
    val status: StatusVerificacao
        get() = when {
            aprovado -> StatusVerificacao.APROVADO
            !motivoRecusa.isNullOrBlank() -> StatusVerificacao.RECUSADO
            else -> StatusVerificacao.PENDENTE
        }
}

data class LocalDoacao(
    val id: Int, val nome: String, val cidade: String, val estado: String, val endereco: String?, val telefone: String?,
    val lat: Double?, val lng: Double?
)

data class Doacao(val id: Int, val doadorId: Int, val hospitalId: Int, val data: LocalDate, val hash: String, val emCampanha: Boolean = false)
data class Necessidade(val id: Int, val hospitalId: Int, val tipo: String, val urgencia: String, val status: String)
data class Vaga(val id: Int, val hospitalId: Int, val data: LocalDate, val hora: String, val capacidade: Int)
data class AgendamentoReg(val id: Int, val vagaId: Int, val doadorId: Int, val status: String)

data class Caso(
    val id: Int, val hospitalId: Int, val apelido: String, val tipo: String, val condicao: String, val frequenciaDias: Int,
    val meta: Int, val ativo: Boolean, val ultimaChamada: LocalDate?, val padrinhos: List<Int>
)

data class Promessa(val doadorId: Int, val data: LocalDate, val confirmada: Boolean)
data class CampanhaReg(
    val codigo: String, val hospitalId: Int, val apelido: String, val metaBolsas: Int, val expiraEm: LocalDate, val ativo: Boolean,
    val promessas: List<Promessa>
)

data class ConvocadoRaro(val doadorId: Int, val resposta: String?, val doacaoConfirmada: Boolean)
data class PedidoRaro(
    val id: Int, val hospitalId: Int, val fenotipo: String, val motivo: String, val apelidoPaciente: String?,
    val criadoEmMs: Long, val expiraEmMs: Long, val aberto: Boolean, val convocados: List<ConvocadoRaro>
)

/** Tudo o que o app sabe, numa foto só. A loja troca esta foto por uma nova a cada operação. */
data class Estado(
    val usuarios: List<Usuario>,
    val doadores: Map<Int, Doador>,
    val hospitais: Map<Int, Hospital>,
    val locais: List<LocalDoacao>,
    val doacoes: List<Doacao>,
    val estoque: Map<Int, Map<String, String>>, // hospitalId -> tipo -> nível
    val necessidades: List<Necessidade>,
    val vagas: List<Vaga>,
    val agendamentos: List<AgendamentoReg>,
    val casos: List<Caso>,
    val campanhas: List<CampanhaReg>,
    val pedidosRaros: List<PedidoRaro>,
    val noticias: List<Noticia>,
    val noticiasOcultas: Set<Int>,
    val coleta: ResultadoColeta?,
    val sessaoUsuarioId: Int?,
    val proximoId: Int,
    val avisoSessao: String? = null,
    val indicacoes: Map<Int, Int> = emptyMap(), // usuário recém-criado -> quem o convidou (vale quando completar o perfil)
    /** Termômetro baixado do servidor do Hemare (hospitais que não existem neste aparelho). */
    val estoqueExterno: List<ItemEstoque> = emptyList(),
    val servidor: String? = null,
    /** Avisos do Android (lembrete de retorno e doação agendada). Desligado = nenhum aviso é agendado. */
    val notificacoes: Boolean = true,
    val sincronizadoEm: Long? = null
) {
    // ---------- sessão ----------
    val usuario: Usuario? get() = usuarios.firstOrNull { it.id == sessaoUsuarioId }
    val papel: TipoConta? get() = usuario?.tipo
    val doadorAtual: Doador? get() = sessaoUsuarioId?.let { doadores[it] }
    val hospitalAtual: Hospital? get() = sessaoUsuarioId?.let { hospitais[it] }

    fun usuarioPorId(id: Int): Usuario? = usuarios.firstOrNull { it.id == id }
    fun nomeDe(id: Int): String = usuarioPorId(id)?.nome ?: "Doador"

    /** Nome social quando existe (direito garantido no atendimento do SUS). */
    fun nomeExibido(id: Int): String = doadores[id]?.nomeSocial?.takeIf { it.isNotBlank() } ?: nomeDe(id)

    fun hospitalNome(id: Int): String = usuarioPorId(id)?.nome ?: "Hospital"

    // ---------- doações ----------
    fun doacoesDe(doadorId: Int): List<Doacao> = doacoes.filter { it.doadorId == doadorId }.sortedBy { it.data }
    fun ultimaDoacaoDe(doadorId: Int): LocalDate? = doacoesDe(doadorId).lastOrNull()?.data
    fun totalDoacoes(doadorId: Int): Int = doacoesDe(doadorId).size

    // ---------- candidatos (para match, alerta, radar) ----------
    fun candidato(d: Doador): Candidato =
        Candidato(d.id, d.tipoSanguineo, d.sexo, d.cidade, d.estado, d.visibilidade, ultimaDoacaoDe(d.id), d.ultimoAlerta)

    fun candidatos(): List<Candidato> = doadores.values.map { candidato(it) }

    // ---------- gamificação ----------
    fun afilhadosDe(doadorId: Int): List<Caso> = casos.filter { it.ativo && it.padrinhos.contains(doadorId) }
    fun amigosQueDoaram(doadorId: Int): Int = doadores.values.count { it.indicadoPor == doadorId && totalDoacoes(it.id) >= 1 }
    fun amigosCadastrados(doadorId: Int): Int = doadores.values.count { it.indicadoPor == doadorId }

    fun conquistas(doadorId: Int, hoje: LocalDate = Datas.hoje()): Conquistas {
        val d = doadores[doadorId]
        val lista = doacoesDe(doadorId)
        return Gamificacao.calcular(
            DadosGamificacao(
                totalDoacoes = lista.size,
                doacoesUltimoAno = Agendamento.doacoesNosUltimos12Meses(lista.map { it.data }, hoje),
                tipoSanguineo = d?.tipoSanguineo, visibilidade = d?.visibilidade ?: "anonimo",
                amigosQueDoaram = amigosQueDoaram(doadorId), afilhados = afilhadosDe(doadorId).size,
                doacoesEmCampanha = lista.count { it.emCampanha },
                redeRara = d?.raro?.status == "confirmado" && d.raro.consentimento
            )
        )
    }

    // ---------- placar ----------
    fun placar(): Placar {
        val linhas = doadores.values.groupBy { Textos.limpar(it.cidade) }.map { (cidade, lista) ->
            LinhaPlacar(cidade, lista.size, lista.sumOf { totalDoacoes(it.id) })
        }
        return Gamificacao.montarPlacar(linhas)
    }

    // ---------- agenda ----------
    fun vagaPorId(id: Int): Vaga? = vagas.firstOrNull { it.id == id }
    fun reservadas(vagaId: Int): Int = agendamentos.count { it.vagaId == vagaId && it.status in listOf("agendado", "compareceu") }
    fun agendamentoAtivoDe(doadorId: Int): AgendamentoReg? {
        val agora = System.currentTimeMillis()
        return agendamentos.firstOrNull { ag ->
            ag.doadorId == doadorId && ag.status == "agendado" && vagaPorId(ag.vagaId)?.let { Agendamento.momentoDaVaga(it.data, it.hora) > agora } == true
        }
    }

    fun historicoDe(doadorId: Int): Agendamento.Historico {
        val doacoesH = doacoesDe(doadorId).map { Agendamento.ItemDoacao(it.data, hospitalNome(it.hospitalId), hospitais[it.hospitalId]?.cidade ?: "", it.hash) }
        val ags = agendamentos.filter { it.doadorId == doadorId }.mapNotNull { ag ->
            val v = vagaPorId(ag.vagaId) ?: return@mapNotNull null
            Agendamento.ItemAgendamento(ag.id, ag.status, v.data, v.hora, hospitalNome(v.hospitalId), hospitais[v.hospitalId]?.cidade ?: "")
        }
        return Agendamento.montarHistorico(doacoesH, ags, doadores[doadorId]?.sexo)
    }

    // ---------- campanhas ----------
    fun campanhaPorCodigo(codigo: String): CampanhaReg? = campanhas.firstOrNull { it.codigo.equals(codigo.trim(), ignoreCase = true) }

    fun situacaoCampanha(c: CampanhaReg): Campanha.Situacao =
        Campanha.situacao(c.metaBolsas, c.promessas.count { it.confirmada }, c.promessas.size, c.expiraEm, c.ativo)

    // ---------- verificação de hospitais ----------
    fun hospitaisAprovados(): List<Hospital> = hospitais.values.filter { it.aprovado }
}

/** Resultado da última coleta de notícias (para o painel do administrador). */
data class ResultadoColeta(val terminouEmMs: Long, val fontes: List<ResultadoFonte>)
data class ResultadoFonte(val id: String, val ok: Boolean, val lidos: Int, val novos: Int, val erro: String?)
