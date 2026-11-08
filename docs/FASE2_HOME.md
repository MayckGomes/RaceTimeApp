# RaceTime 2.0 — Fase 2: Home contextual e navegação

> Registro da Fase 2. A modernização posterior do Campeonato e o estado atual estão em [FASE3_CAMPEONATO.md](FASE3_CAMPEONATO.md).

## Código implementado nesta revisão

- Header compacto com identidade RaceTime, temporada e acesso a Configurações. O header é escuro em ambos os temas e respeita a status bar.
- Quatro destinos principais: Home, Calendário, Campeonato e Favoritos. Configurações não ocupa uma posição na barra inferior e não existe Telemetria.
- Campeonato com abas de acesso a Pilotos e Construtores, reutilizando as telas existentes nesta etapa. A modernização completa das classificações e os indicadores de movimento continuam na Fase 3.
- Home contextual com Weekend Hero: GP, etapa, temporada, circuito/local, data da corrida, janela temporal prudente, próxima sessão conhecida, horário local e countdown local.
- Próxima corrida exibida separadamente somente quando for um GP diferente daquele do hero.
- Último resultado disponível com seleção entre Qualifying, Sprint e Race, conforme resultados efetivamente publicados e carregados.
- Top 3 com nomes, equipes, posições e pontos, quando disponíveis; Q1/Q2/Q3 ou tempo/status são exibidos conforme o tipo e os campos retornados.
- Resumos das classificações de pilotos e construtores, com ações direcionadas à aba correspondente do Campeonato.
- Resumo compacto dos favoritos existentes, por IDs estáveis, com posição/pontos e último resultado carregado quando houver. Ações abrem os detalhes já existentes.
- Pull-to-refresh, carregamento por bloco, vazio, falha de atualização, cache, indicador de dados salvos e última atualização quando há conteúdo desatualizado.
- Calendário integrado à barra inferior, com acesso a cada GP e indicadores que distinguem resultado confirmado de mera passagem da data programada.
- Página inicial do GP com cronograma, seletor de resultados publicados, resultado completo e metadados de circuito. Esta página é necessária para as ações do Home; a expansão de calendário/GP da Fase 4 continua pendente.
- Botão voltar de pilotos/equipes retorna à tela anterior, inclusive quando acessados pelo resultado do GP.
- Configurações acessíveis pelo header, preservando os controles de notificações anteriores e adicionando Sobre, atribuição à Jolpica e acesso aos termos.

## Organização

| Arquivo | Responsabilidade |
| --- | --- |
| `components/home/DashboardCards.kt` | Header, Weekend Hero, countdown, resultado resumido, linha de resultado e avisos de dados salvos |
| `components/home/HomeCards.kt` | Primitivas reaproveitadas: cards, marca vetorial, rótulos, próxima corrida e resumo de standings |
| `presentation/home/HomeOverview.kt` | Composição do dashboard e ações, consumindo estado reativo |
| `presentation/home/HomeViewModel.kt` | Estado compartilhado, favoritos, Clock e atualização; antigos adaptadores de quatro blocos removidos após migração da UI |
| `domain/weekend/HomePresentation.kt` | Deduplicação e ordenação de resultados, datas de sessões, countdown e supressão de GP duplicado |
| `data/local/FavoritesStore.kt` | Observação dos mesmos SharedPreferences/IDs das versões anteriores, sem migração ou alteração de registros |
| `presentation/main/mainScreen.kt` | Quatro destinos, abas do campeonato e preservação de estado com SaveableStateHolder |
| `presentation/circuits/CalendarOverview.kt` | Calendário ligado ao repositório e ao contexto temporal compartilhado |
| `presentation/gp/GrandPrixViewModel.kt` | Dados do GP/resultado, reutilização do cache e fallback offline |
| `presentation/gp/GrandPrixScreen.kt` | Entrada funcional para cronograma, resultados completos e informações |
| `presentation/settings/SettingsDestination.kt` | Rota de configurações com retorno e sem título duplicado |
| `navgation/navgation.kt` | Novas rotas tipadas para configurações e GP; rotas de piloto/equipe preservadas |

## Correção e integridade dos dados

A API pode devolver o mesmo resultado por `last` e por número de rodada. O Home deduplica por temporada/rodada/tipo e prefere a cópia revalidada mais recentemente. A ordenação usa temporada, rodada e horário da sessão retornado pelo calendário, quando conhecido. Sem horário, não é fabricado um timestamp de sessão; um resultado confirmado de corrida é o desempate conservador no mesmo GP.

