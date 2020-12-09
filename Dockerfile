FROM openjdk:11-jre

COPY target/uberjar/stresty-1.0.0-standalone.jar /app.jar

COPY config.edn config.edn

CMD java -XX:-OmitStackTraceInFastThrow -jar /app.jar --ui
