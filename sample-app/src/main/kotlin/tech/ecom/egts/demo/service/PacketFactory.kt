package tech.ecom.egts.demo.service

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import tech.ecom.egts.demo.model.CourierTrackRecord
import tech.ecom.egts.demo.model.CourierTrackRecord.Companion.KISARTID_THRESHOLD_VALUE
import tech.ecom.egts.demo.model.RnisAnalogSensor
import tech.ecom.egts.library.constants.EgtsConstants
import tech.ecom.egts.library.encoder.EgtsPacketEncoder
import tech.ecom.egts.library.encoder.sfrd.record.subrecord.AbstractSubRecordEncoder
import tech.ecom.egts.library.encoder.sfrd.record.subrecord.DispatcherIdentityEncoder
import tech.ecom.egts.library.encoder.sfrd.record.subrecord.PosSubRecordDataEncoder
import tech.ecom.egts.library.encoder.sfrd.record.subrecord.SubRecordResponseEncoder
import tech.ecom.egts.library.model.EgtsPacket
import tech.ecom.egts.library.model.PacketType
import tech.ecom.egts.library.model.sfrd.AppServicesFrameData
import tech.ecom.egts.library.model.sfrd.ResponseServicesFrameData
import tech.ecom.egts.library.model.sfrd.ServiceDataRecords
import tech.ecom.egts.library.model.sfrd.TransportLayerProcessingResult
import tech.ecom.egts.library.model.sfrd.record.RecordData
import tech.ecom.egts.library.model.sfrd.record.ServiceDataRecord
import tech.ecom.egts.library.model.sfrd.record.ServiceType
import tech.ecom.egts.library.model.sfrd.record.subrecord.SubRecord
import tech.ecom.egts.library.model.sfrd.record.subrecord.SubRecordType
import tech.ecom.egts.library.model.sfrd.record.subrecord.types.AnalogSensorData
import tech.ecom.egts.library.model.sfrd.record.subrecord.types.DispatcherIdentity
import tech.ecom.egts.library.model.sfrd.record.subrecord.types.ExternalSensorData
import tech.ecom.egts.library.model.sfrd.record.subrecord.types.PosSubRecordData
import tech.ecom.egts.library.model.sfrd.record.subrecord.types.SubRecordResponse
import tech.ecom.egts.library.model.sfrd.record.subrecord.types.VendorData
import tech.ecom.egts.library.utils.toHexString

@Service
class PacketFactory(
    private val egtsPacketEncoder: EgtsPacketEncoder,
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    @Value("\${egts.dispatcher-id}")
    private var dispatcherId: Int = 0

    companion object {
        const val AUTH_RESULT_PACKET_NUMBER: UShort = 1u
    }

    val MAX_USHORT_ID: UShort = UShort.MAX_VALUE
    val FIRST_PACKET_ID: UShort = 0u
    val FIRST_RECORD_ID: UShort = 0u
    private var packetCounter: UShort = FIRST_PACKET_ID
    private var recordCounter: UShort = FIRST_RECORD_ID

    fun assembleAuthPacket(): EgtsPacket {
        logger.debug("assembleAuthPacket()")

        packetCounter = FIRST_PACKET_ID
        recordCounter = FIRST_RECORD_ID

        val authSubRecord = SubRecord(
            subRecordTypeId = SubRecordType.EGTS_SR_DISPATCHER_IDENTITY.id,
            subRecordData = DispatcherIdentity(dispatcherId = dispatcherId),
        )

        val serviceDataRecord = ServiceDataRecord(
            recordNumber = getRecordNumber(),
            sourceServiceType = ServiceType.AUTH_SERVICE,
            recipientServiceType = ServiceType.AUTH_SERVICE,
            recordData = RecordData(
                subRecordList = listOf(authSubRecord),
            ),
        )

        val servicesFrameData = AppServicesFrameData(
            serviceDataRecords = ServiceDataRecords(
                sdrList = listOf(serviceDataRecord),
            ),
        )

        val egtsPacket = EgtsPacket(
            packetIdentifier = getPacketNumber(),
            packetType = PacketType.APP_DATA,
            servicesFrameData = servicesFrameData,
        )

        return egtsPacket.also {
            logger.info("assembled auth packet as {}", it)
        }
    }

    fun assembleSingleRecordResponsePacket(incomingPacket: EgtsPacket): EgtsPacket {
        logger.debug("assembleSingleRecordResponsePacket()")
        val incomingPacketRecord = (incomingPacket.servicesFrameData as AppServicesFrameData)
            .serviceDataRecords.sdrList.first()

        val responseSubRecord = SubRecordResponse(
            confirmedRecordNumber = incomingPacketRecord.recordNumber.toShort(),
            recordStatus = EgtsConstants.RST_OK,
        )

        val responseSubRecordData = SubRecord(
            subRecordTypeId = SubRecordType.EGTS_SR_RECORD_RESPONSE.id,
            subRecordData = responseSubRecord,
        )

        val responseRecordData = RecordData(
            subRecordList = listOf(responseSubRecordData),
        )

        val responseRecord = ServiceDataRecord(
            recordNumber = getRecordNumber(),
            recordData = responseRecordData,
            sourceServiceType = incomingPacketRecord.sourceServiceType,
            recipientServiceType = incomingPacketRecord.recipientServiceType,
        )

        val responseServiceDataRecords = ServiceDataRecords(
            sdrList = listOf(responseRecord),
        )

        val servicesFrameData = ResponseServicesFrameData(
            responsePacketId = incomingPacket.packetIdentifier,
            processingResult = TransportLayerProcessingResult.EGTS_PC_OK,
            serviceDataRecords = responseServiceDataRecords,
        )

        return EgtsPacket(
            servicesFrameData = servicesFrameData,
            packetIdentifier = getPacketNumber(),
            packetType = PacketType.RESPONSE,
        ).also { logger.info("assembled single record response packet {}", egtsPacketEncoder.encode(it).toHexString()) }
    }

    fun assembleTelematicsPacket(courierTrackRecords: List<CourierTrackRecord>): EgtsPacket {
        logger.debug("assembleTelematicsPacket(list of {} CourierTrackRecords)", courierTrackRecords.size)

        val couriersServiceDataRecords = courierTrackRecords.map { courierTrackRecord ->
            val attId = courierTrackRecord.courierData.attId
            if (attId < 0) throw IllegalArgumentException("attId appears to be negative which should not be the case")
            val recordData = courierTrackRecord.toRecordData()
            ServiceDataRecord(
                recordNumber = getRecordNumber(),
                objectIdentifier = attId.toUInt(),
                sourceServiceType = ServiceType.TELE_DATA_SERVICE,
                recipientServiceType = ServiceType.TELE_DATA_SERVICE,
                recordData = recordData,
            )
        }

        val servicesFrameData = AppServicesFrameData(
            serviceDataRecords = ServiceDataRecords(
                sdrList = couriersServiceDataRecords,
            ),
        )

        return EgtsPacket(
            packetIdentifier = getPacketNumber(),
            packetType = PacketType.APP_DATA,
            servicesFrameData = servicesFrameData,
        ).also { logger.info("assembled telematics packet") }
    }

    private fun getPacketNumber() = packetCounter.also { if (packetCounter++ == MAX_USHORT_ID) packetCounter = FIRST_PACKET_ID }
    private fun getRecordNumber() = recordCounter.also { if (recordCounter++ == MAX_USHORT_ID) recordCounter = FIRST_PACKET_ID }
}

