FROM openjdk:17
EXPOSE 8080
ADD target/chat-gpt-app-0.0.1-SNAPSHOT.jar chat-gpt-app.jar
ENTRYPOINT ["java","-jar","chat-gpt-app.jar"]