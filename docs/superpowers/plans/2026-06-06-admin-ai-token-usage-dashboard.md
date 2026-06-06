# Admin AI Token Usage Dashboard Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build an admin-only React dashboard for viewing each user's AI token usage and managing monthly platform-AI token quotas.

**Architecture:** Keep user-facing AI usage APIs unchanged and add a separate admin API surface protected by Spring Security roles. Add a standalone `web/admin` Vite React app that calls those admin APIs with Bearer Token authentication. Enforce quotas only in the unified `AiModelGateway` path for platform-default AI calls, while continuing to record user-provider usage without blocking it.

**Tech Stack:** Spring Boot 3, Spring Security, JPA/Hibernate, PostgreSQL/H2 tests, OpenAPI, React, TypeScript, Vite, React Router, TanStack Query, Recharts.

---

## Context and Scope

The backend already records request-level AI usage in `ai_usage_logs` and exposes current-user endpoints under `/api/ai-usage/me/**`. This plan extends that foundation for administrators.

Reference ideas from Wei-Shaw/sub2api:

- Separate admin API and user API DTOs.
- Admin dashboard with usage trends, model statistics, and user rankings.
- Admin endpoints protected by an administrator role.

This is still an AI Interview Coach operational capability. Do not add billing, recharge, subscription plans, enterprise interview features, payment integrations, or request-prompt inspection.

## Files and Responsibilities

### Backend

- Modify `backend/src/main/java/com/interviewcoach/user/entity/User.java`
  - Add `role` and `monthlyTokenQuota`.
- Modify `backend/src/main/java/com/interviewcoach/common/security/JwtAuthenticationFilter.java`
  - Convert `User.role` into Spring Security authorities.
- Modify `backend/src/main/java/com/interviewcoach/common/security/SecurityConfig.java`
  - Restrict `/api/admin/**` to `ROLE_ADMIN`.
- Modify `backend/src/main/java/com/interviewcoach/auth/service/AuthService.java`
  - Promote configured admin usernames during login.
- Modify `backend/src/main/java/com/interviewcoach/ai/service/DefaultAiModelGateway.java`
  - Check platform-AI monthly quota before model calls in both `generateJson` and `generateEntity`.
- Create `backend/src/main/java/com/interviewcoach/admin/controller/AdminAiUsageController.java`
  - Admin usage overview, users list, and user detail endpoints.
- Create `backend/src/main/java/com/interviewcoach/admin/controller/AdminUserController.java`
  - User quota update endpoint.
- Create `backend/src/main/java/com/interviewcoach/admin/service/AdminAiUsageService.java`
  - Cross-user AI usage aggregation.
- Create `backend/src/main/java/com/interviewcoach/admin/service/AdminUserQuotaService.java`
  - Admin quota updates and quota DTO composition.
- Create `backend/src/main/java/com/interviewcoach/aiusage/service/AiTokenQuotaService.java`
  - Monthly quota checking for platform-default AI.
- Create DTOs under `backend/src/main/java/com/interviewcoach/common/api/`
  - `AdminAiUsageOverviewDto`
  - `AdminAiUsageUserRowDto`
  - `AdminAiUsageUserDetailDto`
  - `AdminAiUsageUsersPageDto`
  - `AdminTokenQuotaUpdateRequest`
  - `AdminTokenQuotaDto`
- Modify `backend/src/main/java/com/interviewcoach/aiusage/repository/AiUsageLogRepository.java`
  - Add admin aggregation queries.
- Modify `backend/src/main/java/com/interviewcoach/common/error/GlobalExceptionHandler.java`
  - Add `AI_TOKEN_QUOTA_EXCEEDED` as HTTP 429.
- Modify `backend/src/main/resources/application.yml`
  - Add `app.admin.usernames: ${IC_ADMIN_USERNAMES:}`.
- Modify `backend/src/test/resources/application-test.yml`
  - Add a test admin username list.
- Modify `docs/api/openapi.yaml`
  - Document all admin usage and quota APIs.
- Modify `CLAUDE.md`, `AGENTS.md`, and `docs/product/vibecoding-development-plan.md`
  - Explicitly allow `web/admin` as the React admin app directory.
  - Record this as a Post-MVP AI quality operations task, not a billing or enterprise feature.

### Frontend

