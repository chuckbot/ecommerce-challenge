FROM node:20-alpine AS frontend-builder
WORKDIR /app
COPY package.json package-lock.json shadow-cljs.edn ./
RUN npm install
COPY src/cljs src/cljs
COPY src/cljc src/cljc
COPY resources/public resources/public
RUN npx shadow-cljs release app

FROM clojure:tools-deps-alpine AS backend-builder
WORKDIR /app
COPY deps.edn build.clj ./
RUN clojure -P
COPY src/clj src/clj
COPY src/cljc src/cljc
COPY resources resources
COPY --from=frontend-builder /app/resources/public/js/compiled resources/public/js/compiled
RUN clojure -T:build uber

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=backend-builder /app/target/ecommerce-standalone.jar /app/app.jar
EXPOSE 8080
CMD ["java", "-jar", "app.jar"]