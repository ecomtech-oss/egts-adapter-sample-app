package tech.ecom.egts.demo.extension.encoder.sfrd.record.subrecord

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import tech.ecom.egts.demo.extension.model.sfrd.record.subrecord.types.CounterSubRecordData
import tech.ecom.egts.library.encoder.sfrd.record.subrecord.AbstractSubRecordEncoder
import tech.ecom.egts.library.utils.readByte
import tech.ecom.egts.library.utils.readThreeBytesToPositiveInt
import tech.ecom.egts.library.utils.toThreeByteLittleEndianByteArray

class CounterDataEncoder : AbstractSubRecordEncoder<CounterSubRecordData>(
    subRecordTypeId = 25, // EGTS protocol documentation Таблица Б.1 - Список подзаписей сервиса EGTS_TELEDATA_SERVICE
    fieldName = "EGTS_SR_ABS_CNTR_DATA",
) {

    override fun performEncode(egtsEntity: CounterSubRecordData): ByteArray =
        ByteArrayOutputStream().apply {
            with(egtsEntity) {
                write(byteArrayOf(counterNumber))
                write(counterValue.toThreeByteLittleEndianByteArray())
            }
        }.toByteArray()

    override fun performDecode(byteArray: ByteArray): CounterSubRecordData {
        ByteArrayInputStream(byteArray).apply {
            val counterNumber = readByte().also {
                logger.trace("AnalogSensor sensorNumber byteValue is {}", it)
            }

            val counterValue = readThreeBytesToPositiveInt().also {
                logger.trace("AnalogSensor value is {}", it)
            }

            return CounterSubRecordData(
                counterNumber = counterNumber,
                counterValue = counterValue,
            )
        }
    }
}