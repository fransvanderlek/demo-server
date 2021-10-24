FROM amd64/alpine
RUN apk add openjdk11-jre
COPY target/demo-server-1.0-SNAPSHOT.jar app.jar
COPY target/dependency /dependency
ENTRYPOINT ["java","-cp","/app.jar", "org.intelligentindustry.App"]
