package tech.ecom.egts.demo.service

import io.mockk.every
import io.mockk.verify
import java.util.stream.Stream
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.integration.test.context.SpringIntegrationTest
import org.springframework.messaging.support.GenericMessage
import tech.ecom.egts.demo.AbstractIntegrationTest
import tech.ecom.egts.demo.ExceptionTestCase
import tech.ecom.egts.demo.model.CourierTrackRecord
import tech.ecom.egts.demo.model.random
import tech.ecom.egts.library.exception.EgtsAdapterException
import tech.ecom.egts.library.exception.EgtsExceptionErrorCode
import tech.ecom.egts.library.model.sfrd.TransportLayerProcessingResult
import tech.ecom.egts.library.utils.hexStringToByteArray
import com.ninjasquad.springmockk.MockkBean

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringIntegrationTest(noAutoStartup = ["tcpInboundAdapter"])
class TrackRecordsSenderTest : AbstractIntegrationTest() {

    private lateinit var trackRecordsSender: TrackRecordsSender
    private lateinit var packetFactory: PacketFactory

    @MockkBean
    private lateinit var egtsServerConnectionManagerMockk: EgtsServerConnectionManager

    companion object {
        @JvmStatic
        fun exceptionThrownTestCaseProvider(): Stream<ExceptionTestCase> {
            return Stream.of(
                ExceptionTestCase(
                    Exception(),
                ),
                ExceptionTestCase(
                    EgtsAdapterException(EgtsExceptionErrorCode.entries.toTypedArray().random()),
                ),
            )
        }
    }

    @BeforeEach
    fun initService() {
        // just to start packets numeration from 0 for each test
        packetFactory = PacketFactory(
            egtsPacketEncoder,
        )

        trackRecordsSender = TrackRecordsSender(
            packetFactory = packetFactory,
            egtsServerConnectionManager = egtsServerConnectionManagerMockk,
        )

        receivedPacketsStorage.receivedPackets.clear()
    }

    @Test
    fun `WHEN sending track records AND connection is not established THEN openConnectionOrGetError() called`() {
        // given
        every { egtsServerConnectionManagerMockk.isConnectionClosed } returns true
        every { egtsServerConnectionManagerMockk.openConnectionOrGetError() } returns null
        every { egtsServerConnectionManagerMockk.sendPacketOrGetTextError(any()) } returns null
        every { egtsServerConnectionManagerMockk.getResponsePacket(any()) } returns
            assembleTransportLayerResponsePacket(
                responsePacketId = 2u,
            )

        // when
        trackRecordsSender.sendTrackRecords(listOf(CourierTrackRecord.random()))

        // then
        verify(exactly = 1) { egtsServerConnectionManagerMockk.openConnectionOrGetError() }
    }

    @Test
    fun `WHEN sending track records AND connection is established THEN openConnectionOrGetError() NOT called`() {
        // given
        every { egtsServerConnectionManagerMockk.isConnectionClosed } returns false
        every { egtsServerConnectionManagerMockk.sendPacketOrGetTextError(any()) } returns null
        every { egtsServerConnectionManagerMockk.getResponsePacket(any()) } returns
            assembleTransportLayerResponsePacket(
                responsePacketId = 2u,
            )

        // when
        trackRecordsSender.sendTrackRecords(listOf(CourierTrackRecord.random()))

        // then
        verify(exactly = 0) { egtsServerConnectionManagerMockk.openConnectionOrGetError() }
    }

    @Test
    fun `WHEN rnisConnectionService fails to authorize connection THEN error returned`() {
        // given
        every { egtsServerConnectionManagerMockk.isConnectionClosed } returns true
        every { egtsServerConnectionManagerMockk.openConnectionOrGetError() } returns "error"
        // when
        val response = trackRecordsSender.sendTrackRecords(listOf(CourierTrackRecord.random()))

        // then
        assertThat(response).isNotEqualTo(trackRecordsSender.SUCCESS_RESPONSE)
    }

