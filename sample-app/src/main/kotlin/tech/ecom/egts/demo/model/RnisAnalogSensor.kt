package tech.ecom.egts.demo.model

import tech.ecom.egts.library.exception.EgtsAdapterException
import tech.ecom.egts.library.exception.EgtsExceptionErrorCode

enum class RnisAnalogSensor(val sensorNumber: Byte) {
    VEHICLE_TYPE(0xC2.toByte()),
    COURIER_STATUS(0xC5.toByte()),
    KISART_ID(0xC1.toByte()),
    ;

    companion object {
        private val sensorNumberMap = RnisAnalogSensor.entries.associateBy(RnisAnalogSensor::sensorNumber)
        fun fromSensorNumber(sensorNumber: Byte) = sensorNumberMap[sensorNumber]
            ?: throw EgtsAdapterException(
                code = EgtsExceptionErrorCode.EGTS_DECODE_EXCEPTION,
                errorMessage = "no AnalogSensor with number $sensorNumber implemented. Start from AnalogSensor enum",
            )
    }
}