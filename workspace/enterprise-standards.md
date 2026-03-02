# Enterprise Architectural Standards

This document defines the core patterns and standards that all systems in our organization should follow.

## 1. Service Boundaries
- Every service must own its own data. No direct database sharing between services.
- Communication between services should be **Asynchronous** (via Event Bus) for high scale, or **Sync** (REST/gRPC) for immediate consistency.

## 2. API First Design
- APIs must be designed and documented (OpenAPI/Swagger) before implementation.
- Versioning is mandatory for all public-facing interfaces.

## 3. Tool & Agent Governance
- All autonomous agents must use the **Embabel** framework.
- Tools must be exposed via **MCP (Model Context Protocol)** servers.
- Every agentic action must be logged for auditability (via `AuditService`).

## 4. Observability
- All services must expose a `/health` endpoint.
- Distributed tracing (Span IDs) must be propagated through all service calls.

## 5. Decision Records (ADR)
- Any significant change to a service's architecture must be documented in an ADR.
- ADRs should be reviewed by the Architect Assistant (this bot) before reaching human architects.
