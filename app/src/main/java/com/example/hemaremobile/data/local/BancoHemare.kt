package com.example.hemaremobile.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.hemaremobile.data.*
import com.example.hemaremobile.domain.Noticia
import org.json.JSONArray
import org.json.JSONObject

/** Quem guarda o Estado entre uma abertura do app e outra. A loja só conhece esta interface. */
interface RepositorioEstado {
    /** O que foi salvo da última vez, ou null se o banco ainda está vazio. */
    fun carregar(): Estado?
    fun salvar(estado: Estado)
}

/**
 * Banco SQLite do aparelho (sem bibliotecas extras). Cada entidade do site tem a sua tabela:
 * colunas de verdade para o que se consulta (e-mail, tipo, dono) e o restante em JSON.
 */
class BancoHemare(contexto: Context, nome: String? = NOME) : SQLiteOpenHelper(contexto, nome, null, VERSAO), RepositorioEstado {

    override fun onConfigure(db: SQLiteDatabase) {
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE usuarios (id INTEGER PRIMARY KEY, email TEXT NOT NULL UNIQUE, tipo TEXT NOT NULL, json TEXT NOT NULL)")
        db.execSQL("CREATE TABLE doadores (id INTEGER PRIMARY KEY REFERENCES usuarios(id) ON DELETE CASCADE, json TEXT NOT NULL)")
        db.execSQL("CREATE TABLE hospitais (id INTEGER PRIMARY KEY REFERENCES usuarios(id) ON DELETE CASCADE, aprovado INTEGER NOT NULL, json TEXT NOT NULL)")
        db.execSQL("CREATE TABLE locais (id INTEGER PRIMARY KEY, json TEXT NOT NULL)")
        db.execSQL("CREATE TABLE doacoes (id INTEGER PRIMARY KEY, doador_id INTEGER NOT NULL, hospital_id INTEGER NOT NULL, json TEXT NOT NULL)")
        db.execSQL("CREATE TABLE estoque (hospital_id INTEGER NOT NULL, tipo_sanguineo TEXT NOT NULL, nivel TEXT NOT NULL, PRIMARY KEY (hospital_id, tipo_sanguineo))")
        db.execSQL("CREATE TABLE necessidades (id INTEGER PRIMARY KEY, hospital_id INTEGER NOT NULL, json TEXT NOT NULL)")
        db.execSQL("CREATE TABLE vagas (id INTEGER PRIMARY KEY, hospital_id INTEGER NOT NULL, json TEXT NOT NULL)")
        db.execSQL("CREATE TABLE agendamentos (id INTEGER PRIMARY KEY, vaga_id INTEGER NOT NULL, doador_id INTEGER NOT NULL, json TEXT NOT NULL)")
        db.execSQL("CREATE TABLE casos (id INTEGER PRIMARY KEY, hospital_id INTEGER NOT NULL, json TEXT NOT NULL)")
        db.execSQL("CREATE TABLE campanhas (codigo TEXT PRIMARY KEY, hospital_id INTEGER NOT NULL, json TEXT NOT NULL)")
        db.execSQL("CREATE TABLE pedidos_raros (id INTEGER PRIMARY KEY, hospital_id INTEGER NOT NULL, json TEXT NOT NULL)")
        db.execSQL("CREATE TABLE noticias (id INTEGER PRIMARY KEY, json TEXT NOT NULL)")
        db.execSQL("CREATE TABLE estoque_externo (ordem INTEGER PRIMARY KEY, json TEXT NOT NULL)")
        db.execSQL("CREATE TABLE meta (chave TEXT PRIMARY KEY, valor TEXT NOT NULL)")
    }

    override fun onUpgrade(db: SQLiteDatabase, antiga: Int, nova: Int) {
        // Só existe a versão 1. Quando mudar o formato, a migração entra aqui (sem apagar as contas).
    }

    // ---------------------------------------------------------------------------------------------

