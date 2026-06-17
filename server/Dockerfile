FROM ubuntu:22.04

LABEL description="BuildAI Ultra - Android App Generator Server"
LABEL version="1.0.0"

ENV DEBIAN_FRONTEND=noninteractive

# Install Java, Android SDK dependencies
RUN apt-get update && apt-get install -y \
    openjdk-17-jdk-headless \
    wget \
    unzip \
    git \
    file \
    && rm -rf /var/lib/apt/lists/*

# Install Android SDK
ENV ANDROID_HOME=/opt/android_sdk
ENV ANDROID_SDK_ROOT=/opt/android_sdk
ENV JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64

RUN mkdir -p $ANDROID_HOME && \
    cd $ANDROID_HOME && \
    wget -q https://github.com/anomalyco/opencode/releases/download/v0.1.0/android-sdk-34.tar.gz -O sdk.tar.gz && \
    tar -xzf sdk.tar.gz && \
    rm sdk.tar.gz

RUN mkdir -p $ANDROID_HOME/platforms && \
    mkdir -p $ANDROID_HOME/build-tools && \
    ls -la $ANDROID_HOME

# Set up working directory
WORKDIR /app

# Copy server source and templates
COPY BuildServer.java .
COPY templates/ ./templates/

# Compile server
RUN javac BuildServer.java

# Clean up build artifacts
RUN rm -f *.class

# Expose port
EXPOSE 8080

# Volumes for builds
VOLUME ["/app/builds"]

# Run server
CMD ["java", "BuildServer"]
