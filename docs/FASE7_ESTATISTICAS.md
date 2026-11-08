# RaceTime 2.0 — Fase 7: Estatísticas e consolidação

## Entrega integrada

A página do GP ganhou a aba Estatísticas, preservando Cronograma, Resultados e Informações. A barra de abas permite rolagem horizontal para evitar comprimir quatro títulos em aparelhos pequenos. Nos resultados publicados de Corrida/Sprint, uma ação Estatísticas também abre o resumo diretamente.

As estatísticas possuem três seleções internas: Resumo, Pit stops e Voltas. Temporada, rodada, tipo de resultado, seção e piloto mantêm identidades distintas; a rolagem é preservada por seleção. O Home continua compacto, sem incorporar essas listas detalhadas.

Não foram acrescentadas bibliotecas, permissões, imagens ou fontes de dados. A implementação usa os componentes e a arquitetura existentes: Compose/Material 3, ViewModel/StateFlow, RacingRepository e JolpicaGateway.

## Resumo do GP

O resumo aceita resultados efetivamente publicados de Race e Sprint. Qualifying permanece na apresentação de Q1/Q2/Q3; não é usado como grid substituto para cálculos de corrida. Treinos e Sprint Qualifying não recebem resultados fictícios.

Destaques:

- Vencedor e pódio por posições oficiais, sem depender da ordem da resposta.
- Volta mais rápida quando há exatamente um registro válido com FastestLap.rank igual a 1 e tempo interpretável. Não se escolhe um vencedor comparando uma lista incompleta de tempos sem rank.
- Número da volta mais rápida quando informado.
- Maior ganho entre as chegadas válidas retornadas. Empates são preservados; não se escolhe arbitrariamente apenas um piloto.
- Contagem por categoria de status e linhas detalhadas dos participantes, com acesso aos detalhes existentes do piloto.

Por piloto, são apresentados grid, posição no resultado, pontos, voltas completadas, melhor volta informada e status original, apenas quando disponíveis. A posição numérica no resultado não é descrita automaticamente como uma chegada normal.

## Regras de cálculo

A variação é grid menos posição final. Exige:

- Status de chegada Finished ou indicação positiva de volta(s) de atraso.
- Grid e posição final numéricos e positivos.
- positionText ausente ou compatível com a posição numérica.
- Ausência de ambiguidade nas posições de grid/final utilizadas.
- Identificador estável de piloto não vazio nem duplicado no conjunto usado nos cálculos.

Grid zero, posições ausentes/não numéricas, abandonos, desclassificações, não largadas e inconsistências não produzem ganho/perda. Valor positivo indica ganho; negativo, perda; zero, posição mantida. O destaque de maior ganho só existe quando há variação positiva válida.

Os grupos distinguem chegada, abandono reconhecido, desclassificação/exclusão, ausência de largada/qualificação e outros status. O reconhecimento de abandono usa um conjunto explícito de status. Um status desconhecido ou ambíguo, como Not classified, permanece em Outros e é exibido literalmente. Não foi criada uma narrativa de acontecimentos, notícia ou análise de estratégia.

Campos novos FastestLap.lap e laps foram acrescentados com defaults ao ResultEntry, mantendo os resultados já serializados legíveis. O modelo genérico RaceExtra, sem consumidores, foi substituído por PitStop e LapTiming com responsabilidades claras.

## Pit stops

Consulta documentada: `/{season}/{round}/pitstops.json`, sob o prefixo `https://api.jolpi.ca/ergast/f1/`.

O usuário escolhe Pit stops e pressiona Carregar. Abrir o GP ou entrar em Estatísticas não dispara essa consulta automaticamente. São mostrados piloto/ID, número da parada, volta e duração conforme a API. Não são inventados nomes de pilotos ausentes do resultado: o ID verdadeiro é mantido como fallback.

A duração inclui o trânsito de entrada/saída dos boxes e pode incluir tempo de bandeira vermelha. A UI explica que isso não representa o tempo de troca de pneus. Não há ranking de equipe de box nem soma automática dessas durações como perda competitiva.

A cobertura documentada começa em 2011. Para anos anteriores, a UI mostra a limitação e o repositório não consulta o endpoint. Uma resposta vazia em anos suportados é apresentada como dados não disponíveis, sem afirmar que houve zero paradas.

## Voltas por piloto

Consulta documentada: `/{season}/{round}/drivers/{driverId}/laps.json`.