    override fun carregar(): Estado? {
        val db = readableDatabase
        val meta = lerMeta(db)
        if (meta["proximoId"] == null) return null // banco novo, nunca salvo
        return try {
            Estado(
                usuarios = linhas(db, "usuarios", "ORDER BY id") { CodecEstado.usuario(it) },
                doadores = linhas(db, "doadores", "ORDER BY id") { CodecEstado.doador(it) }.associateBy { it.id },
                hospitais = linhas(db, "hospitais", "ORDER BY id") { CodecEstado.hospital(it) }.associateBy { it.id },
                locais = linhas(db, "locais", "ORDER BY id") { CodecEstado.local(it) },
                doacoes = linhas(db, "doacoes", "ORDER BY id") { CodecEstado.doacao(it) },
                estoque = lerEstoque(db),
                necessidades = linhas(db, "necessidades", "ORDER BY id") { CodecEstado.necessidade(it) },
                vagas = linhas(db, "vagas", "ORDER BY id") { CodecEstado.vaga(it) },
                agendamentos = linhas(db, "agendamentos", "ORDER BY id") { CodecEstado.agendamento(it) },
                casos = linhas(db, "casos", "ORDER BY id") { CodecEstado.caso(it) },
                campanhas = linhas(db, "campanhas", "ORDER BY rowid") { CodecEstado.campanha(it) },
                pedidosRaros = linhas(db, "pedidos_raros", "ORDER BY id") { CodecEstado.pedidoRaro(it) },
                noticias = linhas(db, "noticias", "ORDER BY id") { CodecEstado.noticia(it) },
                noticiasOcultas = meta["noticiasOcultas"]?.let { s -> JSONArray(s).let { a -> (0 until a.length()).map { a.getInt(it) }.toSet() } } ?: emptySet(),
                coleta = meta["coleta"]?.let { CodecEstado.coleta(JSONObject(it)) },
                sessaoUsuarioId = meta["sessaoUsuarioId"]?.toIntOrNull(),
                proximoId = meta["proximoId"]!!.toInt(),
                indicacoes = meta["indicacoes"]?.let { CodecEstado.indicacoes(JSONObject(it)) } ?: emptyMap(),
                estoqueExterno = linhas(db, "estoque_externo", "ORDER BY ordem") { CodecEstado.itemEstoque(it) },
                servidor = meta["servidor"],
                sincronizadoEm = meta["sincronizadoEm"]?.toLongOrNull()
            )
        } catch (e: Exception) {
            null // dados ilegíveis: o app começa de novo com os dados de exemplo em vez de travar
        }
    }

    override fun salvar(estado: Estado) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            listOf("estoque_externo", "noticias", "pedidos_raros", "campanhas", "casos", "agendamentos", "vagas", "necessidades", "estoque", "doacoes", "locais", "hospitais", "doadores", "usuarios", "meta")
                .forEach { db.delete(it, null, null) }

            estado.usuarios.forEach { db.insertOrThrow("usuarios", null, valores("id" to it.id, "email" to it.email, "tipo" to it.tipo.name, "json" to CodecEstado.usuario(it).toString())) }
            estado.doadores.values.forEach { db.insertOrThrow("doadores", null, valores("id" to it.id, "json" to CodecEstado.doador(it).toString())) }
            estado.hospitais.values.forEach { db.insertOrThrow("hospitais", null, valores("id" to it.id, "aprovado" to if (it.aprovado) 1 else 0, "json" to CodecEstado.hospital(it).toString())) }
            estado.locais.forEach { db.insertOrThrow("locais", null, valores("id" to it.id, "json" to CodecEstado.local(it).toString())) }
            estado.doacoes.forEach { db.insertOrThrow("doacoes", null, valores("id" to it.id, "doador_id" to it.doadorId, "hospital_id" to it.hospitalId, "json" to CodecEstado.doacao(it).toString())) }
            estado.estoque.forEach { (hospital, tipos) -> tipos.forEach { (tipo, nivel) -> db.insertOrThrow("estoque", null, valores("hospital_id" to hospital, "tipo_sanguineo" to tipo, "nivel" to nivel)) } }
            estado.necessidades.forEach { db.insertOrThrow("necessidades", null, valores("id" to it.id, "hospital_id" to it.hospitalId, "json" to CodecEstado.necessidade(it).toString())) }
            estado.vagas.forEach { db.insertOrThrow("vagas", null, valores("id" to it.id, "hospital_id" to it.hospitalId, "json" to CodecEstado.vaga(it).toString())) }
            estado.agendamentos.forEach { db.insertOrThrow("agendamentos", null, valores("id" to it.id, "vaga_id" to it.vagaId, "doador_id" to it.doadorId, "json" to CodecEstado.agendamento(it).toString())) }
            estado.casos.forEach { db.insertOrThrow("casos", null, valores("id" to it.id, "hospital_id" to it.hospitalId, "json" to CodecEstado.caso(it).toString())) }
            estado.campanhas.forEach { db.insertOrThrow("campanhas", null, valores("codigo" to it.codigo, "hospital_id" to it.hospitalId, "json" to CodecEstado.campanha(it).toString())) }
            estado.pedidosRaros.forEach { db.insertOrThrow("pedidos_raros", null, valores("id" to it.id, "hospital_id" to it.hospitalId, "json" to CodecEstado.pedidoRaro(it).toString())) }
            estado.noticias.forEach { db.insertOrThrow("noticias", null, valores("id" to it.id, "json" to CodecEstado.noticia(it).toString())) }
            estado.estoqueExterno.forEachIndexed { i, it -> db.insertOrThrow("estoque_externo", null, valores("ordem" to i, "json" to CodecEstado.itemEstoque(it).toString())) }

