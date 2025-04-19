package tech.ecom.egts.demo.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import tech.ecom.egts.demo.model.CourierTrackRecord
import tech.ecom.egts.demo.util.JsonUtil
import tech.ecom.egts.library.model.EgtsPacket
import tech.ecom.egts.library.model.sfrd.AppServicesFrameData
import tech.ecom.egts.library.model.sfrd.ResponseServicesFrameData
import tech.ecom.egts.library.model.sfrd.TransportLayerProcessingResult

@Service
class TrackRecordsSender(
    private val packetFactory: PacketFactory,
    private val egtsServerConnectionManager: EgtsServerConnectionManager,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    val SUCCESS_RESPONSE = "sent packet successfully"

    fun sendTrackRecord(courierTrackRecords: List<CourierTrackRecord>): String {
        logger.debug("sending ${courierTrackRecords.size} track records")
        logger.debug(courierTrackRecords.joinToString("\n") { JsonUtil.toPrettyJson(it) })

        if (egtsServerConnectionManager.isConnectionClosed) egtsServerConnectionManager.openConnectionOrGetError()?.let { error -> return error }

        packetFactory.assembleTelematicsPacket(courierTrackRecords).also { packet ->
            egtsServerConnectionManager.sendPacketOrGetTextError(packet)?.let { error -> return error }

            val responsePacket = egtsServerConnectionManager.getResponsePacket(packet.packetIdentifier)
                ?: return "got TCP server timeout while sending packet"

            return responsePacket.assembleResponse()
        }
    }

    private fun EgtsPacket.assembleResponse(): String {
        return if (extractTransportLayerResult(this)) {
            SUCCESS_RESPONSE
        } else {
            "EGTS server returned transport layer error"
        }
    }

    private fun extractTransportLayerResult(egtsPacket: EgtsPacket): Boolean {
        if (egtsPacket.servicesFrameData is AppServicesFrameData) {
            logger.warn("incoming packet with id {} appears not to be response packet - {}", egtsPacket.packetIdentifier, egtsPacket)
            return false
        }
        val responseServicesFrameData = egtsPacket.servicesFrameData as ResponseServicesFrameData

        return (responseServicesFrameData.processingResult == TransportLayerProcessingResult.EGTS_PC_OK).also {
            logger.debug(
                "packet sent {}, transport layer result is {}",
                if (it) "successfully" else "failed",
                responseServicesFrameData.processingResult,
            )
        }
    }
}