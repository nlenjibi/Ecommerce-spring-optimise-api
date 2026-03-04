# Smart E-Commerce System

## Table of Contents
- [Project Overview](#project-overview)
- [Features](#features)
- [Technology Stack](#technology-stack)
- [Architecture](#architecture)
- [Security & Authentication](#security--authentication)
- [Installation](#installation)
- [API Documentation](#api-documentation)
- [Usage](#usage)
#### Configuration
- **Cache Configuration**: Multi-level cache with TTL and size limits
- **Transaction Management**: Explicit boundaries with rollback support
- **Database Configuration**: PostgreSQL with HikariCP connection pool
- **Environment Profiles**: Separate configs for dev/test/production

#### Spring Data JPA Configuration
```yaml
spring:
  jpa:
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
        format_sql: true
        use_sql_comments: true
        jdbc:
          batch_size: 20
          order_inserts: true
    hibernate:
      enable_lazy_load_no_trans: true
    show-sql: false
    properties:
      hibernate:
        default_batch_fetch_size: 20
        order_inserts: true
        generate_statistics: true
        jdbc:
          batch_size: 20
          order_inserts: true
```
- [API Documentation](#api-documentation)
- [Usage](#usage)
- [Validation and Error Handling](#validation-and-error-handling)
- [AOP Implementation](#aop-implementation)
- [Performance Analysis](#performance-analysis)
- [Testing](#testing)
### Spring Data JPA Integration

#### Repository Layer
- **BaseRepository**: Extended JpaRepository with common CRUD and soft delete support
- **ProductSpringDataRepository**: 50+ derived queries and JPQL methods
- **UserSpringDataRepository**: User management with role-based queries
- **CategorySpringDataRepository**: Hierarchical category operations
- **OrderSpringDataRepository**: Order status and analytics queries
- **CartSpringDataRepository**: Cart lifecycle management
- **ReviewSpringDataRepository**: Review filtering and analytics

#### Query Optimization
- Derived queries using Spring Data method naming conventions
- Custom JPQL for complex business logic
- Pagination support across all repositories
- Native SQL for performance-critical operations
- Bulk operations for efficient data processing

#### Transaction Management
- `@EnableTransactionManagement` enabled globally
- Explicit rollback configurations for business exceptions
- Transaction advice for cross-cutting concerns
- Proper propagation and isolation levels
- Compensation patterns for distributed transactions

#### Caching Enhancement
- `@EnableCaching` with configurable cache managers
- Multiple cache managers for different data types
- Cache eviction strategies for data consistency
- Performance monitoring and statistics
- TTL configuration based on data volatility
- [Contributing](#contributing)
- [License](#license)

## Project Overview

This Smart E-Commerce System is a comprehensive web-based Spring Boot application that provides both RESTful and GraphQL APIs for managing e-commerce operations. The system implements modern software engineering practices including layered architecture, dependency injection, validation, exception handling, AOP, and comprehensive API documentation.

## Features

### Core Functionality
- **User Management**: Create, read, update, and delete user accounts
- **Product Catalog**: Comprehensive product management with categories, inventory, and pricing
- **Category Management**: Hierarchical category organization for products
- **RESTful APIs**: Standard REST endpoints following best practices
- **GraphQL APIs**: Flexible queries and mutations for optimized data retrieval
- **Validation**: Comprehensive input validation using Bean Validation
- **Exception Handling**: Centralized error management with meaningful responses
- **API Documentation**: Interactive OpenAPI/Swagger documentation
- **AOP Integration**: Cross-cutting concerns like logging and monitoring
- **Performance Optimization**: Efficient algorithms for sorting, searching, and pagination

### Advanced Features
- **Environment-based Configuration**: Support for dev, test, and prod profiles
- **Constructor-based Dependency Injection**: Following Spring best practices
- **Pagination and Filtering**: Efficient data retrieval with customizable parameters
- **Cross-cutting Concerns**: Centralized logging and performance monitoring

## Technology Stack

| Technology | Version | Purpose |
|------------|---------|---------|
| Java | 21 | Primary programming language |
| Spring Boot | 3.x | Application framework |
| Spring Web | 3.x | Web application support |
| Spring Data JPA | 3.x | Database abstraction layer |
| PostgreSQL | 15+ | Primary database |
| GraphQL | Spring GraphQL | Query language and schema |
| Spring Cache | 3.x | Caching abstraction |
| Maven | 3.9.9 | Build and dependency management |
| Lombok | 1.18.38 | Code generation (annotations, getters/setters) |
| MapStruct | 1.6.3 | Object mapping framework |
| Flyway | 11.14.1 | Database migration tool |
| HikariCP | | High-performance connection pool |
| SpringDoc OpenAPI | 2.8.15 | API documentation |
| Micrometer | | Performance metrics |

| Technology | Version | Purpose |
|------------|---------|---------|
| Java | 21 | Primary programming language |
| Spring Boot | 3.x | Application framework |
| Spring Web | 3.x | Web application support |
| Spring Data JPA | 3.x | Database access layer |
| Spring GraphQL | 3.x | GraphQL support |
| Springdoc OpenAPI | 2.x | API documentation |
| Hibernate | 6.x | ORM framework |
| Database | H2/PostgreSQL | Persistent storage |
| Maven | 3.9+ | Build tool |
| Docker | Latest | Containerization |

## Architecture

### Layered Architecture
```
┌─────────────────┐
│   Controller    │ ← REST & GraphQL Endpoints
├─────────────────┤
│     Service     │ ← Business Logic
├─────────────────┤
│   Repository    │ ← Data Access Layer
├─────────────────┤
│    Database     │ ← Data Storage
└─────────────────┘
```

### Modular Architecture
```
modules/
├── auth/          # Authentication & Security
├── user/          # User Management
├── product/       # Product Catalog
├── category/      # Category Management
├── cart/          # Shopping Cart
├── order/         # Order Processing
├── payment/       # Payment Processing
├── inventory/     # Stock Management
└── ...
```

## Security & Authentication

### Overview
The system implements comprehensive security with JWT-based authentication and OAuth2 support.

### Key Features
- **JWT Authentication**: Stateless token-based authentication
- **OAuth2 Login**: Google, GitHub, Facebook integration
- **Role-Based Access Control**: CUSTOMER, STAFF, ADMIN roles
- **Password Security**: BCrypt hashing with salt
- **Token Blacklisting**: Revoked token management
- **Security Event Logging**: Login attempts, access tracking

### Documentation
For detailed security documentation, see [AUTH.md](AUTH.md)

### Quick Start

```bash
# Login
POST /api/v1/auth/login
{
  "email": "user@example.com",
  "password": "password123"
}

# Use token
curl -H "Authorization: Bearer {accessToken}" \
  http://localhost:8080/api/v1/user/profile

# OAuth2 Login
GET /api/oauth2/authorization/google
```

### Configuration
```yaml
# JWT Settings
jwt:
  secret: your-secret-key
  access-token:
    expiration: 3600000  # 1 hour
  refresh-token:
    expiration: 604800000  # 7 days

# OAuth2
spring.security.oauth2.client.registration.google:
  client-id: ${GOOGLE_CLIENT_ID}
  client-secret: ${GOOGLE_CLIENT_SECRET}
```

## Installation

### Prerequisites
- Java 21 or higher
- Maven 3.6+ or Gradle 7+
- Docker (optional, for containerized deployment)

### Steps
1. Clone the repository:
```bash
git clone <repository-url>
cd smart-ecommerce-system
```

2. Build the project:
```bash
mvn clean install
```

3. Run the application:
```bash
mvn spring-boot:run
```

## Configuration

### Environment Profiles
The application supports multiple environments:

- **Development**: `dev` profile (default)
- **Test**: `test` profile
- **Production**: `prod` profile

### Configuration Files
- `application.yml`: Base configuration
- `application-dev.yml`: Development-specific settings
- `application-test.yml`: Test environment settings
- `application-prod.yml`: Production environment settings

### Key Configuration Properties
```yaml
spring:
  application:
    name: smart-ecommerce
  profiles:
    active: dev
  datasource:
    url: jdbc:h2:mem:ecommerce_db
    username: sa
    password: 
    driver-class-name: org.h2.Driver
  jpa:
    database-platform: org.hibernate.dialect.H2Dialect
    hibernate:
      ddl-auto: create-drop
    show-sql: true
    properties:
      hibernate:
        format_sql: true
  h2:
    console:
      enabled: true
  graphql:
    graphiql:
      enabled: true
    path: /graphql

server:
  port: 8080

logging:
  level:
    com.ecommerce.ecommerceapp: DEBUG
    org.springframework: INFO
    org.springframework.security: INFO

springdoc:
  swagger-ui:
    path: /swagger-ui.html
  api-docs:
    path: /v3/api-docs
```

## API Documentation

The system provides comprehensive API documentation through Springdoc OpenAPI:

- **Swagger UI**: Available at `http://localhost:8080/swagger-ui.html`
- **OpenAPI JSON**: Available at `http://localhost:8080/v3/api-docs`
- **GraphQL Playground**: Available at `http://localhost:8080/graphiql`

### Documentation Features
- Interactive API testing interface
- Request/response examples
- Validation rule documentation
- Error code explanations
- Authentication requirements


### Environment Variables

Create a `.env` file in the root directory:

```env
DB_URL=jdbc:mysql://localhost:3306/ecommerce_dev
DB_USERNAME=root
DB_PASSWORD=your_password
REDIS_HOST=localhost
REDIS_PORT=6379
```
## Usage

### Testing Performance

Run the performance tests to benchmark Spring Data JPA implementations:

```bash
mvn spring-boot:run -Dspring-boot.run.arguments="--com.smart_ecomernce_api.smart_ecomernce_api.PerformanceTest"
```

Expected output:
```
=== Testing Product Repository Performance ===
Found 100 active products, fetched 10 featured products
Pagination with 50 items took 150ms
Search for 'laptop' with 20 results took 200ms
Derived query for category 1 with 20 results took 180ms
Total product repository tests completed in 730ms

=== Testing User Repository Performance ===
User search for 'test' with 20 results took 120ms
Admin user query with 20 results took 110ms
Total user repository tests completed in 230ms

=== Testing Cache Performance ===
100 cache miss operations took 95ms
100 cache hit operations took 105ms
Total cache performance tests completed in 200ms
Cache hit rate: 50.00%
```

### Performance Endpoints

Monitor performance metrics:
- `GET /api/performance/metrics` - System metrics
- `GET /api/performance/cache/{cacheName}` - Cache statistics
- `POST /api/performance/cache/clear` - Clear all caches
- `POST /api/performance/cache/clear/{cacheName}` - Clear specific cache

### Expected Performance Improvements

- **30-50% faster** query performance with derived queries
- **60-80% cache hit rate** with optimized caching strategies
- **Reduced database load** through efficient pagination
- **Better resource utilization** through bulk operations

### Development Notes

The system demonstrates advanced Spring Data JPA patterns including:
- Repository inheritance with common functionality
- Derived query methods with convention-based naming
- Custom JPQL for complex business logic
- Multi-level caching with TTL configuration
- Transaction management with explicit boundaries
- Performance monitoring and metrics collection

### REST API Endpoints

#### User Management
- `GET /api/users` - List all users with pagination
- `GET /api/users/{id}` - Get user by ID
- `POST /api/users` - Create new user
- `PUT /api/users/{id}` - Update user
- `DELETE /api/users/{id}` - Delete user

#### Product Management
- `GET /api/products` - List products with filtering, sorting, pagination
- `GET /api/products/{id}` - Get product by ID
- `POST /api/products` - Create new product
- `PUT /api/products/{id}` - Update product
- `DELETE /api/products/{id}` - Delete product

#### Category Management
- `GET /api/categories` - List all categories
- `GET /api/categories/{id}` - Get category by ID
- `POST /api/categories` - Create new category
- `PUT /api/categories/{id}` - Update category
- `DELETE /api/categories/{id}` - Delete category

### GraphQL Queries and Mutations


#### Categories, Orders, Reviews, Cart, Wishlist
Similar CRUD endpoints available for all entities.

### GraphQL Schema

The GraphQL schema includes the following main types:

#### Core Types
- `User`: User management with roles (ADMIN, CUSTOMER, MERCHANT)
- `Product`: Product catalog with inventory, pricing, and categories
- `Category`: Hierarchical product categories
- `Order`: Order management with status tracking
- `Review`: Product reviews and ratings
- `Cart`: Shopping cart functionality
- `Wishlist`: User wishlists

#### Pagination
All list queries support pagination with `PageInput`:
```graphql
input PageInput {
    page: Int = 0
    size: Int = 20
    sortBy: String = "id"
    direction: SortDirection = ASC
}
```

#### Sample Queries

**Get Products with Filtering:**
```graphql
query {
  products(pagination: {page: 0, size: 10}, filter: {categoryId: 1, minPrice: 10.0}) {
    content {
      id
      name
      price
      category {
        name
      }
    }
    pageInfo {
      totalElements
      currentPage
    }
  }
}
```

**Create Product:**
```graphql
mutation {
    createProduct(input: {
        name: "New Product"
        price: 29.99
        categoryId: 1
    }) {
        id
        name
        price
    }
}
```

## Validation and Error Handling

### Validation Rules
- Email format validation
- Password strength requirements
- Product price positive validation
- Stock quantity validation
- Required field validation
- Custom business rule validation

### Error Response Format
```json
{
  "timestamp": "2023-12-01T10:30:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/users",
  "fieldErrors": {
    "email": "Email format is invalid"
  }
}
```

### Exception Handling
- `@ControllerAdvice` for centralized error handling
- Custom exception classes for business logic errors
- HTTP status code mapping
- Meaningful error messages for clients

## AOP Implementation

### Logging Aspect
- Method execution logging
- Parameter logging
- Return value logging
- Execution time measurement

### Performance Monitoring Aspect
- Method execution time tracking
- Performance metrics collection
- Slow method identification

### Security Aspect
- Access control validation
- Authentication verification
- Authorization checks

### Example AOP Configuration
```java
@Aspect
@Component
public class LoggingAspect {
    
    @Around("within(com.ecommerce.ecommerceapp.modules..*)")
    public Object logAround(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        
        logger.info("Entering method: {} with arguments: {}", 
                   joinPoint.getSignature().getDeclaringTypeName() + "." + joinPoint.getSignature().getName(),
                   joinPoint.getArgs());

        try {
            Object result = joinPoint.proceed();
            
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;
            
            logger.info("Exiting method: {} with result: {}. Execution time: {} ms", 
                       joinPoint.getSignature().getDeclaringTypeName() + "." + joinPoint.getSignature().getName(),
                       result, duration);
            
            if (duration > 1000) { // Log if execution takes more than 1 second
                logger.warn("Method {} took {} ms to execute", 
                           joinPoint.getSignature().getName(), duration);
            }
            
            return result;
        } catch (Exception e) {
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;
            
            logger.error("Exception in method: {} after {} ms. Exception: {}", 
                        joinPoint.getSignature().getDeclaringTypeName() + "." + joinPoint.getSignature().getName(),
                        duration, e.getMessage());
            throw e;
        }
    }
}
```

## Performance Analysis

### REST vs GraphQL Performance Comparison

| Aspect | REST | GraphQL |
|--------|------|---------|
| Data Fetching | Multiple endpoints | Single query for multiple entities |
| Over-fetching | Common issue | Minimal over-fetching |
| Network Requests | More requests for complex data | Fewer requests |
| Caching | Standard HTTP caching | Query-specific caching |
| Flexibility | Fixed response structure | Client-defined response |

### Optimization Techniques Implemented
- **Pagination**: Efficient data retrieval with page-based loading
- **Sorting Algorithms**: Optimized sorting using efficient algorithms
- **Search Algorithms**: Binary search and other efficient search methods
- **Database Indexing**: Strategic indexing for query optimization
- **Connection Pooling**: Efficient database connection management

### Performance Metrics
- Response time monitoring
- Throughput measurement
- Memory usage tracking
- Database query optimization

## Testing

### Test Strategy
- **Unit Tests**: Individual method and class testing
- **Integration Tests**: Component interaction testing
- **API Tests**: Endpoint functionality validation
- **Performance Tests**: Load and stress testing

### Testing Tools
- JUnit 5 for unit testing
- Mockito for mocking dependencies
- REST Assured for API testing
- GraphQL Test for GraphQL endpoint testing

### Test Coverage
- Minimum 80% code coverage requirement
- Critical business logic 100% coverage
- API endpoints comprehensive testing

## Project Structure

```
smart-ecommerce/
│
├── src/
│   ├── main/
│   │   ├── java/com/company/ecommerce/
│   │   │   ├── EcommerceApplication.java
│   │   │   │
│   │   │   ├── config/
│   │   │   ├── security/
│   │   │   ├── common/
│   │   │   ├── modules/
│   │   │   └── integration/
│   │   │
│   │   └── resources/
│   │       ├── graphql/
│   │       ├── application.yml
│   │       ├── db/migration/
│   │       └── templates/
│   │
│   └── test/
│       └── java/com/company/ecommerce/
│
├── pom.xml
└── README.md
```

## Running with Docker

### Build and Run
```bash
# Build the application
mvn clean install

# Build and run with Docker Compose
docker-compose -f docker/docker-compose.yml up --build
```

### Docker Services
- **app**: Spring Boot application running on port 8080
- **db**: PostgreSQL database running on port 5432

## Documentation

### Spring Data JPA Documentation

Comprehensive documentation has been created for the Spring Data JPA implementation:

- **[SPRING_DATA_GUIDE.md](SPRING_DATA_GUIDE.md)** - Complete guide for repository patterns, query optimization, transaction management, and caching strategies
- **[PERFORMANCE_OPTIMIZATION.md](PERFORMANCE_OPTIMIZATION.md)** - Performance benchmarks, optimization techniques, and monitoring strategies

### Key Topics Covered

- **Repository Patterns**: BaseRepository, derived queries, JPQL, native SQL
- **Dynamic Query Building**: JPA Specifications, QueryDSL predicates
- **Transaction Management**: Propagation levels, isolation levels, rollback strategies
- **Caching**: Configuration, usage patterns, performance monitoring
- **Analytics**: Native SQL for complex reporting with PostgreSQL features
- **Testing**: Integration tests for specifications and repositories

## Contributing

### Development Guidelines
1. Follow the layered architecture pattern
2. Use constructor-based dependency injection
3. Implement proper validation and error handling
4. Write comprehensive unit and integration tests
5. Maintain API documentation with Springdoc OpenAPI
6. Apply AOP for cross-cutting concerns
7. Follow Java and Spring Boot best practices

### Code Standards
- Use meaningful variable and method names
- Write clear and concise comments
- Follow the Single Responsibility Principle
- Implement proper exception handling
- Use appropriate design patterns

### Branching Strategy
- `main`: Production-ready code
- `develop`: Integration branch for features
- `feature/*`: Feature development branches
- `hotfix/*`: Urgent production fixes

## Performance Optimization

### Overview
This project implements comprehensive performance optimizations based on VisualVM profiling analysis. See [IMPROVE.md](IMPROVE.md) for detailed documentation.

### Optimizations Implemented

| Category | Improvement |
|----------|-------------|
| **Database** | JOIN FETCH queries, Hibernate batch fetching, composite indexes |
| **JWT Authentication** | Caffeine token cache, request-scoped ThreadLocal |
| **Thread Safety** | LongAdder, Token Bucket algorithm, StampedLock |
| **Memory** | Jackson optimization, Hibernate tuning |
| **Async** | @Async thread pool, CompletableFuture |

### Performance Metrics

| Metric | Before | After |
|--------|--------|-------|
| Product listing query | 11.6 ms | <3 ms |
| JWT validation | 312 ms | <30 ms |
| Thread contention | 3,675 ms wait | Minimal |
| Request blocking | Synchronous | Non-blocking |

### Key Configuration

#### Async Thread Pools
```yaml
async:
  core-pool-size: 10
  max-pool-size: 25
  queue-capacity: 100
```

#### Rate Limiting
```yaml
rate-limit:
  enabled: false  # Set true to enforce
  requests-per-minute: 100
  requests-per-second: 10 JWT Cache
```
```

####yaml
jwt:
  cache:
    max-size: 10000
    expire-after-write-minutes: 10
```

### Testing

Run performance tests:
```bash
mvn test -Dtest=PerformanceMetricsTest
mvn test -Dtest=ConcurrencyTest
mvn test -Dtest=AsyncOperationTest
```

### Monitoring Endpoints

- `GET /actuator/health` - Application health
- `GET /actuator/metrics` - Performance metrics
- `GET /actuator/caches` - Cache statistics

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Acknowledgments

- Spring Boot Framework
- Spring GraphQL
- Springdoc OpenAPI
- Hibernate ORM
- GraphQL Java
- Various open-source libraries and tools