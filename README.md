# Codebase Q&A

> A shared, always-up-to-date codebase knowledge base for engineering teams. No local clone required — anyone on the team can ask questions about any connected repository from a browser and get accurate, grounded answers with file/line references.

## 🚀 Quick Links

- **[Project Overview](01-README.md)** - Complete project description and features
- **[Architecture](02-architecture.md)** - System design and data flow
- **[Build Plan](09-build-plan.md)** - Step-by-step implementation guide

## 📁 Project Structure

```
codebase-qa/
├── backend/                 # Spring Boot backend
├── frontend/                # React frontend (to be created)
├── docs/                    # All documentation files
├── docker-compose.yml       # Local development infrastructure
└── README.md               # This file
```

## 🛠️ Tech Stack

### Backend
- Java 21 + Spring Boot 3.2
- PostgreSQL 15 + pgvector
- AWS SQS, Redis (Upstash)
- Google Gemini API
- JGit, tree-sitter

### Frontend
- React 18 + TypeScript
- Vite, TanStack Query, Zustand
- Tailwind CSS

## 🏃 Getting Started

### Prerequisites
- Java 21+
- Maven 3.8+
- Node.js 18+
- Docker & Docker Compose

### Backend Setup

```bash
# Start infrastructure
docker-compose up -d

# Configure environment
cd backend
cp .env.example .env
# Edit .env with your credentials

# Run backend
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

See [backend/README.md](backend/README.md) for detailed instructions.

### Frontend Setup

```bash
cd frontend
npm install
npm run dev
```

(Frontend will be created in Task 1.5)

## 📚 Documentation

All documentation is in the root directory:

- `01-README.md` - Project overview
- `02-architecture.md` - System architecture
- `03-database-schema.md` - Database design
- `04-api-specification.md` - API endpoints
- `05-backend-guide-part*.md` - Backend implementation
- `06-frontend-guide-part*.md` - Frontend implementation
- `07-infrastructure.md` - Deployment guide
- `08-configuration.md` - Environment variables
- `09-build-plan.md` - Sprint breakdown
- `10-design-system.md` - UI design system

## 🎯 Current Status

**Sprint 1, Task 1.1: Project Initialization** ✅ COMPLETED

- [x] Spring Boot project structure created
- [x] Maven dependencies configured
- [x] Application configuration files created
- [x] Package structure established
- [x] Docker Compose for local development
- [x] Environment configuration templates

**Next:** Task 1.2 - Database Schema + Flyway Migrations

## 📝 License

This is a portfolio project for demonstration purposes.

## 👤 Author

Built following the comprehensive design documents in this repository.