            val meta = mutableMapOf(
                "proximoId" to estado.proximoId.toString(),
                "noticiasOcultas" to JSONArray(estado.noticiasOcultas.toList()).toString(),
                "indicacoes" to CodecEstado.indicacoes(estado.indicacoes).toString()
            )
            estado.sessaoUsuarioId?.let { meta["sessaoUsuarioId"] = it.toString() }
            estado.coleta?.let { meta["coleta"] = CodecEstado.coleta(it).toString() }
            estado.servidor?.let { meta["servidor"] = it }
            estado.sincronizadoEm?.let { meta["sincronizadoEm"] = it.toString() }
            meta.forEach { (k, v) -> db.insertOrThrow("meta", null, valores("chave" to k, "valor" to v)) }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    /** Apaga tudo (usado quando a pessoa quer voltar aos dados de exemplo). */
    fun limpar() {
        val db = writableDatabase
        db.beginTransaction()
        try {
            listOf("estoque_externo", "noticias", "pedidos_raros", "campanhas", "casos", "agendamentos", "vagas", "necessidades", "estoque", "doacoes", "locais", "hospitais", "doadores", "usuarios", "meta")
                .forEach { db.delete(it, null, null) }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    // ---------------------------------------------------------------------------------------------

    private fun valores(vararg pares: Pair<String, Any>) = ContentValues().also { cv ->
        pares.forEach { (k, v) ->
            when (v) {
                is Int -> cv.put(k, v)
                is Long -> cv.put(k, v)
                else -> cv.put(k, v.toString())
            }
        }
    }

    private fun <T> linhas(db: SQLiteDatabase, tabela: String, ordem: String, f: (JSONObject) -> T): List<T> {
        val saida = mutableListOf<T>()
        db.rawQuery("SELECT json FROM $tabela $ordem", null).use { c -> while (c.moveToNext()) saida.add(f(JSONObject(c.getString(0)))) }
        return saida
    }

    private fun lerEstoque(db: SQLiteDatabase): Map<Int, Map<String, String>> {
        val m = mutableMapOf<Int, MutableMap<String, String>>()
        db.rawQuery("SELECT hospital_id, tipo_sanguineo, nivel FROM estoque", null).use { c ->
            while (c.moveToNext()) m.getOrPut(c.getInt(0)) { mutableMapOf() }[c.getString(1)] = c.getString(2)
        }
        return m
    }

    private fun lerMeta(db: SQLiteDatabase): Map<String, String> {
        val m = mutableMapOf<String, String>()
        db.rawQuery("SELECT chave, valor FROM meta", null).use { c -> while (c.moveToNext()) m[c.getString(0)] = c.getString(1) }
        return m
    }

    companion object {
        const val NOME = "hemare.db"
        const val VERSAO = 1
    }
}
