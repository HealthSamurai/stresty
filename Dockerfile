FROM openjdk:11-jre

ADD target/crm-1.0.0-standalone.jar /app.jar

CMD java -XX:-OmitStackTraceInFastThrow -jar /app.jar
