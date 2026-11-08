# RaceTime 2.0 — Fase 4: Calendário e detalhes do GP

## Calendário

O destino Calendário usa um ViewModel próprio para consultar temporadas e seus GPs, reutilizando o gateway, o cache e o WeekendResolver da aplicação.

A lista de anos vem de `seasons.json`, com paginação por `MRData.limit/total`. O ano atual também pode ser consultado explicitamente, mesmo quando ainda não aparece na lista da API; nesse caso, uma resposta vazia é apresentada como ausência de calendário, sem criar GPs fictícios.

Selecionar uma temporada não altera a temporada do Home, do Campeonato ou do agendador de notificações. Cada temporada preserva sua posição de rolagem. Uma troca rápida de ano cancela a consulta anterior e impede que sua resposta substitua a seleção nova.

Cada GP exibe etapa, nome, circuito/local quando informados, data/hora local quando conhecida e um indicador contextual:

- Resultado da corrida efetivamente confirmado, quando já foi carregado.
- GP contextual em uma janela temporal ativa.
- Próxima corrida do calendário selecionado.
- Data programada passada.
- Evento futuro ou horário desconhecido.

A simples passagem de uma data não é convertida em confirmação de corrida concluída, cancelamento ou resultado publicado. O calendário não faz uma consulta de resultados para cada GP apenas para decorar a lista; eventos ainda não consultados podem permanecer com indicação temporal.

A atualização manual e o pull-to-refresh revalidam os dados, preservando os estados de erro/cache/ausência de calendário. A paginação é compartilhada com a infraestrutura já testada nas fases anteriores.

## Página do GP

As abas são Cronograma, Resultados e Informações. Elas preservam suas posições de rolagem; a aba Resultados também preserva a posição por tipo selecionado.

### Cronograma

As sessões são montadas e ordenadas exclusivamente pelos dados da API. São respeitadas as diferenças entre SprintShootout e SprintQualifying, sem duplicar sessões.

O estado temporal é calculado com Clock injetável e atualizado por ticker local, sem polling HTTP. O aplicativo distingue horário futuro, horário desconhecido, janela programada em andamento, horário programado já ultrapassado e resultado publicado. Não usa uma duração oficial fixa nem apresenta o estado temporal como live timing confirmado.

Quando a API informa UTC date/time, a UI converte para o fuso do aparelho, inclusive quando muda o dia local. Se o horário não é conhecido, a data é identificada como data do calendário em UTC; nenhum horário é inventado. O intervalo de datas na aba Informações também é identificado explicitamente como UTC.

Apenas uma sessão com resultado publicado possui a ação de abrir esse resultado. FP1/FP2/FP3 e Sprint Qualifying continuam sendo cronograma, com aviso de ausência de resultados completos nessa API.

### Resultados

O seletor contém Qualifying, Sprint e Race quando presentes no cronograma ou comprovados por um resultado já carregado. Não são consultados resultados de sessões futuras com horário/data conhecidos, nem endpoints inexistentes para treinos.

A tela diferencia:

- Resultado disponível, inclusive com cache durante uma falha de atualização.
- Consulta em andamento.
- Falha de consulta.
- Sessão não listada no cronograma.
- Sessão programada para o futuro.
- Resposta vazia bem-sucedida, sem resultado publicado disponível.
- Horário desconhecido.
- Sessão cujo horário passou, mas cujos resultados ainda não foram consultados: a UI pede atualização em vez de afirmar que não foram publicados.
- Tipo de sessão sem endpoint completo suportado.

Qualifying mostra posição, piloto, equipe e Q1/Q2/Q3 quando fornecidos. Sprint/Race mostram posição, piloto, equipe, pontos, tempo/diferença e status conforme a resposta. Tocar no piloto preserva a navegação para o detalhe existente e o retorno ao GP.

Os participantes de uma resposta paginada continuam agrupados por temporada/rodada/tipo; respostas com metadados de outro GP são rejeitadas. Resultados já carregados continuam acessíveis se a atualização do cronograma ou do resultado falhar offline.

### Informações

São mostrados apenas metadados disponíveis: circuito, local/país, etapa, temporada, data da corrida, intervalo de datas do cronograma e coordenadas válidas quando retornadas. Links de Wikipedia da corrida/circuito aparecem somente quando fornecidos e validados como URLs HTTP(S) do domínio Wikipedia, sem credenciais.

Não foram criados comprimento de pista, recordes, número de voltas, mapas, fotos, logos ou notícias. Se o cronograma estiver ausente e o cabeçalho vier de um resultado confirmado, a tela informa a ausência do cronograma e não fabrica sessões a partir desse fallback.

## Cache e consistência

A página do GP reutiliza o calendário completo salvo quando ele contém metadados válidos e atuais do evento. Caso necessário, consulta o endpoint documentado `/{season}/{round}/races.json`.

Uma atualização confirmada de um único GP pode atualizar/adicionar/remover seu registro do calendário já salvo, sem renovar o timestamp de validade da temporada inteira. Uma resposta vazia bem-sucedida é diferente de erro; uma falha mantém metadados salvos como desatualizados. Um snapshot completo obtido mais recentemente não é sobrescrito por uma consulta individual anterior.

