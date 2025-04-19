package tech.ecom.egts.demo.service

import io.mockk.every
import io.mockk.slot
import io.mockk.verify
import java.util.stream.Stream
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.messaging.support.GenericMessage
import org.springframework.scheduling.annotation.EnableScheduling
import tech.ecom.egts.demo.AbstractIntegrationTest
import tech.ecom.egts.demo.ExceptionTestCase
import tech.ecom.egts.demo.service.EgtsPacketsHelper.Companion.AUTH_RESULT_PACKET
import tech.ecom.egts.library.exception.EgtsAdapterException
import tech.ecom.egts.library.exception.EgtsExceptionErrorCode
import tech.ecom.egts.library.model.EgtsPacket
import tech.ecom.egts.library.model.PacketType
import tech.ecom.egts.library.model.sfrd.ResponseServicesFrameData
import tech.ecom.egts.library.utils.hexStringToByteArray

@EnableScheduling
class EgtsServerConnectionManagerTest : AbstractIntegrationTest() {
    private lateinit var egtsServerConnectionManager: EgtsServerConnectionManager
    private lateinit var packetFactory: PacketFactory

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

        egtsServerConnectionManager = EgtsServerConnectionManager(
            sendingChannel = sendingChannelMockk,
            packetFactory = packetFactory,
            receivedPacketsStorage = receivedPacketsStorage,
        )

        receivedPacketsStorage.receivedPackets.clear()
    }

    @Test
    fun `WHEN service started and authorizeConnection not called THEN isConnectionClosed returns true`() {
        // given
        // nothing sent before

        // then
        assertThat(egtsServerConnectionManager.isConnectionClosed).isTrue()
    }

    @Test
    fun `WHEN authorizeConnection called AND rnis returns result THEN isConnectionClosed set to false and authResult confirmation sent`() {
        // given
        val captor = slot<GenericMessage<EgtsPacket>>()
        every { sendingChannelMockk.send(capture(captor)) } returns true
        val authResultPacket = getAuthResultPacket()
        receivingChannel.send(GenericMessage(setOf(authResultPacket)))

        // when
        val error = egtsServerConnectionManager.openConnectionOrGetError()

        // then
        assertThat(error).isNull()
        assertThat(egtsServerConnectionManager.isConnectionClosed).isFalse()

        verify(exactly = 2) { sendingChannelMockk.send(any()) }

        assertThat(captor.isCaptured).isTrue()
        val authResultConfirmationPacket = captor.captured.payload

        with(authResultConfirmationPacket) {
            assertThat(packetType).isEqualTo(PacketType.RESPONSE)
            assertThat(packetIdentifier).isEqualTo(packetFactory.FIRST_PACKET_ID.plus(1U).toUShort())

            val responsePacketData = servicesFrameData as ResponseServicesFrameData
            assertThat(responsePacketData.responsePacketId).isEqualTo(authResultPacket.packetIdentifier)
        }
    }

    private fun getAuthResultPacket() = egtsPacketEncoder.decode(AUTH_RESULT_PACKET.hexStringToByteArray())

    @Test
    fun `WHEN authorizeConnection called AND rnis does not respond THEN isConnectionClosed keeps true and error returned`() {
        // given
        every { sendingChannelMockk.send(any()) } returns true

        // when
        val error = egtsServerConnectionManager.openConnectionOrGetError()

        // then
        assertThat(error).isNotNull()
        assertThat(egtsServerConnectionManager.isConnectionClosed).isTrue()
    }

    @Test
    fun `WHEN message channel throws exception while authorising THEN isConnectionClosed keeps true and error returned`() {
        // given
        every { sendingChannelMockk.send(any()) } returns true

        // when
        val error = egtsServerConnectionManager.openConnectionOrGetError()

        // then
        assertThat(error).isNotNull()
        assertThat(egtsServerConnectionManager.isConnectionClosed).isTrue()
    }

    @Test
    fun `WHEN setIsConnectionClosed is called THEN isConnectionClosed became true`() {
        // given
        every { sendingChannelMockk.send(any()) } returns true
        receivingChannel.send(GenericMessage(setOf(getAuthResultPacket())))

        // when
        val error = egtsServerConnectionManager.openConnectionOrGetError()
        assertThat(error).isNull()
        assertThat(egtsServerConnectionManager.isConnectionClosed).isFalse()
        egtsServerConnectionManager.setIsConnectionClosedFlagToTrue()

        // then
        assertThat(egtsServerConnectionManager.isConnectionClosed).isTrue()
    }

    @Test
    fun `WHEN packet sent to message channel successfully THEN null returned instead of error`() {
        // given
        every { sendingChannelMockk.send(any()) } returns true

        // when
        val result = egtsServerConnectionManager.sendPacketOrGetTextError(packetFactory.assembleTelematicsPacket(listOf()))

        // then
        assertThat(result).isNull()
    }

    @ParameterizedTest
    @MethodSource("exceptionThrownTestCaseProvider")
    fun `WHEN exception caught while sending packet THEN error returned`(testCase: ExceptionTestCase) {
        // given
        every { sendingChannelMockk.send(any()) } throws testCase.exception
        val packet = packetFactory.assembleTelematicsPacket(listOf())

        // when
        val error = egtsServerConnectionManager.sendPacketOrGetTextError(packet)

        // then
        assertThat(error).isNotNull()
    }
}