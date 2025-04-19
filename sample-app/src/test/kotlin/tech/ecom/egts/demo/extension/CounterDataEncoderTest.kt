package tech.ecom.egts.demo.extension

import kotlin.random.Random
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import tech.ecom.egts.demo.AbstractIntegrationTest
import tech.ecom.egts.demo.extension.encoder.sfrd.record.subrecord.CounterDataEncoder
import tech.ecom.egts.demo.extension.model.sfrd.record.subrecord.types.CounterSubRecordData
import tech.ecom.egts.library.model.EgtsPacket
import tech.ecom.egts.library.model.PacketType
import tech.ecom.egts.library.model.sfrd.AppServicesFrameData
import tech.ecom.egts.library.model.sfrd.ServiceDataRecords
import tech.ecom.egts.library.model.sfrd.record.RecordData
import tech.ecom.egts.library.model.sfrd.record.ServiceDataRecord
import tech.ecom.egts.library.model.sfrd.record.ServiceType
import tech.ecom.egts.library.model.sfrd.record.subrecord.SubRecord

class CounterDataEncoderTest : AbstractIntegrationTest() {
    @Autowired
    private lateinit var counterDataEncoder: CounterDataEncoder

    @Test
    fun `WHEN packet with CounterData subrecord encoded and decoded counter value preserved`() {
        // given
        val counterNumber = Random.nextInt(from = 0, until = 255).toByte()
        val counterValue = Random.nextInt(from = 0, until = 16777215)

        val packet = EgtsPacket(
            packetIdentifier = 1U,
            packetType = PacketType.APP_DATA,
            servicesFrameData = AppServicesFrameData(
                serviceDataRecords = ServiceDataRecords(
                    sdrList = listOf(
                        ServiceDataRecord(
                            recordNumber = 1U,
                            sourceServiceType = ServiceType.TELE_DATA_SERVICE,
                            recipientServiceType = ServiceType.TELE_DATA_SERVICE,
                            recordData = RecordData(
                                subRecordList = listOf(
                                    SubRecord(
                                        subRecordTypeId = counterDataEncoder.subRecordTypeId,
                                        subRecordData = CounterSubRecordData(
                                            counterNumber = counterNumber,
                                            counterValue = counterValue,
                                        )
                                    )
                                )
                            ),
                        )
                    )
                )
            )
        )

        // when
        val encodedDecodedPacket = egtsPacketEncoder.decode(
            egtsPacketEncoder.encode(packet),
        )

        // then
        with(
            (encodedDecodedPacket.servicesFrameData as AppServicesFrameData)
                .serviceDataRecords
                .sdrList.first()
                .recordData
                .subRecordList.first()
        ) {
            assertThat(subRecordTypeId).isEqualTo(counterDataEncoder.subRecordTypeId)
            val counterData = subRecordData as CounterSubRecordData
            assertThat(counterData.counterNumber).isEqualTo(counterNumber)
            assertThat(counterData.counterValue).isEqualTo(counterValue)
        }
    }

}