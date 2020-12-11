FROM openjdk:11-jre

COPY target/stresty.jar /stresty.jar

COPY config.edn config.edn

COPY examples examples

CMD java -XX:-OmitStackTraceInFastThrow -jar /stresty.jar --ui 
