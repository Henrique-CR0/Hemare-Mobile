package com.example.hemaremobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.hemaremobile.navigation.HemareApp
import com.example.hemaremobile.ui.theme.HemareMobileTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val configuracaoViewModel: ConfiguracaoViewModel = viewModel()

            // O visual do Hemare (fundo escuro + destaques vermelhos) é fixo por
            // enquanto — o switch "Tema escuro" em Configuração ainda não liga
            // a nada (ver observação do usuário: nav/telas não precisam ter
            // funcionalidade real ainda).
            HemareMobileTheme(darkTheme = true) {
                HemareApp(configuracaoViewModel = configuracaoViewModel)
            }
        }
    }
}
