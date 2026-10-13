FROM eclipse-temurin:21-jdk
WORKDIR /app
COPY hospital_system/ ./
RUN javac *.java
EXPOSE 10000
CMD ["java", "WebServer"]
