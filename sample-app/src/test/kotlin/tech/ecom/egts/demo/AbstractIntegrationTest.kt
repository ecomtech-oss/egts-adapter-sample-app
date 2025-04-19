package tech.ecom.egts.demo

import io.mockk.mockk
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationContext
import org.springframework.messaging.MessageChannel
import tech.ecom.egts.demo.configuration.ReceivedPacketsStorage
import tech.ecom.egts.library.encoder.EgtsPacketEncoder

@SpringBootTest(
    classes = [EgtsAdapterSampleApp::class],
)
abstract class AbstractIntegrationTest {
    @Autowired
    lateinit var context: ApplicationContext

    @Autowired
    protected lateinit var egtsPacketEncoder: EgtsPacketEncoder

    @Autowired
    protected lateinit var receivingChannel: MessageChannel

    @Autowired
    protected lateinit var receivedPacketsStorage: ReceivedPacketsStorage

    protected val sendingChannelMockk = mockk<MessageChannel>()

}

data class ExceptionTestCase(
    val exception: Exception,
)