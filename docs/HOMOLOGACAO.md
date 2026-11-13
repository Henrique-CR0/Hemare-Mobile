# Homologação do Sistema — Hemare Mobile

> **Data:** 12/11/2026 · **Versão:** 2.0.0 (build debug) · **Branch:** `developer` · **Quem verificou:** a equipe de desenvolvimento (não é uma homologação por usuário externo; ver `docs/TESTES_COM_USUARIOS.md`).

## 1. Objetivo e ambiente

Conferir se o app entrega o que foi combinado (as telas e as regras do site em formato mobile) e registrar, com evidência, **o que foi verificado, como, e o que ficou pendente**.

| Item | Valor |
|---|---|
| Aparelho | Emulador Pixel 10 (Android 17 / API 37) |
| Compilação | AGP 9.3.2 · Gradle 9.5.0 · Kotlin 2.2.10 · `minSdk` 26 |
| Dados | Contas e doações de demonstração (nada é pessoa real) |
| Servidor | Servidor de demonstração (`tools/servidor-demo`); o backend real do site **não** foi usado |

Legenda: ✅ verificado · ⚠️ verificado só em parte ou só por teste de regra · ❌ não verificado.

## 2. Verificação por perfil

### Visitante
| Requisito | Como foi verificado | |
|---|---|:-:|
| Ver Início, Lista, Onde doar, Estoque, Notícias, Guia, Mitos | Navegação manual no emulador (abas e menu) | ✅ |
| Portão de acesso: triagem e apadrinhar pedem conta | Tela "Conta necessária" aberta no emulador | ✅ |
| Entrar com e-mail e senha | Login manual, inclusive só com o teclado (Enter passa para a senha e envia) | ✅ |
| Cadastrar conta de doador | Testes de integração (cadastro, e-mail repetido, senha fraca) | ⚠️ tela não refeita nesta rodada |
| Recuperar senha | Teste da loja (código e nova senha); a tela mostra o código, porque não há e-mail | ⚠️ |

### Doador
| Requisito | Como foi verificado | |
|---|---|:-:|
| Agendar, reagendar e cancelar doação | Manual no emulador (agendar e cancelar) e teste de integração (conflito, reagendar, lotação) | ✅ |
| Aviso de retorno e de doação agendada | Alarmes vistos no sistema; notificação vista na bandeja; regra por teste | ✅ (disparo na hora marcada ❌) |
| Privacidade: anônimo não aparece ao hospital | Teste de integração (match e agenda do hospital) | ✅ |
| Excluir a conta (LGPD) | Teste de integração: dados pessoais saem e a cadeia continua íntegra | ✅ |
| Perfil, carteirinha, ficha PcD, rede de sangue raro | Regras testadas; telas **não** foram reabertas nesta rodada | ⚠️ |

### Hospital
| Requisito | Como foi verificado | |
|---|---|:-:|
| Painel e termômetro do estoque | Manual no emulador (níveis por tipo, "Hospital verificado") | ✅ |
| Hospital pendente não opera até ser aprovado | Teste de integração (cadastro → pendente → bloqueado → aprovado → opera) | ✅ |
| Publicar necessidade, alerta e doadores compatíveis | Teste de integração | ✅ |
| Confirmar doação entra na cadeia de confiança | Teste de integração (cadeia íntegra depois) | ✅ |
| Agenda, campanhas, sangue raro, radar | Regras testadas; telas não reabertas nesta rodada | ⚠️ |

### Administrador
| Requisito | Como foi verificado | |
|---|---|:-:|
| Verificar hospitais (aprovar, recusar com motivo) | Tela aberta no emulador com o hospital pendente; regra por teste de integração | ✅ |
| Cadeia de confiança das doações | Tela de Status mostra "Íntegra: 15 doações conferidas"; testes de adulteração | ✅ |
| Coleta de notícias por RSS | Depende da internet; não repetida nesta rodada | ❌ |

## 3. Requisitos não funcionais

