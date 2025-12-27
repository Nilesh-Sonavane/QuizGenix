package com.quizgenix.exception;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@ControllerAdvice
public class GlobalExceptionHandler {

    // 1. Catch the "File Too Large" Error
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public String handleMaxSizeException(MaxUploadSizeExceededException exc,
            RedirectAttributes redirectAttributes) {

        // 2. Add a custom error message
        redirectAttributes.addAttribute("error", "file_too_large");

        // 3. Redirect back to the settings page
        return "redirect:/admin/settings";
    }
}