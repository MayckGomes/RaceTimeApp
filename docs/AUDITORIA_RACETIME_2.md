# RaceTime 2.0 — auditoria e Fase 1

> Registro da Fase 1. As revisões posteriores estão em [FASE2_HOME.md](FASE2_HOME.md) e [FASE3_CAMPEONATO.md](FASE3_CAMPEONATO.md), e [FASE4_CALENDARIO_GP.md](FASE4_CALENDARIO_GP.md), que registra o estado atual.

## Escopo desta entrega

A solicitação completa substitui o desenho anterior de uma Home com apenas quatro cards. Esta entrega implementa a base da Fase 1 e preserva a interface da versão anterior enquanto ela é validada. As fases 2–7 permanecem como plano obrigatório. O aplicativo completo 2.0 ainda não está concluído.

## Auditoria do projeto encontrado

| Área | Estado encontrado | Decisão |
| --- | --- | --- |
| Identidade | `mayckgomes.com.racetimeapp`, minSdk 26, target/compileSdk 35, versionCode 4 e versionName 1.0.3 | Preservados, inclusive assinatura/configuração de release existente. A alteração de versão de distribuição fica para um release validado. |
| UI | Jetpack Compose, Material 3, tipografia Barlow Condensed/Inter, componentes de cards/tabelas | Reutilizar os componentes adequados. Redesenho completo somente na Fase 2. |
| Linguagem/build | Kotlin 2.2.10; plugin serialization 2.1.21; AGP 9.4.0; wrapper Gradle 9.6.0; arquivo de daemon solicitando JDK 21 | Serialization alinhado ao Kotlin 2.2.10. Mantidos AGP, wrapper, toolchain e distribuição; compatibilidade desse conjunto ainda não foi validada pelo build. Há também uma declaração KSP não utilizada no build raiz, a revisar quando houver acesso ao resolvedor de dependências. |
| Dependências | Ktor 3.1.3, Koin 4.1.0, Navigation 2.8.4, Lifecycle 2.8.7, Compose BOM 2024.09.00, AdMob 24.4.0; WorkManager 2.10.1 na revisão anterior | Mantidas. Adicionados apenas Ktor MockEngine e coroutines-test para testes da lógica crítica. |
| HTTP | `Api` criava clientes próprios; Home possuía outro serviço; rotas e políticas duplicadas | Um HttpClient do Koin e JolpicaGateway compartilhado. Api é uma fachada de compatibilidade para telas legadas. |
| Estado | ViewModels independentes e favoritos criando outros ViewModels | Repositório central para calendário, classificações e disponibilidade de resultados. A remoção total dos ViewModels antigos depende das telas substitutas nas fases seguintes. |
| Tempo | Comparações locais com SimpleDateFormat, primeiro GP da lista e contagem sem horário nas versões antigas | Resolver com Clock injetável, datas UTC e cronograma da API. |
| Persistência | SharedPreferences `racetime_app_cache`, `driversFavorites` e `teamsFavorites` | Mesmos arquivos e IDs; favoritos preservados byte a byte. Cache novo tem namespace próprio. |
| Navegação | Destinos antigos com configurações adicionadas pela revisão anterior | Mantida nesta Fase 1. Na Fase 2 será substituída por Home/Calendário/Campeonato/Favoritos; Configurações sairá da barra inferior. |
| Idiomas | Português brasileiro, inglês, espanhol, francês e italiano | Recursos existentes preservados. Traduções da nova apresentação serão concluídas junto das fases visuais. |
| AdMob | ID de aplicativo e unidades de teste | Preservados; inicialização retirada de recomposições. Nenhuma unidade de produção habilitada. |
| Offline | A navegação inicial bloqueava o app numa tela de falta de conexão | Home abre mesmo offline para mostrar cache, estados vazios ou falhas por seção. |

### Problemas corrigidos na base

- Consultas duplicadas e clientes HTTP independentes.
- Rota legada `next/results` usada como tentativa genérica de buscar resultados recentes.
- Ausência de paginação no calendário e nas telas legadas.
- Campos opcionais obrigatórios nos modelos, capazes de derrubar o parsing de toda a resposta.
- Conversão de posição por `toInt()` em favoritos, sem tolerar posições ausentes ou não numéricas.
- Calendário com seleção contextual dispersa por ViewModel.
- Fixture de piloto sem uso no código de produção removida. Os dados de teste ficam nos testes/previews.
- Placeholders sem uso (`SeasonSummary`, caso de uso com rodada 1 fixa e TestViewModel de versão), removidos após verificar suas referências.

## Fase 1 — implementação

### Fluxo de dados

`HttpClient → JolpicaGateway → ResponseCache/SharedPreferences → RacingRepository → WeekendResolver → HomeViewModel/calendário/notificações`

