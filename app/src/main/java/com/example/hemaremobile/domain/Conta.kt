package com.example.hemaremobile.domain

import java.time.LocalDate

/** Resultado de uma validação: ou deu erro (com a frase para mostrar) ou deu certo. */
sealed class Validacao<out T> {
    data class Erro(val mensagem: String) : Validacao<Nothing>()
    data class Ok<T>(val valor: T) : Validacao<T>()
}

/** Regras de autenticação (backend/regras/auth.js). */
object Auth {
    const val SENHA_MIN = 8
    private const val SENHA_MAX_BYTES = 72
    private const val NOME_MIN = 2
    private const val NOME_MAX = 120
    private const val EMAIL_MAX = 120

    private val SENHAS_FRACAS = listOf(
        "12345678", "123456789", "1234567890", "87654321", "11111111", "00000000", "12341234", "123123123", "abcd1234", "abc12345",
        "qwertyui", "qwerty123", "password", "password1", "senha123", "senha1234", "senhasenha", "mudar123", "hemare123", "brasil123"
    )

    fun normalizarEmail(texto: String?): String = (texto ?: "").trim().lowercase()

    fun emailValido(email: String): Boolean =
        email.length <= EMAIL_MAX && Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]{2,}$").matches(email)

    /** Força visual da senha: 0 a 3 (igual ao medidor do site). */
    fun forcaSenha(senha: String): Int {
        var f = 0
        if (senha.length >= 8) f++
        if (senha.any { it.isUpperCase() } && senha.any { it.isLowerCase() }) f++
        if (senha.any { it.isDigit() } && senha.any { !it.isLetterOrDigit() }) f++
        return f
    }

    val ROTULOS_FORCA = listOf("", "Fraca", "Média", "Forte")

    /** Confere a senha. nome/email: senha igual a um deles é recusada. */
    fun validarSenha(senha: String, email: String = "", nome: String = ""): Validacao<Unit> {
        if (senha.isEmpty()) return Validacao.Erro("Informe a senha.")
        if (senha.length < SENHA_MIN) return Validacao.Erro("A senha deve ter pelo menos $SENHA_MIN caracteres.")
        if (senha.toByteArray(Charsets.UTF_8).size > SENHA_MAX_BYTES) {
            return Validacao.Erro("A senha é longa demais (máximo de $SENHA_MAX_BYTES bytes).")
        }
        val minuscula = senha.lowercase()
        if (SENHAS_FRACAS.contains(minuscula) || senha.all { it == senha[0] }) {
            return Validacao.Erro("Essa senha é fácil demais de adivinhar. Escolha outra.")
        }
        val e = normalizarEmail(email)
        val n = nome.trim().lowercase()
        if ((e.isNotEmpty() && (minuscula == e || minuscula == e.substringBefore("@"))) || (n.isNotEmpty() && minuscula == n)) {
            return Validacao.Erro("A senha não pode ser igual ao seu e-mail ou ao seu nome.")
        }
        return Validacao.Ok(Unit)
    }

    /** Valida o nome do cadastro (2 a 120 caracteres, sem < ou >). Devolve o nome limpo. */
    fun validarNomeCadastro(nome: String): Validacao<String> {
        val n = nome.replace(Regex("\\s+"), " ").trim()
        if (n.length < NOME_MIN || n.length > NOME_MAX || n.any { it.code < 32 || it == '<' || it == '>' }) {
            return Validacao.Erro("Informe o nome (de $NOME_MIN a $NOME_MAX caracteres, sem símbolos como < ou >).")
        }
        return Validacao.Ok(n)
    }

    // ----- limite de tentativas (8 erros em 15 min travam o login da conta) -----
    private const val MAX_TENTATIVAS = 8
    private const val JANELA_MS = 15 * 60 * 1000L
    private val falhas = mutableMapOf<String, Pair<Int, Long>>() // chave -> (quantidade, desde)

    fun bloqueio(chave: String, agora: Long = System.currentTimeMillis()): Int {
        val v = falhas[chave] ?: return 0
        if (agora - v.second >= JANELA_MS) { falhas.remove(chave); return 0 }
        if (v.first < MAX_TENTATIVAS) return 0
        return maxOf(1, Math.ceil((v.second + JANELA_MS - agora) / 1000.0).toInt())
    }

    fun registrarFalha(chave: String, agora: Long = System.currentTimeMillis()) {
        val v = falhas[chave]
        if (v == null || agora - v.second >= JANELA_MS) falhas[chave] = 1 to agora else falhas[chave] = (v.first + 1) to v.second
    }

    fun limparFalhas(chave: String) { falhas.remove(chave) }

    fun mensagemDeEspera(segundos: Int): String {
        val minutos = Math.ceil(segundos / 60.0).toInt()
        return "Muitas tentativas. Tente de novo em " + (if (minutos <= 1) "alguns instantes" else "$minutos minutos") + "."
    }
}

