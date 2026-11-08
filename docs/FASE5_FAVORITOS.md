# RaceTime 2.0 — Fase 5: Favoritos

## Entrega integrada

A tela Favoritos agora possui abas de pilotos e construtores, cards com nome, número quando disponível, equipe, posição, pontos e vitórias do campeonato. O visual reutiliza RacingCard, DataNotice e o tema existente. Cada aba preserva sua posição de rolagem; atualização manual usa pull-to-refresh.

Todos os IDs cadastrados permanecem visíveis, inclusive pilotos aposentados e equipes ausentes da classificação atual. Uma ausência na classificação não apaga o favorito nem fabrica uma posição. Nomes vêm primeiro da classificação, depois do perfil da API, depois do rótulo salvo e, por último, do ID original. Associação ocorre por ID, nunca apenas pelo nome.

Os cards permitem abrir os detalhes existentes, remover o favorito e desfazer a remoção. A ação de escolher favoritos abre a aba correspondente do Campeonato. Os estados vazios explicam como cadastrar um favorito pela estrela. O rodapé mantém um único banner com ID de teste, sem aumentar a publicidade.

## Dados reais e custo de rede

O novo FavoritesViewModel recebe o RacingRepository singleton e o FavoritesStore via Koin. Não cria outros ViewModels nem executa HTTP em componentes visuais.

Somente cards compostos pela lista, incluindo seu pequeno pré-carregamento, solicitam enriquecimento. Não há polling de favoritos nem consultas de carreira em background. A remoção cancela trabalhos daquele ID, e as mudanças realizadas pelas telas antigas continuam observadas pelo mesmo StateFlow.

Rotas consultadas na documentação oficial da Jolpica, usando prefixo `/ergast/f1/` e sufixo `.json`:

- `drivers/{id}` / `constructors/{id}`: cadastro, com cache de sete dias.
- `drivers/{id}/seasons` / `constructors/{id}/seasons`: temporadas retornadas pela API, com cache de 24 horas.
- `{season}/drivers/{id}/results` / `{season}/constructors/{id}/results`: resultados de corrida filtrados pelo ID, com cache de dez minutos na temporada ativa e sete dias no histórico.

O favorito presente no campeonato tenta primeiro a temporada ativa. Se necessário, consulta no máximo duas das temporadas mais recentes retornadas pela API. O limite evita uma varredura de toda a carreira quando faltam resultados. Por isso a UI utiliza **Última corrida encontrada**, e a mensagem de ausência refere-se às temporadas consultadas. Não promete que uma consulta incompleta esgotou todo o histórico.

Paginação, deduplicação por rota, orçamento de chamadas, HTTP 429, backoff e cache offline reutilizam o gateway existente. Atualizações forçadas são limitadas a uma por ID em cada gesto de atualização; rolar novamente após atualizar não força a mesma consulta continuamente. Perfis só são consultados quando necessários para completar o cadastro. A validade do resultado usa seu próprio timestamp, sem herdar a data mais recente do índice de temporadas.

Resultados parciais filtrados por piloto/equipe **não entram no mapa global de resultados de um GP**, evitando publicar um pódio incompleto no Home. O card pode abrir a página correta do GP, onde o resultado completo usa a rota comum da Fase 4.

O resumo de corrida do piloto mostra posição, equipe, pontos, tempo e status quando presentes; abandono não é tratado como chegada normal. Para a equipe, mostra a melhor posição retornada entre seus participantes, sem somar pontos ou inventar um resultado de construtores. Qualifying/Sprint continuam disponíveis na página completa do GP; nesta fase o resumo dos favoritos é especificamente de corrida.

Falhas mantêm dados salvos com aviso de cache/erro e ação de tentar novamente. Dados ausentes permanecem ausentes; não foram adicionadas fotos, logos ou informações fictícias.

## Compatibilidade da persistência

Os arquivos SharedPreferences `driversFavorites` e `teamsFavorites` e suas chaves/valores são os mesmos. Os repositórios legados `driversRepository.kt` e `teamsRepository.kt` permanecem byte a byte iguais aos originais do Git; não existe migração destrutiva ou limpeza automática.

O FavoritesStore acrescenta/remova apenas a chave solicitada, confirma a escrita e informa falha para a UI. Desfazer grava novamente o mesmo ID e rótulo anterior. Preferências de outros favoritos não são modificadas.

