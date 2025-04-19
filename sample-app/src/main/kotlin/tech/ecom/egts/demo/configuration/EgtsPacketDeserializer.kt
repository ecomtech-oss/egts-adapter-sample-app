package tech.ecom.egts.demo.configuration

import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.serializer.Deserializer
import tech.ecom.egts.library.constants.PacketHeaderConstants.Companion.HEADER_LENGTH_FOR_HEADER_WITH_NO_OPTIONAL_FIELDS
import tech.ecom.egts.library.encoder.EgtsPacketEncoder
import tech.ecom.egts.library.model.EgtsPacket

class EgtsPacketDeserializer(
    private val packetEncoder: EgtsPacketEncoder,
) : Deserializer<Set<EgtsPacket>> {
    @Value("\${tcp.probe-interval}")
    private var probeInterval: Long = 100

    companion object {
        const val PRE_DATA_LENGTH_FIELD_BYTES_NUMBER_AT_HEADER = 5
        const val DATA_LENGTH_FIELD_BYTES_NUMBER = 2
        const val POST_DATA_LENGTH_FIELD_BYTES_NUMBER_AT_HEADER = 4
        const val CHECKSUM_BYTES_FIELD_BYTES_NUMBER = 2
    }

    override fun deserialize(inputStream: InputStream): Set<EgtsPacket> {
        val output = mutableSetOf<EgtsPacket>()
        Thread.sleep(probeInterval)
        while (inputStream.available() >= HEADER_LENGTH_FOR_HEADER_WITH_NO_OPTIONAL_FIELDS) {
            output.add(inputStream.getPacket())
        }
        return output
    }

    private fun InputStream.getPacket(): EgtsPacket {
        val preDataLengthBytes = readNBytes(PRE_DATA_LENGTH_FIELD_BYTES_NUMBER_AT_HEADER)
        val dataLengthBytes = readNBytes(DATA_LENGTH_FIELD_BYTES_NUMBER)

        val restOfPacketBytesNumber = POST_DATA_LENGTH_FIELD_BYTES_NUMBER_AT_HEADER +
            dataLengthBytes.getUShortAsInt() + CHECKSUM_BYTES_FIELD_BYTES_NUMBER
        while (available() < restOfPacketBytesNumber) {
            Thread.sleep(probeInterval)
        }

        val restOfPacketBytes = readNBytes(restOfPacketBytesNumber)

        return packetEncoder.decode(preDataLengthBytes + dataLengthBytes + restOfPacketBytes)
    }

    private fun ByteArray.getUShortAsInt(): Int =
        ByteBuffer.wrap(this).order(ByteOrder.LITTLE_ENDIAN).getShort().toUShort().toInt()
}