/** Dados que o formulário do perfil do doador envia. */
data class FormPerfil(
    val nome: String,
    val nomeSocial: String,
    val tipoSanguineo: String,
    val sexo: Char?,
    val genero: String,
    val cidade: String,
    val estado: String,
    val dataNascimento: String, // "dd/MM/aaaa" ou ""
    val pesoKg: String,
    val telefone: String,
    val cpf: String?, // null = não mudou
    val visibilidade: String,
    val contatoEmergenciaNome: String,
    val contatoEmergenciaTelefone: String
)

data class PerfilValidado(
    val nome: String,
    val nomeSocial: String?,
    val tipoSanguineo: String,
    val sexo: Char,
    val genero: String?,
    val cidade: String,
    val estado: String?,
    val dataNascimento: LocalDate?,
    val pesoKg: Int?,
    val telefone: String?,
    val cpf: String?,
    val visibilidade: String,
    val contatoEmergenciaNome: String?,
    val contatoEmergenciaTelefone: String?
)

data class ItemCompletude(val campo: String, val rotulo: String, val peso: Int)
data class Completude(val porcentagem: Int, val faltando: List<ItemCompletude>)

data class VisaoHospital(val identificado: Boolean, val ve: List<Pair<String, String>>, val naoVe: List<String>)

/** Regras do perfil do doador (backend/regras/perfil.js). */
object Perfil {
    const val RH_NULO = "Rh nulo (sangue dourado)"
    val TIPOS_SANGUINEOS = listOf("O-", "O+", "A-", "A+", "B-", "B+", "AB-", "AB+", RH_NULO)

    val GENEROS = listOf(
        "mulher" to "Mulher", "homem" to "Homem", "mulher-trans" to "Mulher trans", "homem-trans" to "Homem trans",
        "nao-binario" to "Não binário", "outro" to "Outro", "nao-informar" to "Prefiro não informar"
    )

    fun idadeEm(nascimento: LocalDate, hoje: LocalDate = Datas.hoje()): Int {
        return java.time.Period.between(nascimento, hoje).years
    }

    private fun validarNome(texto: String, rotulo: String, obrigatorio: Boolean): Validacao<String?> {
        val nome = Textos.limpar(texto)
        if (nome.isEmpty()) return if (obrigatorio) Validacao.Erro("Informe $rotulo.") else Validacao.Ok(null)
        val inicial = rotulo.replaceFirstChar { it.uppercase() }
        if (nome.length < 2 || nome.length > 120) return Validacao.Erro("$inicial deve ter de 2 a 120 caracteres.")
        if (nome.any { it.isDigit() } || !Regex("^\\p{L}[\\p{L} '.-]*$").matches(nome)) {
            return Validacao.Erro("$inicial só pode ter letras, espaços, hífen, apóstrofo e ponto.")
        }
        return Validacao.Ok(nome)
    }