O cache é acessado pelo gateway antes da rede. O repositório expõe o estado tipado; o resolver consome esse estado e calcula o contexto temporal. Koin gerencia as instâncias compartilhadas.

### Arquivos principais

- `domain/weekend/WeekendModels.kt`: temporada, fim de semana (reutiliza RacesCalendar), sessões, tipos, estados temporais, contexto, resultados publicados e estado do campeonato.
- `domain/weekend/WeekendResolver.kt`: seleção do evento relevante, sessão atual programada, próxima sessão, sessão anterior, corrida anterior confirmada e próxima corrida.
- `data/remote/JolpicaGateway.kt`: paginação, cache, deduplicação por rota, espaçamento entre requisições, backoff e HTTP 429.
- `data/local/ResponseCache.kt`: armazenamento persistente com timestamps e leitura conservadora do calendário legado.
- `data/repository/RacingRepository.kt`: calendário, classificações, resultados efetivamente publicados e contexto reativo compartilhado.
- `presentation/home/HomeViewModel.kt`: adaptador para os cards existentes, sem duplicar as regras do calendário.
- `presentation/circuits/circuitsViewModel.kt`: calendário e evento relevante provenientes da mesma fonte.
- `domain/model/RaceSchedule.kt`: fachada compatível com testes e notificações anteriores, delegando ao motor de sessões.
- `notifications/ReminderSyncWorker.kt`: reutiliza o repositório em vez de outro serviço HTTP.
- `di/AppModule.kt`: HttpClient, cache, Clock e repositório únicos.
- `data/api/Api.kt`: mantém as assinaturas antigas para preservar as telas de detalhes, favoritos e classificações.

### Regras temporais

- Nenhum nome de GP, dia da semana ou duração oficial é definido manualmente.
- Sessões FP1/FP2/FP3/Sprint Qualifying/Sprint/Qualifying dependem da presença do campo correspondente da API; a corrida utiliza date/time do GP.
- `SprintQualifying` e `SprintShootout` são compatíveis e não geram sessões duplicadas.
- Sessões sem horário permanecem no cronograma, mas não recebem countdown nem alarme baseado num horário inventado.
- UTC é a referência de seleção; o fuso do dispositivo só deve formatar a apresentação.
- Antes do início, a próxima sessão é aquela com o menor Instant futuro conhecido.
- O estado SCHEDULED_IN_PROGRESS representa apenas uma janela temporal prudente no dia UTC agendado. Não prova atividade ao vivo.
- Sem um término real fornecido pela fonte, o motor usa a próxima sessão conhecida ou a passagem da data UTC para diferenciar sessões temporalmente passadas. Isso é uma política de apresentação, não um horário oficial de encerramento.
- UPCOMING_WEEKEND usa um limiar de apresentação de 48 horas até uma sessão conhecida. Não determina em que dia o GP começa nem a duração de sessões.
- Resultado publicado é uma condição independente da passagem do tempo.
- Resultado ausente não implica GP cancelado. Como a API compatível não garante um indicador explícito de cancelamento, o app mantém a informação como ausente/aguardando, sem inventar a causa.
- O último resultado de corrida pode pertencer ao ano anterior, mesmo se a qualificação do primeiro GP do novo ano já tiver resultado.
- A mudança de ano usa o mesmo Clock UTC, inclusive na tarefa de atualização dos lembretes.
- Não há atualização HTTP a cada segundo: apenas o contexto temporal tem ticker local, com WhileSubscribed para cessar quando não houver consumidores.

### Rede e cache

| Conteúdo | Política inicial |
| --- | --- |
| Calendário | 6 horas; atualização forçada disponível |
| Standings | 10 minutos |
| Resultado recente publicado | 5 minutos |
| Consulta de resultado ainda vazio | 90 segundos, sem polling automático |
| Resultado histórico publicado | 7 dias |
| Resultado histórico ainda indisponível | 6 horas |
| Cadastro de piloto | 7 dias |

Todas as rotas usam `.json`, `limit=100` e `offset`. A paginação segue `MRData.limit` e `MRData.total`; páginas de resultados pertencentes ao mesmo GP são agrupadas por temporada/rodada, preservando todos os participantes.

Consultas simultâneas da mesma rota passam por um Mutex e reutilizam cache. Atualizações forçadas têm intervalo mínimo de 20 segundos por rota. A rede é serializada com intervalo mínimo de 350 ms; tentativas de erro de servidor/rede têm backoff de 1 e 2 segundos. Erros HTTP de cliente não são repetidos indiscriminadamente.

Ao receber 429, aplica-se Retry-After numérico ou espera conservadora; o cooldown e o orçamento local de até 450 chamadas por hora são persistidos para sobreviver a reinícios do processo. Esse orçamento é do aplicativo, não de outros clientes compartilhando o IP; o servidor continua sendo a autoridade sobre limites.

