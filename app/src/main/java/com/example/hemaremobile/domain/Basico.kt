package com.example.hemaremobile.domain

import java.text.Normalizer
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

// Regras puras do Hemare (portadas de backend/regras e frontend/src/regras do site). Sem Android, sem rede, sem estado.

object Datas {
    private val SEMANA = listOf("dom", "seg", "ter", "qua", "qui", "sex", "sáb")
    private val FUSO = ZoneId.of("America/Sao_Paulo")

    fun hoje(): LocalDate = LocalDate.now(FUSO)

    fun diasEntre(de: LocalDate, ate: LocalDate): Int = ChronoUnit.DAYS.between(de, ate).toInt()

    /** "sex 09/10" */
    fun formatar(dia: LocalDate): String =
        SEMANA[dia.dayOfWeek.value % 7] + " " + dm(dia)

    /** "09/10" */
    fun dm(dia: LocalDate): String = "%02d/%02d".format(dia.dayOfMonth, dia.monthValue)

    /** "09/10/2026" */
    fun dmy(dia: LocalDate): String = "%02d/%02d/%04d".format(dia.dayOfMonth, dia.monthValue, dia.year)

    /** Lê "dd/MM/aaaa" (ou "ddMMaaaa"); devolve null se não for uma data válida. */
    fun lerDmy(texto: String): LocalDate? {
        val d = texto.filter { it.isDigit() }
        if (d.length != 8) return null
        return try {
            LocalDate.of(d.substring(4, 8).toInt(), d.substring(2, 4).toInt(), d.substring(0, 2).toInt())
        } catch (e: Exception) {
            null
        }
    }

    /** Máscara de digitação "dd/MM/aaaa". */
    fun mascaraDmy(valor: String): String {
        val d = valor.filter { it.isDigit() }.take(8)
        val sb = StringBuilder()
        d.forEachIndexed { i, c ->
            if (i == 2 || i == 4) sb.append('/')
            sb.append(c)
        }
        return sb.toString()
    }

    /** ['08:00', '08:30', ...] entre o primeiro e o último horário, de X em X minutos (no máximo 24). */
    fun gerarHorarios(primeiro: String, ultimo: String, minutos: Int): List<String> {
        val p = primeiro.split(":").mapNotNull { it.toIntOrNull() }
        val u = ultimo.split(":").mapNotNull { it.toIntOrNull() }
        if (p.size != 2 || u.size != 2 || minutos < 5) return emptyList()
        val lista = mutableListOf<String>()
        var t = p[0] * 60 + p[1]
        while (t <= u[0] * 60 + u[1] && lista.size < 24) {
            lista.add("%02d:%02d".format(t / 60, t % 60))
            t += minutos
        }
        return lista
    }

    /** "há 5 min", "há 2 horas"... (igual a frontend/src/regras/tempo.js). */
    fun haQuanto(instanteMs: Long, agoraMs: Long = System.currentTimeMillis()): String {
        val min = (agoraMs - instanteMs) / 60000
        if (min < 1) return "agora mesmo"
        if (min < 60) return "há $min min"
        val horas = min / 60
        if (horas < 24) return "há $horas " + if (horas == 1L) "hora" else "horas"
        val dias = horas / 24
        if (dias < 7) return "há $dias " + if (dias == 1L) "dia" else "dias"
        if (dias < 30) {
            val s = dias / 7
            return "há $s " + if (s == 1L) "semana" else "semanas"
        }
        return java.time.Instant.ofEpochMilli(instanteMs).atZone(FUSO).toLocalDate().let { dmy(it) }
    }
}

object Textos {
    /** Minúsculas e sem acento, para comparar palavras. */
    fun normalizar(texto: String?): String =
        Normalizer.normalize(texto ?: "", Normalizer.Form.NFD).replace(Regex("\\p{Mn}+"), "").lowercase()

    fun normalizarCidade(cidade: String?): String =
        normalizar(cidade).replace(Regex("\\s+"), " ").trim()

    fun limpar(texto: String?): String = (texto ?: "").replace(Regex("\\s+"), " ").trim()
}

object Documentos {
    fun digitos(texto: String?): String = (texto ?: "").filter { it.isDigit() }

    fun mascaraCpf(valor: String): String {
        val d = digitos(valor).take(11)
        val sb = StringBuilder()
        d.forEachIndexed { i, c ->
            if (i == 3 || i == 6) sb.append('.')
            if (i == 9) sb.append('-')
            sb.append(c)
        }
        return sb.toString()
    }

    fun mascaraTelefone(valor: String): String {
        val d = digitos(valor).take(11)
        val sb = StringBuilder()
        d.forEachIndexed { i, c ->
            if (i == 0) sb.append('(')
            if (i == 2) sb.append(") ")
            if (d.length > 10 && i == 7) sb.append('-')
            if (d.length <= 10 && i == 6) sb.append('-')
            sb.append(c)
        }
        return sb.toString()
    }

    fun mascaraCnpj(valor: String): String {
        val d = digitos(valor).take(14)
        val sb = StringBuilder()
        d.forEachIndexed { i, c ->
            if (i == 2 || i == 5) sb.append('.')
            if (i == 8) sb.append('/')
            if (i == 12) sb.append('-')
            sb.append(c)
        }
        return sb.toString()
    }

    fun mascaraCep(valor: String): String {
        val d = digitos(valor).take(8)
        return if (d.length > 5) d.substring(0, 5) + "-" + d.substring(5) else d
    }

