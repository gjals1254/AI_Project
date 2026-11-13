package com.example.trademark.exception;

import com.example.trademark.exception.AppExceptions.AiServerException;
import com.example.trademark.exception.AppExceptions.DuplicateException;
import com.example.trademark.exception.AppExceptions.InvalidFileException;
import com.example.trademark.exception.AppExceptions.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.ModelAndView;

@ControllerAdvice(annotations = org.springframework.stereotype.Controller.class)
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(NotFoundException.class)
    public ModelAndView notFound(NotFoundException e) {
        return error(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler({InvalidFileException.class, MaxUploadSizeExceededException.class})
    public ModelAndView badFile(Exception e) {
        String msg = e instanceof MaxUploadSizeExceededException
            ? "파일 크기가 너무 큽니다. (최대 10MB)" : e.getMessage();
        return error(HttpStatus.BAD_REQUEST, msg);
    }

    @ExceptionHandler(DuplicateException.class)
    public ModelAndView duplicate(DuplicateException e) {
        return error(HttpStatus.CONFLICT, e.getMessage());
    }

    @ExceptionHandler(AiServerException.class)
    public ModelAndView aiServer(AiServerException e) {
        log.warn("AI 서버 오류: {}", e.getMessage());
        return error(HttpStatus.SERVICE_UNAVAILABLE, e.getMessage());
    }

    private ModelAndView error(HttpStatus status, String message) {
        ModelAndView mav = new ModelAndView("error");
        mav.setStatus(status);
        mav.addObject("status", status.value());
        mav.addObject("message", message);
        return mav;
    }
}
