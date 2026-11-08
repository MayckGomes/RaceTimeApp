# Home — skeleton, ações e cache

## Carregamento

A Home usa placeholders de estrutura para o card do fim de semana, o último resultado e cada resumo de classificação. São skeletons estáticos, sem animação contínua ou dados fictícios. A descrição de carregamento é acessível e traduzida nos cinco idiomas.

O skeleton aparece quando a seção ainda não possui conteúdo e está carregando. Um ranking já disponível não é escondido porque o outro ainda está em consulta. Durante atualização, os dados existentes permanecem visíveis; a tela não é substituída inteira por placeholders. O indicador de pull-to-refresh continua disponível.

## Ações

RaceActionButton centraliza formato, espaçamento, área mínima de 48 dp e cores dos botões. As ações de detalhe do GP, resultado completo, classificação, calendário, escolha de favoritos e tentar novamente foram padronizadas. DataNotice reutiliza o mesmo componente nas telas que já consumiam esse aviso.

O seletor de resultados agora diferencia claramente seleção e estado normal, com cores e bordas compatíveis com os temas. Favoritos na Home são cards clicáveis com nome em cor principal de texto e detalhes secundários, evitando que toda a linha herde a cor de link de um TextButton. As cores dos botões usam pares de superfície/conteúdo do tema, incluindo primaryContainer/onPrimaryContainer na ação principal.

## O cache já existia?

Sim, o projeto já tinha um adaptador persistente PreferenceResponseCache sobre SharedPreferences, dentro de racetime_app_cache, com chaves independentes jolpica:{rota} e timestamp de gravação. O gateway já reutilizava respostas válidas, fazia deduplicação/limitação de rede e retornava dados salvos como desatualizados se a tentativa de rede falhasse. As gravações usam SharedPreferences.apply, com atualização imediata em memória e persistência assíncrona do Android.

A limitação encontrada estava na apresentação/entrada da Home: o ViewModel forçava revalidação em toda abertura e os dados salvos só eram mapeados depois de aguardar a consulta e seus retries. Com isso, o usuário podia ver loading mesmo já tendo dados persistidos, especialmente offline.

## Ajustes feitos

1. restoreHomeCache lê somente dados locais, sem HTTP, e os publica no estado compartilhado antes da revalidação.
2. Restaura calendário, classificações e resultados salvos do calendário atual. Também procura resultados por rodada e last no calendário anterior, mantendo a última corrida acessível entre temporadas quando esse conteúdo já estiver salvo.
3. A abertura/retorno à Home usa force=false, respeitando as validades. Atualização manual, pull-to-refresh e ações de tentar novamente continuam usando force=true, sujeito ao intervalo mínimo de deduplicação do gateway.
4. Um snapshot salvo mais antigo não sobrescreve um estado em memória mais novo. A expiração reavalia o aviso de desatualização sem apagar conteúdo ou uma falha já conhecida da mesma versão.
5. Caches de outra temporada não são atribuídos ao campeonato ativo. Dados históricos continuam identificados por sua temporada e rodada reais.
6. O mesmo mapeamento de resultados é reutilizado nas consultas e na restauração. Dados parciais de favoritos não são lidos como resultado completo do GP.
7. DataNotice distingue cache desatualizado de falha: uma cópia expirada, antes de falhar a atualização, não é descrita como erro de rede.

Os prazos existentes continuam em vigor: calendário em geral seis horas; classificações dez minutos; resultados recentes publicados cinco minutos, ainda vazios com revalidação mais breve; histórico confirmado e cadastros com cache prolongado conforme o repositório. Nenhum polling contínuo foi introduzido.

Cache não cria dados offline que nunca foram consultados. A primeira carga bem-sucedida exige rede, e alterações publicadas pela API depois da última consulta só chegam com atualização. Uma cópia válida pode ser apresentada sem HTTP; uma rota ainda desconhecida ou expirada pode exigir consulta. Não foi prometido cache completo de todos os anos, pilotos ou sessões.

## Validação e estado real

Adicionados cinco testes JVM de restauração (sem HTTP, dados expirados/timestamps, snapshot antigo, temporada incompatível e transição entre temporadas), dois instrumentados de UI (skeleton e callbacks) e dois do adaptador persistente (recriação do leitor e conteúdo corrompido isolado). Totais: 164 testes JVM e 31 instrumentados preparados.

Executados com sucesso: python3 scripts/verify_home_loading.py, incluindo os verificadores anteriores; parsing sintático dos 121 arquivos Kotlin; XMLs/traduções; git diff --check; e integridade do ZIP. Os repositórios legados de favoritos e os identificadores de distribuição foram preservados.

A tentativa de :app:testDebugUnitTest :app:assembleDebug parou antes da compilação no download do Gradle, com java.net.SocketException: Network is unreachable. Não foram executados JUnit/testes instrumentados, nem confirmados layout e persistência após reinício real do processo em aparelho. O parser não substitui compilação/verificação de tipos. Não há APK validado.

Validação manual recomendada: abrir online e carregar uma vez; fechar/reabrir em modo avião; conferir que os dados previamente salvos aparecem antes do fim das tentativas de rede; atualizar manualmente; voltar à conexão; conferir seções sem dados, primeiros acessos, fonte ampliada, temas e navegação dos botões. As mensagens de cache/erro dependem da validade e da falha de consulta de cada fonte, não apenas de o modo avião estar ativado.
