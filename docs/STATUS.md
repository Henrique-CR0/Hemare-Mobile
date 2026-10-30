# Status Report do Desenvolvimento — Hemare Mobile

> **Posição em 30/10/2026**, sobre a branch `developer` (commit `089a66a` e seguintes). Os **números** da seção 2 foram contados no código e na última execução dos testes (`gradlew :app:testDebugUnitTest`). A seção 4 diz **como cada coisa foi verificada**, e o restante é análise escrita por uma pessoa, que deve ser relida a cada rodada. O formato segue o `docs/STATUS.md` do site, e a tela **Admin → Status** do app mostra um resumo dele.

## 1. Resumo em 30 segundos

**Saúde geral: 🟢 verde nas regras, nas telas e nos testes; 🟡 amarelo na integração com o backend real.**

Todas as telas do site existem em formato mobile, com as mesmas regras de negócio, e 107 testes unitários passam. O app abre e navega num emulador Android 17. O banco SQLite é criado e preenchido no aparelho, e o cliente da API **sincronizou com sucesso** com um servidor de demonstração, gravando os dados no banco. **Falta validar com o backend real do site** (que exige PostgreSQL) e passar login, agenda e doações para a API: hoje as contas e as doações existem só no aparelho. Por isso o app serve para **demonstração e piloto fechado**, não para publicação.

| Área | Situação | Em uma frase |
|---|:-:|---|
| Funcionalidades | 🟢 | Visitante, doador, hospital e administrador têm as telas e as regras do site. |
| Qualidade e testes | 🟢 | 107 testes unitários das regras, da loja e dos lembretes, todos passando. Sem teste de tela nem de aparelho. |
| Serviços do Android | 🟢 | Notificações de lembrete (retorno e doação agendada) agendadas no AlarmManager; verificadas no emulador. |
| Banco de dados | 🟢 | SQLite com 15 tabelas, criado e preenchido no emulador. Falta provar a sessão ao reabrir o app e um teste automatizado. |
| Integração com a API | 🟡 | Cliente funcionando contra um servidor de demonstração (4 rotas públicas); sem teste contra o backend real. |
| Segurança e privacidade | 🟡 | Senha só como resumo SHA-256, privacidade do doador testada; o site usa bcrypt e token no servidor. |
| Documentação | 🟢 | README (com os passos para demonstrar a API), este relatório e comentários nas regras. |

## 2. Números

| Indicador | Valor |
|---|---|
| Testes automáticos | **107 testes** em 22 classes (6 arquivos), **todos passando** ✅ (0 falhas, 0 erros, 0 ignorados) |
| Código do app | 41 arquivos Kotlin · 8.187 linhas |
| Código de testes | 905 linhas |
| Rotas de tela | 28 registradas na navegação (públicas, entrada, doador, hospital, administrador) |
| Tabelas do banco | 15 (usuários, doadores, hospitais, locais, doações, estoque, necessidades, vagas, agendamentos, casos, campanhas, pedidos raros, notícias, estoque externo, meta) |
| Rotas da API usadas pelo app | 4 na sincronização (`/locais`, `/hospital/estoque-publico`, `/noticias/feed`) mais `/saude`; `/auth/login` está implementada no cliente, mas **nenhuma tela a usa ainda**. O backend tem 91 rotas. |
| Ambiente de teste | Emulador Pixel 10, Android 17 (API 37); AGP 9.3.2, Gradle 9.5.0, Kotlin 2.2.10 |
| Histórico | 24 commits; tudo publicado no GitHub (`master` e `developer` iguais) |

## 3. O que foi entregue

