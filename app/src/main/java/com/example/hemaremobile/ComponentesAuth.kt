package com.example.hemaremobile

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import com.example.hemaremobile.ui.theme.HemareVermelho

@Composable
fun CampoAuth(
    valor: String,
    aoAlterar: (String) -> Unit,
    rotulo: String,
    tipoTeclado: KeyboardType = KeyboardType.Text,
    ehSenha: Boolean = false,
    modifier: Modifier = Modifier
) {
    var mostrarSenha by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = valor,
        onValueChange = aoAlterar,
        label = { Text(rotulo) },
        singleLine = true,
        visualTransformation = if (ehSenha && !mostrarSenha) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = if (ehSenha) KeyboardType.Password else tipoTeclado),
        trailingIcon = if (ehSenha) {
            {
                IconButton(onClick = { mostrarSenha = !mostrarSenha }) {
                    Icon(
                        imageVector = if (mostrarSenha) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                        contentDescription = if (mostrarSenha) "Ocultar senha" else "Mostrar senha"
                    )
                }
            }
        } else null,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = HemareVermelho,
            cursorColor = HemareVermelho
        ),
        modifier = modifier.fillMaxWidth()
    )
}
