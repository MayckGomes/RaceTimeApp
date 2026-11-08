# RaceTime 2.0 — Fase 3: Campeonato

> Registro da Fase 3. A revisão posterior e o estado atual da entrega estão em [FASE4_CALENDARIO_GP.md](FASE4_CALENDARIO_GP.md).

## Entrega no código

A seção Campeonato agora usa uma tela dedicada com abas Pilotos e Construtores e cards de classificação. As tabelas antigas não são mais usadas nesse destino; seus componentes continuam no projeto onde ainda são necessários para favoritos e telas legadas.

Cada item apresenta posição oficial, nome completo, equipe(s) quando informadas, número quando disponível, pontos e vitórias. Os três primeiros recebem destaque discreto. Favoritos já cadastrados têm uma indicação de estrela. Tocar no item abre o detalhe existente do piloto ou construtor pelo ID estável da API.

As listas têm rolagem independente por aba e preservam o estado ao alternar abas e destinos. Os cards não impõem altura ou corte por uma única linha ao nome. Ações de atualizar e pull-to-refresh usam o repositório compartilhado, sem HTTP nos composables.

A tela distingue classificação vazia, carregamento, falha e dados salvos. Pilotos e construtores possuem metadados próprios de rodada, falha, cache e última atualização, para que uma resposta atrasada de um endpoint não seja atribuída à outra aba.

## Variação de posição

A variação é calculada por ID estável: posição anterior menos posição atual.

- Positivo: subiu.
- Negativo: caiu.
- Zero: manteve.
- Ausência de informação comparável: indicador omitido, sem substituir por uma falsa posição mantida.

As consultas de histórico são ativadas quando a seção Campeonato é aberta. A Home não precisa consultar rodadas anteriores antes disso. Uma vez solicitado, o histórico é reutilizado nas atualizações do estado compartilhado, com cache e deduplicação existentes.

Para habilitar a comparação de uma aba, o repositório exige:

1. Metadados consistentes de temporada e rodada da classificação atual.
2. Rodada atual maior que 1.
3. Resultado de Race publicado para a rodada atual.
4. Resultado de Race publicado para a rodada imediatamente anterior.
5. Classificação anterior não vazia, da mesma temporada e da rodada solicitada.
6. Posições numéricas positivas do participante nas duas classificações e correspondência inequívoca por ID.

Uma classificação atualizada com pontos da Sprint não é suficiente para afirmar que a rodada terminou. Sem resultado de Race, não há seta. Se a rodada anterior não tiver resultados disponíveis, o aplicativo também omite a comparação, sem inferir cancelamento ou buscar muitas rodadas em sequência.

Pilotos e construtores podem estar publicados em rodadas diferentes. Cada aba valida e compara suas próprias rodadas. O cabeçalho informa as duas rodadas usadas na comparação.

Um snapshot completo salvo pode sustentar a comparação offline, desde que seus metadados sejam válidos e existam resultados confirmados das duas corridas. Nesse caso, o texto informa que a variação usa dados salvos. Resultado vazio ou cache sem correspondência não gera movimentação.

A ordenação usa as posições retornadas pela API, sem inventar desempate por pontos, vitórias ou nome. Registros sem ID são descartados; duplicatas idênticas são deduplicadas; cópias conflitantes do mesmo ID são omitidas. Posições não numéricas como D/E permanecem depois das posições numéricas e não recebem variação.

## Arquivos principais

| Arquivo | Alteração |
| --- | --- |
| `domain/weekend/ChampionshipRules.kt` | Ordenação e validação das condições de movimentação |
| `domain/weekend/WeekendModels.kt` | Metadados independentes das duas classificações; correspondência única no cálculo de posição |
| `data/repository/RacingRepository.kt` | Atualização compartilhada serializada, classificações por aba, histórico e provas de publicação de resultados |
| `presentation/championship/ChampionshipViewModel.kt` | Estado reativo e atualização da seção |
| `presentation/championship/ChampionshipScreen.kt` | Abas, lista responsiva, refresh e estados de rede/cache |
| `components/championship/StandingItem.kt` | Card reutilizável, favoritos e indicação acessível de posição |
| `presentation/main/mainScreen.kt` | Integração da nova tela, preservando os quatro destinos e os acessos vindos do Home |
| `components/ads/bannerAds.kt` | Destruição do AdView ao sair da composição |
| `res/values*/championship_strings.xml` | Novos textos nos cinco idiomas |

## Rede, favoritos e anúncios

As classificações continuam no cache de 10 minutos; snapshots anteriores usam cache de 24 horas. Atualização manual pode revalidar, respeitando o intervalo mínimo, orçamento de chamadas e tratamento de HTTP 429 do gateway. Requisições simultâneas da Home e do Campeonato compartilham esse cache e seus locks.

O conteúdo atual é publicado antes de terminar as consultas de histórico, permitindo leitura enquanto a comparação é verificada. Não existe polling contínuo nem consulta disparada pelo countdown.

Os arquivos e métodos originais de favoritos continuam byte a byte iguais ao repositório de origem. Esta fase apenas lê sua indicação na lista; adicionar/remover continua disponível no detalhe existente.

A integração AdMob foi preservada com um único banner de teste após a lista. Não há banner intercalado entre posições, aumento da quantidade de anúncios nem IDs de produção. O AdView é destruído ao deixar a composição para evitar manter recursos da aba fechada.

## Testes e validação

Adicionados nesta fase:

- 8 testes JVM das regras de ordenação, identificação e movimentação.
- 8 testes JVM do repositório com MockEngine: rodadas confirmadas, primeira rodada, Sprint sem Race, rodada anterior indisponível, rodadas diferentes por endpoint, metadados incompatíveis, ativação do histórico sob demanda e fallback de snapshot salvo.
- 2 testes instrumentados: nome longo com fonte ampliada/abertura do detalhe e descrição acessível da variação.

A suíte total contém 63 testes JVM e 6 instrumentados, incluindo os exemplos originais.

Executados com sucesso:

- `python3 scripts/verify_phase3.py`, incluindo as verificações estáticas das fases anteriores.
- Parser sintático de Kotlin em 90 arquivos, sem erros detectados.
- Parsing de XMLs, recursos e argumentos de formatação, incluindo os 10 recursos novos nos cinco idiomas.
- Conferência da integração da tela, preservação de estados de lista, navegação e descarte do banner.
- Comparação dos arquivos de favoritos e dos campos de identidade, versão e assinatura com o Git de origem.
- `git diff --check` e integridade do ZIP.

**JUnit, testes instrumentados e build não foram executados com sucesso.** A tentativa de `:app:testDebugUnitTest :app:assembleDebug` parou antes da compilação ao baixar o Gradle, com `java.net.SocketException: Network is unreachable`. Não há APK nem revisão visual em aparelho/emulador. O parser sintático não substitui compilação ou verificação de tipos.

No Android Studio/Windows:

```text
gradlew.bat :app:testDebugUnitTest :app:assembleDebug --console=plain
gradlew.bat :app:connectedDebugAndroidTest
```

O segundo comando exige aparelho/emulador conectado. Para a revisão manual, conferir temas claro/escuro, fonte ampliada, nomes longos, mudança de abas, volta dos detalhes, cache offline e ausência de variação na primeira rodada ou antes da publicação dos resultados.

## Estado do plano

Fases 1–3 possuem código integrado, com validação de execução ainda pendente pelo impedimento descrito. A Fase 3 não está aprovada para produção antes de compilar e executar os testes.

Fases 4–7 permanecem pendentes: expansão do calendário/GP, modernização completa de favoritos, ampliação de notificações/configurações e estatísticas.
