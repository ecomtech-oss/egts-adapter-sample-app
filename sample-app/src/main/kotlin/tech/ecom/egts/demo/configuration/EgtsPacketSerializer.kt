package tech.ecom.egts.demo.configuration

import java.io.OutputStream
import org.springframework.core.serializer.Serializer
import tech.ecom.egts.library.encoder.EgtsPacketEncoder
import tech.ecom.egts.library.model.EgtsPacket

class EgtsPacketSerializer(
    private val packetEncoder: EgtsPacketEncoder,
) : Serializer<EgtsPacket> {
    override fun serialize(packet: EgtsPacket, outputStream: OutputStream) =
        outputStream.run {
            write(packetEncoder.encode(packet))
        }
}