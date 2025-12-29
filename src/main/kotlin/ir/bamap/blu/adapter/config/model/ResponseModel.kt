package ir.bamap.blu.adapter.config.model

import org.springframework.http.HttpRequest
import org.springframework.http.HttpStatusCode

open class ResponseModel(
    val request: HttpRequest,
    val body: String = "",
    val statusCode: HttpStatusCode,
) {
}