O seletor contém os participantes do resultado publicado da corrida. Selecionar um nome não busca dados por si só: o botão Carregar voltas faz a consulta daquele piloto. Isso evita baixar centenas ou milhares de timings de todo o pelotão ao abrir a tela.

Cada item mostra número da volta, tempo e posição registrada quando retornados. Os registros são históricos; não são live timing ou tracking dos carros. A cobertura documentada começa em 1996, e consultas anteriores são evitadas.

Trocar a consulta para outro piloto cancela o trabalho anterior ainda em andamento. Os dados já carregados permanecem separados por ID; uma resposta antiga não substitui a seleção nova. Trocar de GP cancela os trabalhos de extras e limpa seu estado contextual.

Pit stops e voltas são consultados somente quando existe resultado publicado da corrida principal com a mesma temporada e rodada. Não são reutilizados como dados da Sprint nem consultados como se estivessem disponíveis ao vivo durante a sessão.

## Parsing, cache e rede

Os dois endpoints usam a paginação comum por limit/offset/total. O parsing de Laps/Timings e PitStops reúne páginas da mesma identidade, ordena os registros e remove duplicações idênticas. Metadados de outro GP ou timings de outro piloto sinalizam falha e não são associados à seleção solicitada. Registros conflitantes para a mesma identidade são omitidos, com indicação de falha, em vez de escolher arbitrariamente um valor.

A política é de cinco minutos para dados recentes ou respostas ainda vazias e sete dias para histórico já preenchido. Existe atualização explícita, sujeita ao intervalo mínimo de deduplicação do gateway. Os critérios comuns de User-Agent, backoff, HTTP 429, orçamento local e serialização das chamadas permanecem em vigor.

Falhas de rede preservam dados salvos com timestamp e aviso de cache/erro. A UI distingue consulta não solicitada, loading, erro, resposta vazia bem-sucedida e cobertura histórica não oferecida. Atualizar o GP revalida os extras que já foram solicitados: pit stops e o último piloto consultado, sem iniciar uma varredura de todos os pilotos.

Não existe polling de estatísticas ou trabalho em segundo plano para essa fase. O worker da Fase 6 continua restrito ao calendário.

## Arquivos principais

| Arquivo | Responsabilidade |
| --- | --- |
| domain/weekend/RaceStatistics.kt | Status, posições válidas, parsing de duração, destaques, empates e cobertura |
| domain/weekend/WeekendModels.kt | Campos opcionais da volta mais rápida e voltas completadas; ganho conservador |
| data/repository/RacingRepository.kt | Mapeamento dos resultados e consultas paginadas de pit stops/voltas com cache |
| presentation/gp/GrandPrixViewModel.kt | Estado de extras, pré-condições, cancelamento e refresh contextual |
| presentation/gp/GrandPrixStatistics.kt | Resumo, pit stops, seleção de piloto e carregamento explícito de voltas |
| presentation/gp/GrandPrixScreen.kt | Aba Estatísticas, acesso pelos resultados e preservação de seleção/rolagem |
| components/statistics/RaceStatisticsCards.kt | Destaques e linha de estatística reutilizáveis |
| res/values*/statistics_strings.xml | 38 recursos nos cinco idiomas |
| scripts/verify_phase7.py | Verificação estática consolidada das sete fases |
| README.md / ALTERACOES_HOME.md | Estado real da entrega, documentação e comandos de validação |

## Testes preparados e verificações executadas

Adicionados nesta fase: **33 testes JVM e cinco instrumentados**. Totais do projeto: **154 JVM e 24 instrumentados**, incluindo os exemplos originais. Esses números representam fontes de testes; não são uma declaração de execução aprovada.

As regras cobrem pódio, ordenação, posições duplicadas, grid zero/ausente, divergência de positionText, chegadas válidas, abandonos, desconhecidos, não largada e desclassificação, ganho/perda/manutenção, empate no maior ganho, fastest rank válido/ausente/ambíguo, tempos inválidos/overflow, cobertura, identidade, Sprint e defaults de serialização.

Os testes de repositório cobrem paginação dos dois endpoints, filtro por piloto, cobertura sem HTTP, GP/piloto incompatíveis, vazio versus falha, cache offline, campos opcionais, duplicação/conflito, mapeamento de FastestLap/laps, cache histórico prolongado e revalidação mais breve de respostas vazias.

Os testes instrumentados preparados verificam fonte ampliada/nome longo, callbacks de piloto, ausência de ganho para abandono, pit stops somente por ação explícita, seleção sem consulta automática de voltas e mensagens distintas de vazio/erro.

Executados com sucesso neste ambiente:

