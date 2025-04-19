package tech.ecom.egts.demo.service

import kotlin.random.Random
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import tech.ecom.egts.demo.AbstractIntegrationTest
import tech.ecom.egts.demo.model.CourierTrackRecord
import tech.ecom.egts.demo.model.CourierTrackRecord.Companion.KISARTID_THRESHOLD_VALUE
import tech.ecom.egts.demo.model.RnisAnalogSensor
import tech.ecom.egts.demo.model.random
import tech.ecom.egts.demo.service.EgtsPacketsHelper.Companion.AUTH_RESULT_PACKET
import tech.ecom.egts.library.constants.EgtsConstants.Companion.RST_OK
import tech.ecom.egts.library.model.EgtsPacket
import tech.ecom.egts.library.model.PacketType
import tech.ecom.egts.library.model.sfrd.AppServicesFrameData
import tech.ecom.egts.library.model.sfrd.ResponseServicesFrameData
import tech.ecom.egts.library.model.sfrd.TransportLayerProcessingResult
import tech.ecom.egts.library.model.sfrd.record.RecordData
import tech.ecom.egts.library.model.sfrd.record.subrecord.SubRecordType
import tech.ecom.egts.library.model.sfrd.record.subrecord.types.AnalogSensorData
import tech.ecom.egts.library.model.sfrd.record.subrecord.types.ExternalSensorData
import tech.ecom.egts.library.model.sfrd.record.subrecord.types.PosSubRecordData
import tech.ecom.egts.library.model.sfrd.record.subrecord.types.SubRecordResponse
import tech.ecom.egts.library.utils.hexStringToByteArray

class PacketFactoryTest : AbstractIntegrationTest() {

    private lateinit var packetFactory: PacketFactory

    @BeforeEach
    fun initNewFactory() {
        packetFactory = PacketFactory(
            egtsPacketEncoder,
        )
    }

    @Test
    fun `WHEN assembling response packet THEN its attributes match ones of original packet`() {
        // given
        val packet = packetFactory.assembleAuthPacket()

        // when
        val responsePacket = packetFactory.assembleSingleRecordResponsePacket(packet)

        // then
        with(responsePacket) {
            assertThat(packetType).isEqualTo(PacketType.RESPONSE)
            with(servicesFrameData as ResponseServicesFrameData) {
                assertThat(responsePacketId).isEqualTo(packet.packetIdentifier)
                assertThat(processingResult).isEqualTo(TransportLayerProcessingResult.EGTS_PC_OK)
                with(serviceDataRecords!!.sdrList.first()) {
                    val sourcePacketRecord = (packet.servicesFrameData as AppServicesFrameData).serviceDataRecords.sdrList.first()
                    assertThat(sourceServiceType).isEqualTo(sourcePacketRecord.sourceServiceType)
                    assertThat(recipientServiceType).isEqualTo(sourcePacketRecord.recipientServiceType)
                    with(recordData.subRecordList.first()) {
                        assertThat(subRecordTypeId).isEqualTo(SubRecordType.EGTS_SR_RECORD_RESPONSE.id)
                        with(subRecordData as SubRecordResponse) {
                            assertThat(confirmedRecordNumber).isEqualTo(sourcePacketRecord.recordNumber.toShort())
                            assertThat(recordStatus).isEqualTo(RST_OK)
                        }
                    }
                }
            }
        }
    }

    @Test
    fun `WHEN packets assembled starting from auth packet THEN packets and records numeration does cycle 0 65535`() {
        // given
        val authPacket = packetFactory.assembleAuthPacket()
        val trackRecordsListWithSingleRecord = listOf(CourierTrackRecord.random())

        // then
        assertThat(authPacket.packetIdentifier).isEqualTo(packetFactory.FIRST_PACKET_ID)
        assertThat(assembleAuthConfirmationPacket().packetIdentifier).isEqualTo((packetFactory.FIRST_PACKET_ID + 1u).toUShort())
        ((packetFactory.FIRST_PACKET_ID + 2u).toUShort()..packetFactory.MAX_USHORT_ID).forEach { expectedPacketNumber ->
            val expectedRecordNumber = expectedPacketNumber
            packetFactory.assembleTelematicsPacket(trackRecordsListWithSingleRecord)
                .assertPacketAndRecordNumbers(
                    expectedPacketNumber = expectedPacketNumber.toUShort(),
                    expectedRecordNumber = expectedRecordNumber.toUShort(),
                )
        }
    }