- Create `web/admin/package.json`
  - Vite React scripts: `dev`, `build`, `test`.
- Create `web/admin/vite.config.ts`
  - Proxy `/api` to `http://localhost:8080`.
- Create `web/admin/src/main.tsx`
  - App bootstrap.
- Create `web/admin/src/App.tsx`
  - Routes and authenticated layout.
- Create `web/admin/src/api/http.ts`
  - Bearer Token handling and JSON error parsing.
- Create `web/admin/src/api/adminUsage.ts`
  - Typed admin usage/quota API client.
- Create `web/admin/src/auth/AuthStore.ts`
  - Token persistence in `localStorage`.
- Create `web/admin/src/pages/LoginPage.tsx`
  - Dev login flow for admin users.
- Create `web/admin/src/pages/UsageDashboardPage.tsx`
  - Dashboard page.
- Create `web/admin/src/components/`
  - KPI cards, usage trend chart, ranking charts, user table, user detail drawer, quota editor.
- Create `web/admin/src/styles.css`
  - Quiet operational UI styling.

## API Contract

All admin endpoints require `Authorization: Bearer <token>` and `ROLE_ADMIN`.

### `GET /api/admin/ai-usage/overview`

Query:

- `startDate`: optional `yyyy-MM-dd`
- `endDate`: optional `yyyy-MM-dd`

Response:

```json
{
  "totalUsers": 12,
  "activeUsers": 8,
  "quotaExceededUsers": 2,
  "summary": {
    "totalRequests": 120,
    "successfulRequests": 110,
    "failedRequests": 10,
    "estimatedRequests": 4,
    "totalInputTokens": 1000,
    "totalOutputTokens": 500,
    "totalCacheCreationTokens": 0,
    "totalCacheReadTokens": 0,
    "totalReasoningTokens": 0,
    "totalTokens": 1500
  },
  "daily": [],
  "topUsers": [],
  "topModels": [],
  "topTasks": [],
  "providers": []
}
```

### `GET /api/admin/ai-usage/users`

Query:

- `startDate`: optional `yyyy-MM-dd`
- `endDate`: optional `yyyy-MM-dd`
- `keyword`: optional username or email substring
- `page`: default `0`
- `size`: default `20`, max `100`
- `sort`: one of `totalTokensDesc`, `totalRequestsDesc`, `usernameAsc`, `createdAtDesc`

Response:

```json
{
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1,
  "items": [
    {
      "userId": "00000000-0000-0000-0000-000000000001",
      "username": "demo",
      "email": "demo@example.com",
      "role": "USER",
      "createdAt": "2026-06-06T00:00:00Z",
      "monthlyTokenQuota": 1000000,
      "currentMonthTokens": 120000,
      "remainingMonthlyTokens": 880000,
      "quotaExceeded": false,
      "lastUsedAt": "2026-06-06T01:00:00Z",
      "summary": {
        "totalRequests": 10,
        "successfulRequests": 9,
        "failedRequests": 1,
        "estimatedRequests": 0,
        "totalInputTokens": 100,
        "totalOutputTokens": 50,
        "totalCacheCreationTokens": 0,
        "totalCacheReadTokens": 0,
        "totalReasoningTokens": 0,
        "totalTokens": 150
      }
    }
  ]
}
```

### `GET /api/admin/ai-usage/users/{userId}`

Returns the selected user's profile, quota state, summary, daily points, and task/model/provider breakdowns.

### `PATCH /api/admin/users/{userId}/token-quota`

Request:

```json
{
  "monthlyTokenQuota": 1000000
}
```

Use `null` to remove the limit. Use `0` to block platform-default AI calls.

Response:

```json
{
  "userId": "00000000-0000-0000-0000-000000000001",
  "monthlyTokenQuota": 1000000,
  "currentMonthTokens": 120000,
  "remainingMonthlyTokens": 880000,
  "quotaExceeded": false
}
```

## Implementation Tasks

### Task 1: Add User Role and Admin Authentication

**Files:**

