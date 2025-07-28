#!/bin/bash

# Test script to reproduce the startup error
echo "Testing application startup..."

# Set required environment variables to dummy values for testing
export OPENAI_API_KEY="test-key"
export GOOGLE_CUSTOM_API_KEY="test-google-key"
export GOOGLE_CUSTOM_CSI="test-csi"

# Try to start the application
./gradlew bootRun --args="--spring.main.web-application-type=none --spring.main.lazy-initialization=true" --quiet