    private fun EgtsPacket.assertPacketAndRecordNumbers(expectedPacketNumber: UShort, expectedRecordNumber: UShort) {
        assertThat(this.packetIdentifier).isEqualTo(expectedPacketNumber)
        assertThat(this.getFirstRecordNumber()).isEqualTo(expectedRecordNumber)
    }

    private fun EgtsPacket.getFirstRecordNumber(): UShort {
        return when (val servicesFrameData = this.servicesFrameData) {
            is AppServicesFrameData ->
                servicesFrameData
                    .serviceDataRecords
                    .sdrList.first()
                    .recordNumber
            is ResponseServicesFrameData ->
                servicesFrameData
                    .serviceDataRecords
                    ?.sdrList
                    ?.first()
                    ?.recordNumber!!
            else -> throw IllegalArgumentException("Unsupported serviceFrameData type: ${this::class}")
        }
    }

    private fun assembleAuthConfirmationPacket() = packetFactory
        .assembleSingleRecordResponsePacket(
            egtsPacketEncoder.decode(AUTH_RESULT_PACKET.hexStringToByteArray()),
        )

    @Test
    fun `WHEN assembleAuthPacket called then packets and records numeration reset`() {
        // given
        packetFactory.assembleAuthPacket().assertPacketAndRecordNumbers(
            expectedPacketNumber = packetFactory.FIRST_PACKET_ID,
            expectedRecordNumber = packetFactory.FIRST_RECORD_ID,
        )
        assembleAuthConfirmationPacket().assertPacketAndRecordNumbers(
            expectedPacketNumber = (packetFactory.FIRST_PACKET_ID + 1U).toUShort(),
            expectedRecordNumber = (packetFactory.FIRST_RECORD_ID + 1U).toUShort(),
        )
        packetFactory.assembleTelematicsPacket(listOf(CourierTrackRecord.random())).assertPacketAndRecordNumbers(
            expectedPacketNumber = (packetFactory.FIRST_PACKET_ID + 2U).toUShort(),
            expectedRecordNumber = (packetFactory.FIRST_RECORD_ID + 2U).toUShort(),
        )

        // then
        packetFactory.assembleAuthPacket().assertPacketAndRecordNumbers(
            expectedPacketNumber = packetFactory.FIRST_PACKET_ID,
            expectedRecordNumber = packetFactory.FIRST_RECORD_ID,
        )
        assembleAuthConfirmationPacket().assertPacketAndRecordNumbers(
            expectedPacketNumber = (packetFactory.FIRST_PACKET_ID + 1U).toUShort(),
            expectedRecordNumber = (packetFactory.FIRST_RECORD_ID + 1U).toUShort(),
        )
        packetFactory.assembleTelematicsPacket(listOf(CourierTrackRecord.random())).assertPacketAndRecordNumbers(
            expectedPacketNumber = (packetFactory.FIRST_PACKET_ID + 2U).toUShort(),
            expectedRecordNumber = (packetFactory.FIRST_RECORD_ID + 2U).toUShort(),
        )
    }

    @Test
    fun `WHEN CourierTrackRecord transformed to RecordData THEN presence of kisart_id at AnalogSensor subrecord depends on its value`() {
        // given
        val courierTrackRecordWithSmallKisartId = CourierTrackRecord.random(
            kisartId = KISARTID_THRESHOLD_VALUE,
        )
        val courierTrackRecordWithBigKisartId = CourierTrackRecord.random(
            kisartId = KISARTID_THRESHOLD_VALUE + 1,
        )

        // when
        val sensorDataWithSmallKisart = extractAnalogSensorDataList(courierTrackRecordWithSmallKisartId.toRecordData())
        val sensorDataWithBigKisart = extractAnalogSensorDataList(courierTrackRecordWithBigKisartId.toRecordData())

        // then
        assertThat(sensorDataWithSmallKisart.find {
            it.analogSensorNumber == RnisAnalogSensor.KISART_ID.sensorNumber
        }).isNotNull
        assertThat(sensorDataWithBigKisart.find {
            it.analogSensorNumber == RnisAnalogSensor.KISART_ID.sensorNumber
        }).isNull()
    }

    private fun extractAnalogSensorDataList(recordData: RecordData): List<AnalogSensorData> {
        return recordData.subRecordList
            .filter { it.subRecordTypeId == SubRecordType.EGTS_SR_ABS_AN_SENS_DATA.id }
            .map { it.subRecordData as AnalogSensorData }
    }

