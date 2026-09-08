package com.airline.notificationservice.service;

import org.springframework.stereotype.Service;
import org.thymeleaf.ITemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Map;

@Service
public class EmailTemplateEngine {

    private final ITemplateEngine templateEngine;

    public EmailTemplateEngine(ITemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
    }

    public String render(String templateName, Map<String, Object> variables) {
        Context context = new Context();
        if (variables != null) {
            context.setVariables(variables);
        }
        return templateEngine.process(templateName, context);
    }
}
