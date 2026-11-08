# RaceTime 2.0 — entrega consolidada

As sete fases estão integradas ao projeto existente, com dados da Jolpica.

- [Auditoria e motor](docs/AUDITORIA_RACETIME_2.md).
- [Home e navegação](docs/FASE2_HOME.md).
- [Campeonato](docs/FASE3_CAMPEONATO.md).
- [Calendário e GP](docs/FASE4_CALENDARIO_GP.md).
- [Favoritos](docs/FASE5_FAVORITOS.md).
- [Notificações e Configurações](docs/FASE6_NOTIFICACOES.md).
- [Estatísticas](docs/FASE7_ESTATISTICAS.md).
- [Correção de compilação em Favoritos](docs/CORRECAO_COMPILACAO_FAVORITOS.md).
- [Nome no Home e perfil do piloto](docs/REVISAO_PERFIL_PILOTO.md).
- [Skeleton, botões e cache da Home](docs/REVISAO_HOME_CACHE.md).

A revisão mais recente adiciona skeleton por seção, padroniza as ações da Home e restaura o cache local antes de aguardar HTTP. Abertura respeita as validades; atualização manual revalida os dados. Favoritos, idiomas, AdMob de teste, package, versão e configuração de assinatura foram preservados.

Verificações estáticas aprovadas em 121 arquivos Kotlin. Há 164 testes JVM e 31 instrumentados preparados, ainda não executados neste ambiente. O build continua bloqueado antes de compilar pelo download do Gradle, com Network is unreachable. Faltam compilação, testes, revisão visual e validação no aparelho; não há APK gerado nem aprovação para produção.

Nenhum dado esportivo, imagem de piloto ou gráfico foi simulado para preencher a interface. Não foram habilitados live timing, telemetria ou alertas imediatos de resultados de favoritos, nem presumida autorização comercial dos dados. Nenhuma alteração foi publicada no GitHub ou em loja.
