# RaceTime 2.0 — Fase 6: Notificações e Configurações

## Entrega integrada

Notificações continuam dentro de Configurações, acessível pelo header. O menu inferior mantém Home, Calendário, Campeonato e Favoritos, sem telemetria ou destino separado de notificações.

A tela oferece controle individual de FP1, FP2, FP3, Sprint Qualifying, Sprint, Qualifying e Race. As quatro antecedências são No horário, 15 minutos, 30 minutos e uma hora. As opções persistem ao fechar o aplicativo. Sessões ausentes ou sem horário conhecido não recebem horário inventado nem alarme.

A interface mostra permissão de notificações, precisão disponível, quantidade de alarmes pendentes, próximo aviso em horário local, última atualização real do calendário e falhas de sincronização/agendamento. Os controles têm área de toque mínima, rótulo acessível e quebra de linha. As antecedências usam FlowRow para telas pequenas e fonte ampliada. Vinte e quatro novos recursos foram traduzidos nos cinco idiomas existentes.

Outras opções de Configurações:

- Aparência Sistema, Escuro ou Claro, persistente e aplicada ao MaterialTheme, inclusive nas telas antigas que ainda usam o mesmo tema.
- Idioma: Android 13 ou superior usa a seleção por aplicativo do próprio Android, declarando os cinco idiomas no localeConfig. Em Android 12L ou anterior, o app continua seguindo o idioma do aparelho; a tela explica isso e abre os idiomas do sistema. Não foi adicionado AppCompat nem um seletor próprio para essas versões.
- Sobre: versão instalada e link do projeto.
- Informações legais: fonte dos dados, AdMob e links de licença/condições da Jolpica. Nenhuma autorização comercial foi presumida ou habilitada em produção.

## Mecanismo escolhido

AlarmManager realiza a entrega local porque os avisos se referem a um horário escolhido pelo usuário. Quando autorizado, usa setExactAndAllowWhileIdle; caso contrário, setAndAllowWhileIdle. A precisão exata é opcional e a UI informa a possibilidade de atraso. Não foi usado USE_EXACT_ALARM, serviço em primeiro plano, full-screen intent ou solicitação de isenção das otimizações de bateria.

POST_NOTIFICATIONS é solicitado apenas quando o usuário tenta ativar os lembretes em Android 13 ou superior. Negar não bloqueia o restante do aplicativo. A tela oferece acesso às configurações do sistema quando notificações/canal estão bloqueados. O acesso especial para alarmes precisos só é solicitado pelo botão explícito; a permissão é conferida antes de armar cada alarme, com fallback aproximado se necessário.

WorkManager serve apenas para revalidar o calendário. Há um trabalho periódico único, aproximadamente a cada 12 horas, com exigência de conexão, e atualização manual/ao ativar. Preferências menores são aplicadas ao calendário salvo; não forçam HTTP em cada toque quando o calendário ainda está atual. A periodicidade é cancelada ao desativar os lembretes. Retry usa backoff exponencial de 30 minutos e limite por execução; o gateway também mantém seus limites, cache, deduplicação e tratamento de HTTP 429.

O scheduler usa Clock injetável e o mesmo cronograma Session/raceSessions do WeekendResolver. Home, página do GP e worker compartilham RacingRepository e JolpicaGateway. Atualizações do Home/GP repassam o timestamp real da fonte, evitando apresentar uma leitura de cache como nova sincronização. Um calendário mais antigo não substitui um snapshot mais recente, exceto quando a marca salva está no futuro após uma correção do relógio do aparelho.

## Reconciliação e entrega

A regra pura planReminders compara calendário, preferências, horário atual, alarmes existentes, permissão e eventos já entregues:

- Mantém alarmes cujo ID e horário de disparo não mudaram.
- Cancela sessões removidas, categorias desativadas, horários antigos e alarmes vencidos.
- Reagenda alteração de antecedência ou horário e impede duplicação por sessão.
- Limita a janela aos 64 próximos avisos. Atualizações posteriores repõem a janela; não é criado um alarme para cada item de todo o histórico.
- Bloqueia nova entrega de uma sessão já notificada, mesmo se o horário agendado tiver mudado. A chave inclui temporada, rodada e tipo de sessão; os marcadores antigos também são reconhecidos.
- Restaura alarmes após boot, atualização do app, alteração do relógio/fuso/idioma e concessão do acesso a alarmes precisos. A inicialização do app e o retorno a Configurações também revalidam permissões e rearmam os alarmes salvos.

