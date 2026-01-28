<div align="center">
  <h2><strong>Nexa</strong></h2>
</div>



<div align="center">
  <!-- Main tech badges based on project internals -->
  <img src="https://img.shields.io/badge/Kotlin-purple?style=for-the-badge&logo=kotlin" alt="Kotlin">
  <img src="https://img.shields.io/badge/Spring Boot-brightgreen?style=for-the-badge&logo=spring" alt="Spring Boot">
  <img src="https://img.shields.io/badge/JPA-orange?style=for-the-badge" alt="JPA">
  <img src="https://img.shields.io/badge/Microservices-blue?style=for-the-badge" alt="Microservices">
  <img src="https://img.shields.io/badge/PostgreSQL-lightblue?style=for-the-badge&logo=postgresql" alt="PostgreSQL">
  <br>
  <strong>A modular microservices project built in Kotlin with Spring Boot and PostgreSQL.</strong>
</div>



---

##  Project Overview

**Nexa** is a modular microservices project built on independent but coordinated services. The project includes a social network style API backend. Each service is developed separately for easier scaling and maintenance.

### Services Overview

Each service (users, posts, reactions, media, config, discovery, and gateway) has its own domain:

* **config-service** — Centralized configuration for all services.  
* **discovery-service** — Service registry for dynamic lookup (Eureka Server).  
* **gateway** — Routing and external interface through the API Gateway.  
* **user-service** — User authentication, profiles, and management.  
* **post-service** — CRUD operations for posts.  
* **reaction-service** — Likes, reactions, and interactions.  
* **comment-service** — Creating and retrieving comments.  
* **media-service** — Uploading and delivering media files.



