FROM openjdk:8u151-jdk-alpine
ADD docker-daemon.json docker-daemon.json
ADD bootsy-operator.jar app.jar
ENTRYPOINT ["java","-Xmx128M","-jar","/app.jar"]