    fun validar(f: FormPerfil, hoje: LocalDate = Datas.hoje()): Validacao<PerfilValidado> {
        val nome = validarNome(f.nome, "seu nome", true)
        if (nome is Validacao.Erro) return nome
        val nomeSocial = validarNome(f.nomeSocial, "o nome social", false)
        if (nomeSocial is Validacao.Erro) return nomeSocial

        if (!TIPOS_SANGUINEOS.contains(f.tipoSanguineo)) return Validacao.Erro("Escolha o tipo sanguíneo.")
        val sexo = f.sexo ?: return Validacao.Erro("Escolha o sexo usado no intervalo entre doações (M ou F).")

        val cidade = Textos.limpar(f.cidade)
        if (cidade.length < 2 || cidade.length > 100) return Validacao.Erro("Informe sua cidade.")

        var estado: String? = null
        if (f.estado.isNotBlank()) {
            estado = f.estado.trim().uppercase()
            if (!Estados.SIGLAS.contains(estado)) return Validacao.Erro("Estado inválido.")
        }

        var genero: String? = null
        if (f.genero.isNotBlank()) {
            if (GENEROS.none { it.first == f.genero }) return Validacao.Erro("Escolha uma opção de gênero da lista.")
            genero = f.genero
        }

        var nascimento: LocalDate? = null
        if (f.dataNascimento.isNotBlank()) {
            nascimento = Datas.lerDmy(f.dataNascimento) ?: return Validacao.Erro("Data de nascimento inválida.")
            if (nascimento.isAfter(hoje) || idadeEm(nascimento, hoje) > 110) return Validacao.Erro("Confira a data de nascimento.")
        }

        var peso: Int? = null
        if (f.pesoKg.isNotBlank()) {
            peso = f.pesoKg.trim().toIntOrNull()
            if (peso == null || peso < 20 || peso > 300) return Validacao.Erro("O peso deve ser um número inteiro de kg (20 a 300).")
        }

        var telefone: String? = null
        if (f.telefone.isNotBlank()) {
            val d = Documentos.digitos(f.telefone)
            if (d.length < 10 || d.length > 11) return Validacao.Erro("Telefone inválido: use DDD + número.")
            telefone = Documentos.formatarTelefone(d)
        }

        var cpf: String? = null
        if (!f.cpf.isNullOrBlank()) {
            if (!Documentos.cpfValido(f.cpf)) return Validacao.Erro("CPF inválido. Confira os números.")
            cpf = Documentos.formatarCpf(f.cpf)
        }

        val visibilidade = if (f.visibilidade == "identificado") "identificado" else "anonimo"
        if (visibilidade == "identificado" && telefone == null) {
            return Validacao.Erro("Para aparecer aos hospitais, informe um telefone de contato.")
        }

        val emNome = validarNome(f.contatoEmergenciaNome, "o nome do contato de emergência", false)
        if (emNome is Validacao.Erro) return emNome
        var emTel: String? = null
        if (f.contatoEmergenciaTelefone.isNotBlank()) {
            val d = Documentos.digitos(f.contatoEmergenciaTelefone)
            if (d.length < 10 || d.length > 11) return Validacao.Erro("Telefone do contato de emergência inválido.")
            emTel = Documentos.formatarTelefone(d)
        }
        val emNomeValor = (emNome as Validacao.Ok).valor
        if ((emNomeValor == null) != (emTel == null)) {
            return Validacao.Erro("Para o contato de emergência, informe nome e telefone (ou deixe os dois vazios).")
        }

        return Validacao.Ok(
            PerfilValidado(
                nome = (nome as Validacao.Ok).valor!!, nomeSocial = (nomeSocial as Validacao.Ok).valor, tipoSanguineo = f.tipoSanguineo,
                sexo = sexo, genero = genero, cidade = cidade, estado = estado, dataNascimento = nascimento, pesoKg = peso,
                telefone = telefone, cpf = cpf, visibilidade = visibilidade,
                contatoEmergenciaNome = emNomeValor, contatoEmergenciaTelefone = emTel
            )
        )
    }

