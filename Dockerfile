FROM gradle:6.7.1 as builder

COPY --chown=gradle:gradle . /home/gradle/src
WORKDIR /home/gradle/src
RUN gradle build --no-daemon

FROM openjdk:8-jre-slim

RUN mkdir /app

RUN apt-get update && apt-get install -y libfreetype6 fontconfig

COPY --from=builder /home/gradle/src/build/libs/ /app/

ENTRYPOINT ["java","-jar","/app/app.jar"]
