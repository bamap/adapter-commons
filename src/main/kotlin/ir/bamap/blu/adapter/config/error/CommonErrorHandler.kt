package ir.bamap.blu.adapter.config.error

import ir.bamap.blu.adapter.config.model.ResponseModel
import ir.bamap.blu.exception.ServiceUnavailableException
import org.springframework.web.reactive.function.client.ClientRequest
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.ExchangeFilterFunction
import org.springframework.web.reactive.function.client.ExchangeFunction
import org.springframework.web.reactive.function.client.WebClientRequestException
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono
import java.util.function.Function
import java.util.function.Predicate

class CommonErrorHandler {

    fun errorHandler(bluErrorDecoder: BluErrorDecoder): ExchangeFilterFunction {
        return ExchangeFilterFunction.ofResponseProcessor(Function { response: ClientResponse ->
            if (response.statusCode().isError) {
                return@Function response.bodyToMono<String>()
                    .flatMap({ body ->
                        val request = response.request()
                        val responseModel = ResponseModel(request, body, response.statusCode())
                        Mono.error(bluErrorDecoder.decode(responseModel))
                    })
            }
            Mono.just(response)
        })
    }

    fun connectionErrorHandler(serviceName: String): ExchangeFilterFunction {
        return ExchangeFilterFunction { request: ClientRequest, next: ExchangeFunction ->
            next.exchange(request)
                .onErrorMap(
                    Predicate { ex: Throwable? -> ex is WebClientRequestException },
                    Function { ex: Throwable? -> ServiceUnavailableException(serviceName) }
                )
        }
    }
}