package tech.ecom.egts.demo.service

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.messaging.MessageChannel
import org.springframework.messaging.support.GenericMessage
import org.springframework.stereotype.Service
import tech.ecom.egts.demo.configuration.ReceivedPacketsStorage
import tech.ecom.egts.demo.service.PacketFactory.Companion.AUTH_RESULT_PACKET_NUMBER
import tech.ecom.egts.demo.util.JsonUtil
import tech.ecom.egts.library.exception.EgtsAdapterException
import tech.ecom.egts.library.model.EgtsPacket

@Service
class EgtsServerConnectionManager(
    @Qualifier("sendingChannel")
    private val sendingChannel: MessageChannel,
    private val packetFactory: PacketFactory,
    private val receivedPacketsStorage: ReceivedPacketsStorage,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    var isConnectionClosed = true

    fun setIsConnectionClosedFlagToTrue() {
        isConnectionClosed = true.also {
            logger.info("set isConnectionClosed flag to true")
        }
    }

    fun openConnectionOrGetError(): String? {
        return try {
            logger.trace("connection is actually closed")
            if (!authorizeConnection()) {
                return "failed to authorize at EGTS server"
            }
            null
        } catch (egtsAdapterException: EgtsAdapterException) {
            logger.warn(
                "caught EgtsAdapterException with message {} while trying to establish connection",
                egtsAdapterException.message,
                egtsAdapterException,
            )
            "caught EgtsAdapterException with message ${egtsAdapterException.message}"
        } catch (e: Exception) {
            logger.warn(
                "caught {} exception with message {} while trying to establish connection",
                e.javaClass.simpleName,
                e.message,
                e,
            )
            "failed to sent packet trough TCP connection"
        }
    }

    private fun authorizeConnection(): Boolean {
        logger.debug("authorizeConnection()")

        if (!sendingChannel.send(GenericMessage(packetFactory.assembleAuthPacket()))) {
            logger.warn("failed to send auth packet")
            return false
        }

        getResponsePacket(AUTH_RESULT_PACKET_NUMBER)?.let { authResult ->
            logger.trace("got auth result packet {} and assembled response", authResult)
            sendingChannel.send(GenericMessage(packetFactory.assembleSingleRecordResponsePacket(authResult)))
            isConnectionClosed = false
            return true
        } ?: run {
            logger.trace("failed to get auth result paket")
            return false
        }
    }

    fun sendPacketOrGetTextError(packet: EgtsPacket): String? {
        return try {
            sendingChannel.send(GenericMessage(packet))
            null
        } catch (egtsAdapterException: EgtsAdapterException) {
            logger.warn(
                "caught EgtsAdapterException with message {} while trying to establish connection",
                egtsAdapterException.message,
                egtsAdapterException,
            )
            "caught EgtsAdapterException with message ${egtsAdapterException.message}"
        } catch (e: Exception) {
            logger.warn("caught {} exception with message {} while sending packet", e.javaClass.simpleName, e.message, e)
            "failed to send packet trough TCP connection"
        }
    }

    fun getResponsePacket(sentPacketIdentifier: UShort): EgtsPacket? {
        Thread.sleep(1000L)
        return receivedPacketsStorage.receivedPackets[sentPacketIdentifier]?.let {
            receivedPacketsStorage.receivedPackets.remove(sentPacketIdentifier)
            return it.also {
                logger.info("received packet: \n{}", JsonUtil.toPrettyJson(it))
            }
        } ?: run {
            logger.warn("failed to get response to packet {}", sentPacketIdentifier, )
            return null
        }
    }
}