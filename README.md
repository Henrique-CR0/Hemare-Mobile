# 🩸 Hemare Mobile

Versão Android nativa do [Hemare](https://github.com/Henrique-CR0/Hemare), plataforma que conecta doadores de sangue a hemocentros e hospitais, incentivando a doação voluntária e regular.

Este repositório contém o app completo em Jetpack Compose, com as regras do site portadas para Kotlin e dados locais de demonstração (ainda sem chamadas ao backend do Hemare).

## Tecnologias

- **Kotlin** 2.2.10
- **Jetpack Compose** (Material 3) + Compose BOM 2026.02.01
- **Navigation Compose** — barra de abas por tipo de conta + rotas com argumentos
- **MVVM** — `HemareStore` (ViewModel) expõe um `Estado` imutável via `StateFlow`
- `compileSdk` / `targetSdk` 37, `minSdk` 26 (Android 8.0+)

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
2. Clique em **Create Device**, escolha um modelo (ex.: Pixel 8) e uma versão do Android (qualquer uma a partir da 8.0 / API 26), e finalize.

**Opção B — Celular físico:**
1. No celular, ative as **Opções do desenvolvedor** (Configurações → Sobre o telefone → toque 7x em "Número da versão").
2. Dentro de Opções do desenvolvedor, ative a **Depuração USB**.
3. Conecte o celular ao computador via USB e aceite a autorização que aparece na tela do celular.

### 5. Rodar
Escolha o emulador ou dispositivo no seletor ao lado do botão de play (topo do Android Studio) e clique no botão verde **▶**. O app compila, instala e abre sozinho.

Não é necessário configurar chaves de API, variáveis de ambiente ou backend — esta versão funciona inteiramente com dados de exemplo (mock), local no próprio app.

## Funcionalidades

O app reproduz as telas e as regras do site (versão de 30/10/2026) em formato mobile. A barra de baixo muda conforme o tipo de conta:

| Conta | Abas |
|---|---|
| Visitante | Início · Lista · Entrar · Configuração |
| Doador | Início · Lista · Minha área · Configuração |
| Hospital | Minha área · Perfil · Lista · Configuração |
| Administrador | Admin · Status · Lista · Configuração |

Telas que exigem conta (triagem, apadrinhar, agenda...) mostram o **portão de acesso** ao visitante e voltam para a tela pedida depois do login. Hospital só opera depois de **aprovado** por um administrador.

### Para todos
Início, Lista (menu muda por tipo de conta), Onde doar (busca, GPS, distância, abrir no mapa e ligar), Termômetro de estoque, Previsão do sangue (clima por cidade), Notícias (coletadas de feeds RSS reais, com filtros por categoria e estado), Guia da doação, Mitos e verdades, Seus direitos, Placar das cidades e Campanhas "Quem doa por mim" (por código).

### Doador
- **Cadastro e login** com as validações do site, recuperação de senha (sem e-mail: o código aparece na tela, só demonstração) e código de convite ("Traga um amigo").
- **Posso doar?** com as regras novas de triagem (idade, peso, situações recentes, saúde).
- **Minha área**: nível, emblemas, próxima doação, lembrete de retorno, afilhados, promessas, convite de amigos.
- **Agendar doação** (hospital, dia, horário, regras de intervalo e limite anual) e **Minhas doações** (linha do tempo, cancelar, remarcar).
- **Apadrinhar** pacientes e **prometer** doação em campanhas.
- **Meu perfil**: dados, foto, privacidade (anônimo/identificado, com a prévia do que o hospital vê), contato de emergência, **rede de sangue raro**, **Ficha PcD**, **carteirinha digital** (salva na galeria), troca de senha, baixar meus dados e excluir a conta (LGPD).

### Hospital
Termômetro do estoque, publicar necessidades (com alerta a doadores compatíveis e aptos), doadores compatíveis, abrir horários em lote e acompanhar a agenda do dia (compareceu/faltou), pacientes para apadrinhamento, campanhas de reposição, pedidos de sangue raro, **Radar de aptidão** e perfil da instituição (CNPJ, CNES, busca de endereço por CEP, status da verificação).

### Administrador
Verificação de hospitais (aprovar, recusar com motivo, revogar), coleta de notícias ("Coletar agora" e situação por fonte) e Status do desenvolvimento.

### Configuração
Tema claro/escuro, alto contraste, tamanho do texto (A− / A+), Libras (botão flutuante; a tradução não está embutida), notificações, conta e sair.

## Dados, banco e servidor

- **Banco de dados local (SQLite):** contas, doações, agenda, campanhas e a sessão ficam guardados no aparelho (`data/local/BancoHemare.kt`, 15 tabelas) e voltam quando o app é reaberto. A senha é guardada só como resumo SHA-256.
- **Servidor do Hemare (opcional):** em **Configuração → Servidor do Hemare**, informe o endereço do backend (ex.: `http://192.168.0.10:3000`). O app baixa locais, termômetro de estoque e notícias pela API (`data/api/ApiHemare.kt`, conforme o `docs/API.md` do site). Sem servidor, usa os dados do aparelho.
- **Ainda não validado:** o cliente da API nunca falou com um servidor real, e o banco não foi testado em aparelho. Veja os riscos em [docs/STATUS.md](docs/STATUS.md).
- Conexões diretas da internet: notícias (RSS), CEP (ViaCEP), GPS, compartilhar/WhatsApp/ligar e abrir mapa (aplicativo externo).
- Contas de teste: `doador@hemare.com` / `doador123` · `hospital@hemare.com` / `hospital123` · `pendente@hemare.com` / `hospital123` (hospital em análise) · `admin@hemare.com` / `admin1234`.

## Testes

98 testes unitários (regras do domínio e a loja), todos passando:

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
$env:JAVA_TOOL_OPTIONS = "-Djdk.net.unixdomain.tmpdir=C:\tmpjdk -Djava.io.tmpdir=C:\tmpjdk"
.\gradlew.bat --offline --no-daemon :app:testDebugUnitTest
```

No Android Studio: botão direito em `app/src/test` → **Run Tests**. O **Status Report do desenvolvimento** está em [docs/STATUS.md](docs/STATUS.md).

## Estrutura do projeto

```
app/src/main/java/com/example/hemaremobile/
├── MainActivity.kt, ConfiguracaoScreen/ViewModel.kt, BotaoLibras.kt, CabecalhoVermelho.kt
├── navigation/Navegacao.kt   # abas por tipo de conta, guarda de rotas (portões), destino de retorno
├── domain/                   # regras puras portadas do site (triagem, elegibilidade, agenda, gamificação...)
├── data/                     # Estado imutável, HemareStore (ViewModel), dados de exemplo, operações por papel, RSS e CEP
│   ├── local/                # BancoHemare (SQLite) e CodecEstado (JSON das tabelas)
│   └── api/                  # ApiHemare: cliente HTTP do backend do Hemare
└── ui/                       # Rotas/portões/menu (Acesso.kt), componentes, telas (públicas, entrada, doador, hospital, admin), tema
```

## Compilar pela linha de comando (Windows)

Se o caminho do usuário tem espaços, o Gradle falha ao abrir o socket. Use uma pasta temporária sem espaços:

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
$env:JAVA_TOOL_OPTIONS = "-Djdk.net.unixdomain.tmpdir=C:\tmpjdk -Djava.io.tmpdir=C:\tmpjdk"
.\gradlew.bat --offline --no-daemon :app:assembleDebug
```

## Roadmap

- [x] Telas e regras equivalentes às do site (visitante, doador, hospital, admin)
- [x] Banco de dados local e cliente da API (rotas públicas)
- [x] 98 testes unitários e Status Report
- [ ] Validar o cliente contra o backend e passar login, agenda e doações para a API
- [ ] Testes de tela e testes instrumentais do banco
- [ ] Mapa dentro do app e tradução real em Libras (VLibras)

## Autores

- Henrique Carneiro Ribeiro
- Lucas Pereira Vietiez