| Requisito | Evidência | |
|---|---|:-:|
| Dados continuam depois de fechar o app | Teste no emulador: sessão e conta criada voltam numa "loja reaberta" | ✅ |
| Senha não fica legível | Teste no emulador lê o arquivo do banco: nenhuma senha em texto | ✅ |
| Preferências (tema) continuam | Tema claro escolhido, app fechado e reaberto: continua claro | ✅ |
| Banco incompleto não derruba o app | Teste no emulador (apaga uma tabela e carrega) | ✅ |
| `http://` só na versão de teste | Manifesto da versão debug; a de lançamento exige HTTPS | ✅ |
| Funciona em outras versões do Android | Só o Android 17 foi testado (o app suporta desde o 8) | ❌ |
| Desempenho (tempo de abertura, memória) | Não medido | ❌ |
| Acessibilidade (leitor de tela, contraste, tamanho do texto) | Tema verificado; alto contraste e tamanho do texto não reverificados; leitor de tela não testado | ⚠️ |

## 4. Defeitos encontrados nesta homologação

| # | Defeito | Gravidade | Situação |
|---|---|:-:|---|
| D1 | **Excluir a conta mudava o id nas doações e quebrava a cadeia de confiança** (o site mantém o id). Achado por um teste de integração. | Alta | ✅ Corrigido e coberto por teste |
| D2 | `ReinicioReceiver` usava `.use` em `SQLiteOpenHelper`, que só é fechável a partir do Android 13 (o app roda desde o 8). | Média | ✅ Corrigido (`try/finally`); **não executado** em Android 8–12 |
| D3 | Tema, contraste, Libras e tamanho do texto voltavam ao padrão a cada abertura. | Média | ✅ Corrigido e verificado |
| D4 | Telas abertas por cima de outras não tinham botão Voltar. | Média | ✅ Corrigido e verificado |
| D5 | Abas internas ficavam cortadas na borda, sem indicar que rolam. | Baixa | ✅ Corrigido (degradê) e verificado |
| D6 | O Status do administrador mostrava o resumo do site (datado de 30/10) como se fosse do app. | Baixa | ✅ Corrigido |
| D7 | Banco vazio ao abrir, até a primeira ação; e app fechando com banco incompleto. | Média | ✅ Corrigidos nas rodadas anteriores |
| D8 | Botão do teclado não avançava de campo; no login a senha não enviava. | Baixa | ✅ Corrigido e verificado |
| D9 | Botão "Salvar alterações" do perfil do doador fica no fim de um formulário longo. | Baixa | ❌ Aberto |
| D10 | Avisos do compilador de código obsoleto (4). | Baixa | ✅ Corrigidos |

## 5. Pendências para uma homologação completa

1. Repetir, no emulador, as telas marcadas ⚠️ (perfil, carteirinha, ficha PcD, rede de sangue raro, agenda e campanhas do hospital).
2. Rodar em Android 8, 12 e 13 (ou 14), por causa do D2 e das permissões de notificação.
3. Testar com o **backend real** do site (hoje só com o servidor de demonstração).
4. Aplicar o roteiro de `docs/TESTES_COM_USUARIOS.md` com pessoas de verdade.
5. Esperar o horário de um alarme chegar e ver a notificação disparar sozinha; reiniciar o emulador e ver os alarmes voltarem.

## 6. Parecer

**Aprovado para demonstração e piloto fechado, com ressalvas.**

- Aprovado porque as regras de negócio, o banco, a sessão, a privacidade e a cadeia de confiança foram verificadas por testes e no emulador, e os defeitos achados foram corrigidos.
- Ressalvas: não validado com o backend real; contas e doações existem só no aparelho; faltam testes com usuários e em outras versões do Android.
- **Não aprovado para publicação** enquanto os itens 3 e 4 da seção 5 não forem feitos.

| Responsável pela homologação | Data | Assinatura |
|---|---|---|
| (preencher) | | |