- Modify: `backend/src/main/java/com/interviewcoach/user/entity/User.java`
- Modify: `backend/src/main/java/com/interviewcoach/common/security/JwtAuthenticationFilter.java`
- Modify: `backend/src/main/java/com/interviewcoach/common/security/SecurityConfig.java`
- Modify: `backend/src/main/java/com/interviewcoach/auth/service/AuthService.java`
- Modify: `backend/src/main/resources/application.yml`
- Modify: `backend/src/test/resources/application-test.yml`
- Test: `backend/src/test/java/com/interviewcoach/auth/controller/AuthControllerTest.java`
- Test: `backend/src/test/java/com/interviewcoach/admin/AdminSecurityTest.java`

- [ ] Run GitNexus impact before editing:

```bash
rtk npx gitnexus impact --target User --direction upstream
rtk npx gitnexus impact --target JwtAuthenticationFilter --direction upstream
rtk npx gitnexus impact --target SecurityConfig --direction upstream
rtk npx gitnexus impact --target AuthService --direction upstream
```

- [ ] Write tests proving configured admin usernames receive `ROLE_ADMIN` and ordinary users cannot call `/api/admin/**`.

- [ ] Add `role` to `User`, defaulting to `USER` in `@PrePersist`.

- [ ] Add `app.admin.usernames` configuration and promote matching usernames during Dev Login and Apple Login.

- [ ] Update `JwtAuthenticationFilter` to create `SimpleGrantedAuthority("ROLE_" + user.getRole())`.

- [ ] Update `SecurityConfig` so `/api/admin/**` requires `hasRole("ADMIN")`.

- [ ] Run:

```bash
rtk mvn -q -Dtest=AuthControllerTest,AdminSecurityTest test
```

### Task 2: Add Admin Usage Aggregation APIs

**Files:**

- Create: `backend/src/main/java/com/interviewcoach/admin/controller/AdminAiUsageController.java`
- Create: `backend/src/main/java/com/interviewcoach/admin/service/AdminAiUsageService.java`
- Create DTOs under `backend/src/main/java/com/interviewcoach/common/api/`
- Modify: `backend/src/main/java/com/interviewcoach/aiusage/repository/AiUsageLogRepository.java`
- Test: `backend/src/test/java/com/interviewcoach/admin/AdminAiUsageControllerTest.java`

- [ ] Run GitNexus impact before editing:

```bash
rtk npx gitnexus impact --target AiUsageLogRepository --direction upstream
```

- [ ] Write tests for overview, user list, user detail, date filters, keyword filters, and non-admin 403.

- [ ] Implement admin DTOs with camelCase JSON fields only.

- [ ] Implement aggregation using `ai_usage_logs` and `users`; do not return prompt, completion, request headers, API keys, or raw AI payload.

- [ ] Add OpenAPI schemas and paths.

- [ ] Run:

```bash
rtk mvn -q -Dtest=AdminAiUsageControllerTest,AiUsageControllerTest test
```

### Task 3: Add Monthly Token Quota Management

**Files:**

- Modify: `backend/src/main/java/com/interviewcoach/user/entity/User.java`
- Create: `backend/src/main/java/com/interviewcoach/admin/controller/AdminUserController.java`
- Create: `backend/src/main/java/com/interviewcoach/admin/service/AdminUserQuotaService.java`
- Create: `backend/src/main/java/com/interviewcoach/aiusage/service/AiTokenQuotaService.java`
- Modify: `backend/src/main/java/com/interviewcoach/ai/service/DefaultAiModelGateway.java`
- Modify: `backend/src/main/java/com/interviewcoach/common/error/GlobalExceptionHandler.java`
- Test: `backend/src/test/java/com/interviewcoach/admin/AdminUserQuotaControllerTest.java`
- Test: `backend/src/test/java/com/interviewcoach/aiusage/AiTokenQuotaServiceTest.java`
- Test: `backend/src/test/java/com/interviewcoach/ai/DefaultAiModelGatewayTest.java`

- [ ] Run GitNexus impact before editing:

```bash
rtk npx gitnexus impact --target DefaultAiModelGateway --direction upstream
rtk npx gitnexus impact --target GlobalExceptionHandler --direction upstream
```

- [ ] Write failing tests:
  - Admin can set quota.
  - `null` quota means unlimited.
  - `0` quota blocks platform-default AI.
  - Platform-default AI over quota returns 429 and does not call model client.
  - User-provider AI remains allowed and usage is still recorded.

- [ ] Add `monthlyTokenQuota` to `User`.

- [ ] Implement `AiTokenQuotaService` using current calendar month boundaries in the server timezone.

