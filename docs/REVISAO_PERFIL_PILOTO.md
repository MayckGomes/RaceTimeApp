# Revisão do nome no Home e do perfil do piloto

O cabeçalho principal deixou de usar o texto fixo RACE TIME e agora utiliza app_name, a mesma identidade definida pelo aplicativo: RaceTime App. A fonte condensada, o espaçamento reduzido e a altura flexível mantêm o nome legível, inclusive quando precisa quebrar linha com fontes ampliadas. Não foram alterados o nome do package, os identificadores, versão ou assinatura.

A tela do piloto foi reformulada a partir da referência visual fornecida, com elementos gráficos próprios: cabeçalho com nome completo, número em destaque, diagonais e cor da equipe do campeonato atual. O perfil é organizado em Visão geral, Última corrida e Sobre. Os cards mostram posição, pontos e vitórias da temporada atual, equipe e acesso ao seu detalhe. A última corrida encontrada mostra temporada, GP, posição no resultado, equipe, pontos, tempo/status e abre o resultado completo correto. Abandono não é apresentado como chegada normal.

Dados pessoais incluem nacionalidade, número, código e nascimento/idade quando efetivamente disponíveis. Datas ausentes, inválidas ou futuras não produzem uma idade fictícia. Nomes são compostos pelos campos reais givenName/familyName, com remoção dos espaços externos. Em ausência do cadastro, a tela usa os dados disponíveis de classificação/favoritos, sem apagar o piloto nem fabricar posição no campeonato. O gráfico, os valores exemplificativos e as fotos da referência não foram inseridos como dados reais do piloto.

O novo DriverDetailsViewModel utiliza Koin, o mesmo RacingRepository, o mesmo cache e o mesmo FavoritesStore. A fachada antiga de API não é criada dentro do novo ViewModel. O ViewModel antigo foi removido após verificar seus consumidores; DriverScreen e sua rota existente foram preservados.

Favoritos continuam nos arquivos e IDs originais. Novos favoritos adicionados pelo perfil salvam o nome completo disponível como rótulo, em vez de repetir o ID como nome. Remoção, atualização e falha de escrita são refletidas pela fonte reativa existente. Registros antigos não são migrados nem apagados automaticamente.

A tela utiliza carregamento progressivo, atualização manual/pull-to-refresh, avisos de cache/erro e dados históricos sob demanda do mesmo fluxo filtrado usado pelos favoritos. A última corrida só é associada ao próprio ID. Ausência na classificação atual não implica que o cadastro histórico desapareceu. Não há polling HTTP na tela.

Arquivos principais: components/home/DashboardCards.kt, presentation/driver/driverScreen.kt, presentation/driver/DriverDetailsViewModel.kt, domain/weekend/DriverPresentation.kt, domain/weekend/FavoriteModels.kt, data/repository/RacingRepository.kt, di/AppModule.kt e os cinco driver_strings.xml.

Validação: python3 scripts/verify_driver_profile.py, incluindo as verificações estáticas das sete fases, parsing dos 116 arquivos Kotlin, XMLs, nove novas traduções/argumentos nos cinco idiomas, identidade do app, integração e favoritos originais. git diff --check e integridade do ZIP também foram conferidos.

Adicionados cinco testes JVM (nome, iniciais, idade, datas inválidas e mapeamento do perfil) e três instrumentados (nome longo/fonte ampliada, navegação para equipe/corrida histórica e ausência de idade fictícia). O projeto possui 159 testes JVM e 27 instrumentados, ainda não executados neste ambiente.

A tentativa de :app:compileDebugKotlin continua bloqueada antes da compilação pelo download do Gradle, com Network is unreachable. Não há confirmação de compilação, testes Android ou revisão visual no aparelho. Validar fontes ampliadas, temas, navegação, favoritos após reinício, cadastro histórico/offline e os cinco idiomas no Android Studio/aparelho. O pacote ainda não é um APK validado.
