FROM maven:3.6.0-jdk-17-slim as build

WORKDIR /app

COPY src /app/src

COPY pom.xml /app

RUN mvn -f /app/pom.xml clean package -Dspring.profiles.active=prod

FROM apline:3.13

ENV MYSOL_HOST  110.40.230.65
ENV MYSOL_USER_NAME root
ENV MYSOL_PASSWORD 123456
ENV DATABASE_NAME jilijili-music

RUN  apk add --update --on-cache openjdk18-jre-base \
     && rm -f /var/cache/apk/*

WORKDIR /app