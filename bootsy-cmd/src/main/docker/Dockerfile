FROM openjdk:8u151-jdk-alpine
ADD bootsy.jar app.jar
ENTRYPOINT ["java","-Xmx128M","-jar","/app.jar"]
