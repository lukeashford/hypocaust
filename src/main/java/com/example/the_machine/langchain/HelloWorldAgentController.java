package com.example.the_machine.langchain;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for the Hello World LangChain agent.
 * Provides endpoints to interact with the agent.
 */
@RestController
@RequestMapping("/api/langchain")
public class HelloWorldAgentController {

    private final HelloWorldAgentService helloWorldAgentService;

    public HelloWorldAgentController(HelloWorldAgentService helloWorldAgentService) {
        this.helloWorldAgentService = helloWorldAgentService;
    }

    /**
     * Endpoint to get a greeting from the LangChain agent.
     *
     * @param name The name to greet (optional, defaults to "World")
     * @return A greeting message from the agent
     */
    @GetMapping("/greeting")
    public String getGreeting(@RequestParam(defaultValue = "World") String name) {
        return helloWorldAgentService.getGreeting(name);
    }
}