| Fase | O que o usuário ganhou |
|---|---|
| **Apresentação** | Abas Início, Lista e Configuração, tema escuro, alto contraste, botão de Libras, login e painel de hospital simples. |
| **Domínio** | Regras do backend do site em Kotlin puro: triagem, elegibilidade, compatibilidade ABO/Rh, agenda, gamificação, apadrinhamento, campanhas, sangue raro, ficha PcD, notícias e cadeia de confiança (hash SHA-256). |
| **Telas equivalentes ao site** | Navegação por tipo de conta com portões de acesso; doador (agenda, histórico, perfil, carteirinha, LGPD), hospital (estoque, necessidades, agenda, campanhas, raro, radar) e administrador (verificação, notícias, status). |
| **Banco, API e testes** | Banco SQLite com sessão e dados guardados entre aberturas; cliente da API com tela de servidor em Configuração; senhas guardadas só como resumo; 98 testes unitários. |
| **Serviços do Android** | Notificações de lembrete: aviso de quando a pessoa pode doar de novo (às 9h) e da doação agendada (véspera às 18h e 2 horas antes), agendadas no `AlarmManager`, que sobrevivem a fechar o app e a reiniciar o aparelho; permissão do Android 13+; botão de teste na Configuração. |
| **Verificação no emulador** | App aberto e navegado no Android 17; servidor de demonstração (`tools/servidor-demo`); sincronização comprovada de ponta a ponta; botões de ação fixos no rodapé de login, cadastro e completar perfil. |

## 4. Como cada coisa foi verificada

| Item | Como | Resultado |
|---|---|---|
| Regras de negócio e loja | 98 testes unitários na JVM | ✅ passam |
| Compilação e instalação | `assembleDebug` e instalação no emulador Pixel 10 (Android 17) | ✅ |
| Abertura e navegação | Uso manual: início, entrar, configuração, login, cadastro | ✅ |
| Banco criado e preenchido | Database Inspector do Android Studio e leitura do arquivo `hemare.db`: 15 tabelas, 16 usuários, 15 doações, 210 vagas | ✅ |
| Senha protegida | Leitura da coluna `senha`: só um resumo de 64 caracteres | ✅ |
| Integração com a API | `GET /saude`, `/locais`, `/hospital/estoque-publico` e `/noticias/feed` vistos no terminal do servidor; 3 locais, 4 níveis de estoque e 3 notícias gravados no banco | ✅ contra servidor de demonstração |
| Notificação de teste | Botão em Configuração: sem permissão o app avisa; ao ligar pede a permissão do sistema; com ela a notificação aparece na bandeja (conferida no `dumpsys notification`) | ✅ |
| Alarmes do lembrete | Agendar uma doação registrou `hemare.lembrete.2` (véspera, 02/11 18:00) e `.3` (2 h antes, 03/11 08:00) no AlarmManager; cancelar o agendamento os removeu (`Reason=alarm_cancelled`) | ✅ |
| Notificação disparada pelo alarme na hora marcada | Não esperei o horário chegar; a exibição foi testada só pelo botão de teste | ⚠️ pendente |
| Alarmes depois de reiniciar o aparelho | `ReinicioReceiver` escrito, nunca executado | ⚠️ pendente |
| Pedido da permissão de rede local | Caixa do sistema aparece, e a sincronização continua sozinha ao tocar em "Allow" | ✅ |
| **Sessão volta ao reabrir o app** | Tentei por script e não concluí o login; **não foi confirmado** | ⚠️ pendente |
| Backend real do site | Não há PostgreSQL nem Docker nesta máquina | ❌ não testado |
| Build de lançamento (release) | Nunca gerado | ❌ não testado |
| Testes de tela e do banco no aparelho | Não existem | ❌ não escritos |

## 5. Problemas encontrados e corrigidos nesta rodada

| Problema | Causa | Correção |
|---|---|---|
| O app fechava ao abrir | O banco foi criado pela metade (emulador encerrado durante a criação) e a leitura falhava | O banco ilegível agora é apagado e recriado em vez de derrubar o app |
| Sincronização dava "Não consegui falar com o servidor" | O Android 17 exige a permissão `ACCESS_LOCAL_NETWORK` para a rede local; o app a pede agora antes de sincronizar | Permissão declarada e pedida em tempo de execução |
| `http://` bloqueado | O Android recusa tráfego sem HTTPS | Liberado **só na versão debug**; a de lançamento exige HTTPS |
| Botão de ação sumia ao rolar | Ficava no fim da lista | Botão fixo no rodapé de login, cadastro e completar perfil |
| Junção de locais perdia as coordenadas | Nomes com espaços diferentes não casavam | Comparação ignorando espaços e acentos (teste cobre) |

