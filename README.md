# Clean CRUD Framework

A robust Java library for implementing clean architecture-based CRUD operations with clear separation between API, domain, and infrastructure layers.

## Overview

The Clean CRUD Framework provides a comprehensive template for building CRUD (Create, Read, Update, Delete) endpoints following clean architecture principles. It enforces a clear separation of concerns between different layers of your application, making your code more maintainable, testable, and adaptable to change.

A key feature of this framework is the hiding of infrastructure IDs from the web layer, ensuring that your domain remains isolated from infrastructure concerns.

## Key Features

- **Clean Architecture Implementation**: Strict separation between API, domain, and infrastructure layers
- **Complete CRUD Operations**: Ready-to-use templates for Create, Read, Update, and Delete operations
- **Infrastructure ID Isolation**: Domain IDs are separated from persistence IDs
- **Spring Boot Integration**: Seamlessly works with Spring Boot applications
- **Flexible Adapters**: Customizable adapters between different layers
- **Security Policies**: Built-in support for domain-level security policies
- **Validation**: Comprehensive validation at all layers
- **Error Handling**: Consistent error handling across the application

## Architecture

The framework is built on clean architecture principles with three main layers:

### 1. API Layer

The API layer is divided into two sub-layers:

- **Web Layer**: Handles HTTP requests and responses using Spring REST controllers
- **Application Layer**: Orchestrates use cases and transforms between web and domain models

### 2. Domain Layer

The domain layer contains:

- **Domain Models**: Core business entities and value objects
- **Domain Services**: Business logic and rules
- **Domain Exceptions**: Business-specific exceptions
- **Validation**: Domain-specific validation rules
- **Security Policies**: Access control rules at the domain level

### 3. Infrastructure Layer

The infrastructure layer includes:

- **Persistence Adapters**: Adapters between domain and persistence models
- **Repositories**: Data access using Spring Data JPA
- **ID Management**: Separation between domain IDs and persistence IDs

## How It Works

### ID Isolation

One of the key features of this framework is the isolation of infrastructure IDs from the web layer:

1. **Domain IDs**: Used within the domain layer and exposed to the web layer
2. **Persistence IDs**: Used only within the infrastructure layer
3. **ID Adapters**: Convert between domain IDs and persistence IDs

This approach ensures that your domain remains clean and free from infrastructure concerns.

### Layer Communication

Communication between layers follows clean architecture principles:

1. **Web → Application**: Web controllers call application services
2. **Application → Domain**: Application services use domain services and models
3. **Domain → Infrastructure**: Domain services use infrastructure adapters through interfaces
4. **Infrastructure → Domain**: Infrastructure adapters convert persistence models to domain models

## Installation

Add the following dependency to your Maven `pom.xml`:

```xml
<dependency>
    <groupId>io.github.de-gupta</groupId>
    <artifactId>cleanCrud</artifactId>
    <version>0.0.4-SNAPSHOT</version>
</dependency>
```

## Usage

### Basic Implementation Steps

1. **Define Domain Models**:
   - Create domain models that implement `BaseDomainModel`
   - Define domain-specific validation rules

2. **Create Persistence Models**:
   - Implement JPA entities
   - Create adapters between domain and persistence models

3. **Implement ID Adapters**:
   - Create adapters to convert between domain IDs and persistence IDs

4. **Define Web Models**:
   - Create DTOs for web requests and responses
   - Implement adapters between web and domain models

5. **Configure Controllers**:
   - Extend the appropriate controller templates for your CRUD operations

### Example Implementation

A full-fledged example implementation is available in the companion repository cleanCrud-sampleImplementation.