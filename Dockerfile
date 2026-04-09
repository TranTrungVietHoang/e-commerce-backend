# Stage 1: Build file .jar bằng Maven
FROM maven:3.9.5-eclipse-temurin-17 AS builder
WORKDIR /app
COPY pom.xml .
COPY src ./src
# Build bỏ qua test để tiết kiệm thời gian
RUN mvn clean package -DskipTests

# Stage 2: Đẩy file .jar vào môi trường chạy thực tế (Siêu nhẹ)
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
# Lấy file .jar ở Stage 1 sang Stage 2
COPY --from=builder /app/target/*.jar app.jar
# Mở cổng 8080
EXPOSE 8080
# Lệnh chạy ứng dụng với cấu hình múi giờ Việt Nam
ENTRYPOINT ["java", "-Duser.timezone=Asia/Ho_Chi_Minh", "-jar", "app.jar"]
