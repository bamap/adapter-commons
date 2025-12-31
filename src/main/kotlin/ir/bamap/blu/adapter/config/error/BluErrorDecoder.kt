package ir.bamap.blu.adapter.config.error

import ir.bamap.blu.adapter.config.error.handler.ByStatusErrorDecoderStrategy
import ir.bamap.blu.adapter.config.error.handler.ErrorDecoderStrategy
import ir.bamap.blu.adapter.config.model.JsonResponseModel
import ir.bamap.blu.adapter.config.model.ResponseModel
import ir.bamap.blu.exception.BluException
import ir.bamap.blu.exception.ExternalServiceException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import tools.jackson.core.JacksonException
import tools.jackson.core.type.TypeReference
import tools.jackson.databind.ObjectMapper

open class BluErrorDecoder(
    protected val objectMapper: ObjectMapper
) {
    protected val logger: Logger = LoggerFactory.getLogger(javaClass)
    protected val strategies: MutableList<ErrorDecoderStrategy> = mutableListOf()

    init {
        initStrategies()
    }

    fun decode(response: ResponseModel): BluException {
        val jsonBody = convertBodyToMap(response.body)
        val jsonResponseModel = JsonResponseModel(jsonBody, response)

        logError(jsonResponseModel)

        for (strategy in strategies) {
            strategy.getExceptionOrNull(jsonResponseModel)
                ?.let { return it }
        }

        return ExternalServiceException(response.statusCode.value(), jsonBody)
    }

    protected open fun logError(response: JsonResponseModel) {
        val responseLog = mapOf("body" to response.jsonBody, "status" to response.statusCode.value())
        val requestLog = mapOf("url" to response.request.uri.toString(), "method" to response.request.method.toString())

        logger.atError()
            .setMessage("Error in External Service")
            .addKeyValue("response", responseLog)
            .addKeyValue("request", requestLog)
            .log()


    }

    protected fun convertBodyToMap(response: String): Map<String, Any?> {
        try {
            return objectMapper.readValue(response, object : TypeReference<Map<String, Any?>>() {})
        } catch (e: JacksonException) {
            val mapArguments = mapOf("response" to response)
            logger.atError()
                .setMessage("Error convert response to json")
                .addKeyValue("response", mapArguments)
                .log()
            return mapArguments
        }
    }

    protected fun initStrategies() {
        this.strategies.add(ByStatusErrorDecoderStrategy())
    }
}