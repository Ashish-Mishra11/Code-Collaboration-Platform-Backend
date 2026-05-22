#!/bin/bash
# Pull all required Docker images for the execution service
echo "Pulling Docker images for all supported languages..."

docker pull python:3.12-slim
docker pull node:20-slim
docker pull eclipse-temurin:21-jdk-alpine
docker pull gcc:13
docker pull golang:1.22-alpine
docker pull rust:1.77-slim

echo ""
echo "All images pulled successfully!"
docker images | grep -E "python|node|eclipse-temurin|gcc|golang|rust"
