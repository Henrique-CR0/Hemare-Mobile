package com.example.hemaremobile.data.local

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import com.example.hemaremobile.data.*
import com.example.hemaremobile.domain.Noticia
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.time.LocalDate

/** Converte cada parte do Estado de e para JSON, que é o que as tabelas do banco guardam. */
object CodecEstado {
    // ---------- utilitários ----------
    private fun JSONObject.str(k: String): String? = if (isNull(k) || !has(k)) null else getString(k)
    private fun JSONObject.int(k: String): Int? = if (isNull(k) || !has(k)) null else getInt(k)
    private fun JSONObject.dbl(k: String): Double? = if (isNull(k) || !has(k)) null else getDouble(k)
    private fun JSONObject.data(k: String): LocalDate? = str(k)?.let { LocalDate.parse(it) }
    private fun JSONObject.lista(k: String): List<String> = getJSONArray(k).let { a -> (0 until a.length()).map { a.getString(it) } }
    private fun JSONObject.listaInt(k: String): List<Int> = getJSONArray(k).let { a -> (0 until a.length()).map { a.getInt(it) } }
    private fun <T> JSONObject.objetos(k: String, f: (JSONObject) -> T): List<T> = getJSONArray(k).let { a -> (0 until a.length()).map { f(a.getJSONObject(it)) } }
    private fun JSONObject.poeNulo(k: String, v: Any?): JSONObject = put(k, v ?: JSONObject.NULL)
    private fun arr(itens: Collection<*>) = JSONArray().also { a -> itens.forEach { a.put(it) } }

    // ---------- usuário ----------
    fun usuario(u: Usuario) = JSONObject().put("id", u.id).put("nome", u.nome).put("email", u.email).put("senha", u.senha)
        .put("tipo", u.tipo.name).put("criadoEm", u.criadoEm.toString())

    fun usuario(j: JSONObject) = Usuario(j.getInt("id"), j.getString("nome"), j.getString("email"), j.getString("senha"), TipoConta.valueOf(j.getString("tipo")), LocalDate.parse(j.getString("criadoEm")))

    // ---------- doador ----------
    fun fotoParaTexto(b: Bitmap?): String? {
        if (b == null) return null
        val saida = ByteArrayOutputStream()
        b.compress(Bitmap.CompressFormat.PNG, 100, saida)
        return Base64.encodeToString(saida.toByteArray(), Base64.NO_WRAP)
    }

    fun textoParaFoto(s: String?): Bitmap? {
        if (s == null) return null
        val bytes = Base64.decode(s, Base64.NO_WRAP)
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }

    fun doador(d: Doador) = JSONObject().put("id", d.id).put("tipoSanguineo", d.tipoSanguineo).put("sexo", d.sexo.toString())
        .poeNulo("genero", d.genero).poeNulo("nomeSocial", d.nomeSocial).put("cidade", d.cidade).poeNulo("estado", d.estado)
        .poeNulo("dataNascimento", d.dataNascimento?.toString()).poeNulo("pesoKg", d.pesoKg).poeNulo("telefone", d.telefone)
        .poeNulo("cpf", d.cpf).put("visibilidade", d.visibilidade).poeNulo("contatoNome", d.contatoEmergenciaNome)
        .poeNulo("contatoTelefone", d.contatoEmergenciaTelefone).put("querLembrete", d.querLembrete).put("codigoIndicacao", d.codigoIndicacao)
        .poeNulo("indicadoPor", d.indicadoPor).poeNulo("ultimoAlerta", d.ultimoAlerta?.toString())
        .put("pcd", JSONObject().put("declarado", d.pcd.declarado).put("tipos", arr(d.pcd.tipos)).put("apoios", arr(d.pcd.apoios)))
        .put("raro", JSONObject().poeNulo("fenotipo", d.raro.fenotipo).poeNulo("alcance", d.raro.alcance).put("consentimento", d.raro.consentimento)
            .poeNulo("status", d.raro.status).poeNulo("codigo", d.raro.codigo).poeNulo("ultimaNotificacao", d.raro.ultimaNotificacao?.toString()))
        .poeNulo("foto", fotoParaTexto(d.foto))