- [ ] Call quota check in `DefaultAiModelGateway.generateJson` and `generateEntity` after provider resolution and before calling the model.

- [ ] Add `AiTokenQuotaExceededException` and map it to `AI_TOKEN_QUOTA_EXCEEDED` with HTTP 429.

- [ ] Run:

```bash
rtk mvn -q -Dtest=AdminUserQuotaControllerTest,AiTokenQuotaServiceTest,DefaultAiModelGatewayTest test
```

### Task 4: Create React Admin App Shell

**Files:**

- Create: `web/admin/package.json`
- Create: `web/admin/vite.config.ts`
- Create: `web/admin/tsconfig.json`
- Create: `web/admin/index.html`
- Create: `web/admin/src/main.tsx`
- Create: `web/admin/src/App.tsx`
- Create: `web/admin/src/styles.css`
- Create: `web/admin/src/auth/AuthStore.ts`
- Create: `web/admin/src/api/http.ts`
- Create: `web/admin/src/pages/LoginPage.tsx`

- [ ] Add Vite React TypeScript app under `web/admin`.

- [ ] Configure `/api` proxy to `http://localhost:8080`.

- [ ] Implement login page using `POST /api/auth/dev-login`.

- [ ] Store token in `localStorage` as `interviewCoachAdminToken`.

- [ ] Implement 401 handling that clears token and returns to login.

- [ ] Run:

```bash
cd web/admin && rtk npm install
cd web/admin && rtk npm run build
```

### Task 5: Build Usage Dashboard UI

**Files:**

- Create: `web/admin/src/api/adminUsage.ts`
- Create: `web/admin/src/pages/UsageDashboardPage.tsx`
- Create: `web/admin/src/components/KpiCard.tsx`
- Create: `web/admin/src/components/UsageTrendChart.tsx`
- Create: `web/admin/src/components/UsageRankingChart.tsx`
- Create: `web/admin/src/components/UserUsageTable.tsx`
- Create: `web/admin/src/components/UserUsageDrawer.tsx`
- Create: `web/admin/src/components/QuotaEditor.tsx`

- [ ] Implement typed API calls for overview, users list, user detail, and quota update.

- [ ] Build a dense operational dashboard:
  - KPI cards.
  - Daily token trend.
  - Model, task, and provider rankings.
  - User usage table with search, date filter, pagination, and sorting.
  - User detail drawer with quota editor.

- [ ] Use symbols/icons for refresh, search, close, and save controls where available.

- [ ] Do not add marketing copy, landing pages, decorative backgrounds, or unrelated product surfaces.

- [ ] Run:

```bash
cd web/admin && rtk npm run build
```

### Task 6: Documentation and Final Regression

**Files:**

- Modify: `CLAUDE.md`
- Modify: `AGENTS.md`
- Modify: `docs/product/vibecoding-development-plan.md`
- Modify: `docs/api/openapi.yaml`

- [ ] Update directory rules to explicitly allow `web/admin`.

- [ ] Document this as Post-MVP AI quality operations and cost governance.

- [ ] Re-state that this is not subscription billing, payments, enterprise interview, or quota monetization.

- [ ] Run GitNexus changed-scope check:

```bash
rtk npx gitnexus detect-changes
```

- [ ] Run backend regression:

```bash
cd backend && rtk mvn -q test
```

- [ ] Run frontend build:

```bash
cd web/admin && rtk npm run build
```

## Acceptance Criteria

- Admin can log in and view global AI token usage.
- Admin can see each user's token usage, quota, remaining quota, and breakdowns.
- Admin can set, clear, or zero a user's monthly platform AI quota.
- Ordinary users cannot call admin APIs.
- Current-user `/api/ai-usage/me/**` behavior remains unchanged.
- Platform-default AI is blocked after quota is reached.
- User OpenAI-compatible Provider usage is still recorded but not blocked by quota.
- No prompt, completion, resume raw text, answer raw text, API key, Authorization header, or full request headers are returned or logged.

## Known Non-Goals

- No billing, recharge, subscription, payment, invoice, balance, or pricing multiplier.
- No enterprise interview/admin hiring workflow.
- No request-level raw prompt/completion inspection.
- No usage cleanup task in v1.
- No separate production migration framework unless the project adopts one later; current backend uses Hibernate `ddl-auto`.
