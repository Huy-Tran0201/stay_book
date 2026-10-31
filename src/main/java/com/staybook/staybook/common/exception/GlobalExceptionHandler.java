package com.staybook.staybook.common.exception;

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.NoHandlerFoundException;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public String handleBusinessException(BusinessException exception, Model model) {
        model.addAttribute("message", exception.getMessage());
        return "error/500";
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public String handleNotFound() {
        return "error/404";
    }

    @ExceptionHandler(Exception.class)
    public String handleUnexpectedException(Model model) {
        model.addAttribute("message", "Đã có lỗi xảy ra. Vui lòng thử lại sau.");
        return "error/500";
    }
}
