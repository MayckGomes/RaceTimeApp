# Correção de compilação — Favoritos

O log enviado pelo usuário chegou a compileDebugKotlin e apontou um erro no tratamento de falha de FavoritesViewModel: DataBlock<CapturedType(out FavoriteProfile?)> não era compatível com DataBlock<FavoriteProfile?>.

A expressão Elvis entre o perfil existente e DataBlock(null), seguida de copy, produzia um tipo projetado. O trecho agora obtém um FavoriteDetails tipado e copia seu profile, preservando o tipo exato e os dados disponíveis. O fallback do caminho de sucesso também declara FavoriteProfile? explicitamente. Não houve alteração no modelo genérico global nem nos favoritos persistidos.

As verificações estáticas das sete fases e git diff --check passaram. A tentativa local de :app:compileDebugKotlin continua bloqueada antes de compilar pelo download do Gradle (Network is unreachable). A correção ainda precisa ser recompilada no ambiente Android do usuário; não há APK validado.

Os avisos de depreciação presentes no log não causaram a falha de compilação Kotlin. Nenhuma configuração Gradle foi alterada para ocultá-los.