Quando a atualização afeta a temporada ativa e já existe uma base de calendário compartilhada, o agendador reutiliza o calendário atualizado para cancelar/reagendar conforme as preferências existentes. Consultar temporadas históricas não troca a temporada dos lembretes.

O total de etapas no card separado de próxima corrida usa a maior rodada conhecida, evitando exibir um ordinal maior que um total calculado apenas pela quantidade de registros quando existem lacunas no calendário.

Os critérios de rede das fases anteriores continuam em vigor: cache, deduplicação, espaçamento de chamadas, limite local, HTTP 429 e backoff. Não foram adicionadas dependências.

## Organização dos arquivos

| Arquivo | Responsabilidade |
| --- | --- |
| `domain/weekend/CalendarRules.kt` | Indicadores do calendário, tipos suportados, elegibilidade de consultas, estados de resultados e validação de links |
| `presentation/circuits/CalendarViewModel.kt` | Temporadas, seleção independente, cancelamento e contexto reativo |
| `presentation/circuits/CalendarOverview.kt` | Seletor de ano, lista cronológica, cache, refresh e acesso ao GP |
| `presentation/circuits/CalendarDestination.kt` | Mantém a rota antiga do calendário apontando para a apresentação nova, com retorno correto |
| `presentation/gp/GrandPrixViewModel.kt` | Metadados do GP, consultas elegíveis, resultados salvos e estados independentes |
| `presentation/gp/GrandPrixScreen.kt` | Cronograma, resultados e informações, com estados de rolagem preservados |
| `components/calendar/SessionScheduleItem.kt` | Item reutilizável do cronograma |
| `data/repository/RacingRepository.kt` | Temporadas, metadados individuais, validação da identidade do GP e atualização pontual do calendário |
| `data/remote/JolpicaGateway.kt` | Leitura e atualização pontual do cache sem alterar sua validade global |
| `domain/models/seasonCalendar.kt` / `lastResultsModel.kt` | Campos opcionais de URL e coordenadas da API |
| `res/values*/gp_strings.xml` | Textos novos nos cinco idiomas |

As duas telas/VM antigos de calendário foram retirados depois de redirecionar sua rota existente para a tela nova. As rotas tipadas do aplicativo e os detalhes de pilotos/equipes continuam preservados.

## Testes e verificação

Adicionados nesta fase:

- 10 testes JVM de regras do calendário, sessões suportadas, estados de disponibilidade, consultas futuras, identidade por temporada e validação de links.
- 9 testes JVM do repositório: paginação de temporadas/calendário, reutilização do cache, fallback em erro, evento removido, metadados incompatíveis, seleção histórica independente, alteração pontual de horário e inclusão de GP confirmado sem inventar outras rodadas.
- 2 testes instrumentados do cronograma: ausência de horário/resultados de treino e ação de resultado publicado.

Totais do projeto: 82 testes JVM e 8 instrumentados, incluindo exemplos originais.

Executados com sucesso:

- `python3 scripts/verify_phase4.py`, incluindo as verificações estáticas anteriores.
- Parser sintático de Kotlin em 95 arquivos, sem erros detectados.
- Parsing dos XMLs, resolução de strings e conferência de 15 novos recursos/argumentos nos cinco idiomas.
- Conferência da seleção independente de ano, cancelamento de consulta antiga, preservação de rolagem e cache do GP.
- Comparação de favoritos e de namespace, applicationId, versão e assinatura com o Git original.
- `git diff --check` e integridade do ZIP.

**Build, testes JVM, testes instrumentados e revisão visual ainda não foram executados com sucesso neste ambiente.** A tentativa de `:app:testDebugUnitTest :app:assembleDebug` parou antes da compilação ao baixar a distribuição Gradle, com `java.net.SocketException: Network is unreachable`. Também não foi possível conferir a compatibilidade/disponibilidade do toolchain original pelo resolvedor de dependências. Não há APK gerado. O parser sintático não substitui verificação de tipos nem compilação Compose.

No Windows:

```text
gradlew.bat :app:testDebugUnitTest :app:assembleDebug --console=plain
gradlew.bat :app:connectedDebugAndroidTest
```

O segundo comando exige aparelho/emulador conectado. Validar manualmente: troca rápida de anos, calendário vazio, fonte ampliada, rolagem ao voltar do GP, mudança de fuso/dia local, sessão futura, horário ausente, resultado vazio/falha, modo offline e atualização de horários.

## Estado do plano

Fases 1–4 têm código integrado, com validação de execução pendente pelo impedimento descrito. Não estão aprovadas para produção antes de compilar e executar os testes.

Fases 5–7 continuam pendentes: modernização completa dos favoritos, ampliação de notificações/configurações e estatísticas. Pit stops/voltas/ganhos de posição e destaques do GP permanecem na fase de estatísticas; nenhum dado foi simulado para preenchê-los nesta entrega.

Atualização posterior: a Fase 5 foi integrada; ver [FASE5_FAVORITOS.md](FASE5_FAVORITOS.md). Os números e o estado acima registram a entrega da Fase 4.
