package ir.bamap.blu.adapter.config.error.handler

import ir.bamap.blu.adapter.config.model.JsonResponseModel
import ir.bamap.blu.exception.AccessDeniedException
import ir.bamap.blu.exception.BluException
import org.springframework.http.HttpStatus

class ByStatusErrorDecoderStrategy : ErrorDecoderStrategy {

    override fun getExceptionOrNull(response: JsonResponseModel): BluException? {
        if (response.statusCode == HttpStatus.FORBIDDEN)
            return AccessDeniedException("", "")

        return null
    }
}