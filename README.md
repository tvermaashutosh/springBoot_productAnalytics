# Product Analytics

A high-performance, event-driven product analytics backend built with Spring Boot. This system tracks product views, provides real-time analytics on product popularity, and features an AI-powered natural language SQL query interface.

## Features

- **Real-time Product View Tracking** - Track product views by user IP with async processing
- **Analytics Insights** - Query recently viewed and frequently viewed products
- **Custom Caching System** - Pluggable eviction strategies (LRU/LFU) and write policies (WriteThrough/WriteBack)
- **Event-Driven Architecture** - Apache Kafka integration for scalable async data processing
- **AI-Powered SQL Generation** - Natural language to SQL conversion using Groq LLM (Llama 3.3 70B)
- **SQL Safety Validation** - Parser-based validation to prevent SQL injection and block DML operations
- **Non-blocking I/O** - Async endpoints with `CompletableFuture` for high throughput
- **Runtime Configuration** - Switch cache strategies via API without restart

## Tech Stack

| Component | Technology |
|-----------|------------|
| Framework | Spring Boot 3.5.9 |
| Language | Java 17 |
| Database | PostgreSQL (Aiven Cloud) |
| ORM | Spring Data JPA + Hibernate |
| Messaging | Apache Kafka |
| SQL Parsing | JSqlParser 5.3 |
| LLM Provider | Groq API (Llama 3.3 70B) |
| Documentation | SpringDoc OpenAPI |
| Build Tool | Maven |

## Architecture

```
┌─────────────────────────────────────────────────────────────────────────┐
│                              REST API Layer                             │
│         AnalyticsController │ QueryController │ CacheController         │
└───────────────────────────────────┬─────────────────────────────────────┘
                                    │
┌───────────────────────────────────▼─────────────────────────────────────┐
│                             Service Layer                               │
│             AnalyticsService │ QueryService │ CacheService              │
└──────────────┬────────────────────┬─────────────────────┬───────────────┘
               │                    │                     │
       ┌───────▼───────┐    ┌───────▼───────┐    ┌────────▼────────┐
       │  Cache Layer  │    │  Kafka Layer  │    │   Repository    │
       │  SimpleCache  │    │  Producer/    │    │   JPA +         │
       │  LRU/LFU      │    │  Consumer     │    │   JDBC          │
       └───────────────┘    └───────────────┘    └────────┬────────┘
                                                          │
                                                 ┌────────▼────────┐
                                                 │   PostgreSQL    │
                                                 └─────────────────┘
```

### Cache System Architecture

The custom cache implementation supports pluggable strategies:

**Eviction Strategies:**
- `LRUEvictionStrategy` - Least Recently Used
- `LFUEvictionStrategy` - Least Frequently Used

**Write Strategies:**
- `WriteThroughStrategy` - Synchronous write to cache and database
- `WriteBackStrategy` - Write to cache first, async database update via Kafka

### Key-Based Executor

Thread pool with N executors (where N = cache size) that routes operations by key hash. This ensures:
- Key-level serialization (no race conditions on same key)
- Maximum concurrency across different keys

## API Reference

Base path: `/api`

### Product Analytics

| Method | Endpoint | Description | Parameters |
|--------|----------|-------------|------------|
| GET | `/product/all` | List all products | - |
| GET | `/product/one` | Get single product | `productId` |
| PUT | `/product/view` | Track a product view | `productId`, `userIp` |
| GET | `/product/recent` | Get recently viewed products | - |
| GET | `/product/frequent` | Get frequently viewed products | - |

### Query Engine

| Method | Endpoint | Description | Parameters |
|--------|----------|-------------|------------|
| GET | `/query/generate` | Generate SQL from natural language | `prompt` |
| GET | `/query/execute` | Execute SQL query (SELECT only) | `query` |
| GET | `/query/history` | View cached query history | - |

### Cache Management

| Method | Endpoint | Description | Parameters |
|--------|----------|-------------|------------|
| PUT | `/cache/eviction` | Set eviction strategy | `strategy` (LRU/LFU) |
| PUT | `/cache/write` | Set write strategy | `strategy` (WriteThrough/WriteBack) |
| PUT | `/cache/parallelWriteThrough` | Enable parallel write-through | `yes` (true/false) |
| GET | `/cache/stats` | View cache statistics | - |
| DELETE | `/cache/clear` | Clear a cache | `cacheName` |

### Response Format

All endpoints return a standardized response:

```json
{
  "message": "Success message",
  "body": { }
}
```

## Getting Started

### Prerequisites

- Java 17+
- Maven 3.8+
- PostgreSQL database
- Apache Kafka cluster (with SASL/SCRAM + SSL)
- Groq API key (for LLM features)

### Installation

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd productAnalytics
   ```

2. **Configure environment variables** (for production)
   ```bash
   export DB_URL=jdbc:postgresql://your-host:port/database?sslmode=require
   export DB_USER=your-username
   export DB_PASS=your-password
   export KAFKA_BOOTSTRAP_SERVERS=your-kafka-host:port
   export KAFKA_SASL_JAAS_CONFIG='org.apache.kafka.common.security.scram.ScramLoginModule required username="user" password="pass";'
   export KAFKA_TRUSTSTORE_PASSWORD=your-truststore-password
   export KAFKA_CAPEM_PATH=/path/to/ca.pem
   export LLM_API_KEY=your-groq-api-key
   ```

3. **Build the project**
   ```bash
   ./mvnw clean package -DskipTests
   ```

4. **Run the application**
   ```bash
   # Development mode
   ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

   # Production mode
   java -jar target/productAnalytics-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
   ```

### Docker

Build and run using Docker:

```bash
docker build -t product-analytics .
docker run -p 8080:8080 \
  -e DB_URL=... \
  -e DB_USER=... \
  -e DB_PASS=... \
  -e KAFKA_BOOTSTRAP_SERVERS=... \
  -e KAFKA_SASL_JAAS_CONFIG=... \
  -e KAFKA_TRUSTSTORE_PASSWORD=... \
  -e KAFKA_CAPEM_PATH=... \
  -e LLM_API_KEY=... \
  product-analytics