    fun doador(j: JSONObject): Doador {
        val p = j.getJSONObject("pcd")
        val r = j.getJSONObject("raro")
        return Doador(
            j.getInt("id"), j.getString("tipoSanguineo"), j.getString("sexo").first(), j.str("genero"), j.str("nomeSocial"), j.getString("cidade"), j.str("estado"),
            j.data("dataNascimento"), j.int("pesoKg"), j.str("telefone"), j.str("cpf"), j.getString("visibilidade"), j.str("contatoNome"), j.str("contatoTelefone"),
            j.getBoolean("querLembrete"), j.getString("codigoIndicacao"), j.int("indicadoPor"), j.data("ultimoAlerta"),
            PcdInfo(p.getBoolean("declarado"), p.lista("tipos"), p.lista("apoios")),
            RaroInfo(r.str("fenotipo"), r.str("alcance"), r.getBoolean("consentimento"), r.str("status"), r.str("codigo"), r.data("ultimaNotificacao")),
            textoParaFoto(j.str("foto"))
        )
    }

    // ---------- hospital e locais ----------
    fun hospital(h: Hospital) = JSONObject().put("id", h.id).put("cnpj", h.cnpj).put("cnes", h.cnes).put("cep", h.cep).put("endereco", h.endereco)
        .put("numero", h.numero).put("bairro", h.bairro).poeNulo("complemento", h.complemento).put("cidade", h.cidade).put("estado", h.estado)
        .put("aprovado", h.aprovado).poeNulo("motivoRecusa", h.motivoRecusa).poeNulo("lat", h.lat).poeNulo("lng", h.lng)

    fun hospital(j: JSONObject) = Hospital(
        j.getInt("id"), j.getString("cnpj"), j.getString("cnes"), j.getString("cep"), j.getString("endereco"), j.getString("numero"), j.getString("bairro"),
        j.str("complemento"), j.getString("cidade"), j.getString("estado"), j.getBoolean("aprovado"), j.str("motivoRecusa"), j.dbl("lat"), j.dbl("lng")
    )

    fun local(l: LocalDoacao) = JSONObject().put("id", l.id).put("nome", l.nome).put("cidade", l.cidade).put("estado", l.estado)
        .poeNulo("endereco", l.endereco).poeNulo("telefone", l.telefone).poeNulo("lat", l.lat).poeNulo("lng", l.lng)

    fun local(j: JSONObject) = LocalDoacao(j.getInt("id"), j.getString("nome"), j.getString("cidade"), j.getString("estado"), j.str("endereco"), j.str("telefone"), j.dbl("lat"), j.dbl("lng"))

    // ---------- doações, necessidades, agenda ----------
    fun doacao(d: Doacao) = JSONObject().put("id", d.id).put("doadorId", d.doadorId).put("hospitalId", d.hospitalId).put("data", d.data.toString()).put("hash", d.hash).put("emCampanha", d.emCampanha)
    fun doacao(j: JSONObject) = Doacao(j.getInt("id"), j.getInt("doadorId"), j.getInt("hospitalId"), LocalDate.parse(j.getString("data")), j.getString("hash"), j.getBoolean("emCampanha"))

    fun necessidade(n: Necessidade) = JSONObject().put("id", n.id).put("hospitalId", n.hospitalId).put("tipo", n.tipo).put("urgencia", n.urgencia).put("status", n.status)
    fun necessidade(j: JSONObject) = Necessidade(j.getInt("id"), j.getInt("hospitalId"), j.getString("tipo"), j.getString("urgencia"), j.getString("status"))

    fun vaga(v: Vaga) = JSONObject().put("id", v.id).put("hospitalId", v.hospitalId).put("data", v.data.toString()).put("hora", v.hora).put("capacidade", v.capacidade)
    fun vaga(j: JSONObject) = Vaga(j.getInt("id"), j.getInt("hospitalId"), LocalDate.parse(j.getString("data")), j.getString("hora"), j.getInt("capacidade"))

    fun agendamento(a: AgendamentoReg) = JSONObject().put("id", a.id).put("vagaId", a.vagaId).put("doadorId", a.doadorId).put("status", a.status)
    fun agendamento(j: JSONObject) = AgendamentoReg(j.getInt("id"), j.getInt("vagaId"), j.getInt("doadorId"), j.getString("status"))

    // ---------- apadrinhamento, campanhas, sangue raro ----------
    fun caso(c: Caso) = JSONObject().put("id", c.id).put("hospitalId", c.hospitalId).put("apelido", c.apelido).put("tipo", c.tipo).put("condicao", c.condicao)
        .put("frequenciaDias", c.frequenciaDias).put("meta", c.meta).put("ativo", c.ativo).poeNulo("ultimaChamada", c.ultimaChamada?.toString()).put("padrinhos", arr(c.padrinhos))

