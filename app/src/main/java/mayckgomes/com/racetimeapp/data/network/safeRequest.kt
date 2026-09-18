package mayckgomes.com.racetimeapp.data.network

import io.ktor.client.call.*
import io.ktor.client.statement.*
import io.ktor.client.plugins.*
import java.io.IOException

suspend fun <T> safeRequest(block: suspend () -> T): ApiResult<T> {
    return try {
        ApiResult.Success(block())
    } catch (e: ResponseException) {
        when (e.response.status.value) {
            401 -> ApiResult.Unauthorized
            404 -> ApiResult.NotFound
            409 -> ApiResult.Conflict
            else -> ApiResult.UnknownError(e.message)
        }
    } catch (e: IOException) {
        ApiResult.NetworkError
    } catch (e: Exception) {
        ApiResult.UnknownError(e.message)
    }
}