fun CourierTrackRecord.toPosSubRecordData(): PosSubRecordData {
    if (navigationData.direction !in 0.0..360.0) throw IllegalArgumentException("Direction must fit in range 0..360.0")
    return PosSubRecordData(
        navigationTime = time,
        latitude = navigationData.latitude,
        longitude = navigationData.longitude,
        isValid = navigationData.isValid,
        speed = navigationData.speed,
        direction = navigationData.direction.toInt(),
    )
}

fun CourierTrackRecord.toRecordData(): RecordData {
    val posSubRecord = SubRecord(
        subRecordTypeId = SubRecordType.EGTS_SR_POS_DATA.id,
        subRecordData = toPosSubRecordData(),
    )
    val vehicleTypeAnalogSensorSubRecord = SubRecord(
        subRecordTypeId = SubRecordType.EGTS_SR_ABS_AN_SENS_DATA.id,
        subRecordData = AnalogSensorData(
            analogSensorNumber = RnisAnalogSensor.VEHICLE_TYPE.sensorNumber,
            analogSensorValue = courierData.vehicleType.code,
        ),
    )
    val courierStatusAnalogSensorSubRecord = SubRecord(
        subRecordTypeId = SubRecordType.EGTS_SR_ABS_AN_SENS_DATA.id,
        subRecordData = AnalogSensorData(
            analogSensorNumber = RnisAnalogSensor.COURIER_STATUS.sensorNumber,
            analogSensorValue = courierData.courierStatus.code,
        ),
    )

    val rnisExternalDataString = with(courierData) {
        val kisartIdString = with(courierData) {
            if (kisartId != null && kisartId > KISARTID_THRESHOLD_VALUE) "kisartid=$kisartId;" else ""
        }
        val vplateString = vehicleNumber?.let { "vplate=$it;" } ?: ""
        val bagplatesString = "bagplates=$backpackNumber"
        val ordersString = if (orderId?.isNotEmpty() == true) ";orders=${orderId.joinToString(separator = ",")}" else ""
        "$kisartIdString$vplateString$bagplatesString$ordersString"
    }

    val rnisExternalDataSubRecord = SubRecord(
        subRecordTypeId = SubRecordType.EGTS_SR_EXT_DATA.id,
        subRecordData = ExternalSensorData(
            vendorsData = VendorData(
                data = rnisExternalDataString,
            ),
        ),
    )

    val subRecordList = mutableListOf(
        posSubRecord,
        vehicleTypeAnalogSensorSubRecord,
        courierStatusAnalogSensorSubRecord,
        rnisExternalDataSubRecord,
    )

    with(courierData) {
        if (kisartId != null && kisartId <= KISARTID_THRESHOLD_VALUE) {
            subRecordList.add(
                SubRecord(
                    subRecordTypeId = SubRecordType.EGTS_SR_ABS_AN_SENS_DATA.id,
                    subRecordData = AnalogSensorData(
                        analogSensorNumber = RnisAnalogSensor.KISART_ID.sensorNumber,
                        analogSensorValue = kisartId,
                    ),
                ),
            )
        }
    }

    return RecordData(
        subRecordList = subRecordList,
    )
}