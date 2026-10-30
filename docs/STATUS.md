# Status Report do Desenvolvimento — Hemare Mobile

> **Posição em 30/10/2026**, sobre a branch `developer`. Os **números** da seção 2 foram contados no código e na última execução dos testes (`gradlew :app:testDebugUnitTest`). O restante é análise escrita por uma pessoa e deve ser relida a cada rodada. O formato segue o `docs/STATUS.md` do site, e a tela **Admin → Status** do app mostra um resumo dele.

## 1. Resumo em 30 segundos

**Saúde geral: 🟢 verde nas regras e nas telas, 🟡 amarelo na integração.** Todas as telas do site existem em formato mobile e as regras de negócio foram portadas e testadas. O app guarda os dados num banco SQLite do aparelho e tem um cliente da API do backend, mas **ainda não foi testado contra um servidor real** e o banco não foi exercitado em aparelho. Por isso ele serve hoje para **demonstração e piloto fechado**, não para publicação.

| Área | Situação | Em uma frase |
|---|:-:|---|
| Funcionalidades | 🟢 | Visitante, doador, hospital e administrador têm as mesmas telas e regras do site. |
| Qualidade e testes | 🟢 | 98 testes unitários das regras e da loja, todos passando. Nenhum teste de tela ou de aparelho ainda. |
| Banco de dados | 🟡 | SQLite local com tabelas por entidade; compila, mas não foi executado em emulador/celular nem coberto por teste. |
| Integração com a API | 🟡 | Cliente HTTP com login, locais, estoque público e notícias, conforme `docs/API.md` do site; sem servidor para testar. |
| Segurança e privacidade | 🟡 | Senha só como resumo SHA-256 no aparelho e privacidade do doador testada; o site usa bcrypt e token no servidor. |
| Documentação | 🟢 | README, este relatório e comentários nas regras apontando o arquivo do site de onde vieram. |

## 2. Números

| Indicador | Valor |
|---|---|
| Testes automáticos | **98 testes** em 21 classes (5 arquivos), **todos passando** ✅ (0 falhas, 0 ignorados) |
| Código do app | 38 arquivos Kotlin · 7.937 linhas (antes do banco e da API) |
| Código de testes | 808 linhas |
| Telas | 38 rotas: públicas, de entrada, do doador, do hospital, do administrador, mais o portão de acesso |
| Tabelas do banco | 15 (usuários, doadores, hospitais, locais, doações, estoque, necessidades, vagas, agendamentos, casos, campanhas, pedidos raros, notícias, estoque externo, meta) |
| Rotas da API usadas | 4 de 91: `/auth/login`, `/locais`, `/hospital/estoque-publico` e `/noticias/feed` (mais `/saude` para conferir a conexão) |
| Histórico | 18 commits locais; última publicação no GitHub: commit `003f31a` |

## 3. O que foi entregue

| Fase | O que o usuário ganhou |
|---|---|
| **Apresentação** (início do projeto) | Abas Início, Lista e Configuração, tema escuro, alto contraste, botão de Libras, login e painel de hospital simples. |
| **Domínio** | Regras do backend do site em Kotlin puro: triagem, elegibilidade, compatibilidade ABO/Rh, agenda, gamificação, apadrinhamento, campanhas, sangue raro, ficha PcD, notícias e cadeia de confiança (hash SHA-256). |
| **Telas equivalentes ao site** | Navegação por tipo de conta com portões de acesso; doador (agenda, histórico, perfil, carteirinha, LGPD), hospital (estoque, necessidades, agenda, campanhas, raro, radar) e administrador (verificação, notícias, status). |
| **Integração e testes** (esta rodada) | Banco SQLite com a sessão e os dados guardados entre aberturas; cliente da API com tela de servidor em Configuração; senhas guardadas só como resumo; 98 testes unitários. |

## 4. Como os componentes se ligam

```
Telas (Compose)  →  HemareStore (ViewModel, Estado imutável)  →  Regras do domínio (Kotlin puro)
                          │
                          ├── RepositorioEstado → BancoHemare (SQLite) ── grava 300 ms depois da última mudança
                          └── ApiHemare (HTTP/JSON) → backend do Hemare (opcional, endereço em Configuração)
```

- As telas só leem o `Estado` e chamam operações da loja; nunca falam com o banco nem com a rede.
- Sem servidor configurado, o app usa os dados do aparelho. Com servidor, **Configuração → Salvar e sincronizar** troca locais, termômetro de estoque e notícias pelos do servidor, mantendo as coordenadas que o servidor não envia.
- Notícias também podem ser coletadas direto dos feeds RSS (Admin → Notícias), e o CEP vem do ViaCEP.

## 5. Riscos e pendências

| # | Risco | Gravidade | O que fazer |
|---|---|:-:|---|
| R1 | O cliente da API nunca falou com um servidor de verdade; os formatos foram lidos do código do backend, não observados. | 🟡 | Subir o backend (`npm start`), apontar o app para ele e conferir cada rota. Depois usar o login real em vez das contas locais. |
| R2 | O banco SQLite não foi executado em aparelho nem tem teste (precisa de teste instrumentado). | 🟡 | Rodar no emulador, fechar e reabrir o app; escrever `androidTest` de salvar e carregar. |
| R3 | Contas e dados de doação existem só no aparelho: nada é compartilhado entre pessoas. | 🔴 para produção | Passar login, agenda e doações para a API (a maior parte das 91 rotas ainda não é usada). |
| R4 | Senha local com SHA-256 e sal fixo, não bcrypt; o banco não é criptografado. | 🟡 | Resolvido de verdade quando o login passar a ser do servidor (R3). |
| R5 | A recuperação de senha mostra o código na tela (não há e-mail). | 🟡 | Usar `/recuperacao` do servidor. |
| R6 | Não há teste de tela (Compose) nem de navegação; só regras e loja. | 🟡 | Adicionar testes de UI para os portões de acesso e para as abas por tipo de conta. |
| R7 | O botão de Libras não traduz; o mapa abre um aplicativo externo. | 🟢 | Embutir o VLibras e um mapa na tela, se for prioridade. |

## 6. Como rodar os testes

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
$env:JAVA_TOOL_OPTIONS = "-Djdk.net.unixdomain.tmpdir=C:\tmpjdk -Djava.io.tmpdir=C:\tmpjdk"
.\gradlew.bat --offline --no-daemon :app:testDebugUnitTest
```

O relatório HTML fica em `app/build/reports/tests/testDebugUnitTest/index.html`. Os testes rodam na JVM, sem aparelho. Por isso não cobrem o que usa `org.json` ou o SQLite do Android (exportar dados, banco, respostas da API).

## 7. Próximos passos, em ordem

1. Rodar o app num emulador e conferir a persistência (R2).
2. Subir o backend do site e validar o cliente da API (R1).
3. Trocar login, agenda e doações para a API (R3, R4, R5).
4. Testes de tela e testes instrumentados do banco (R2, R6).
