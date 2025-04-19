package tech.ecom.egts.demo.util

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule

object JsonUtil {
    private val objectMapper = ObjectMapper().registerModule(KotlinModule.Builder().build())

    fun toPrettyJson(obj: Any): String {
        return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(obj)
    }
}