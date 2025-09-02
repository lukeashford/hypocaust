package com.example.the_machine.models;

import lombok.RequiredArgsConstructor;
import lombok.val;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ModelRegistry {

    private final List<ModelProvider> providers;

    private Map<String, ChatModel> models;

    @PostConstruct
    void initializeModels() {
        models = createModelRegistry();
        log.info("Initialized {} models: {}", models.size(), models.keySet());
    }

    private Map<String, ChatModel> getModels() {
        return models;
    }

    private Map<String, ChatModel> createModelRegistry() {
        return providers.stream()
                .flatMap(provider -> provider.createModels().entrySet().stream()
                        .map(entry -> Map.entry(
                                provider.getProviderName() + ":" + entry.getKey(),
                                entry.getValue()
                        )))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public ChatModel getModel(String providerAndModel) {
        if (providerAndModel == null || !providerAndModel.contains(":")) {
            throw new IllegalArgumentException(
                "Invalid model format. Expected 'provider:model', got '" + providerAndModel + "'"
            );
        }
        
        val model = getModels().get(providerAndModel);
        if (model == null) {
            throw new ModelNotFoundException(
                "Model not found: " + providerAndModel + ". Available models: " + listAvailableModels()
            );
        }
        return model;
    }

    public ChatModel getModel(String provider, String model) {
        if (provider == null || provider.trim().isEmpty()) {
            throw new IllegalArgumentException("Provider cannot be null or empty");
        }
        if (model == null || model.trim().isEmpty()) {
            throw new IllegalArgumentException("Model cannot be null or empty");
        }
        return getModel(provider + ":" + model);
    }

    public Set<String> listAvailableModels() {
        return getModels().keySet();
    }
}