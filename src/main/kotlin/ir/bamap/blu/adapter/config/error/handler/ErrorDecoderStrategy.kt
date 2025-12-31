package ir.bamap.blu.adapter.config.error.handler

import ir.bamap.blu.adapter.config.model.JsonResponseModel
import ir.bamap.blu.exception.BluException

interface ErrorDecoderStrategy {
    fun getExceptionOrNull(response: JsonResponseModel): BluException?
}