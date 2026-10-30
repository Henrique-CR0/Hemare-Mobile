package com.example.hemaremobile.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import com.example.hemaremobile.domain.Conquistas
import java.time.LocalDate

private const val LARGURA = 680
private const val ALTURA = 420

/** Desenha a carteirinha digital do doador (igual ao cartão que o site monta no navegador). Nada vai para o servidor. */
fun desenharCarteirinha(
    nome: String, nomeSocial: String?, tipoSanguineo: String, cidade: String, estado: String?, conquistas: Conquistas,
    doadorDesde: LocalDate?, foto: Bitmap?, contatoNome: String?, contatoTelefone: String?, mostrarEmergencia: Boolean
): Bitmap {
    val imagem = Bitmap.createBitmap(LARGURA, ALTURA, Bitmap.Config.ARGB_8888)
    val c = Canvas(imagem)
    val pintura = Paint(Paint.ANTI_ALIAS_FLAG)

    fun texto(t: String, x: Float, y: Float, tamanho: Float, negrito: Boolean, cor: Int, alinhamento: Paint.Align = Paint.Align.LEFT, largura: Float? = null) {
        pintura.reset(); pintura.isAntiAlias = true
        pintura.color = cor; pintura.textSize = tamanho; pintura.textAlign = alinhamento
        pintura.typeface = if (negrito) Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD) else Typeface.SANS_SERIF
        var t2 = t
        if (largura != null) while (t2.length > 3 && pintura.measureText(t2) > largura) t2 = t2.dropLast(2)
        c.drawText(t2, x, y, pintura)
    }

    // fundo
    val retangulo = RectF(0f, 0f, LARGURA.toFloat(), ALTURA.toFloat())
    pintura.shader = LinearGradient(0f, 0f, LARGURA.toFloat(), ALTURA.toFloat(), 0xFFE8112D.toInt(), 0xFF8A0A20.toInt(), Shader.TileMode.CLAMP)
    c.drawRoundRect(retangulo, 28f, 28f, pintura)
    pintura.reset(); pintura.isAntiAlias = true
    c.save()
    val recorte = Path().apply { addRoundRect(retangulo, 28f, 28f, Path.Direction.CW) }
    c.clipPath(recorte)
    pintura.color = 0x12FFFFFF
    c.drawCircle(LARGURA - 40f, 40f, 190f, pintura)
    c.restore()

    // topo
    texto("🩸 Hemare", 36f, 52f, 26f, true, 0xFFFFFFFF.toInt())
    texto("CARTEIRINHA DO DOADOR", LARGURA - 36f, 50f, 14f, true, 0xCCFFFFFF.toInt(), Paint.Align.RIGHT)

    // foto
    val cx = 36f + 70f
    val cy = 120f + 70f
    c.save()
    val circulo = Path().apply { addCircle(cx, cy, 70f, Path.Direction.CW) }
    c.clipPath(circulo)
    pintura.reset(); pintura.color = 0xFFFFE1E5.toInt()
    c.drawRect(cx - 70, cy - 70, cx + 70, cy + 70, pintura)
    if (foto != null) c.drawBitmap(Bitmap.createScaledBitmap(foto, 140, 140, true), cx - 70, cy - 70, null)
    else texto("🩸", cx - 34, cy + 22, 68f, false, 0xFFC8102E.toInt())
    c.restore()
    pintura.reset(); pintura.isAntiAlias = true; pintura.style = Paint.Style.STROKE; pintura.strokeWidth = 5f; pintura.color = 0xFFFFFFFF.toInt()
    c.drawCircle(cx, cy, 70f, pintura)

    // dados
    val exibido = nomeSocial?.takeIf { it.isNotBlank() } ?: nome
    texto(exibido, 200f, 150f, 30f, true, 0xFFFFFFFF.toInt(), largura = 300f)
    if (!nomeSocial.isNullOrBlank()) texto("Nome civil: $nome", 200f, 176f, 14f, false, 0xCCFFFFFF.toInt(), largura = 300f)
    texto("${conquistas.nivel.icone} ${conquistas.nivel.nome}", 200f, 212f, 18f, true, 0xFFFFFFFF.toInt(), largura = 300f)
    val doacoes = "${conquistas.totalDoacoes} " + (if (conquistas.totalDoacoes == 1) "doação" else "doações") +
        (doadorDesde?.let { " · doador desde %02d/%04d".format(it.monthValue, it.year) } ?: "")
    texto(doacoes, 200f, 238f, 15f, false, 0xE6FFFFFF.toInt(), largura = 300f)
    val local = listOfNotNull(cidade.takeIf { it.isNotBlank() }, estado?.takeIf { it.isNotBlank() }).joinToString(" - ")
    if (local.isNotEmpty()) texto("📍 $local", 200f, 262f, 15f, false, 0xE6FFFFFF.toInt(), largura = 300f)

    // tipo sanguíneo
    val rotuloTipo = if (tipoSanguineo.startsWith("Rh nulo")) "Rh nulo" else tipoSanguineo
    pintura.reset(); pintura.isAntiAlias = true; pintura.color = 0xFFFFFFFF.toInt()
    c.drawCircle(LARGURA - 36f - 66f, 190f, 66f, pintura)
    texto(rotuloTipo, LARGURA - 36f - 66f, 190f + (if (rotuloTipo.length > 3) 11f else 20f), if (rotuloTipo.length > 3) 30f else 52f, true, 0xFFC8102E.toInt(), Paint.Align.CENTER, 112f)
    texto("TIPO SANGUÍNEO", LARGURA - 36f - 66f, 282f, 11f, true, 0xD9FFFFFF.toInt(), Paint.Align.CENTER, 130f)

    // rodapé
    pintura.reset(); pintura.isAntiAlias = true; pintura.color = 0x38000000
    c.drawRoundRect(RectF(20f, ALTURA - 106f, LARGURA - 20f, ALTURA - 20f), 18f, 18f, pintura)
    if (mostrarEmergencia && !contatoNome.isNullOrBlank() && !contatoTelefone.isNullOrBlank()) {
        texto("EM EMERGÊNCIA AVISAR", 40f, ALTURA - 76f, 12f, true, 0xBFFFFFFF.toInt())
        texto("$contatoNome · $contatoTelefone", 40f, ALTURA - 50f, 20f, true, 0xFFFFFFFF.toInt(), largura = LARGURA - 80f)
    } else {
        texto("Doe sangue, salve até 4 vidas", 40f, ALTURA - 56f, 20f, true, 0xFFFFFFFF.toInt(), largura = LARGURA - 80f)
    }
    texto("Cartão informativo: não substitui exame nem documento oficial.", 40f, ALTURA - 28f, 11f, false, 0xB3FFFFFF.toInt(), largura = LARGURA - 80f)
    return imagem
}