    fun cpfValido(texto: String): Boolean {
        val cpf = digitos(texto)
        if (cpf.length != 11 || cpf.all { it == cpf[0] }) return false
        var soma = 0
        for (i in 0 until 9) soma += (cpf[i] - '0') * (10 - i)
        var d1 = (soma * 10) % 11
        if (d1 == 10) d1 = 0
        if (d1 != cpf[9] - '0') return false
        soma = 0
        for (i in 0 until 10) soma += (cpf[i] - '0') * (11 - i)
        var d2 = (soma * 10) % 11
        if (d2 == 10) d2 = 0
        return d2 == cpf[10] - '0'
    }

    fun cnpjValido(texto: String): Boolean {
        val cnpj = digitos(texto)
        if (cnpj.length != 14 || cnpj.all { it == cnpj[0] }) return false
        fun calc(tamanho: Int): Int {
            var soma = 0
            var pos = tamanho - 7
            for (i in tamanho downTo 1) {
                soma += (cnpj[tamanho - i] - '0') * pos
                pos--
                if (pos < 2) pos = 9
            }
            val r = soma % 11
            return if (r < 2) 0 else 11 - r
        }
        return calc(12) == cnpj[12] - '0' && calc(13) == cnpj[13] - '0'
    }

    /** "123.456.789-09" -> "***.456.789-**". */
    fun mascararCpf(texto: String?): String? {
        val d = digitos(texto)
        return if (d.length == 11) "***." + d.substring(3, 6) + "." + d.substring(6, 9) + "-**" else null
    }

    fun formatarCpf(texto: String): String = mascaraCpf(texto)

    fun formatarTelefone(texto: String): String = mascaraTelefone(texto)
}

object Compatibilidade {
    /** A chave é o tipo do RECEPTOR; a lista são os tipos de DOADOR aceitos. */
    private val COMPATIVEIS = mapOf(
        "O-" to listOf("O-"),
        "O+" to listOf("O-", "O+"),
        "A-" to listOf("O-", "A-"),
        "A+" to listOf("O-", "O+", "A-", "A+"),
        "B-" to listOf("O-", "B-"),
        "B+" to listOf("O-", "O+", "B-", "B+"),
        "AB-" to listOf("O-", "A-", "B-", "AB-"),
        "AB+" to listOf("O-", "O+", "A-", "A+", "B-", "B+", "AB-", "AB+")
    )

    val TIPOS_PEDIDO = listOf("O-", "O+", "A-", "A+", "B-", "B+", "AB-", "AB+")

    fun podeDoar(tipoDoador: String, tipoReceptor: String): Boolean =
        COMPATIVEIS[tipoReceptor]?.contains(tipoDoador) == true

    fun doadoresCompativeis(tipoReceptor: String): List<String> = COMPATIVEIS[tipoReceptor] ?: emptyList()
}

data class ResultadoElegibilidade(val apto: Boolean, val motivo: String, val diasRestantes: Int?)

object Elegibilidade {
    /** Intervalo mínimo entre doações de sangue total: homens 60 dias, mulheres 90. */
    val INTERVALO_DIAS = mapOf('M' to 60, 'F' to 90)

    /** Limite de doações em 12 meses (art. 38 da Portaria 11.685/2026). */
    val LIMITE_ANUAL = mapOf('M' to 4, 'F' to 3)

    fun verificar(ultimaDoacao: LocalDate?, sexo: Char?, hoje: LocalDate = Datas.hoje()): ResultadoElegibilidade {
        val intervalo = INTERVALO_DIAS[sexo] ?: return ResultadoElegibilidade(false, "Sexo inválido", null)
        if (ultimaDoacao == null) return ResultadoElegibilidade(true, "Primeira doação", 0)
        val passados = Datas.diasEntre(ultimaDoacao, hoje)
        if (passados >= intervalo) return ResultadoElegibilidade(true, "Intervalo cumprido", 0)
        return ResultadoElegibilidade(false, "Aguardando intervalo", intervalo - passados)
    }

    /** Em que dia a pessoa fica apta (nunca antes de 'aPartirDe'). Null se o sexo não foi informado. */
    fun aptoEmDia(ultimaDoacao: LocalDate?, sexo: Char?, aPartirDe: LocalDate): LocalDate? {
        val e = verificar(ultimaDoacao, sexo, aPartirDe)
        val dias = e.diasRestantes ?: return null
        return aPartirDe.plusDays(dias.toLong())
    }
}

object Distancia {
    /** Haversine, em km inteiros. */
    fun calcular(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Int {
        val r = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
            Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.sin(dLng / 2) * Math.sin(dLng / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return Math.round(r * c).toInt()
    }
}

object Estados {
    val UFS = listOf(
        "AC" to "Acre", "AL" to "Alagoas", "AP" to "Amapá", "AM" to "Amazonas", "BA" to "Bahia", "CE" to "Ceará",
        "DF" to "Distrito Federal", "ES" to "Espírito Santo", "GO" to "Goiás", "MA" to "Maranhão", "MT" to "Mato Grosso",
        "MS" to "Mato Grosso do Sul", "MG" to "Minas Gerais", "PA" to "Pará", "PB" to "Paraíba", "PR" to "Paraná",
        "PE" to "Pernambuco", "PI" to "Piauí", "RJ" to "Rio de Janeiro", "RN" to "Rio Grande do Norte",
        "RS" to "Rio Grande do Sul", "RO" to "Rondônia", "RR" to "Roraima", "SC" to "Santa Catarina",
        "SP" to "São Paulo", "SE" to "Sergipe", "TO" to "Tocantins"
    )
    val SIGLAS: List<String> = UFS.map { it.first }
}