Os detalhes antigos continuam acessíveis. Um construtor fora do campeonato passa a usar seu cadastro histórico real em vez de depender apenas da classificação atual. As telas de detalhes ganharam rolagem, retorno com área de toque adequada, descrição acessível da estrela, coroutine vinculada à composição e tratamento de erro que encerra o loading e permite tentar novamente. Package, assinatura, versão e configuração de distribuição foram preservados.

## Arquivos principais

| Arquivo | Responsabilidade |
| --- | --- |
| `domain/weekend/FavoriteModels.kt` | Resumos por ID, nomes de fallback e seleção do participante correto |
| `data/local/FavoritesStore.kt` | Leitura reativa e escrita compatível de favoritos |
| `data/repository/RacingRepository.kt` | Perfil e histórico filtrado, paginação e metadados de cache |
| `presentation/favorites/FavoritesViewModel.kt` | Estado, enriquecimento sob demanda, cancelamento e refresh |
| `presentation/favorites/favoritsScreen.kt` | Abas, estados vazios, remoção/desfazer e navegação |
| `components/favorites/FavoriteCard.kt` | Card reutilizável com classificação e corrida encontrada |
| `presentation/main/mainScreen.kt` / `di/AppModule.kt` | Integração com Campeonato e injeção existente |
| `data/api/Api.kt` | Fallback de cadastro de construtores fora do campeonato |
| `presentation/driver/driverScreen.kt` e ViewModel | Detalhe preservado com rolagem e recuperação de erro |
| `presentation/team/teamScreen.kt` e ViewModel | Detalhe preservado, inclusive equipe histórica |
| `res/values*/favorites_strings.xml` | Vinte recursos em português brasileiro, inglês, espanhol, francês e italiano |

O ViewModel antigo de favoritos, que instanciava outros ViewModels e descartava favoritos ausentes do campeonato, foi substituído depois da integração da nova tela.

## Verificação e limitações reais

Adicionados: 16 testes JVM e três instrumentados. O projeto contém 98 testes JVM e 11 instrumentados, incluindo exemplos originais. São fontes de testes preparadas para execução; não são resultados de testes aprovados.

Cobertura adicionada:

- Preservação de favoritos fora do campeonato e identificação por ID.
- Prioridade de nomes, colisão de IDs entre categorias, duplicação de classificação e remoção sem reaparecimento pelo cache.
- Melhor posição apenas da equipe correta e status de abandono preservado.
- Perfil com campos opcionais e rejeição de cadastro com outro ID.
- Temporada ativa, histórico limitado às temporadas da API, paginação de construtores, timestamp próprio do resultado e cache em erro.
- Não contaminação dos resultados globais por consultas parciais de favoritos.
- Cards com fonte ampliada/nome longo, ações de detalhes/remover e resultado para temporada/rodada corretas.
- Persistência após recriar o store, remoção e desfazer, mantendo outros registros. O teste usa arquivos isolados e não limpa favoritos reais.

Executados com sucesso: `python3 scripts/verify_phase5.py`, incluindo as verificações estáticas das fases anteriores; parsing sintático de 101 arquivos Kotlin; parsing de XML e referências de strings; equivalência dos argumentos das traduções; comparação dos repositórios legados com o Git; e `git diff --check`.

**Build e testes não executaram com sucesso.** A tentativa de `bash gradlew :app:testDebugUnitTest :app:assembleDebug --console=plain` parou ao baixar `gradle-9.6.0-bin.zip`, com `java.net.SocketException: Network is unreachable`, antes de compilar. Não houve verificação de tipos, execução JUnit/instrumentada, inspeção visual no aparelho ou geração de APK. O parser sintático não substitui essas validações, e a compatibilidade/disponibilidade do toolchain original continua pendente.

Em ambiente Android com acesso às dependências:

```text
gradlew.bat :app:testDebugUnitTest :app:assembleDebug --console=plain
gradlew.bat :app:connectedDebugAndroidTest
```

Conferir manualmente atualização de versão com favoritos antigos, cadastro/remover/desfazer, reinício do app, modo offline, equipe histórica, mudança de aba e retorno dos detalhes, aparelho pequeno e fontes ampliadas.

## Próximas fases

Fases 1–5 têm código integrado; aprovação funcional/produção depende do build e dos testes pendentes. Fase 6: ampliar configurações e notificações individuais, incluindo no horário, permissões e reagendamento. Fase 7: estatísticas do GP, pit stops/voltas e destaques suportados pela API. Não foram feitas alterações de monetização nem presumida autorização comercial dos dados.

Atualização posterior: a Fase 6 foi integrada; ver [FASE6_NOTIFICACOES.md](FASE6_NOTIFICACOES.md). Os totais e o estado acima registram a entrega da Fase 5.