```

## Configuration

### Application Properties

| Property | Description | Default |
|----------|-------------|---------|
| `server.servlet.context-path` | API base path | `/api` |
| `spring.jpa.hibernate.ddl-auto` | DDL mode | `create-drop` |
| `spring.kafka.topic` | Kafka topic name | `product-view-topic` |
| `spring.llm.model` | LLM model | `llama-3.3-70b-versatile` |

### Default Cache Configuration

| Setting | Default Value |
|---------|---------------|
| Cache Size | 3 |
| Eviction Strategy | LRU |
| Write Strategy | WriteBack |
| Parallel WriteThrough | false |
| Async DB Write via Kafka | true |

## Database Schema

### Products Table (`product_bangalore_hyderabad`)

| Column | Type | Constraints |
|--------|------|-------------|
| id | UUID | Primary Key (auto-generated) |
| product_id | VARCHAR | Unique, Not Null |
| name | VARCHAR | Unique, Not Null |
| price | DOUBLE | Not Null |
| description | TEXT | - |
| image | VARCHAR | Unique, Not Null |
| created | TIMESTAMP | Not Null (auto-set) |

### Product Views Table (`product_view_bangalore_hyderabad`)

| Column | Type | Constraints |
|--------|------|-------------|
| id | UUID | Primary Key (auto-generated) |
| product_id | VARCHAR | Not Null |
| user_ip | VARCHAR | Not Null |
| view_count | INT | Not Null |
| last_updated | TIMESTAMP | Not Null (auto-updated) |
| | | Unique(product_id, user_ip) |

## Project Structure

```
src/main/java/com/example/productAnalytics/
├── ProductAnalyticsApplication.java    # Application entry point
├── controller/                         # REST endpoints
│   ├── AnalyticsController.java
│   ├── QueryController.java
│   └── CacheController.java
├── service/                            # Business logic
│   ├── AnalyticsService.java
│   ├── QueryService.java
│   └── CacheService.java
├── model/                              # JPA entities
│   ├── Product.java
│   └── ProductView.java
├── repository/                         # Data access
│   ├── ProductRepository.java
│   └── ProductViewRepository.java
├── cache/                              # Custom cache implementation
│   └── SimpleCache.java
├── evictionStrategy/                   # Cache eviction policies
│   ├── IEvictionStrategy.java
│   ├── LRUEvictionStrategy.java
│   └── LFUEvictionStrategy.java
├── writeStrategy/                      # Cache write policies
│   ├── IWriteStrategy.java
│   ├── WriteThroughStrategy.java
│   ├── WriteBackStrategy.java
│   └── WriteAroundStrategy.java
├── producer/                           # Kafka producer
│   └── KafkaProducer.java
├── consumer/                           # Kafka consumer
│   └── KafkaConsumer.java
├── config/                             # Configuration classes
│   ├── AppConfig.java
│   ├── KafkaConfig.java
│   └── LLMConfig.java
├── factory/                            # Factory pattern
│   └── CacheFactory.java
├── executor/                           # Concurrency management
│   └── KeyBasedExecutor.java
├── dto/                                # Data transfer objects
│   └── ViewEvent.java
├── util/                               # Utilities
│   └── ApiResponseBuilder.java
└── exception/                          # Exception handling
    └── GlobalExceptionHandler.java
```

## Usage Examples

### Track a Product View

```bash
curl -X PUT "http://localhost:8080/api/product/view?productId=A9K3Q&userIp=192.168.1.10"
```

### Get Recent Products

```bash
curl "http://localhost:8080/api/product/recent"
```

### Generate SQL from Natural Language

```bash
curl "http://localhost:8080/api/query/generate?prompt=show%20me%20products%20under%20$50"
```

### Execute a Query

```bash
curl "http://localhost:8080/api/query/execute?query=SELECT%20name,%20price%20FROM%20product_bangalore_hyderabad%20WHERE%20price%20%3C%2050"
```

### Switch Cache Eviction Strategy

```bash
curl -X PUT "http://localhost:8080/api/cache/eviction?strategy=LFU"
```

### View Cache Statistics

```bash
curl "http://localhost:8080/api/cache/stats"
```

## API Documentation

OpenAPI/Swagger documentation is available at:
```
http://localhost:8080/api/swagger-ui.html
```

## Data Flow

### Product View Tracking

```
PUT /api/product/view
       │
       ▼
AnalyticsService.view()
       │
       ├──► Update recentCache (LRU)
       │         │
       │         └──► [If asyncDBWriteThroughKafka=true]
       │                    │
       │                    ▼
       │              KafkaProducer
       │                    │
       │                    ▼
       │              KafkaConsumer
       │                    │
       │                    ▼
       │              Database Update
       │
       └──► Update frequentCache (LFU)
```

## Security Features

- **SQL Injection Prevention**: JSqlParser validates that only SELECT statements are executed
- **Kafka Security**: SASL/SCRAM authentication with SSL/TLS encryption
- **Environment Variables**: Production credentials stored in environment variables, not code
- **Truststore Management**: SSL certificates managed at runtime
