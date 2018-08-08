FROM openjdk:8-jre-slim

EXPOSE 443

VOLUME /data

RUN mkdir /ardent
RUN mkdir /translations

COPY Dockerfile ./translations/all.zip* /translations/
COPY ./config /ardent
COPY ./ardent.jks /ardent
COPY ./build/libs/Ardent-1.0-SNAPSHOT.jar /ardent

WORKDIR /ardent

ENTRYPOINT ["java", "-jar", "Ardent-1.0-SNAPSHOT.jar", "config", "ardent.jks"]