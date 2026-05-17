package ir.bamap.blu.adapter.config.error

import ir.bamap.blu.adapter.config.error.handler.ByStatusErrorDecoderStrategy
import ir.bamap.blu.adapter.config.error.handler.ErrorDecoderStrategy
import ir.bamap.blu.adapter.config.model.JsonResponseModel
import ir.bamap.blu.adapter.config.model.ResponseModel
import ir.bamap.blu.exception.BluException
import ir.bamap.blu.exception.ExternalServiceException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.web.reactive.function.client.ClientRequest
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.ExchangeFilterFunction
import org.springframework.web.reactive.function.client.ExchangeFunction
import org.springframework.web.reactive.function.client.WebClientRequestException
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono
import tools.jackson.core.JacksonException
import tools.jackson.core.type.TypeReference
import tools.jackson.databind.ObjectMapper
import java.util.function.Function
import java.util.function.Predicate
import javax.naming.ServiceUnavailableException

open class AdapterErrorHandler(
    protected val serviceName: String,
    protected val objectMapper: ObjectMapper
) {
    protected val logger: Logger = LoggerFactory.getLogger(javaClass)
    protected val strategies: MutableList<ErrorDecoderStrategy> = mutableListOf()

    init {
        initStrategies()
    }

    open fun handleResponseError(): ExchangeFilterFunction {
        return ExchangeFilterFunction.ofResponseProcessor(Function { response: ClientResponse ->
            if (response.statusCode().isError) {
                return@Function response.bodyToMono<String>()
                    .flatMap { body ->
                        val request = response.request()
                        val responseModel = ResponseModel(request, body, response.statusCode())
                        Mono.error(decode(responseModel))
                    }
            }
            Mono.just(response)
        })
    }

    open fun handleConnectionError(): ExchangeFilterFunction {
        return ExchangeFilterFunction { request: ClientRequest, next: ExchangeFunction ->
            next.exchange(request)
                .onErrorMap(
                    Predicate { ex: Throwable? -> ex is WebClientRequestException },
                    Function { ex: Throwable? -> ServiceUnavailableException(serviceName) }
                )
        }
    }

    protected open fun decode(response: ResponseModel): BluException {
        val jsonBody = convertBodyToMap(response.body)
        val jsonResponseModel = JsonResponseModel(jsonBody, response)

        logError(jsonResponseModel)

        for (strategy in strategies) {
            strategy.getExceptionOrNull(jsonResponseModel)
                ?.let { return it }
        }

        return ExternalServiceException(serviceName, response.statusCode.value(), jsonBody)
    }

    protected open fun logError(response: JsonResponseModel) {
        val responseLog = mapOf("body" to response.jsonBody, "status" to response.statusCode.value())
        val requestLog = mapOf("url" to response.request.uri.toString(), "method" to response.request.method.toString())

        logger.atError()
            .setMessage("Error in External Service")
            .addKeyValue("serviceName", serviceName)
            .addKeyValue("response", responseLog)
            .addKeyValue("request", requestLog)
            .log()
    }

    protected open fun convertBodyToMap(response: String): Map<String, Any?> {
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

    protected open fun initStrategies() {
        this.strategies.add(ByStatusErrorDecoderStrategy())
    }
}