    @ParameterizedTest
    @MethodSource("exceptionThrownTestCaseProvider")
    fun `WHEN rnisConnectionService catches an exception while sending packet THEN error returned`(
        testCase: ExceptionTestCase,
    ) {
        // given
        val egtsServerConnectionManager = EgtsServerConnectionManager(
            sendingChannel = sendingChannelMockk,
            packetFactory = packetFactory,
            receivedPacketsStorage = receivedPacketsStorage,
        )
        val trackRecordsSender = TrackRecordsSender(
            packetFactory = packetFactory,
            egtsServerConnectionManager = egtsServerConnectionManager,
        )

        every { sendingChannelMockk.send(any()) } throws testCase.exception

        // when
        val response = trackRecordsSender.sendTrackRecords(listOf(CourierTrackRecord.random()))

        // then
        assertThat(response).isNotEqualTo(trackRecordsSender.SUCCESS_RESPONSE)
    }

    @Test
    fun `WHEN rnisConnectionService returns an error while sending packet THEN error returned`() {
        // given
        every { egtsServerConnectionManagerMockk.isConnectionClosed } returns false
        every { egtsServerConnectionManagerMockk.sendPacketOrGetTextError(any()) } returns "error message"

        // when
        val response = trackRecordsSender.sendTrackRecords(listOf(CourierTrackRecord.random()))

        // then
        assertThat(response).isNotEqualTo(trackRecordsSender.SUCCESS_RESPONSE)
    }

    @Test
    fun `WHEN rnis answers WITH transport layer success THEN success returned`() {
        // given
        every { egtsServerConnectionManagerMockk.isConnectionClosed } returns false
        every { egtsServerConnectionManagerMockk.sendPacketOrGetTextError(any()) } returns null
        every { egtsServerConnectionManagerMockk.getResponsePacket(any()) } returns
            assembleTransportLayerResponsePacket(
                responsePacketId = 2u,
            )

        receivingChannel.send(
            GenericMessage(
                setOf(
                    assembleTransportLayerResponsePacket(
                        packetIdentifier = packetFactory.FIRST_PACKET_ID,
                        responsePacketId = packetFactory.FIRST_PACKET_ID,
                        processingResult = TransportLayerProcessingResult.EGTS_PC_OK,
                    ),
                ),
            ),
        )

        // when
        val response = trackRecordsSender.sendTrackRecords(listOf(CourierTrackRecord.random()))

        // then
        assertThat(response).isEqualTo(trackRecordsSender.SUCCESS_RESPONSE)
    }

    @Test
    fun `WHEN rnis answers WITH transport layer error THEN error returned from service`() {
        // given
        packetFactory.assembleAuthPacket()
        packetFactory
            .assembleSingleRecordResponsePacket(
                egtsPacketEncoder.decode(
                    EgtsPacketsHelper.AUTH_RESULT_PACKET.hexStringToByteArray(),
                ),
            )

        every { egtsServerConnectionManagerMockk.isConnectionClosed } returns false
        every { egtsServerConnectionManagerMockk.sendPacketOrGetTextError(any()) } returns null
        every { egtsServerConnectionManagerMockk.getResponsePacket(any()) } returns
            assembleTransportLayerResponsePacket(
                responsePacketId = 2u,
                processingResult = TransportLayerProcessingResult.EGTS_PC_DATACRC_ERROR,
            )

        receivingChannel.send(
            GenericMessage(
                setOf(
                    assembleTransportLayerResponsePacket(
                        packetIdentifier = packetFactory.FIRST_PACKET_ID,
                        responsePacketId = packetFactory.FIRST_PACKET_ID.plus(2u).toUShort(),
                        processingResult = TransportLayerProcessingResult.EGTS_PC_DATACRC_ERROR,
                    ),
                ),
            ),
        )

        val response = trackRecordsSender.sendTrackRecords(listOf(CourierTrackRecord.random()))

        // then
        assertThat(response).isNotEqualTo(trackRecordsSender.SUCCESS_RESPONSE)
    }

    @Test
    fun `WHEN rnis does not respond THEN timeout error returned from service`() {
        // given
        every { egtsServerConnectionManagerMockk.isConnectionClosed } returns false
        every { egtsServerConnectionManagerMockk.sendPacketOrGetTextError(any()) } returns null
        every { egtsServerConnectionManagerMockk.getResponsePacket(any()) } returns null

        // when
        val response = trackRecordsSender.sendTrackRecords(listOf(CourierTrackRecord.random()))

        // then
        assertThat(response).isNotEqualTo(trackRecordsSender.SUCCESS_RESPONSE)
    }
}