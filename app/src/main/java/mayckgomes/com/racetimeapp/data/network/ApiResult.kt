package mayckgomes.com.racetimeapp.data.network

sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    object Unauthorized : ApiResult<Nothing>()
    object NotFound : ApiResult<Nothing>()
    object Conflict : ApiResult<Nothing>()
    object NetworkError : ApiResult<Nothing>()
    data class UnknownError(val message: String?) : ApiResult<Nothing>()
}
