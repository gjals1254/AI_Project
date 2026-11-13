package com.example.stress.exception;

import com.example.stress.exception.AppExceptions.AiServerException;
import com.example.stress.exception.AppExceptions.NotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;

@ControllerAdvice(annotations = org.springframework.stereotype.Controller.class)
public class GlobalExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    public ModelAndView notFound(NotFoundException e) {
        return error(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(AiServerException.class)
    public ModelAndView aiServer(AiServerException e) {
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
