package com.example.hemaremobile.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val HemareLightColorScheme = lightColorScheme(
    primary = HemareVermelho,
    onPrimary = HemareSuperficie,
    secondary = HemareAmarelo,
    onSecondary = HemareTextoEscuro,
    tertiary = HemareVerde,
    background = HemareFundo,
    onBackground = HemareTextoEscuro,
    surface = HemareSuperficie,
    onSurface = HemareTextoEscuro,
    surfaceVariant = HemareRosaClaro,
    onSurfaceVariant = HemareTextoSuave,
    error = HemareVermelhoEscuro
)

private val HemareDarkColorScheme = darkColorScheme(
    primary = HemareVermelhoAcao,
    onPrimary = HemareSuperficie,
    secondary = HemareAmarelo,
    onSecondary = HemareTextoEscuro,
    tertiary = HemareVerde,
    background = HemareFundoEscuro,
    onBackground = HemareTextoClaro,
    surface = HemareSuperficieEscura,
    onSurface = HemareTextoClaro,
    surfaceVariant = HemareSuperficieEscura,
    onSurfaceVariant = HemareTextoSuaveEscuro,
    error = HemareVermelhoEscuro
)

/** Alto contraste: preto/branco/amarelo puros, igual ao modo de acessibilidade do site. */
private val HemareAltoContrasteColorScheme = darkColorScheme(
    primary = HemareContrasteDestaque,
    onPrimary = HemareContrasteFundo,
    secondary = HemareContrasteDestaque,
    onSecondary = HemareContrasteFundo,
    tertiary = HemareContrasteDestaque,
    background = HemareContrasteFundo,
    onBackground = HemareContrasteTexto,
    surface = HemareContrasteSuperficie,
    onSurface = HemareContrasteTexto,
    surfaceVariant = HemareContrasteFundo,
    onSurfaceVariant = HemareContrasteDestaque,
    error = HemareContrasteErro,
    outline = HemareContrasteTexto
)

/**
 * Tema do Hemare Mobile. Sem dynamic color: a marca tem uma identidade
 * vermelho/dourado própria (igual ao site), então não deixamos o Android
 * substituir pela paleta do papel de parede do usuário.
 */
@Composable
fun HemareMobileTheme(
    darkTheme: Boolean = false,
    altoContraste: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        altoContraste -> HemareAltoContrasteColorScheme
        darkTheme -> HemareDarkColorScheme
        else -> HemareLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