O snapshot planejado é salvo antes de armar os alarmes, permitindo validar um receiver que chega imediatamente no modo No horário. Falhas individuais deixam de ser contadas como alarmes armados e são informadas na UI. Receivers usam goAsync e IO para persistência local; não executam HTTP diretamente e sempre finalizam o broadcast.

Na entrega são verificadas novamente as preferências, a permissão/canal, o evento já entregue e a janela de validade. O ID precisa estar no conjunto de alarmes ativos. Receivers obsoletos não publicam uma notificação apenas porque um PendingIntent antigo disparou.

O receptor anterior descartava now >= startMillis, inviabilizando No horário. Agora aceita a entrega no início e uma tolerância de cinco minutos depois do horário previsto. **Essa tolerância é uma validade do aviso, não duração oficial da sessão nem confirmação de atividade em andamento.** Depois desse prazo, o aviso é descartado. Um novo alarme atrasado não é criado ao mudar preferências/abrir o app; um alarme já devido e salvo pode continuar nessa pequena janela enquanto aguarda entrega.

A mensagem usa início previsto e horário local. Se o sistema atrasou até depois do início previsto, o texto identifica o horário programado sem afirmar que a sessão começou de fato. Não promete entrega imediata ou precisão absoluta em Doze, após force-stop ou sob restrições do fabricante. Caso o calendário mude enquanto o aparelho está sem conexão, o app pode conservar um aviso baseado na última programação conhecida; a tela informa essa limitação.

## Abertura do GP

Cada notificação recebe PendingIntent explícito para MainActivity com URI própria racetime://gp/{season}/{round}. Isso evita que uma notificação posterior substitua o destino das anteriores.

MainActivity trata tanto onCreate quanto onNewIntent e entrega um alvo validado à navegação Compose. A rota tipada grandPrixRoute abre a página correspondente, mantendo retorno para o aplicativo. Alvos pendentes sobrevivem à recriação da Activity; após o consumo, não são reaplicados a cada recomposição/configuração. Somente temporada/rodada positivas e o formato esperado são aceitos.

## Compatibilidade

O arquivo session_reminders e as chaves settings, calendar, scheduled, delivered e last_sync foram preservados.

Os campos legados practice e qualifying permanecem decodificáveis. Quando os campos individuais não existem no JSON salvo, FP1/FP2/FP3 herdam practice e Sprint Qualifying herda qualifying. Categorias antigas desativadas não são reativadas por uma migração. A gravação usa encodeDefaults=true, evitando perder uma escolha individual igual ao default dependente de outro campo.

SessionReminder ganhou season/round opcionais, com fallback para os mesmos componentes já presentes no ID antigo. Não é necessário apagar o calendário ou os alarmes salvos para atualizar a versão.

Favoritos, applicationId, namespace, versão, assinatura e configuração de distribuição permanecem preservados. Não foram adicionadas bibliotecas ou novas permissões nesta fase. AdMob mantém os identificadores de teste.

## Resultados de favoritos

Não há toggle ou worker de resultados de favoritos: falta uma estratégia de publicação em background que seja confiável sem polling frequente. A fonte de integração futura é o mapa de PublishedResult efetivamente confirmado no repositório compartilhado, combinado com os IDs do FavoritesStore; o agendador de calendário continua separado desse fluxo.

A Jolpica não foi tratada como serviço de push. Nenhuma consulta periódica de resultados ou promessa de alerta imediatamente após a corrida foi adicionada. Essa limitação aparece em Configurações.

## Arquivos afetados

| Arquivo | Responsabilidade |
| --- | --- |
| domain/model/SessionReminders.kt | Preferências compatíveis, identidade, validade de entrega e plano de reconciliação testável |
| notifications/ReminderStore.kt | Serialização, persistência, marcadores de entrega e falhas |
| notifications/ReminderScheduler.kt | AlarmManager, permissões/canal, reconciliação e publicação local |
| notifications/ReminderReceiver.kt | Entrega e restauração assíncronas |
| notifications/ReminderSyncWorker.kt | Revalidação limitada de calendário e cancelamento dos trabalhos |
| notifications/ReminderTarget.kt | Validação do destino do GP |
| presentation/settings/SettingsViewModel.kt | Estado de Configurações, persistência e atualização sem HTTP na UI |
| presentation/settings/SettingsScreen.kt | Controles individuais, antecedência, status, aparência, idioma e informações legais |
| data/local/AppearancePreferences.kt / ui/theme/Theme.kt | Preferência de tema compartilhada |
| MainActivity.kt / navgation/navgation.kt | Consumo de notificação com app aberto/fechado e preservação do destino |
| RaceTimeApp.kt / di/AppModule.kt | Inicialização e injeção existentes |
| presentation/home/HomeViewModel.kt / presentation/gp/GrandPrixViewModel.kt | Timestamp real do calendário repassado ao scheduler |
| AndroidManifest.xml / res/xml/locales_config.xml | Restauração por idioma e idiomas por aplicativo |
| res/values*/preferences_strings.xml e notification_strings.xml | Cinco idiomas, precisão e limitações de sincronização |

