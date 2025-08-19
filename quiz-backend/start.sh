#!/bin/bash


echo "Starting Spring Boot Quiz Application..."
echo "OpenAI API Key: ${OPENAI_API_KEY:0:20}..."
echo "Model: gpt-4o-mini"
echo "Base URL: https://api.openai.com/v1"
echo ""

# Start the application
mvn spring-boot:run
