package tech.ecom.egts.demo.configuration

import org.springframework.stereotype.Service
import tech.ecom.egts.library.model.EgtsPacket

@Service
class ReceivedPacketsStorage {
    val receivedPackets = mutableMapOf<UShort, EgtsPacket>()
}