package com.quizgenix.controller;

import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;

@Controller
public class CustomErrorController implements ErrorController {

    @RequestMapping("/error")
    public String handleError(HttpServletRequest request, Model model) {
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);

        String title = "Something went wrong";
        String message = "An unexpected error occurred. Please try again later.";
        String errorCode = "???";
        String icon = "⚠️"; // Default icon

        if (status != null) {
            int statusCode = Integer.parseInt(status.toString());
            errorCode = String.valueOf(statusCode);

            switch (statusCode) {
                case 404:
                    title = "Page Not Found";
                    message = "Oops! The page you are looking for doesn't exist or has been moved.";
                    icon = "🔍";
                    break;
                case 500:
                    title = "Internal Server Error";
                    message = "Our servers are having a bad day. We are working to fix it!";
                    icon = "🔥";
                    break;
                case 403:
                    title = "Access Denied";
                    message = "You don't have permission to view this page. Restricted area!";
                    icon = "⛔";
                    break;
                case 400:
                    title = "Bad Request";
                    message = "The server couldn't understand your request.";
                    icon = "❓";
                    break;
                default:
                    title = "Unexpected Error";
                    message = "Something went wrong, but we're not sure what.";
                    icon = "😵";
                    break;
            }
        }

        model.addAttribute("errorCode", errorCode);
        model.addAttribute("errorTitle", title);
        model.addAttribute("errorMessage", message);
        model.addAttribute("errorIcon", icon);

        return "error"; // Points to templates/error.html
    }
}