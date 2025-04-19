package tech.ecom.egts.demo.model

import java.sql.Timestamp
import java.util.UUID
import kotlin.random.Random

data class CourierTrackRecord(
    val id: UUID = UUID.randomUUID(),
    val orderId: List<UUID>?,
    val time: Timestamp,
    val courierData: CourierData,
    val navigationData: NavigationData,
) {
    companion object {
        const val KISARTID_THRESHOLD_VALUE = 16777215
    }
}

data class CourierData(
    val attId: Int,
    val courierId: Int,
    val kisartId: Int?,
    val courierStatus: CourierStatus,
    val vehicleType: VehicleType,
    val vehicleNumber: String?,
    val backpackNumber: String,
)

data class NavigationData(
    val latitude: Double,
    val longitude: Double,
    val isValid: Boolean,
    val speed: Double,
    val direction: Double,
)

enum class CourierStatus(val code: Int) {
    IDLE(0), // свободен
    PICKING_ORDER(1), // забирает заказ
    DELIVERING_ORDER(2), // доставляет заказ
    HANDING_OVER_ORDER(3), // отдает заказ покупателю
    ;

    companion object {
        private val map = entries.associateBy(CourierStatus::code)
        fun fromCode(code: Int): CourierStatus {
            return map[code]!!
        }
    }
}

enum class VehicleType(val code: Int) {
    BY_FOOT(0), // пешком
    PERSONAL_MOBILITY_DEVICE(1), // СИМ
    BICYCLE(2), // велосипед
    CAR(3), // автомобиль
    ;

    companion object {
        private val map = entries.associateBy(VehicleType::code)
        fun fromCode(code: Int): VehicleType {
            return map[code]!!
        }
    }
}

fun CourierTrackRecord.Companion.random(
    attId: Int = Random.nextInt(from = 0, until = 10000000),
    kisartId: Int? = if (Random.nextBoolean()) Random.nextInt(from = 0, until = Int.MAX_VALUE) else null,
    direction: Double = Random.nextDouble(0.0, 360.0),
) = CourierTrackRecord(
    id = UUID.randomUUID(),
    orderId = if (Random.nextBoolean()) List(Random.nextInt(from = 1, until = 3)) { UUID.randomUUID() } else null,
    time = Timestamp(System.currentTimeMillis()),
    courierData = CourierData(
        attId = attId,
        courierId = Random.nextInt(from = 0, until = 10000000),
        kisartId = kisartId,
        courierStatus = CourierStatus.entries.random(),
        vehicleType = VehicleType.entries.random(),
        vehicleNumber = if (Random.nextBoolean()) generateRandomString() else null,
        backpackNumber = generateRandomString(),
    ),
    navigationData = NavigationData(
        latitude = Random.nextDouble(from = -90.0, until = 90.0),
        longitude = Random.nextDouble(from = -180.0, until = 180.0),
        isValid = Random.nextBoolean(),
        speed = Random.nextDouble(from = 0.0, until = 1000.0),
        direction = direction
    ),
)

fun generateRandomString() = List(Random.nextInt(from = 1, until = 10)) {
    Random.nextInt(from = 'a'.code, until = 'z'.code + 1).toChar()
}.joinToString(separator = "")
