package tech.ecom.egts.demo.service

import tech.ecom.egts.library.model.sfrd.TransportLayerProcessingResult
import tech.ecom.egts.library.model.EgtsPacket
import tech.ecom.egts.library.model.PacketType
import tech.ecom.egts.library.model.sfrd.ResponseServicesFrameData

class EgtsPacketsHelper {
    companion object {
        const val AUTH_RESULT_PACKET = "x01x00x03x0bx00x0bx00x01x00x01xc2x04x00x01x00x00x01x01x09x01x00x00xddx45"
    }
}

fun assembleTransportLayerResponsePacket(
    packetIdentifier: UShort = 0u,
    responsePacketId: UShort,
    processingResult: TransportLayerProcessingResult = TransportLayerProcessingResult.EGTS_PC_OK,
) = EgtsPacket(
    packetIdentifier = packetIdentifier,
    packetType = PacketType.RESPONSE,
    servicesFrameData = ResponseServicesFrameData(
        responsePacketId = responsePacketId,
        processingResult = processingResult,
    ),
)