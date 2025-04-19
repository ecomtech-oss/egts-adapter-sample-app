package tech.ecom.egts.demo.extension

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import tech.ecom.egts.demo.extension.encoder.sfrd.record.subrecord.CounterDataEncoder

@Configuration
open class ImplementedEncodersConfiguration {
    @Bean
    @ConditionalOnProperty("egts.initialize-encoders")
    open fun counterSubRecordDataEncoder() = CounterDataEncoder()
}