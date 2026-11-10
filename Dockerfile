FROM node:20-alpine AS frontend-builder
RUN apk add --no-cache openjdk21-jre-headless
WORKDIR /app
COPY package.json package-lock.json shadow-cljs.edn ./
RUN npm install
COPY src/cljs src/cljs
COPY src/cljc src/cljc
COPY resources/public/index.html resources/public/index.html
COPY resources/public/css resources/public/css
RUN npx shadow-cljs release app

FROM clojure:tools-deps-alpine AS backend-builder
WORKDIR /app
COPY deps.edn build.clj ./
RUN clojure -P && clojure -P -T:build
COPY src/clj src/clj
COPY src/cljc src/cljc
COPY resources resources
COPY --from=frontend-builder /app/resources/public/js/compiled resources/public/js/compiled
RUN clojure -T:build uber

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=backend-builder /app/target/ecommerce-standalone.jar /app/app.jar
COPY ["Code Challenge E-Commerce.csv", "/app/Code Challenge E-Commerce.csv"]
EXPOSE 8080
CMD ["java", "-jar", "app.jar"]
