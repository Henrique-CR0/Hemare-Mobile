package com.example.hemaremobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.hemaremobile.data.local.BancoHemare
import com.example.hemaremobile.services.AgendadorLembretes
import com.example.hemaremobile.data.HemareStore
import com.example.hemaremobile.navigation.HemareRaiz
import com.example.hemaremobile.ui.theme.HemareMobileTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val configuracaoViewModel: ConfiguracaoViewModel = viewModel()
            val contexto = applicationContext
            val store: HemareStore = viewModel(factory = viewModelFactory { initializer { HemareStore(BancoHemare(contexto), AgendadorLembretes(contexto)::reagendar) } })
            val configuracaoState by configuracaoViewModel.uiState.collectAsState()
            val densidade = LocalDensity.current

            CompositionLocalProvider(
                LocalDensity provides Density(densidade.density, densidade.fontScale * configuracaoState.escalaFonte)
            ) {
                HemareMobileTheme(
                    darkTheme = configuracaoState.temaEscuro,
                    altoContraste = configuracaoState.altoContraste
                ) {
                    HemareRaiz(configuracaoViewModel = configuracaoViewModel, store = store)
                }
            }
        }
    }
}
