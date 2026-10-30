package com.example.hemaremobile.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationManager
import android.net.Uri

fun abrirUrl(contexto: Context, url: String) {
    try {
        contexto.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (e: Exception) { /* sem app para abrir o link */ }
}

fun ligar(contexto: Context, telefone: String) {
    val so = telefone.filter { it.isDigit() || it == '+' }
    try {
        contexto.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$so")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (e: Exception) { /* aparelho sem telefone */ }
}

fun abrirNoMapa(contexto: Context, nome: String, lat: Double, lng: Double) {
    abrirUrl(contexto, "geo:$lat,$lng?q=" + Uri.encode("$lat,$lng($nome)"))
}

fun compartilharTexto(contexto: Context, texto: String) {
    val envio = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, texto)
    try {
        contexto.startActivity(Intent.createChooser(envio, "Compartilhar").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (e: Exception) { /* nada para compartilhar */ }
}

fun compartilharWhatsApp(contexto: Context, texto: String) {
    abrirUrl(contexto, "https://wa.me/?text=" + Uri.encode(texto))
}

fun copiarTexto(contexto: Context, texto: String) {
    val cm = contexto.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
    cm.setPrimaryClip(android.content.ClipData.newPlainText("Hemare", texto))
}

/** Última posição conhecida do aparelho (precisa da permissão de localização já concedida). */
@SuppressLint("MissingPermission")
fun ultimaLocalizacao(contexto: Context): Location? {
    val lm = contexto.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    return try {
        listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER)
            .mapNotNull { if (lm.isProviderEnabled(it) || it == LocationManager.PASSIVE_PROVIDER) lm.getLastKnownLocation(it) else null }
            .maxByOrNull { it.time }
    } catch (e: SecurityException) {
        null
    }
}

/** Lê a foto escolhida, recorta ao centro em quadrado e reduz para 256x256 (apaga também os metadados da câmera). */
fun lerFoto(contexto: Context, uri: Uri): android.graphics.Bitmap? {
    return try {
        val limites = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
        contexto.contentResolver.openInputStream(uri)?.use { android.graphics.BitmapFactory.decodeStream(it, null, limites) }
        var amostra = 1
        while (limites.outWidth / (amostra * 2) >= 512 && limites.outHeight / (amostra * 2) >= 512) amostra *= 2
        val opcoes = android.graphics.BitmapFactory.Options().apply { inSampleSize = amostra }
        val original = contexto.contentResolver.openInputStream(uri)?.use { android.graphics.BitmapFactory.decodeStream(it, null, opcoes) } ?: return null
        val lado = minOf(original.width, original.height)
        val recorte = android.graphics.Bitmap.createBitmap(original, (original.width - lado) / 2, (original.height - lado) / 2, lado, lado)
        android.graphics.Bitmap.createScaledBitmap(recorte, 256, 256, true)
    } catch (e: Exception) {
        null
    }
}

/** Salva a imagem na galeria (Pictures/Hemare). Devolve true se deu certo. */
fun salvarNaGaleria(contexto: Context, imagem: android.graphics.Bitmap, nome: String): Boolean {
    return try {
        val valores = android.content.ContentValues().apply {
            put(android.provider.MediaStore.Images.Media.DISPLAY_NAME, nome)
            put(android.provider.MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(android.provider.MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Hemare")
        }
        val uri = contexto.contentResolver.insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, valores) ?: return false
        contexto.contentResolver.openOutputStream(uri)?.use { imagem.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
        true
    } catch (e: Exception) {
        false
    }
}