    @Test
    fun `WHEN CourierTrackRecord transformed to RecordData THEN attribute values at data match initial ones of record`() {
        // given
        val courierTrackRecord = CourierTrackRecord.random()
        val subRecordList = courierTrackRecord.toRecordData().subRecordList

        // when
        val posSubRecordData = subRecordList
            .first { it.subRecordTypeId == SubRecordType.EGTS_SR_POS_DATA.id }
            .subRecordData!! as PosSubRecordData

        val absAnalogSensorDataList = subRecordList
            .filter { it.subRecordTypeId == SubRecordType.EGTS_SR_ABS_AN_SENS_DATA.id }
            .map { it.subRecordData as AnalogSensorData }
        val vehicleTypeAnalogSensorValue = absAnalogSensorDataList
            .first { it.analogSensorNumber == RnisAnalogSensor.VEHICLE_TYPE.sensorNumber }
            .analogSensorValue
        val courierStatusAnalogSensorValue = absAnalogSensorDataList
            .first { it.analogSensorNumber == RnisAnalogSensor.COURIER_STATUS.sensorNumber }
            .analogSensorValue
        val kisartIdAnalogSensorValue = absAnalogSensorDataList
            .firstOrNull { it.analogSensorNumber == RnisAnalogSensor.KISART_ID.sensorNumber }
            ?.analogSensorValue

        val rnisExternalDataValue = subRecordList
            .first { it.subRecordTypeId == SubRecordType.EGTS_SR_EXT_DATA.id }
            .subRecordData as ExternalSensorData

        // then
        assertThat(posSubRecordData).isEqualTo(courierTrackRecord.toPosSubRecordData())

        with(courierTrackRecord) {
            assertThat(vehicleTypeAnalogSensorValue).isEqualTo(courierData.vehicleType.code)
            assertThat(courierStatusAnalogSensorValue).isEqualTo(courierData.courierStatus.code)
            kisartIdAnalogSensorValue?.let { assertThat(kisartIdAnalogSensorValue).isEqualTo(courierData.kisartId) }
        }

        with(rnisExternalDataValue.vendorsData) {
            assertThat(data?.first()).isNotEqualTo(";")
            assertThat(data?.last()).isNotEqualTo(";")
        }
    }

    @Test
    fun `WHEN CourierTrackRecord transformed to PosSubRecordData THEN position attributes match ones of initial record`() {
        // given
        val courierTrackRecord = CourierTrackRecord.random()

        // when
        val posSubRecordData = courierTrackRecord.toPosSubRecordData()

        // then
        with(posSubRecordData) {
            assertThat(navigationTime).isEqualTo(courierTrackRecord.time)
            assertThat(latitude).isEqualTo(courierTrackRecord.navigationData.latitude)
            assertThat(longitude).isEqualTo(courierTrackRecord.navigationData.longitude)
            assertThat(isValid).isEqualTo(courierTrackRecord.navigationData.isValid)
            assertThat(speed).isEqualTo(courierTrackRecord.navigationData.speed)
            assertThat(direction).isEqualTo(courierTrackRecord.navigationData.direction.toInt())
        }
    }

    @Test
    fun `WHEN CourierTrackRecord transformed to RecordData AND direction does not fit to 360 degrees interval THEN exception is thrown`() {
        assertThrows<IllegalArgumentException> {
            CourierTrackRecord.random(
                direction = Random.nextDouble(from = Double.MIN_VALUE, until = 0.0),
            ).toRecordData()
        }

        assertThrows<IllegalArgumentException> {
            CourierTrackRecord.random(
                direction = Random.nextDouble(from = Double.MIN_VALUE, until = 0.0),
            ).toRecordData()
        }

        assertThrows<IllegalArgumentException> {
            CourierTrackRecord.random(
                direction = Random.nextDouble(from = 360.1, until = Double.MAX_VALUE),
            ).toRecordData()
        }
    }

    @Test
    fun `WHEN telematics packet assembled and attId is negative THEN exception is thrown`() {
        // given
        val trackRecordsListWithNegativeAttId = listOf(
            CourierTrackRecord.random(
                attId = Random.nextInt(from = Int.MIN_VALUE, until = 0),
            ),
        )

        // when
        assertThrows<java.lang.IllegalArgumentException> {
            packetFactory.assembleTelematicsPacket(trackRecordsListWithNegativeAttId)
        }
    }
}