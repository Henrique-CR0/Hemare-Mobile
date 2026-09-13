# 🩸 Hemare Mobile

Versão Android nativa do [Hemare](https://github.com/Henrique-CR0/Hemare), plataforma que conecta doadores de sangue a hemocentros e hospitais, incentivando a doação voluntária e regular.

Este repositório contém a **Presentation Layer** do app: telas navegáveis em Jetpack Compose, com dados fixos (mock) por enquanto — sem chamadas reais ao backend do Hemare ainda. As camadas Domain / Repository / Data ficam para uma etapa futura.

## Tecnologias

- **Kotlin** 2.2.10
- **Jetpack Compose** (Material 3) + Compose BOM 2026.02.01
- **Navigation Compose** — bottom navigation com 3 abas + navegação aninhada dentro da aba Lista
- **MVVM** — cada tela com seu `ViewModel` expondo `UiState` (via `StateFlow`)
- `compileSdk` / `targetSdk` 37, `minSdk` 24 (Android 7.0+)

## Como rodar (em qualquer máquina)

### 1. Instalar o Android Studio
Baixe e instale o [Android Studio](https://developer.android.com/studio) (Windows, macOS ou Linux). Ele já vem com o JDK e o Android SDK necessários — não precisa instalar nada disso separadamente. Na primeira abertura, siga o assistente de configuração padrão (ele baixa as ferramentas do SDK sozinho).

### 2. Clonar o repositório
Com o [Git](https://git-scm.com/downloads) instalado, rode no terminal:
```bash
git clone https://github.com/Henrique-CR0/Hemare-Mobile.git
```
(Ou baixe o ZIP pelo botão **Code → Download ZIP** na página do repositório, se preferir não usar Git.)

### 3. Abrir o projeto
No Android Studio, vá em **File → Open** e selecione a pasta `Hemare-Mobile` que você acabou de clonar/extrair. Espere o Gradle sincronizar as dependências — aparece uma barra de progresso na parte inferior da janela, e pode demorar alguns minutos na primeira vez (baixando bibliotecas).

### 4. Preparar onde rodar o app
Você precisa de um emulador Android **ou** um celular físico:

**Opção A — Emulador (não precisa de celular):**
1. No Android Studio, abra o **Device Manager** (ícone de celular na barra lateral direita, ou **Tools → Device Manager**).
2. Clique em **Create Device**, escolha um modelo (ex.: Pixel 8) e uma versão do Android (qualquer uma a partir da 7.0 / API 24), e finalize.

**Opção B — Celular físico:**
1. No celular, ative as **Opções do desenvolvedor** (Configurações → Sobre o telefone → toque 7x em "Número da versão").
2. Dentro de Opções do desenvolvedor, ative a **Depuração USB**.
3. Conecte o celular ao computador via USB e aceite a autorização que aparece na tela do celular.

### 5. Rodar
Escolha o emulador ou dispositivo no seletor ao lado do botão de play (topo do Android Studio) e clique no botão verde **▶**. O app compila, instala e abre sozinho.

Não é necessário configurar chaves de API, variáveis de ambiente ou backend — esta versão funciona inteiramente com dados de exemplo (mock), local no próprio app.

## Funcionalidades

O app tem 3 abas principais, acessíveis pela barra inferior:

### 🏠 Início
Tela de boas-vindas com a mensagem principal do Hemare ("Sua doação pode salvar até 4 vidas"), cards com estatísticas rápidas (vidas salvas por doação, tempo médio de doação, percentual da população que doa) e uma ilustração decorativa.

### 📋 Lista
Hub de conteúdo educativo sobre doação de sangue, com 3 seções:

| Item | Conteúdo |
|---|---|
| **Onde doar** | Lista de hemocentros (dados de exemplo) com nome, cidade/estado, endereço e telefone, com campo de busca por cidade/estado. |
| **Guia de doação** | Orientações organizadas em 3 fases — antes, durante e depois de doar — adaptadas do guia do site Hemare. |
| **Mitos e verdades** | 16 afirmações comuns sobre doação de sangue, cada uma marcada como Mito / Verdade / Depende, com explicação baseada em fontes oficiais (Ministério da Saúde, Hemominas, Hemoce, Pró-Sangue). |

### ⚙️ Configuração
- **Preferências**: alternar entre tema claro e escuro (funcional — muda as cores do app inteiro) e ativar/desativar notificações (guardado apenas em memória por enquanto).
- **Sobre o app**: versão do app e créditos dos desenvolvedores.

## Estrutura do projeto

```
app/src/main/java/com/example/hemaremobile/
├── MainActivity.kt              # ponto de entrada, aplica o tema e monta o app
├── InicioScreen.kt               # tela Início
├── ListaScreen.kt                 # hub da aba Lista
├── OndeDoarScreen.kt             # sub-tela: hemocentros
├── GuiaScreen.kt                  # sub-tela: guia de doação
├── MitosScreen.kt                 # sub-tela: mitos e verdades
├── ConfiguracaoScreen.kt          # tela Configuração
├── ConfiguracaoViewModel.kt       # estado de preferências (tema, notificações)
├── CabecalhoVermelho.kt           # cabeçalho vermelho reutilizado nas telas
├── navigation/
│   └── Navegacao.kt               # bottom navigation + NavHost + rotas
└── ui/theme/
    ├── Color.kt                   # paleta de cores da marca Hemare
    ├── Theme.kt                   # esquemas de cor claro/escuro (Material 3)
    └── Type.kt                    # tipografia
```

## Roadmap

- [x] Presentation Layer (telas + navegação + dados mock)
- [ ] Domain Layer (regras de negócio — compatibilidade sanguínea, elegibilidade, distância)
- [ ] Repository/Data Layer (integração com a API do [backend Hemare](https://github.com/Henrique-CR0/Hemare/tree/main/backend))
- [ ] Persistência real de preferências
- [ ] Login e área do doador
- [ ] Localização real e mapa em "Onde doar"

## Autores

- Henrique Carneiro Ribeiro
- Lucas Pereira Vietiez