## 6. Riscos e pendências

| # | Risco | Gravidade | O que fazer |
|---|---|:-:|---|
| R1 | O cliente da API só foi validado contra um **servidor de demonstração** escrito por nós; o backend real pode ter respostas diferentes. | 🟡 | Subir o backend (`npm start` com PostgreSQL), apontar o app para ele e conferir cada rota. |
| R2 | Contas e doações existem só no aparelho: nada é compartilhado entre pessoas. | 🔴 para produção | Passar login (`/auth/login`), agenda e doações para a API. A maior parte das 91 rotas ainda não é usada. |
| R3 | A sessão após reabrir o app não foi confirmada, e o banco não tem teste automatizado. | 🟡 | Ensaiar no emulador e escrever `androidTest` de salvar e carregar. |
| R4 | Senha local com SHA-256 e sal fixo, não bcrypt; o banco não é criptografado. | 🟡 | Resolvido de verdade quando o login passar a ser do servidor (R2). |
| R5 | A recuperação de senha mostra o código na tela (não há e-mail). | 🟡 | Usar `/recuperacao` do servidor. |
| R6 | Sem teste de tela (Compose) nem de navegação. | 🟡 | Testar os portões de acesso e as abas por tipo de conta. |
| R7 | O Android Studio sugere atualizar para AGP 9.5.0 e Gradle 9.6.0; não foi adotado nem testado. | 🟢 | Manter 9.3.2 / 9.5.0 até haver tempo para testar a atualização. |
| R8 | O botão de Libras não traduz; o mapa abre um aplicativo externo. | 🟢 | Embutir o VLibras e um mapa na tela, se for prioridade. |
| R10 | O aviso de lembrete usa alarme não exato: pode atrasar alguns minutos, e fabricantes com economia de bateria agressiva podem adiá-lo. O disparo na hora marcada e o reagendamento após reiniciar ainda não foram vistos. | 🟡 | Testar com o horário próximo e reiniciando o emulador; avaliar `WorkManager` se for preciso. |
| R9 | Os formulários longos (perfil do doador) ainda têm o botão "Salvar" no fim da rolagem. | 🟢 | Fixar o botão no rodapé dessas telas também. |

## 7. Como demonstrar e como rodar

- **App:** Android Studio → configuração `app` → ▶ no emulador `Pixel_10`.
- **Servidor de demonstração:** `node tools/servidor-demo/servidor.js 3000`; no app, **Configuração → Servidor do Hemare → `http://10.0.2.2:3000` → Salvar e sincronizar**.
- **Banco ao vivo:** View → Tool Windows → App Inspection → Database Inspector → `hemare.db`.
- **Testes:**

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
$env:JAVA_TOOL_OPTIONS = "-Djdk.net.unixdomain.tmpdir=C:\tmpjdk -Djava.io.tmpdir=C:\tmpjdk"
.\gradlew.bat --offline --no-daemon :app:testDebugUnitTest
```

O relatório HTML dos testes fica em `app/build/reports/tests/testDebugUnitTest/index.html`. Os testes rodam na JVM, sem aparelho, e por isso não cobrem o que usa `org.json` ou o SQLite do Android (exportar dados, banco, respostas da API).

## 8. Próximos passos, em ordem

1. Ensaiar a sessão ao reabrir o app e escrever o teste do banco (R3).
2. Subir o backend real do site e validar o cliente da API (R1).
3. Trocar login, agenda e doações para a API (R2, R4, R5).
4. Testes de tela e de navegação (R6).
5. Fixar o botão "Salvar" no rodapé dos formulários longos (R9).
