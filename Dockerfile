FROM eclipse-temurin:21-jdk-alpine-3.24 AS build
RUN apk add --no-cache binutils
WORKDIR /workspace

COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN --mount=type=cache,target=/root/.m2 sh mvnw -B -q dependency:go-offline

COPY src/main src/main
RUN --mount=type=cache,target=/root/.m2 sh mvnw -B -q package -DskipTests \
 && cp target/foody-api-*.jar app.jar \
 && java -Djarmode=tools -jar app.jar extract --layers --destination extracted

RUN jdeps --ignore-missing-deps --multi-release 21 --print-module-deps -q --recursive \
      --class-path 'extracted/dependencies/lib/*' extracted/application/app.jar > modules.txt \
 && jlink \
      --add-modules "$(cat modules.txt),java.instrument,java.management,java.naming,java.security.jgss,jdk.crypto.ec,jdk.management,jdk.unsupported,jdk.localedata" \
      --include-locales=en,pt-BR \
      --strip-debug --no-man-pages --no-header-files --compress=zip-9 \
      --output /javaruntime \
 && find /javaruntime -name '*.so' -exec strip -p --strip-unneeded {} +

FROM alpine:3.24
RUN addgroup -S foody && adduser -S -G foody foody
ENV JAVA_HOME=/opt/java
ENV PATH="${JAVA_HOME}/bin:${PATH}"
COPY --from=build /javaruntime ${JAVA_HOME}

WORKDIR /app
COPY --from=build /workspace/extracted/dependencies/ ./
COPY --from=build /workspace/extracted/spring-boot-loader/ ./
COPY --from=build /workspace/extracted/snapshot-dependencies/ ./
COPY --from=build /workspace/extracted/application/ ./

USER foody
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=50", "-XX:+ExitOnOutOfMemoryError", "-jar", "app.jar"]