    fun caso(j: JSONObject) = Caso(
        j.getInt("id"), j.getInt("hospitalId"), j.getString("apelido"), j.getString("tipo"), j.getString("condicao"), j.getInt("frequenciaDias"),
        j.getInt("meta"), j.getBoolean("ativo"), j.data("ultimaChamada"), j.listaInt("padrinhos")
    )

    fun campanha(c: CampanhaReg) = JSONObject().put("codigo", c.codigo).put("hospitalId", c.hospitalId).put("apelido", c.apelido).put("metaBolsas", c.metaBolsas)
        .put("expiraEm", c.expiraEm.toString()).put("ativo", c.ativo)
        .put("promessas", JSONArray().also { a -> c.promessas.forEach { a.put(JSONObject().put("doadorId", it.doadorId).put("data", it.data.toString()).put("confirmada", it.confirmada)) } })

    fun campanha(j: JSONObject) = CampanhaReg(
        j.getString("codigo"), j.getInt("hospitalId"), j.getString("apelido"), j.getInt("metaBolsas"), LocalDate.parse(j.getString("expiraEm")), j.getBoolean("ativo"),
        j.objetos("promessas") { Promessa(it.getInt("doadorId"), LocalDate.parse(it.getString("data")), it.getBoolean("confirmada")) }
    )

    fun pedidoRaro(p: PedidoRaro) = JSONObject().put("id", p.id).put("hospitalId", p.hospitalId).put("fenotipo", p.fenotipo).put("motivo", p.motivo)
        .poeNulo("apelidoPaciente", p.apelidoPaciente).put("criadoEmMs", p.criadoEmMs).put("expiraEmMs", p.expiraEmMs).put("aberto", p.aberto)
        .put("convocados", JSONArray().also { a -> p.convocados.forEach { a.put(JSONObject().put("doadorId", it.doadorId).poeNulo("resposta", it.resposta).put("doacaoConfirmada", it.doacaoConfirmada)) } })

    fun pedidoRaro(j: JSONObject) = PedidoRaro(
        j.getInt("id"), j.getInt("hospitalId"), j.getString("fenotipo"), j.getString("motivo"), j.str("apelidoPaciente"), j.getLong("criadoEmMs"), j.getLong("expiraEmMs"),
        j.getBoolean("aberto"), j.objetos("convocados") { ConvocadoRaro(it.getInt("doadorId"), it.str("resposta"), it.getBoolean("doacaoConfirmada")) }
    )

    // ---------- notícias ----------
    fun noticia(n: Noticia) = JSONObject().put("id", n.id).put("titulo", n.titulo).put("resumo", n.resumo).put("url", n.url).poeNulo("imagem", n.imagem)
        .put("publicadaEm", n.publicadaEm).put("fonte", n.fonte).put("categoria", n.categoria).poeNulo("uf", n.uf).put("chave", n.chave).put("busca", n.busca)

    fun noticia(j: JSONObject) = Noticia(
        j.getInt("id"), j.getString("titulo"), j.getString("resumo"), j.getString("url"), j.str("imagem"), j.getLong("publicadaEm"), j.getString("fonte"),
        j.getString("categoria"), j.str("uf"), j.getString("chave"), j.getString("busca")
    )

    // ---------- estoque (tabela com colunas de verdade) e itens do termômetro externo ----------
    fun itemEstoque(i: ItemEstoque) = JSONObject().put("tipo", i.tipo).put("hospital", i.hospital).put("cidade", i.cidade).put("estado", i.estado).put("nivel", i.nivel)
    fun itemEstoque(j: JSONObject) = ItemEstoque(j.getString("tipo"), j.getString("hospital"), j.getString("cidade"), j.getString("estado"), j.getString("nivel"))

    // ---------- meta ----------
    fun coleta(c: ResultadoColeta) = JSONObject().put("terminouEmMs", c.terminouEmMs)
        .put("fontes", JSONArray().also { a -> c.fontes.forEach { a.put(JSONObject().put("id", it.id).put("ok", it.ok).put("lidos", it.lidos).put("novos", it.novos).poeNulo("erro", it.erro)) } })

    fun coleta(j: JSONObject) = ResultadoColeta(j.getLong("terminouEmMs"), j.objetos("fontes") { ResultadoFonte(it.getString("id"), it.getBoolean("ok"), it.getInt("lidos"), it.getInt("novos"), it.str("erro")) })

    fun indicacoes(m: Map<Int, Int>) = JSONObject().also { o -> m.forEach { (k, v) -> o.put(k.toString(), v) } }
    fun indicacoes(j: JSONObject): Map<Int, Int> = j.keys().asSequence().associate { it.toInt() to j.getInt(it) }
}