A data de qualificação/Sprint é obtida do cronograma. Quando ele não está disponível, a data do resultado é identificada explicitamente como data da corrida, evitando apresentá-la como data da sessão.

O detalhe do GP conserva os resultados já carregados pela Home mesmo se não conseguir obter o calendário offline ou se a consulta pela rodada numérica falhar e o cache estiver sob o alias `last`.

O resumo de favoritos representa apenas dados efetivamente carregados no repositório. Não afirma que um resultado não consultado é o resultado mais recente de toda a carreira do piloto. Favoritos antigos ou fora da classificação atual continuam presentes pelo ID e pelo nome salvo, com dados adicionais apenas quando disponíveis.

As sessões de treino e Sprint Qualifying aparecem apenas no cronograma, com aviso de que esta API não fornece seus resultados completos. Não há indicador de live timing, duração oficial inventada, foto, traçado ou logotipo esportivo adicionado.

## Temas, idiomas e acessibilidade

- Tema claro preservado e superfícies escuras aproximadas à paleta do prompt.
- Vermelho de destaque mais claro no tema escuro para melhorar contraste de textos/ícones sobre superfícies escuras; sem gradientes ou imagens externas.
- Cards sem alturas fixas; nomes e detalhes podem ocupar mais linhas.
- Rankings lado a lado apenas a partir de 600 dp úteis; em celulares ficam empilhados para preservar leitura e fontes ampliadas.
- Estados de listas/tabs preservados ao alternar destinos; voltar dos destinos secundários leva à Home.
- Novos textos do dashboard e os recursos de Home/notificações usados nesta entrega possuem traduções em PT-BR, EN, ES, FR e IT. Foram verificados 97 recursos e seus argumentos de formatação nos cinco idiomas.
- O tema segue o sistema e o idioma segue a configuração do aparelho. Seletores próprios de aparência/idioma ainda não foram adicionados.

## Preservações

Sem alteração de package, applicationId, versionCode, versionName, assinatura ou configuração de distribuição. Os repositórios originais de favoritos continuam byte a byte iguais ao Git de origem. Não houve migração destrutiva nem renomeação de suas chaves/arquivos.

A infraestrutura AdMob e seus IDs de teste permanecem no projeto. O Home não recebe banners adicionais; as telas antigas que os utilizam continuam disponíveis. Nenhum anúncio de produção ou autorização comercial foi habilitado.

## Validação executada e impedimentos

Executados com sucesso:

- `python3 scripts/verify_phase2.py`: parser sintático de Kotlin em 83 arquivos; parsing dos XMLs; referências de strings; ausência de recursos duplicados; traduções/argumentos; quatro destinos; acesso a configurações; preservação de estado e retorno de detalhes.
- Comparação dos arquivos de favoritos e dos campos de identidade/assinatura/versão com o repositório original.
- `git diff --check`.
- Verificação de integridade do ZIP.

Adicionados 7 testes JVM para apresentação do Home: ordenação por sessão, alias duplicado, resultado vazio, mudança de GP, countdown, supressão de próxima corrida duplicada e data correta da qualificação. A suíte JVM totaliza 47 testes contando o exemplo original.

Adicionados 3 testes instrumentados dos componentes: cautela sobre janela programada, ação de tentar novamente e abertura do resultado completo. A suíte instrumentada totaliza 4 testes contando o exemplo original.

**Nenhum teste JUnit/instrumentado foi executado neste ambiente.** A tentativa de `:app:testDebugUnitTest :app:assembleDebug` parou antes da compilação ao baixar o Gradle, com `java.net.SocketException: Network is unreachable`. Não existe APK gerado nem validação visual executada em emulador/aparelho. O parser sintático não verifica tipos, dependências ou compilação Compose.

Para validar no Windows:

```text
gradlew.bat :app:testDebugUnitTest :app:assembleDebug --console=plain
gradlew.bat :app:connectedDebugAndroidTest
```

O segundo comando exige aparelho/emulador conectado. No Linux/macOS, substituir `gradlew.bat` por `./gradlew`.

## Pendências reais

Fase 2 tem código integrado, mas requer build, execução dos testes e revisão visual em telas pequenas/fontes ampliadas. Não deve ser considerada aprovada para produção antes disso.

Fases 3–7 continuam pendentes: modernização completa do Campeonato, expansão de calendário/GP, tela completa de favoritos, ampliação das preferências/agendamento e estatísticas. O cronograma e a página de resultados iniciais desta revisão não substituem todo o escopo da Fase 4. Categorias individuais de notificações, opção no horário, abertura do GP pela notificação, seletores de aparência/idioma e cálculos de estatísticas ainda não foram implementados nesta fase.
