# Project GRID

This is backend part of educational GRID project. Instruction describes how to clone and run the project on your local
machine.

## Getting Started

These instructions will help you get a copy of the project and running it on your local machine for development and
testing purposes.

## How to Run

This application requires
pre-installed [JDK 17](https://www.oracle.com/java/technologies/javase/jdk17-archive-downloads.html) or
higher. [See more.](https://www.oracle.com/java/technologies/downloads/#jdk19-windows)

* Clone this repository

```
git clone https://github.com/GRID-Game-Store/backend
```

* Make sure you are using JDK 17 and Maven
* You can build the project and run the tests by running ```mvn clean package```
  or
  ```mvn install / mvn clean install```

### Run Spring Boot app using Maven:

Now you can launch the server 8082.

* run Spring Boot app using Maven:
  ```mvn spring-boot:run```
* [optional] Run Spring Boot app with``` java -jar command
  java -jar target/backend-0.0.1-SNAPSHOT.jar```

### Run Spring Boot app using Docker:
To run Spring Boot app using Docker:
* Run the Docker Compose file inside backend folder:
  ```docker-compose -f docker-compose.yml up```

### Running Keycloak in docker

Keycloak is used for authentication and can be started using Docker Compose.
The provided docker-compose.yml file includes a Keycloak service and a PostgreSQL database for Keycloak.

To run Keycloak along with the backend:

* Run the Docker Compose file:
  ```docker-compose -f docker-compose.yml up```

This will start Keycloak on port 8084.
It is recommended to start Keycloak before the backend to ensure that the authentication service is available.

[Link to documentation](https://github.com/GRID-Game-Store/documentation/tree/main/backend)

## API Documentation

The API documentation is available via Swagger UI. After starting the application, you can access it at:
```http://localhost:8084/swagger```
This interface provides a comprehensive list of all available endpoints and allows you to test them directly from your browser.

## GRID Demo Video

Check out our demo video showcasing the main features of the GRID project:

### Main Page

https://github.com/user-attachments/assets/540fdc0d-b89e-4ab5-b09a-98554d3e0f59

### AI Chat Consultant

https://github.com/user-attachments/assets/48e87a2d-fe04-44a7-b9c8-6060a6840901

### Authentication, Authorization 

https://github.com/user-attachments/assets/759e81ef-abc9-462f-bc86-2146ac7ed2e6

### Payment Abilities

https://github.com/user-attachments/assets/8648e341-3ea2-4f1c-968f-13e65f6395f2


The demo includes:
- Overview of the main page
- AI-powered chat consultant using Vertext Gemini
- Registration and authorization process with Keycloak
- Payment integration with Stripe and PayPal
- Admin panel walkthrough

## Features

- **AI Chat Consultant**: Powered by Vertext Gemini, providing intelligent customer support.
- **Secure Authentication**: Implemented using Keycloak for robust user management.
- **Multiple Payment Options**: Integrated with Stripe and PayPal for flexible payment processing.
- **Admin Panel**: Comprehensive admin interface for easy management of the platform with a help of Astro.js and Spring Boot

## Built With

* [Maven](https://maven.apache.org/) - Dependency Management
* [Spring Boot](https://spring.io/projects/spring-boot) - Server Framework
* [JUnit](https://junit.org/junit5/) - Testing Framework
* [Keycloak](https://www.keycloak.org/) - Identity and Access Management
* [Swagger](https://swagger.io/) - API Documentation
* [Docker](https://www.docker.com/) - Containerization Platform

## Authors:

* [SEM24](https://github.com/SEM24)

[Link to backend documentation](https://github.com/GRID-Game-Store/documentation/tree/main/backend)

---

## 中文翻译

这是教育项目 GRID 的后端部分。本说明描述了如何在本地机器上克隆并运行该项目。//

## 快速开始

这些说明将帮助你获取项目副本，并在本地进行开发与测试。

## 如何运行

本应用需要预先安装 [JDK 17](https://www.oracle.com/java/technologies/javase/jdk17-archive-downloads.html) 或更高版本。
[更多信息](https://www.oracle.com/java/technologies/downloads/#jdk19-windows)

* 克隆此仓库

```
git clone https://github.com/GRID-Game-Store/backend
```

* 确保使用 JDK 17 和 Maven
* 你可以通过运行 `mvn clean package` 来构建项目并运行测试
  或
  `mvn install / mvn clean install`

### 使用 Maven 运行 Spring Boot 应用：

现在你可以在 8082 端口启动服务器。

* 使用 Maven 运行 Spring Boot 应用：
  `mvn spring-boot:run`
* [可选] 使用 `java -jar` 命令运行 Spring Boot 应用
  `java -jar target/backend-0.0.1-SNAPSHOT.jar`

### 使用 Docker 运行 Spring Boot 应用：
要使用 Docker 运行 Spring Boot 应用：
* 在 backend 文件夹内运行 Docker Compose 文件：
  `docker-compose -f docker-compose.yml up`

### 在 Docker 中运行 Keycloak

Keycloak 用于认证，可通过 Docker Compose 启动。
提供的 docker-compose.yml 文件包含 Keycloak 服务和 Keycloak 的 PostgreSQL 数据库。

要同时运行 Keycloak 与后端：

* 运行 Docker Compose 文件：
  `docker-compose -f docker-compose.yml up`

这将会在 8084 端口启动 Keycloak。
建议在后端之前启动 Keycloak，以确保认证服务可用。

[文档链接](https://github.com/GRID-Game-Store/documentation/tree/main/backend)

## API 文档

API 文档通过 Swagger UI 提供。启动应用后，可访问：
`http://localhost:8084/swagger`
该界面提供所有可用端点的完整列表，并允许直接在浏览器中测试。

## GRID 演示视频

查看我们的演示视频，展示 GRID 项目的主要功能：

### 主页面

https://github.com/user-attachments/assets/540fdc0d-b89e-4ab5-b09a-98554d3e0f59

### AI 聊天顾问

https://github.com/user-attachments/assets/48e87a2d-fe04-44a7-b9c8-6060a6840901

### 认证与授权

https://github.com/user-attachments/assets/759e81ef-abc9-462f-bc86-2146ac7ed2e6

### 支付能力

https://github.com/user-attachments/assets/8648e341-3ea2-4f1c-968f-13e65f6395f2

演示内容包括：
- 主页面概览
- 使用 Vertext Gemini 的 AI 聊天顾问
- 基于 Keycloak 的注册与授权流程
- Stripe 与 PayPal 支付集成
- 管理面板演示

## 功能特性

- **AI 聊天顾问**：由 Vertext Gemini 驱动，提供智能客户支持。
- **安全认证**：使用 Keycloak 实现稳健的用户管理。
- **多种支付方式**：集成 Stripe 和 PayPal，支持灵活支付处理。
- **管理面板**：借助 Astro.js 与 Spring Boot 提供完整的管理界面，便于平台管理。

## 技术栈

* [Maven](https://maven.apache.org/) - 依赖管理
* [Spring Boot](https://spring.io/projects/spring-boot) - 服务端框架
* [JUnit](https://junit.org/junit5/) - 测试框架
* [Keycloak](https://www.keycloak.org/) - 身份与访问管理
* [Swagger](https://swagger.io/) - API 文档
* [Docker](https://www.docker.com/) - 容器化平台

## 作者：

* [SEM24](https://github.com/SEM24)

[后端文档链接](https://github.com/GRID-Game-Store/documentation/tree/main/backend)