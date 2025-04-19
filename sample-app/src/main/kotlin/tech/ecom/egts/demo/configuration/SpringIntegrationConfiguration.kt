package tech.ecom.egts.demo.configuration

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.integration.channel.DirectChannel
import org.springframework.integration.config.EnableIntegration
import org.springframework.integration.dsl.IntegrationFlow
import org.springframework.integration.ip.tcp.TcpReceivingChannelAdapter
import org.springframework.integration.ip.tcp.TcpSendingMessageHandler
import org.springframework.integration.ip.tcp.connection.TcpNetClientConnectionFactory
import org.springframework.messaging.MessageChannel
import tech.ecom.egts.library.encoder.EgtsPacketEncoder
import tech.ecom.egts.library.model.EgtsPacket
import tech.ecom.egts.library.model.PacketType
import tech.ecom.egts.library.model.sfrd.ResponseServicesFrameData

@Configuration
@EnableIntegration
open class SpringIntegrationConfiguration(
    private val packetEncoder: EgtsPacketEncoder,
    private val receivedPacketsStorage: ReceivedPacketsStorage,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Value("\${tcp.server-address}")
    private var serverAddress = "localhost"
    @Value("\${tcp.server-port}")
    private var serverPort = 8080

    @Bean
    open fun serializer(): EgtsPacketSerializer =
        EgtsPacketSerializer(packetEncoder)

    @Bean
    open fun deserializer(): EgtsPacketDeserializer =
        EgtsPacketDeserializer(packetEncoder)

    @Bean
    open fun sendingChannel(): MessageChannel = DirectChannel()

    @Bean
    open fun receivingChannel(): MessageChannel = DirectChannel()

    @Bean
    open fun connectionCloser(connectionFactory: TcpNetClientConnectionFactory): () -> Unit {
        return {
            connectionFactory.stop()
            logger.info(
                "TCP connection to {}:{} has been closed.",
                connectionFactory.host, connectionFactory.port,
            )
            connectionFactory.start()
            logger.info(
                "TCP connection to {}:{} is ready to be established.",
                connectionFactory.host, connectionFactory.port,
            )
        }
    }

    @Bean
    open fun connectionFactory(
        serializer: EgtsPacketSerializer,
        deserializer: EgtsPacketDeserializer,
    ): TcpNetClientConnectionFactory {
        val factory = TcpNetClientConnectionFactory(
            serverAddress,
            serverPort,
        )
        factory.serializer = serializer
        factory.deserializer = deserializer
        factory.isSingleUse = false // Keep the connection open
        return factory.also {
            logger.info("created connection factory for {}:{}", serverAddress, serverPort)
        }
    }

    @Bean
    open fun sendingMessageHandler(connectionFactory: TcpNetClientConnectionFactory): TcpSendingMessageHandler {
        val handler = TcpSendingMessageHandler()
        handler.setConnectionFactory(connectionFactory)
        return handler
    }

    @Bean
    open fun tcpInboundAdapter(
        connectionFactory: TcpNetClientConnectionFactory,
        @Qualifier("receivingChannel")
        receivingChannel: MessageChannel,
    ): TcpReceivingChannelAdapter {
        val adapter = TcpReceivingChannelAdapter()
        adapter.setConnectionFactory(connectionFactory)
        adapter.outputChannel = receivingChannel
        return adapter
    }

    @Bean
    open fun outgoingFlow(
        connectionFactory: TcpNetClientConnectionFactory,
        @Qualifier("sendingChannel")
        sendingChannel: MessageChannel,
        sendingMessageHandler: TcpSendingMessageHandler,
    ): IntegrationFlow {
        return IntegrationFlow.from(sendingChannel)
            .handle(sendingMessageHandler)
            .get()
    }

    @Bean
    open fun receivingFlow(
        @Qualifier("receivingChannel")
        receivingChannel: MessageChannel,
    ): IntegrationFlow {
        return IntegrationFlow.from(receivingChannel)
            .handle { message ->
                @Suppress("UNCHECKED_CAST")
                val packetsMap = (message.payload as Set<EgtsPacket>)
                    .associateBy {
                        if (it.packetType == PacketType.APP_DATA) {
                            it.packetIdentifier
                        } else {
                            (it.servicesFrameData as ResponseServicesFrameData).responsePacketId
                        }
                    }
                receivedPacketsStorage.receivedPackets.putAll(packetsMap)
            }
            .get()
    }
}