    /** Quanto do perfil está preenchido e o que falta (com o peso de cada item). */
    fun completude(
        nome: String?, temFoto: Boolean, tipoSanguineo: String?, sexo: Char?, cidade: String?, estado: String?,
        nascimento: LocalDate?, telefone: String?, temCpf: Boolean, genero: String?, pesoKg: Int?,
        contatoNome: String?, contatoTel: String?
    ): Completude {
        val itens = listOf(
            Triple("nome", "Nome", 5) to !nome.isNullOrBlank(),
            Triple("foto", "Adicionar uma foto", 10) to temFoto,
            Triple("tipoSanguineo", "Informar o tipo sanguíneo", 15) to !tipoSanguineo.isNullOrBlank(),
            Triple("sexo", "Informar o sexo do intervalo entre doações", 10) to (sexo != null),
            Triple("cidade", "Informar a cidade", 10) to !cidade.isNullOrBlank(),
            Triple("estado", "Informar o estado", 5) to !estado.isNullOrBlank(),
            Triple("dataNascimento", "Informar a data de nascimento", 10) to (nascimento != null),
            Triple("telefone", "Informar um telefone", 10) to !telefone.isNullOrBlank(),
            Triple("cpf", "Informar o CPF", 10) to temCpf,
            Triple("genero", "Informar o gênero", 5) to !genero.isNullOrBlank(),
            Triple("pesoKg", "Informar o peso", 5) to (pesoKg != null),
            Triple("contatoEmergencia", "Cadastrar um contato de emergência", 5) to (!contatoNome.isNullOrBlank() && !contatoTel.isNullOrBlank())
        )
        val total = itens.sumOf { it.first.third }
        val feito = itens.filter { it.second }.sumOf { it.first.third }
        val faltando = itens.filter { !it.second }.sortedByDescending { it.first.third }
            .map { ItemCompletude(it.first.first, it.first.second, it.first.third) }
        return Completude(Math.round(feito * 100f / total), faltando)
    }

    /** O que o hospital enxerga de um doador: UMA função, usada no match e na "visão do hospital" do perfil. */
    fun visaoDoHospital(nome: String, nomeSocial: String?, tipo: String, cidade: String?, visibilidade: String, telefone: String?): VisaoHospital {
        val identificado = visibilidade == "identificado"
        val ve = listOf(
            "Nome" to if (identificado) (nomeSocial?.takeIf { it.isNotBlank() } ?: nome) else "Doador anônimo",
            "Telefone" to if (identificado) (telefone ?: "Não informado") else "Protegido 🔒",
            "Tipo sanguíneo" to tipo,
            "Cidade" to (cidade?.takeIf { it.isNotBlank() } ?: "—")
        )
        val naoVe = listOf("Foto", "CPF", "Data de nascimento", "Peso", "Gênero", "Condição de PcD", "E-mail", "Contato de emergência", "Histórico de doações")
        return VisaoHospital(identificado, ve, naoVe)
    }
}

data class Nivel(val numero: Int, val nome: String, val icone: String)
data class ProximoNivel(val nome: String, val icone: String, val faltam: Int)
data class Emblema(val id: String, val icone: String, val nome: String, val descricao: String, val conquistado: Boolean)
data class Conquistas(
    val totalDoacoes: Int, val vidasSalvas: Int, val nivel: Nivel, val proximoNivel: ProximoNivel?, val progresso: Int,
    val emblemas: List<Emblema>
)
data class DadosGamificacao(
    val totalDoacoes: Int, val doacoesUltimoAno: Int, val tipoSanguineo: String?, val visibilidade: String,
    val amigosQueDoaram: Int, val afilhados: Int, val doacoesEmCampanha: Int, val redeRara: Boolean
)
data class LinhaPlacar(val cidade: String, val doadores: Int, val doacoes: Int)
data class CidadePlacar(val posicao: Int, val cidade: String, val doadores: Int, val doacoes: Int, val vidasSalvas: Int)
data class Placar(val totalDoacoes: Int, val totalVidas: Int, val totalDoadores: Int, val cidades: List<CidadePlacar>)

/** Níveis, emblemas e placar das cidades (backend/regras/gamificacao.js). */
object Gamificacao {
    const val VIDAS_POR_DOACAO = 4
    private const val MINIMO_DOADORES_PLACAR = 3
    private const val TAMANHO_PLACAR = 10

    private val NIVEIS = listOf(
        Triple(0, "Futuro doador", "🌱"), Triple(1, "Gota de esperança", "💧"), Triple(3, "Doador frequente", "🩸"),
        Triple(5, "Guardião da vida", "🛡️"), Triple(10, "Herói do sangue", "🦸"), Triple(20, "Lenda Hemare", "🏆")
    )

    private data class DefEmblema(val id: String, val icone: String, val nome: String, val descricao: String, val cond: (DadosGamificacao) -> Boolean)

