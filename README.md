# Student Feedback Analytics Portal

This project is a fully containerized, microservices-based application designed to analyze student feedback. Users can upload a CSV file of feedback, which is then processed by a series of services. A Large Language Model (LLM) is used to perform sentiment analysis, extract key themes, and identify improvement areas, which are then displayed on a user-friendly dashboard.

This project serves as a practical, end-to-end example of a modern distributed system, demonstrating concepts like service discovery, API gateways, inter-service communication, and AI integration.

## Features

* **Microservices Architecture:** The system is broken down into independent, scalable services (Gateway, Registry, Data Service, Analytics Service).
* **Service Discovery:** Uses Netflix Eureka for dynamic service registration and discovery.
* **Centralized Routing:** Uses Spring Cloud Gateway as a single entry point for all frontend requests.
* **Secure File Upload:** Validates file size (10MB max) and type (CSV only).
* **AI-Powered Insights:** Integrates with the Google Gemini API to perform deep analysis on raw text feedback.
* **Dynamic Dashboard:** A React frontend displays analytics, including sentiment charts (via `recharts`) and a categorized list of key themes.
* **Containerized:** All services, including the frontend, are designed to run in Docker containers and are orchestrated with `docker-compose`.

## System Architecture

The application follows a classic microservice pattern. All user-facing requests are proxied through an API Gateway, which discovers the location of downstream services via the Service Registry.

```mermaid
graph TD
    subgraph "Browser (Client)"
        UI[Frontend UI<br>(React, Port 3000)<br>Displays charts & data]
    end

    subgraph "Backend Infrastructure (Server-Side)"
        Gateway[API Gateway<br>(Spring Cloud Gateway, Port 8080)<br>Routes /api/data/**]
        Registry[Service Registry<br>(Netflix Eureka, Port 8761)<br>Handles service discovery]
        
        subgraph "Data Services"
            DataSvc[Data Service<br>(Spring Boot, Port 8081)<br>Handles upload, validation, temp storage]
        end

        subgraph "Analytics Services"
            AnalyticsSvc[Analytics Service<br>(Spring Boot, Port 8082)<br>Converts CSV to text, calls LLM]
        end
    end

    subgraph "External Services"
        LLM[External LLM API<br>(Google Gemini)]
    end

    %% --- Connections ---
    UI -- "POST /api/data/upload\n(proxied to 8080)" --> Gateway
    
    Gateway -- "Routes to lb://DATA-SERVICE" --> DataSvc
    
    DataSvc -- "1. Registers" --> Registry
    AnalyticsSvc -- "2. Registers" --> Registry
    Gateway -- "3. Discovers Services" --> Registry
    
    DataSvc -- "POST /analyze\n(@LoadBalanced RestTemplate)" --> AnalyticsSvc
    
    AnalyticsSvc -- "POST /generateContent\n(HTTPS API Call)" --> LLM

```
## Technology Stack

| Category | Technology |
|:---|:---|
| **Backend** | Java 17+, Spring Boot 3.x, Spring Cloud (Gateway, Eureka) |
| **Frontend** | React, `recharts`, Tailwind CSS |
| **AI Integration** | Google Gemini API |
| **Containerization** | Docker, Docker Compose |
| **Build Tool** | Maven |

## Project Structure

This is a multi-module Maven project, best opened from the root `pom.xml` in IntelliJ IDEA.
## Project Structure

This is a multi-module Maven project, best opened from the root `pom.xml` in IntelliJ IDEA.

```text
StudentFeedbackPortal/
├── .idea/
├── api-gateway/         # Spring Cloud Gateway Module
├── analytics-service/   # Spring Boot Module (LLM)
├── data-service/          # Spring Boot Module (Upload)
├── service-registry/      # Spring Cloud Eureka Module
├── frontend-ui/           # React Application
├── docker-compose.yml     # Docker Orchestration
├── pom.xml                # Parent Maven POM
└── README.md
```

## Getting Started

### Prerequisites

* **Java JDK 17** or newer.

* **IntelliJ IDEA** or a comparable IDE.

* **Node.js & npm** (for running the frontend).

* **Docker Desktop** (for running with `docker-compose`).

* **Google Gemini API Key**: You must have a Gemini API key.

    1. Get one for free from [Google AI Studio](https://aistudio.google.com/app/apikey).

    2. Click "Create API key in new project".

### 1. Configure Environment Variable (Critical)

You must set the `GEMINI_API_KEY` for the `analytics-service` to work.

**For IntelliJ (Recommended for Dev):**

1. Go to `Run` -> `Edit Configurations...`.

2. Find and select `AnalyticsServiceApplication`.

3. Click `Modify options` -> `Environment variables`.

4. Click `+` and add:

    * **Name:** `GEMINI_API_KEY`

    * **Value:** `your-secret-api-key-goes-here`

5. Click **OK** to save.

**For Docker Compose / Terminal:**
You need to export the variable in the terminal session you'll be running `docker-compose` from:

```bash
export GEMINI_API_KEY="your-secret-api-key-goes-here"
```
### 2. Running in Development (IntelliJ + npm)

This is the best way to develop and debug the application. **Run the services in this order.**

1. **Run Service Registry:**

    * Open `service-registry/src/main/java/.../ServiceRegistryApplication.java`.

    * Right-click and select `Run 'ServiceRegistryApplication'`.

    * Wait for it to start. You can check the dashboard at `http://localhost:8761/`.

2. **Run API Gateway:**

    * Open `api-gateway/src/main/java/.../ApiGatewayApplication.java`.

    * Right-click and `Run`.

3. **Run Data Service:**

    * Open `data-service/src/main/java/.../DataServiceApplication.java`.

    * Right-click and `Run`.

4. **Run Analytics Service:**

    * *(Ensure you set the `GEMINI_API_KEY` in the Run Configuration first!)*

    * Open `analytics-service/src/main/java/.../AnalyticsServiceApplication.java`.

    * Right-click and `Run`.

    * After a moment, all services should appear as `UP` in the Eureka dashboard.

5. **Run Frontend UI:**

    * Open a new terminal in IntelliJ (`View` -> `Tool Windows` -> `Terminal`).

    * `cd frontend-ui`

    * `npm install`

    * `npm start`

Your browser will open to `http://localhost:3000`. The application is now fully operational!

### 3. Running with Docker Compose (Production-like)

This method runs all services as containers.

1. **Build Spring Boot Jars:**

    * For *each* of the 4 Spring modules (`api-gateway`, `data-service`, `analytics-service`, `service-registry`), run the Maven build:

   ```bash
   # Example for one service
   cd api-gateway
   mvn clean package -DskipTests
   cd ..
   
   # Repeat for all 4
   ```

2. **Set Environment Variable:**

    * Make sure you have exported your API key in your terminal:

   ```bash
   export GEMINI_API_KEY="your-secret-api-key-goes-here"
3. **Run Docker Compose:**

    * From the project's root directory (where `docker-compose.yml` is):

   ```bash
   docker-compose up --build
   ```
    * This will build the Docker images for all services and start them.

The application will be available at http://localhost:3000.





## Service & Port Reference

| Service | Port | URL |
|:---|:---|:---|
| **Frontend UI** | `3000` | `http://localhost:3000` |
| **API Gateway** | `8080` | `http://localhost:8080` |
| **Service Registry** | `8761` | `http://localhost:8761` |
| **Data Service** | `8081` | (Internal) |
| **Analytics Service** | `8082` | (Internal) |