## Validações

Adicionados nesta fase: 23 testes JVM e oito instrumentados. Totais: **121 testes JVM e 19 instrumentados**, incluindo exemplos originais. Estes são testes preparados no código-fonte, não uma suíte executada com sucesso.

Os testes novos cobrem migração dos grupos antigos, persistência de escolhas individuais, sete tipos separados, quatro antecedências, valores inválidos, plano sem duplicação, reboot, desativação, remoção/alteração de sessão, alteração de antecedência, marcador de entrega legado/novo, permissão revogada, modo No horário, expiração, preservação de alarme já devido, ausência de horários, SprintShootout, fuso, janela de 64 alarmes, destino antigo, SharedPreferences após recriação, aparência sem modificar favoritos, URI de notificação e acessibilidade do componente.

Executados com sucesso:

- python3 scripts/verify_phase6.py, incluindo todas as verificações estáticas das fases anteriores.
- Parser sintático dos 107 arquivos Kotlin, XMLs, strings, 24 novos recursos/argumentos nos cinco idiomas, manifesto e integração de navegação.
- Comparação dos repositórios de favoritos originais e identificadores de distribuição com o Git.
- git diff --check e integridade do pacote ZIP.

**Build e testes continuam bloqueados neste ambiente.** A tentativa de bash gradlew :app:testDebugUnitTest :app:assembleDebug --console=plain parou antes de compilar no download de gradle-9.6.0-bin.zip, com java.net.SocketException: Network is unreachable. Não foram executados JUnit, testes instrumentados, revisão visual ou validação real de entrega/boot/permissões. Não há APK gerado. O parser não verifica tipos nem substitui a compilação Compose; a compatibilidade/disponibilidade do toolchain original ainda depende da resolução das dependências.

Em um ambiente Android configurado:

```text
gradlew.bat :app:testDebugUnitTest :app:assembleDebug --console=plain
gradlew.bat :app:connectedDebugAndroidTest
```

Matriz manual pendente em Android 26, 31/32, 33 e 34/35:

| Cenário | Verificar no aparelho/emulador |
| --- | --- |
| Atualização com preferências antigas | Categorias e antecedência conservadas; favoritos intactos |
| POST_NOTIFICATIONS negada, concedida e revogada | Nenhum envio sem permissão; UI coerente; rearmar ao voltar |
| Canal desativado | Avisos bloqueados e acesso às configurações do sistema |
| Acesso exato negado/concedido/revogado | Fallback aproximado, aviso de limitação e restauração posterior |
| No horário / 15 / 30 / 60 | Trigger correto, texto prudente e nenhuma entrega muito tardia |
| Preferência/horário removido ou alterado | Cancelamento de PendingIntent antigo e ausência de duplicatas |
| Reinício e atualização do app | Alarmes preservados/restaurados com IDs corretos |
| Mudança de fuso/relógio/idioma | UTC mantido, hora local e idioma reavaliados |
| Toque com app fechado/aberto e várias notificações | GP correto, sem troca de destino, botão voltar e rotação |
| Offline, reconexão, 429, falha do sistema | Cache preservado, retry limitado e falha visível |
| Tela pequena/fonte ampliada | Controles acessíveis e chips sem overflow |
| Tema e idioma | Persistência após reinício e retorno das configurações Android |

## Referências oficiais consultadas

- https://developer.android.com/develop/background-work/services/alarms
- https://developer.android.com/about/versions/14/changes/schedule-exact-alarms
- https://developer.android.com/develop/ui/compose/notifications/notification-permission
- https://developer.android.com/guide/topics/resources/app-languages
- https://github.com/jolpica/jolpica-f1/tree/main/docs

## Estado do plano

Fases 1–6 possuem código integrado, com build e validação de execução pendentes pelo impedimento acima. Não devem ser tratadas como aprovadas para produção. A Fase 7 (estatísticas, pit stops/voltas sob demanda e destaques verificáveis do GP) continua pendente.

Atualização posterior: a Fase 7 foi integrada; ver [FASE7_ESTATISTICAS.md](FASE7_ESTATISTICAS.md). Os totais e o estado acima registram a entrega da Fase 6.