Falha de rede mantém cache com `stale/failed`. Resposta vazia bem-sucedida permanece vazia e não é transformada em falha. Uma paginação interrompida não substitui um cache completo por uma lista parcial.

O User-Agent usa `RaceTimeApp/<versionName instalado>`. Não há polling para simular live timing. O cache não altera os arquivos de favoritos nem apaga seus registros.

### Limitações conhecidas da Fase 1

- Não há confirmação de live timing ou de fim real de sessão.
- A consulta é da temporada ativa UTC e usa resultados anteriores como fallback. Um seletor de temporadas e a exibição de calendário já publicado do próximo ano pertencem à fase de calendário; não é inventada uma próxima corrida quando a temporada carregada acabou.
- Um evento sem resultados não pode ser declarado cancelado sem evidência da fonte.
- As telas antigas continuam usando a fachada de compatibilidade; a modernização visual e a simplificação desses ViewModels ainda não foram realizadas.
- As preferências e o agendamento da revisão anterior foram preservados, mas categorias individuais, opção no horário e abertura direta do GP serão ampliados na Fase 6.

## Testes e execução

A suíte local contém 40 testes JUnit contando o teste de exemplo original: resolver (incluindo fronteira de ano, Sprint e fuso), paginação, vazio, rede/cache, 429, deduplicação, parsing opcional, helpers de calendário e regras de lembretes.

**JUnit não executado neste ambiente.** O comando abaixo foi tentado e parou antes da compilação, ao baixar `gradle-9.6.0-bin.zip`, com `java.net.SocketException: Network is unreachable`.

```text
gradlew.bat :app:testDebugUnitTest :app:assembleDebug --console=plain
```

No Linux/macOS: `./gradlew :app:testDebugUnitTest :app:assembleDebug --console=plain`.

Verificações executadas com sucesso:

- `git diff --check`.
- Parser sintático de Kotlin em 75 arquivos, sem erros de sintaxe detectados; isso não verifica tipos, resolução de dependências ou compilação Compose.
- Parsing dos XMLs e resolução dos recursos de strings.
- Presença dos cinco conjuntos de idioma existentes.
- Comparação byte a byte dos repositórios de favoritos originais.
- Comparação de namespace, applicationId, versionCode, versionName e signingConfig com o repositório original.
- Integridade do arquivo ZIP.

O script `scripts/verify_phase1.py` reproduz as verificações estáticas; o parser Kotlin é opcional (`tree-sitter` e `tree-sitter-kotlin`). Nenhuma ferramenta de parser faz parte do aplicativo Android.

Não foi gerado APK. Não foi validado no emulador/aparelho. As fases seguintes não devem ser consideradas aprovadas antes de executar a suíte e a compilação.

## Plano obrigatório das fases seguintes

| Fase | Entrega planejada | Estado |
| --- | --- | --- |
| 1 | Motor, parsing, estado central, cache e testes | Código integrado; validação de execução bloqueada |
| 2 | Header com configurações, Weekend Hero, resultados selecionáveis, resumos e favoritos; quatro destinos | Implementada em revisão posterior; ver FASE2_HOME.md |
| 3 | Campeonato com abas, linhas responsivas e movimentação comprovável | Implementada em revisão posterior; ver FASE3_CAMPEONATO.md |
| 4 | Calendário e página do GP com cronograma, Qualifying/Sprint/Race e informações confiáveis | Implementada em revisão posterior; ver FASE4_CALENDARIO_GP.md |
| 5 | Favoritos modernizados, compatibilidade dos IDs e resumos reais | Implementada em revisão posterior; ver FASE5_FAVORITOS.md |
| 6 | Preferências individuais, no horário/antecedência, permissões e navegação direta ao GP | Implementada em revisão posterior; ver FASE6_NOTIFICACOES.md |
| 7 | Estatísticas verificáveis, pit stops e voltas sob demanda | Implementada em revisão posterior; ver FASE7_ESTATISTICAS.md |

## Referências e uso comercial

Foram consultados os documentos e serializers oficiais do repositório Jolpica para calendário, resultados, standings, Sprint, voltas e pit stops. Os quatro tipos de resultados não suportados (FP1/FP2/FP3/Sprint Qualifying) não são apresentados como endpoints completos disponíveis.

- https://github.com/jolpica/jolpica-f1/tree/main/docs
- https://github.com/jolpica/jolpica-f1/blob/main/docs/rate_limits.md
- https://github.com/jolpica/jolpica-f1/blob/main/docs/ergast_differences.md
- https://github.com/jolpica/jolpica-f1/blob/main/TERMS.md

Na documentação consultada, os dados são oferecidos para uso não comercial sob CC BY-NC-SA 4.0; uso comercial exige contato com a Jolpica. Manter IDs de teste do AdMob não resolve a autorização de monetização. Esta entrega preserva a infraestrutura de anúncios e não presume aprovação comercial.
