package mayckgomes.com.racetimeapp.domain.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DriverInfoResponse(
    val MRData: MRdataDriverInfo
)

@Serializable
@SerialName("MRData")
data class MRdataDriverInfo(
    val DriverTable: DriverInfoTable
)

@Serializable
@SerialName("DriverTable")
data class DriverInfoTable(
    val driverId: String,
    val Drivers: List<DriverInfo>
)

@Serializable
@SerialName("Driver")
data class DriverInfo(
    val permanentNumber: String,
    val givenName: String,
    val familyName: String,
    val dateOfBirth: String,
    val nationality: String
)

val driverInfoTest = listOf(
    DriverInfo(
        permanentNumber = "12",
        givenName = "kimi",
        familyName = "antonelli",
        dateOfBirth = "",
        nationality = "italian"
    ),
    DriverInfo(
        permanentNumber = "12",
        givenName = "kimi",
        familyName = "antonelli",
        dateOfBirth = "",
        nationality = "italian"
    ),
    DriverInfo(
        permanentNumber = "12",
        givenName = "kimi",
        familyName = "antonelli",
        dateOfBirth = "",
        nationality = "italian"
    )

)