- `python3 scripts/verify_phase7.py`, incluindo todas as verificações estáticas anteriores.
- Parsing sintático de 113 arquivos Kotlin; parsing de XML e resolução de strings.
- Consistência dos 38 recursos/argumentos novos nos cinco idiomas.
- Conferência estática de integração, pré-condições, rotas, cancelamento e carregamento sob demanda.
- Comparação dos repositórios de favoritos originais, identidade e configuração de distribuição com o Git.
- `git diff --check` e integridade do ZIP.

**Build e testes ainda não executaram com sucesso.** A tentativa de `bash gradlew :app:testDebugUnitTest :app:assembleDebug --console=plain` falhou antes da compilação ao baixar `gradle-9.6.0-bin.zip`, com `java.net.SocketException: Network is unreachable`. Não houve verificação de tipos, execução JUnit/instrumentada, teste de rede real no app ou revisão visual no aparelho. Não há APK gerado. O parser sintático não substitui a compilação Kotlin/Compose; a resolução/compatibilidade do toolchain original continua pendente.

## Validação final necessária

Em ambiente com Android SDK e acesso às dependências:

```text
gradlew.bat :app:testDebugUnitTest :app:assembleDebug --console=plain
gradlew.bat :app:connectedDebugAndroidTest
```

O segundo comando exige aparelho/emulador. Além da suíte, conferir:

- GP com e sem Sprint, resposta ainda vazia e temporada histórica fora da cobertura de extras.
- Piloto com abandono, desclassificação, grid zero e status desconhecido; campos omitidos pela API.
- Race/Sprint sem misturar grid, pit stops ou voltas entre os tipos.
- Troca rápida de piloto/GP, retorno de detalhe, rolagem por seleção e atualização manual.
- Cache offline, reconexão, 429 e integridade dos favoritos ao atualizar uma instalação existente.
- Fonte ampliada, telefone pequeno, modos claro/escuro, cinco idiomas e system bars.
- Entrega real dos lembretes, permissões, fuso, reboot e abertura do GP com app aberto/fechado, conforme a matriz da Fase 6.

A configuração de release original com signingConfigs.debug foi preservada por instrução de continuidade. Antes de distribuir uma atualização de produção, é necessário revisar a assinatura com a chave esperada pelo aplicativo já publicado. Não houve publicação, push para GitHub ou alteração de identificadores nesta entrega.

## Estado dos critérios de aceitação

| Critério | Implementação no código | Validação real |
| --- | --- | --- |
| Contexto, próxima sessão e último resultado | Resolver, calendário e resultados compartilhados | Pendente de execução |
| Quatro destinos e Campeonato unificado | Home, Calendário, Campeonato, Favoritos | Pendente de execução |
| Calendário e página de cada GP | Temporadas, cronograma, resultados e informações | Pendente de execução |
| Favoritos persistidos | Arquivos/IDs originais, leitura reativa, remoção/desfazer | Comparação estática aprovada; teste de atualização pendente |
| Notificações em Configurações | Preferências individuais e reconciliação local | Reboot/permissões/entrega pendentes |
| Estatísticas reais | Destaques e extras sob demanda da mesma API | Pendente de execução |
| Conectividade intermitente | Cache, timestamps, falhas e respostas vazias distintas | Pendente de execução |
| Responsividade e acessibilidade | Layouts adaptáveis e componentes acessíveis | Revisão no aparelho pendente |
| Cinco idiomas | Recursos mantidos e traduções novas consistentes | Revisão visual pendente |
| AdMob | Infraestrutura preservada, IDs de teste | Integração real pendente |
| Arquitetura | Componentes existentes, separação e repositório compartilhado | Compilação e testes pendentes |
| Build/testes | Suíte e comandos preparados | Bloqueados pelo download do Gradle, antes de compilar |

As sete fases possuem código integrado. Isso não equivale a uma versão aprovada para produção: os impedimentos e as validações restantes estão registrados acima. Permanecem deliberadamente fora do produto live timing, telemetria, rádio, clima/notícias em tempo real e alertas imediatos de resultados de favoritos, por falta de fonte/estratégia confiável.

## Referências oficiais consultadas

- https://github.com/jolpica/jolpica-f1/blob/main/docs/endpoints/results.md
- https://github.com/jolpica/jolpica-f1/blob/main/docs/endpoints/laps.md
- https://github.com/jolpica/jolpica-f1/blob/main/docs/endpoints/pitstops.md
- https://github.com/jolpica/jolpica-f1/tree/main/docs
