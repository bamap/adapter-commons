package ir.bamap.blu.adapter.config.model

import org.springframework.http.HttpRequest
import org.springframework.http.HttpStatusCode

class JsonResponseModel(
    val jsonBody: Map<String, Any?>,
    request: HttpRequest,
    body: String = "",
    statusCode: HttpStatusCode,
) : ResponseModel(request, body, statusCode) {

    constructor(jsonBody: Map<String, Any?>, responseModel: ResponseModel) :
            this(jsonBody, responseModel.request, responseModel.body, responseModel.statusCode)
}