    private val EMBLEMAS = listOf(
        DefEmblema("primeira", "🎉", "Primeira doação", "Fez a primeira doação confirmada.") { it.totalDoacoes >= 1 },
        DefEmblema("regular", "📅", "Doador regular", "Doou 2 ou mais vezes nos últimos 12 meses.") { it.doacoesUltimoAno >= 2 },
        DefEmblema("vidas-20", "❤️", "20 vidas", "Suas doações podem ter salvado 20 vidas.") { it.totalDoacoes * VIDAS_POR_DOACAO >= 20 },
        DefEmblema("identificado", "📣", "Pronto para chamar", "Aceitou ser avisado em emergências.") { it.visibilidade == "identificado" },
        DefEmblema("universal", "🌍", "Doador universal", "Tipo O-: seu sangue serve para todos.") { it.tipoSanguineo == "O-" },
        DefEmblema("dourado", "🌟", "Sangue dourado", "Rh nulo: um dos sangues mais raros do mundo.") { it.tipoSanguineo?.startsWith("Rh nulo") == true },
        DefEmblema("recrutador", "🤝", "Recrutador", "Um amigo que você convidou fez a primeira doação.") { it.amigosQueDoaram >= 1 },
        DefEmblema("multiplicador", "📈", "Multiplicador", "Três amigos que você convidou já doaram.") { it.amigosQueDoaram >= 3 },
        DefEmblema("padrinho", "💝", "Padrinho", "Apadrinhou um paciente que precisa de transfusões regulares.") { it.afilhados >= 1 },
        DefEmblema("solidario", "🤲", "Solidário", "Doou por alguém em uma campanha de reposição.") { it.doacoesEmCampanha >= 1 },
        DefEmblema("rede-rara", "💎", "Rede de sangue raro", "Faz parte da rede de emergência de fenótipos raros, confirmada por um hospital.") { it.redeRara }
    )

    fun calcular(d: DadosGamificacao): Conquistas {
        val total = maxOf(0, d.totalDoacoes)
        var indice = 0
        NIVEIS.forEachIndexed { i, n -> if (total >= n.first) indice = i }
        val atual = NIVEIS[indice]
        val proximo = NIVEIS.getOrNull(indice + 1)
        val progresso = if (proximo != null) Math.round((total - atual.first) * 100f / (proximo.first - atual.first)) else 100
        return Conquistas(
            totalDoacoes = total,
            vidasSalvas = total * VIDAS_POR_DOACAO,
            nivel = Nivel(indice + 1, atual.second, atual.third),
            proximoNivel = proximo?.let { ProximoNivel(it.second, it.third, it.first - total) },
            progresso = progresso,
            emblemas = EMBLEMAS.map { Emblema(it.id, it.icone, it.nome, it.descricao, it.cond(d)) }
        )
    }

    /** Só aparecem cidades com pelo menos 3 doadores (com menos, daria para adivinhar quem é quem). */
    fun montarPlacar(linhas: List<LinhaPlacar>): Placar {
        val totalDoacoes = linhas.sumOf { it.doacoes }
        val cidades = linhas
            .filter { it.doadores >= MINIMO_DOADORES_PLACAR && it.doacoes > 0 }
            .sortedWith(compareByDescending<LinhaPlacar> { it.doacoes }.thenByDescending { it.doadores }.thenBy { it.cidade })
            .take(TAMANHO_PLACAR)
            .mapIndexed { i, l -> CidadePlacar(i + 1, l.cidade, l.doadores, l.doacoes, l.doacoes * VIDAS_POR_DOACAO) }
        return Placar(totalDoacoes, totalDoacoes * VIDAS_POR_DOACAO, linhas.sumOf { it.doadores }, cidades)
    }
}

/**
 * Guarda da senha: o banco local nunca recebe a senha em texto. É um resumo SHA-256 com o e-mail como sal.
 * (O site usa bcrypt no servidor; aqui é só o necessário para a conta de demonstração não ficar legível no aparelho.)
 */
object Senhas {
    fun hash(senha: String, email: String): String {
        val bytes = java.security.MessageDigest.getInstance("SHA-256").digest("hemare|${email.trim().lowercase()}|$senha".toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
