# Testes com Usuários — Hemare Mobile

> **Situação: roteiro pronto, NÃO aplicado.** Nenhuma pessoa testou o app ainda, então este documento não traz resultados. Ele existe para que você aplique o teste e registre o que acontecer.

## 1. Objetivo

Descobrir onde pessoas reais travam, erram ou se perdem ao usar o app, e se conseguem fazer as tarefas principais sem ajuda. O foco é a **usabilidade**, e não as regras de negócio (essas já têm testes automatizados).

## 2. Quem convidar (4 a 6 pessoas bastam)

| Perfil | Quantas | Por quê |
|---|:-:|---|
| Pessoa que nunca doou sangue | 2 | Representa quem chega sem conhecer o processo |
| Doador regular ou ex-doador | 1 | Conhece o processo e compara com a experiência real |
| Pessoa da área da saúde (ou do hemocentro) | 1 | Avalia o painel do hospital e a linguagem |
| Pessoa com pouca familiaridade com celular | 1 | Mostra problemas de clareza e de tamanho de texto |

Evite colegas do projeto: eles já sabem como o app funciona.

## 3. Como aplicar (cerca de 20 minutos por pessoa)

**Preparação**
- Use o emulador (ou um celular com o app instalado) com os dados de demonstração. Para começar do zero: Configurações do aparelho → Apps → Hemare Mobile → Armazenamento → Limpar dados.
- Contas de teste: `doador@hemare.com` / `doador123` · `hospital@hemare.com` / `hospital123` · `admin@hemare.com` / `admin1234`.
- **Dados pessoais:** peça à pessoa para **não usar dados reais** (CPF, telefone, nome verdadeiro) e explique que nada é enviado a ninguém. Se for gravar a tela ou o áudio, peça autorização antes.

**Fala inicial (leia igual para todos)**
> "Estamos testando o aplicativo, e não você. Se algo for difícil, o problema é do aplicativo. Vou pedir algumas tarefas e quero que você fale em voz alta o que está pensando. Não vou ajudar durante as tarefas, mas pode me perguntar qualquer coisa no final. Pode parar a qualquer momento."

**Durante**
- Entregue uma tarefa por vez, **sem dizer onde clicar**.
- Observe, não ajude. Anote o que a pessoa disse, onde hesitou e onde errou.
- Se ficar travada por mais de 2 minutos, anote "não concluiu", dê uma dica pequena e siga em frente.

## 4. Tarefas

### Para qualquer pessoa
| # | Tarefa (leia assim) | Sucesso quando | Tempo-alvo |
|---|---|---|---|
| T1 | "Descubra se você poderia doar sangue hoje." | Faz a triagem "Posso doar?" e entende o resultado | 3 min |
| T2 | "Encontre um lugar para doar perto de você." | Abre "Onde doar" e acha um local | 1 min |
| T3 | "Marque uma doação para a próxima semana, de manhã." | Confirma um agendamento (precisa entrar com a conta de doador) | 3 min |
| T4 | "Você mudou de ideia: cancele esse horário." | Cancela o agendamento | 1 min |
| T5 | "O texto está pequeno. Deixe maior." | Usa A+ em Configuração | 1 min |
| T6 | "Quero receber aviso quando eu puder doar de novo." | Liga as notificações e o lembrete de retorno | 2 min |

### Para quem tem perfil de hospital
| # | Tarefa | Sucesso quando | Tempo-alvo |
|---|---|---|---|
| T7 | "O estoque de O- está em emergência. Informe isso." | Define o nível do tipo O- como Emergência | 2 min |
| T8 | "Veja quem pode ajudar com um pedido de sangue O+." | Publica a necessidade e abre os doadores compatíveis | 3 min |

### Para quem tem perfil de administrador
| # | Tarefa | Sucesso quando | Tempo-alvo |
|---|---|---|---|
| T9 | "Aprove o hospital que está aguardando." | Aprova `pendente@hemare.com` | 2 min |

## 5. O que registrar (uma linha por tarefa e por pessoa)

| Pessoa | Tarefa | Concluiu? (sim / com ajuda / não) | Tempo | Erros e hesitações | Frase marcante |
|---|---|---|---|---|---|
| P1 | T1 | | | | |
| P1 | T2 | | | | |
| P2 | … | | | | |

**Indicadores**
- **Taxa de sucesso** de cada tarefa = pessoas que concluíram sem ajuda ÷ total. Meta: pelo menos 80% nas tarefas T1 a T4.
- **Tempo** comparado ao tempo-alvo.
- **Erros**: toques no lugar errado, telas abertas sem querer, voltas atrás.

## 6. Questionário final (SUS, Escala de Usabilidade do Sistema)

Peça uma nota de 1 (discordo totalmente) a 5 (concordo totalmente) para cada frase:

1. Acho que gostaria de usar este app com frequência.
2. Achei o app mais complicado do que precisava.
3. Achei o app fácil de usar.
4. Acho que precisaria de ajuda de alguém para conseguir usar o app.
5. Achei que as funções do app estão bem integradas.
6. Achei que o app tem muita inconsistência.
7. Imagino que a maioria das pessoas aprenderia a usar este app rapidamente.
8. Achei o app muito pesado de usar.
9. Senti-me muito confiante usando o app.
10. Precisei aprender muitas coisas antes de conseguir usar o app.

**Como calcular a nota (0 a 100):** nas frases **ímpares**, subtraia 1 da nota; nas **pares**, subtraia a nota de 5. Some tudo e multiplique por 2,5. Tire a média entre as pessoas. Referência usual: acima de 68 é considerado bom.

**Perguntas abertas**
- O que foi mais difícil?
- O que você mais gostou?
- Teve alguma palavra ou botão que você não entendeu?
- Você confiaria neste app para marcar uma doação de verdade? Por quê?

## 7. Depois dos testes

1. Junte todos os registros numa tabela.
2. Dê uma **gravidade** a cada problema: 1 (cosmético) · 2 (atrapalha, mas resolve) · 3 (trava a tarefa) · 4 (impede de usar).
3. Priorize os de gravidade 3 e 4 e os que **mais de uma pessoa** teve.
4. Registre o resultado em `docs/STATUS.md` (seção 4) e no parecer de `docs/HOMOLOGACAO.md`.

## 8. Ideias de melhoria que o teste pode confirmar ou descartar

São hipóteses nossas, **não resultados**:
- A triagem "Posso doar?" é muito longa para quem nunca doou?
- As pessoas percebem que as abas internas rolam para o lado (agora há um degradê)?
- O botão "Salvar alterações" do perfil, no fim de um formulário longo, passa despercebido?
- O texto das notificações é claro?
