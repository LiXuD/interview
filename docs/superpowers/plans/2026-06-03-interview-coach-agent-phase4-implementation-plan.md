# InterviewCoachAgent Phase 4 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a persistent, event-driven `InterviewCoachAgent` for every `userId + targetId`, using cloud real AI as its reasoning engine and controlled backend tools to coordinate assessment, training, mock interview, progress, and coaching memory.

**Architecture:** Persist Agent state separately from business facts, `CoachingMemory`, and `Progress`. Persist idempotent `CoachEvent` records inside business transactions, process them after commit, and let a bounded Agent runner call `AiModelGateway` for structured decisions. Use a two-pass, whitelist-only tool loop, then apply validated memory updates and future training task adjustments without exposing raw model output to iOS.

**Tech Stack:** Spring Boot 3.5, Java 17, Spring Data JPA, Spring Security, Spring AI through `AiModelGateway`, Micrometer, PostgreSQL/H2, OpenAPI, SwiftUI iOS 17+, async/await, Codable DTO.

---

## 0. Approved Inputs And Non-Negotiable Constraints

Implement against:

- `docs/superpowers/specs/2026-06-03-interview-coach-agent-design.md`
- `docs/product/vibecoding-development-plan.md` Phase 4 Task 34-41
- `CLAUDE.md`
- `AGENTS.md`

Hard constraints:

- The product Agent uses cloud real AI. Do not introduce a local model.
- `LocalPlatformAiClient` may only provide deterministic output for default unit/CI tests; it is not product acceptance evidence.
- iOS never calls a model directly and never parses raw AI text.
- Agent data is isolated by current authenticated user and target.
- Agent state must not contain resume source text, user answer source text, API keys, Authorization headers, prompt/completion, or hidden chain-of-thought.
- `CoachingMemory` remains the source of long-term trust semantics. `inferred` is not fact; `rejected` never re-enters fact context.
- Assessment, Training, MockInterview, Report, and Progress remain authoritative business facts.
- Agent failures must not roll back completed business facts.
- Mock interview context remains limited to the latest 6 rounds / 12 messages.
- Run GitNexus impact analysis before editing every existing symbol and `gitnexus_detect_changes(scope="staged")` before every commit.
- Use `rtk` for every shell command.
- Implement and commit one small slice at a time. Do not implement multiple Phase 4 tasks in one commit.

## 1. Locked Architecture Decisions

### 1.1 Persistent Agent Identity

`InterviewCoachAgent` is created when a target is created and is unique by `user_id + target_id`.

```text
Agent = current coaching goal + focus dimensions + next action + last decision
CoachingMemory = long-term trusted understanding
Progress = structured score and trend aggregation
Business entities = authoritative completed work
```

### 1.2 Persistent Event Record, Not Memory-Only Events

Business services write an idempotent `CoachEvent` record in the same transaction as the completed fact. After commit, an Agent dispatcher processes the event. This prevents AI failures from rolling back assessment, training, mock interview, or correction data and leaves a retryable audit record.

### 1.3 Async User Execution Context

The Agent runner executes after the HTTP request and therefore cannot rely on the request thread's `SecurityContext`. It must load the event owner and establish a temporary, tightly scoped Spring Security authentication context before calling `AiModelGateway`, then restore or clear the previous context in `finally`.

This is required so the Agent uses the correct user's OpenAI-compatible cloud Provider when one is configured.

### 1.4 Controlled Two-Pass Tool Loop

The Agent may use at most:

- 2 model calls per event.
- 3 whitelist tool calls per event.
- 3 event processing attempts.

Pass 1 may request read-only tools. Pass 2 receives low-risk structured tool summaries and must return a final decision with no further tool requests. Unknown tools, unsupported operations, and calls over budget are rejected.

### 1.5 Decision-Carried Updates

Task 39 extends the final Agent decision with optional structured memory updates and future pending training task replacements. This lets Agent reasoning, next recommendation, and memory/plan updates share one goal and avoids separate unconscious AI calls for the same completed fact.

## 2. File And Responsibility Map

### New Backend Agent Module

| File | Responsibility |
|------|----------------|
| `backend/src/main/java/com/interviewcoach/agent/entity/InterviewCoachAgent.java` | Persistent Agent identity and current state |
| `backend/src/main/java/com/interviewcoach/agent/entity/CoachEvent.java` | Persistent idempotent event and retry status |
| `backend/src/main/java/com/interviewcoach/agent/repository/InterviewCoachAgentRepository.java` | User/target-scoped Agent queries and deletion |
| `backend/src/main/java/com/interviewcoach/agent/repository/CoachEventRepository.java` | Event lookup, claim, retry, and deletion |
| `backend/src/main/java/com/interviewcoach/agent/service/InterviewCoachAgentService.java` | Create/query/update Agent state |
| `backend/src/main/java/com/interviewcoach/agent/service/CoachEventService.java` | Record idempotent events in business transactions |
| `backend/src/main/java/com/interviewcoach/agent/service/InterviewCoachAgentRunner.java` | Bounded Agent reasoning and state application |
| `backend/src/main/java/com/interviewcoach/agent/service/AgentDecisionApplicationService.java` | Atomically apply final decision, updates, and event completion |
| `backend/src/main/java/com/interviewcoach/agent/service/AgentContextService.java` | Build low-risk fact snapshot without raw answers |
| `backend/src/main/java/com/interviewcoach/agent/service/AgentPromptFactory.java` | Build Agent decision prompt contract |
| `backend/src/main/java/com/interviewcoach/agent/service/AgentDecisionValidator.java` | Validate dimensions, actions, tools, updates, and stage compatibility |
| `backend/src/main/java/com/interviewcoach/agent/service/AgentStageResolver.java` | Derive current stage from business facts in code |
| `backend/src/main/java/com/interviewcoach/agent/service/AgentDeterministicDecisionPolicy.java` | Resolve obvious prerequisite actions without model calls |
| `backend/src/main/java/com/interviewcoach/agent/service/AgentUserExecutionContext.java` | Establish and clean async user SecurityContext |
| `backend/src/main/java/com/interviewcoach/agent/service/CoachEventDispatcher.java` | AFTER_COMMIT async dispatch |
| `backend/src/main/java/com/interviewcoach/agent/service/CoachEventRetryScheduler.java` | Retry failed/pending events within attempt budget |
| `backend/src/main/java/com/interviewcoach/agent/service/AgentMetrics.java` | Low-risk Agent run and tool metrics |
| `backend/src/main/java/com/interviewcoach/agent/config/AgentRuntimeConfig.java` | Bounded executor, async, and scheduling configuration |
| `backend/src/main/java/com/interviewcoach/agent/config/AgentRuntimeProperties.java` | Model/tool/attempt/freshness budgets |
| `backend/src/main/java/com/interviewcoach/agent/controller/InterviewCoachAgentController.java` | Target-scoped query and app-session-start API |
| `backend/src/main/java/com/interviewcoach/agent/model/AgentContextSnapshot.java` | Internal low-risk context passed to prompt factory |
| `backend/src/main/java/com/interviewcoach/agent/model/AgentToolContext.java` | Internal target/user/event context for tools |
| `backend/src/main/java/com/interviewcoach/agent/model/AgentToolResult.java` | Internal structured tool result |
| `backend/src/main/java/com/interviewcoach/agent/tool/CoachAgentTool.java` | Whitelist tool contract |
| `backend/src/main/java/com/interviewcoach/agent/tool/CoachAgentToolRegistry.java` | Tool lookup and rejection of unknown calls |
| `backend/src/main/java/com/interviewcoach/agent/tool/*.java` | Six focused read-only tool implementations |

### New Shared Backend DTOs

| File | Responsibility |
|------|----------------|
| `backend/src/main/java/com/interviewcoach/common/api/InterviewCoachAgentDto.java` | API response for persistent Agent state |
| `backend/src/main/java/com/interviewcoach/common/api/AgentRecommendedActionDto.java` | Structured next action type and title |
| `backend/src/main/java/com/interviewcoach/common/api/AgentToolCallDto.java` | Structured whitelist tool request |
| `backend/src/main/java/com/interviewcoach/common/api/AgentDecisionDto.java` | Structured cloud AI decision |
| `backend/src/main/java/com/interviewcoach/common/api/AgentMemoryUpdateDto.java` | Optional decision-carried coaching memory update |
| `backend/src/main/java/com/interviewcoach/common/api/AgentTrainingPlanAdjustmentDto.java` | Optional decision-carried plan adjustment |
| `backend/src/main/java/com/interviewcoach/common/api/AgentTrainingTaskAdjustmentDto.java` | Replacement content for one pending task |

### Existing Backend Files To Modify

| File | Planned Change |
|------|----------------|
| `backend/src/main/java/com/interviewcoach/ai/service/AiPrompt.java` | Add `agentDecision` task constant |
| `backend/src/main/java/com/interviewcoach/ai/service/DefaultAiModelGateway.java` | Require real AI for Agent decision |
| `backend/src/main/java/com/interviewcoach/ai/service/AiStructuredOutputService.java` | Generate and validate `AgentDecisionDto` |
| `backend/src/main/java/com/interviewcoach/ai/service/LocalPlatformAiClient.java` | Deterministic test-only Agent decision JSON |
| `backend/src/main/java/com/interviewcoach/target/service/InterviewTargetService.java` | Create/delete Agent and record target event |
| `backend/src/main/java/com/interviewcoach/profile/service/CandidateProfileService.java` | Record summary-confirmed event |
| `backend/src/main/java/com/interviewcoach/assessment/service/AssessmentService.java` | Record assessment-completed event; later remove direct memory AI call |
| `backend/src/main/java/com/interviewcoach/training/service/TrainingService.java` | Record training events; later apply Agent plan adjustments and remove direct memory AI call |
| `backend/src/main/java/com/interviewcoach/mockinterview/service/MockInterviewService.java` | Record mock-completed event; later remove direct memory AI call |
| `backend/src/main/java/com/interviewcoach/coachingmemory/service/CoachingMemoryService.java` | Record correction event and save decision-carried memory |
| `backend/src/main/java/com/interviewcoach/auth/service/AuthService.java` | Delete Agent and events before user deletion |
| `backend/src/main/resources/application.yml` | Agent runtime budgets and retry interval |
| `backend/src/test/resources/application-test.yml` | Deterministic synchronous Agent dispatcher for default tests |
| `backend/src/test/resources/application-live-ai-test.yml` | Explicit live Agent runtime settings |
| `docs/api/openapi.yaml` | Agent endpoints and schemas |
| `docs/ai/prompt-contracts.md` | `agentDecision` prompt contract |
| `docs/ai/provider-contracts.md` | Cloud real AI and Agent observability rules |
| `docs/privacy/data-policy.md` | Agent state/event/tool privacy inventory |
| `README.md` | Agent runtime and live acceptance commands |

### New iOS Files

| File | Responsibility |
|------|----------------|
| `ios/InterviewCoach/InterviewCoach/Core/API/DTO/InterviewCoachAgentDTO.swift` | Decode Agent API response |
| `ios/InterviewCoach/InterviewCoach/Features/CoachAgent/CoachAgentView.swift` | Persistent coach state and action entry |

### Existing iOS Files To Modify

| File | Planned Change |
|------|----------------|
| `ios/InterviewCoach/InterviewCoach/Features/Targets/TargetDetailView.swift` | Add persistent coach entry and destinations |
| `ios/InterviewCoach/InterviewCoach.xcodeproj/project.pbxproj` | Register new Swift files |

## 3. Task 34: Agent Identity, State, And Query API

### Task 34A: Persist Agent Identity

**Files:**

- Create: `backend/src/main/java/com/interviewcoach/agent/entity/InterviewCoachAgent.java`
- Create: `backend/src/main/java/com/interviewcoach/agent/repository/InterviewCoachAgentRepository.java`
- Test: `backend/src/test/java/com/interviewcoach/agent/InterviewCoachAgentRepositoryTest.java`

- [ ] **Step 1: Run required GitNexus impact checks before editing existing symbols**

Use `gitnexus_impact` with `direction="upstream"` for:

```text
InterviewTargetService
AuthService
```

Expected: review direct callers and warn before implementation if risk is HIGH or CRITICAL.

- [ ] **Step 2: Write the failing repository test**

```java
@SpringBootTest
@ActiveProfiles("test")
class InterviewCoachAgentRepositoryTest {
    @Autowired InterviewCoachAgentRepository agentRepository;
    @Autowired InterviewTargetRepository targetRepository;
    @Autowired UserRepository userRepository;

    @Test
    void userAndTargetAreUnique() {
        User user = saveUser("agent_unique_user");
        InterviewTarget target = saveTarget(user, "Java Backend");

        agentRepository.saveAndFlush(agent(user, target));

        assertThrows(DataIntegrityViolationException.class,
                () -> agentRepository.saveAndFlush(agent(user, target)));
    }
}
```

Add explicit fixture helpers in the same test:

```java
private User saveUser(String username) {
    User user = new User();
    user.setUsername(username);
    return userRepository.save(user);
}

private InterviewTarget saveTarget(User user, String title) {
    InterviewTarget target = new InterviewTarget();
    target.setUser(user);
    target.setTitle(title);
    target.setJd("Test JD");
    return targetRepository.save(target);
}

private InterviewCoachAgent agent(User user, InterviewTarget target) {
    InterviewCoachAgent agent = new InterviewCoachAgent();
    agent.setUser(user);
    agent.setTarget(target);
    return agent;
}
```

- [ ] **Step 3: Run the test to verify it fails**

Run:

```bash
rtk mvn -q -f backend/pom.xml -Dtest=InterviewCoachAgentRepositoryTest test
```

Expected: FAIL because `InterviewCoachAgent` and its repository do not exist.

- [ ] **Step 4: Create the minimal entity and repository**

Implement the entity with a database uniqueness constraint and optimistic version:

```java
@Entity
@Table(
        name = "interview_coach_agents",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_interview_coach_agent_user_target",
                columnNames = {"user_id", "target_id"}))
public class InterviewCoachAgent {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "target_id", nullable = false)
    private InterviewTarget target;

    @Column(nullable = false)
    private String status = "active";

    @Column(nullable = false)
    private String currentStage = "targetSetup";

    @Column(nullable = false, columnDefinition = "TEXT")
    private String currentGoal = "确认候选人摘要并建立面试准备基线";

    @ElementCollection
    @CollectionTable(name = "interview_coach_agent_focus_dimensions",
            joinColumns = @JoinColumn(name = "agent_id"))
    @OrderColumn(name = "sort_order")
    @Column(name = "dimension", nullable = false)
    private List<String> activeFocusDimensions = new ArrayList<>();

    @Column(nullable = false)
    private String nextRecommendedActionType = "confirmProfile";

    @Column(nullable = false, columnDefinition = "TEXT")
    private String nextRecommendedActionTitle = "确认候选人摘要";

    @Column
    private String lastEventType;

    @Column(columnDefinition = "TEXT")
    private String lastDecisionSummary;

    @Column(nullable = false)
    private String lastRunOutcome = "never";

    @Column
    private String lastErrorType;

    @Column
    private Instant lastRunAt;

    @Version
    private long version;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;
}
```

Add `@PrePersist` / `@PreUpdate` timestamps and explicit getters/setters following the existing entity style. Do not use Lombok.

Repository contract:

```java
public interface InterviewCoachAgentRepository extends JpaRepository<InterviewCoachAgent, UUID> {
    Optional<InterviewCoachAgent> findByTargetIdAndUserId(UUID targetId, UUID userId);
    void deleteByTargetId(UUID targetId);
    void deleteByUserId(UUID userId);
}
```

- [ ] **Step 5: Run the repository test**

Run:

```bash
rtk mvn -q -f backend/pom.xml -Dtest=InterviewCoachAgentRepositoryTest test
```

Expected: PASS.

- [ ] **Step 6: Stage, run GitNexus change detection, and commit Task 34A**

Run:

```bash
rtk git add backend/src/main/java/com/interviewcoach/agent/entity/InterviewCoachAgent.java backend/src/main/java/com/interviewcoach/agent/repository/InterviewCoachAgentRepository.java backend/src/test/java/com/interviewcoach/agent/InterviewCoachAgentRepositoryTest.java
```

Then run `gitnexus_detect_changes(scope="staged")`.

Commit:

```bash
rtk git commit -m "feat(agent): 持久化面试教练 Agent 身份" \
  -m "为每个用户目标岗位建立唯一且可乐观锁保护的 InterviewCoachAgent 状态实体。" \
  -m "Agent-Task: Task 34 Agent 身份、状态与查询 API" \
  -m "Agent-Model: gpt-5" \
  -m "Agent-Decision: 使用 userId + targetId 唯一约束并将 Agent 状态与业务事实分离。" \
  -m "Agent-Limitation: 尚未提供查询 API 或事件驱动决策。"
```

### Task 34B: Create, Query, And Delete Agent State

**Files:**

- Create: `backend/src/main/java/com/interviewcoach/common/api/AgentRecommendedActionDto.java`
- Create: `backend/src/main/java/com/interviewcoach/common/api/InterviewCoachAgentDto.java`
- Create: `backend/src/main/java/com/interviewcoach/agent/service/InterviewCoachAgentService.java`
- Create: `backend/src/main/java/com/interviewcoach/agent/controller/InterviewCoachAgentController.java`
- Modify: `backend/src/main/java/com/interviewcoach/target/service/InterviewTargetService.java`
- Modify: `backend/src/main/java/com/interviewcoach/auth/service/AuthService.java`
- Modify: `docs/api/openapi.yaml`
- Test: `backend/src/test/java/com/interviewcoach/agent/InterviewCoachAgentControllerTest.java`
- Test: `backend/src/test/java/com/interviewcoach/target/controller/TargetControllerTest.java`
- Test: `backend/src/test/java/com/interviewcoach/auth/controller/AuthControllerTest.java`

- [ ] **Step 1: Write failing API and lifecycle tests**

The controller test must prove persistence, camelCase DTO shape, authentication, and cross-user isolation:

```java
@Test
void targetCreationCreatesPersistentAgentForOwnerOnly() throws Exception {
    String ownerToken = loginAndGetToken("agent_owner");
    String targetId = createTarget(ownerToken);

    mockMvc.perform(get("/api/targets/" + targetId + "/coach-agent")
                    .header("Authorization", "Bearer " + ownerToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.targetId").value(targetId))
            .andExpect(jsonPath("$.status").value("active"))
            .andExpect(jsonPath("$.currentStage").value("targetSetup"))
            .andExpect(jsonPath("$.nextRecommendedAction.type").value("confirmProfile"))
            .andExpect(jsonPath("$.nextRecommendedAction.title").isString())
            .andExpect(jsonPath("$.activeFocusDimensions").isArray())
            .andExpect(jsonPath("$.lastRunOutcome").value("never"))
            .andExpect(jsonPath("$.version").isNumber())
            .andExpect(jsonPath("$.prompt").doesNotExist())
            .andExpect(jsonPath("$.completion").doesNotExist());

    String otherToken = loginAndGetToken("agent_other");
    mockMvc.perform(get("/api/targets/" + targetId + "/coach-agent")
                    .header("Authorization", "Bearer " + otherToken))
            .andExpect(status().isNotFound());
}
```

Extend deletion tests to assert the repository has no Agent after target deletion and account deletion.

- [ ] **Step 2: Run tests to verify they fail**

Run:

```bash
rtk mvn -q -f backend/pom.xml -Dtest=InterviewCoachAgentControllerTest,TargetControllerTest,AuthControllerTest test
```

Expected: FAIL because the API, DTOs, and lifecycle integration do not exist.

- [ ] **Step 3: Add DTOs**

```java
public record AgentRecommendedActionDto(String type, String title) {
}
```

```java
public record InterviewCoachAgentDto(
        String id,
        String targetId,
        String status,
        String currentStage,
        String currentGoal,
        List<String> activeFocusDimensions,
        AgentRecommendedActionDto nextRecommendedAction,
        String lastEventType,
        String lastDecisionSummary,
        String lastRunOutcome,
        String lastErrorType,
        String lastRunAt,
        long version,
        String createdAt,
        String updatedAt
) {
}
```

- [ ] **Step 4: Implement service and controller**

Service methods:

```java
@Transactional
public InterviewCoachAgent createForTarget(User user, InterviewTarget target)

@Transactional(readOnly = true)
public InterviewCoachAgentDto getByTarget(UUID targetId, UUID userId)

@Transactional
public void deleteByTarget(UUID targetId)

@Transactional
public void deleteByUser(UUID userId)
```

Controller:

```java
@RestController
@RequestMapping("/api/targets/{targetId}/coach-agent")
public class InterviewCoachAgentController {
    @GetMapping
    public InterviewCoachAgentDto getAgent(@PathVariable UUID targetId) {
        return agentService.getByTarget(targetId, SecurityUtils.currentUser().getId());
    }
}
```

`getByTarget` must first validate target ownership with `InterviewTargetRepository.findByIdAndUserId(...)`; do not reveal whether another user's Agent exists.

- [ ] **Step 5: Integrate creation and deletion lifecycle**

In `InterviewTargetService.createTarget(...)`, create the Agent immediately after saving the target:

```java
target = targetRepository.save(target);
agentService.createForTarget(user, target);
return toDto(target);
```

Delete Agent data before deleting target or user:

```java
agentRepository.deleteByTargetId(targetId);
```

```java
agentRepository.deleteByUserId(userId);
```

Task 35A adds persistent events. From that point onward, deletion order must become:

```text
CoachEvent -> InterviewCoachAgent -> InterviewTarget / User
```

- [ ] **Step 6: Add OpenAPI path and schemas**

Add:

```yaml
/api/targets/{targetId}/coach-agent:
  get:
    tags: [CoachAgent]
    summary: Get persistent interview coach Agent state for a target
    operationId: getInterviewCoachAgent
```

Define `InterviewCoachAgentDto` and `AgentRecommendedActionDto` with required camelCase fields.

- [ ] **Step 7: Run focused tests and full backend tests**

Run:

```bash
rtk mvn -q -f backend/pom.xml -Dtest=InterviewCoachAgentControllerTest,TargetControllerTest,AuthControllerTest test
rtk mvn -q -f backend/pom.xml test
```

Expected: PASS.

- [ ] **Step 8: Stage, detect changes, and commit Task 34B**

Use `gitnexus_detect_changes(scope="staged")` before commit.

Commit summary:

```text
feat(agent): 提供持续教练状态查询 API
```

## 4. Task 35: CoachEvent, AgentDecision, And Event Runner

### Task 35A: Persist Idempotent Coach Events

**Files:**

- Create: `backend/src/main/java/com/interviewcoach/agent/entity/CoachEvent.java`
- Create: `backend/src/main/java/com/interviewcoach/agent/repository/CoachEventRepository.java`
- Create: `backend/src/main/java/com/interviewcoach/agent/service/CoachEventService.java`
- Modify: `backend/src/main/java/com/interviewcoach/target/service/InterviewTargetService.java`
- Modify: `backend/src/main/java/com/interviewcoach/auth/service/AuthService.java`
- Test: `backend/src/test/java/com/interviewcoach/agent/CoachEventServiceTest.java`
- Test: `backend/src/test/java/com/interviewcoach/target/controller/TargetControllerTest.java`
- Test: `backend/src/test/java/com/interviewcoach/auth/controller/AuthControllerTest.java`

- [ ] **Step 1: Run required GitNexus impact checks**

Use `gitnexus_impact` for:

```text
InterviewTargetService.deleteTarget
AuthService.deleteUser
```

- [ ] **Step 2: Write failing idempotency and deletion tests**

```java
@Test
void recordingSameSourceEventTwiceReturnsOnePersistentEvent() {
    CoachEvent first = eventService.recordEvent(
            user, targetId, "ASSESSMENT_COMPLETED", "assessment", sourceId);
    CoachEvent second = eventService.recordEvent(
            user, targetId, "ASSESSMENT_COMPLETED", "assessment", sourceId);

    assertEquals(first.getId(), second.getId());
    assertEquals(1, eventRepository.count());
}
```

Also verify:

- No raw payload field exists.
- Cross-user target recording is rejected.
- Two identical correction discriminators produce one event.
- Different correction discriminators for the same memory produce distinct events.
- Target deletion and account deletion remove events before Agents.

- [ ] **Step 3: Run the test to verify it fails**

```bash
rtk mvn -q -f backend/pom.xml -Dtest=CoachEventServiceTest,TargetControllerTest,AuthControllerTest test
```

Expected: FAIL because event types do not exist.

- [ ] **Step 4: Implement event entity and repository**

Persist only identifiers and low-risk status:

```java
@Entity
@Table(
        name = "coach_events",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_coach_event_idempotency_key",
                columnNames = "idempotency_key"))
public class CoachEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "agent_id", nullable = false)
    private InterviewCoachAgent agent;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "target_id", nullable = false)
    private UUID targetId;

    @Column(nullable = false)
    private String eventType;

    @Column(nullable = false)
    private String sourceType;

    @Column(nullable = false)
    private UUID sourceId;

    @Column(name = "idempotency_key", nullable = false)
    private String idempotencyKey;

    @Column(nullable = false)
    private String status = "pending";

    @Column(nullable = false)
    private int attemptCount;

    @Column
    private String lastErrorType;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column
    private Instant processedAt;
}
```

Add `@PrePersist` timestamp initialization and explicit getters/setters. Do not add a payload column.

Repository methods:

```java
Optional<CoachEvent> findByIdempotencyKey(String idempotencyKey);
List<CoachEvent> findTop20ByStatusInAndAttemptCountLessThanOrderByCreatedAtAsc(
        Collection<String> statuses, int maxAttempts);

@Modifying
@Query("""
        update CoachEvent e
           set e.status = 'processing',
               e.attemptCount = e.attemptCount + 1
         where e.id = :id
           and e.status in ('pending', 'failed')
        """)
int claimForProcessing(@Param("id") UUID id);

void deleteByTargetId(UUID targetId);
void deleteByUserId(UUID userId);
```

- [ ] **Step 5: Implement event recording**

`CoachEventService.recordEvent(...)` must:

1. Validate target ownership.
2. Load the existing Agent.
3. Build a SHA-256 idempotency key from an explicit discriminator.
4. Return an existing event when the key already exists.
5. Save no event payload and no user source text.

Use stable discriminators:

```text
Normal completed fact: eventType + ":" + sourceType + ":" + sourceId
Memory correction: eventType + ":" + memoryId + ":" + field + ":" + itemIndex + ":" + source + ":" + content
App session start: eventType + ":" + targetId + ":" + sixHourTimeBucket
```

Only the SHA-256 digest is persisted as `idempotencyKey`; correction content is never persisted in the event.

Provide two explicit entry points so ordinary facts cannot accidentally use a random key:

```java
public CoachEvent recordEvent(
        User user, UUID targetId, String eventType, String sourceType, UUID sourceId)

public CoachEvent recordEvent(
        User user, UUID targetId, String eventType, String sourceType, UUID sourceId,
        String idempotencyDiscriminator)
```

- [ ] **Step 6: Update deletion order**

Before deleting an Agent, delete its events:

```java
eventRepository.deleteByTargetId(targetId);
agentRepository.deleteByTargetId(targetId);
```

```java
eventRepository.deleteByUserId(userId);
agentRepository.deleteByUserId(userId);
```

- [ ] **Step 7: Run focused tests**

```bash
rtk mvn -q -f backend/pom.xml -Dtest=CoachEventServiceTest,TargetControllerTest,AuthControllerTest test
```

Expected: PASS.

- [ ] **Step 8: Stage, detect changes, and commit Task 35A**

Commit summary:

```text
feat(agent): 持久化幂等教练事件
```

### Task 35B: Add Structured Cloud AI Agent Decision

**Files:**

- Create: `backend/src/main/java/com/interviewcoach/common/api/AgentToolCallDto.java`
- Create: `backend/src/main/java/com/interviewcoach/common/api/AgentDecisionDto.java`
- Modify: `backend/src/main/java/com/interviewcoach/ai/service/AiPrompt.java`
- Modify: `backend/src/main/java/com/interviewcoach/ai/service/DefaultAiModelGateway.java`
- Modify: `backend/src/main/java/com/interviewcoach/ai/service/AiStructuredOutputService.java`
- Modify: `backend/src/main/java/com/interviewcoach/ai/service/LocalPlatformAiClient.java`
- Modify: `docs/ai/prompt-contracts.md`
- Test: `backend/src/test/java/com/interviewcoach/ai/AiStructuredOutputServiceTest.java`
- Test: `backend/src/test/java/com/interviewcoach/assessment/AssessmentRealAiGateTest.java`

- [ ] **Step 1: Run GitNexus impact checks**

Use `gitnexus_impact` for:

```text
AiPrompt
DefaultAiModelGateway
AiStructuredOutputService
LocalPlatformAiClient
```

- [ ] **Step 2: Write failing structured-output tests**

Add tests for valid output, invalid dimension, invalid action, blank rationale, and real-AI gate:

```java
@Test
void agentDecisionRejectsUnknownFocusDimension() {
    PlatformAiClient client = prompt -> """
            {
              "currentGoal":"提升系统设计表达",
              "focusDimensions":["unknownDimension"],
              "recommendedAction":{"type":"continueTraining","title":"继续训练"},
              "rationaleSummary":"最近回答需要更清晰的权衡说明。",
              "toolCalls":[],
              "memoryUpdateRequired":false,
              "planAdjustmentRequired":false
            }
            """;

    assertThrows(AiParseException.class,
            () -> serviceWith(client).generateAgentDecision(
                    new AiPrompt("agentDecision", "target-1", "system", "user")));
}
```

- [ ] **Step 3: Run tests to verify they fail**

```bash
rtk mvn -q -f backend/pom.xml -Dtest=AiStructuredOutputServiceTest,AssessmentRealAiGateTest test
```

Expected: FAIL because the Agent task and DTOs do not exist.

- [ ] **Step 4: Add decision DTOs**

```java
public record AgentToolCallDto(String toolName, String operation, String reason) {
}
```

```java
public record AgentDecisionDto(
        String currentGoal,
        List<String> focusDimensions,
        AgentRecommendedActionDto recommendedAction,
        String rationaleSummary,
        List<AgentToolCallDto> toolCalls,
        boolean memoryUpdateRequired,
        boolean planAdjustmentRequired
) {
}
```

- [ ] **Step 5: Add task constant and real-AI requirement**

```java
public static final String TASK_AGENT_DECISION = "agentDecision";
```

Add `TASK_AGENT_DECISION` to `DefaultAiModelGateway.REAL_AI_REQUIRED_TASKS`.

- [ ] **Step 6: Add generation and validation**

```java
public AgentDecisionDto generateAgentDecision(AiPrompt prompt) {
    AgentDecisionDto structuredResult =
            generateStructuredFromSpringProvider(prompt, AgentDecisionDto.class);
    if (structuredResult != null) {
        AgentDecisionDto validated =
                validateStructured(prompt, structuredResult, (dto, p) -> validateAgentDecisionShape(dto));
        if (validated != null) return validated;
    }
    return generateAndValidate(prompt, AgentDecisionDto.class,
            (dto, p) -> validateAgentDecisionShape(dto));
}
```

Keep shape validation private to `AiStructuredOutputService`. The AI module must not depend on the Agent module.

```java
private void validateAgentDecisionShape(AgentDecisionDto dto) {
    if (dto == null) throw new IllegalArgumentException("AgentDecision is null");
    requireText(dto.currentGoal(), "currentGoal");
    requireList(dto.focusDimensions(), "focusDimensions");
    if (dto.focusDimensions().size() > 3
            || dto.focusDimensions().stream().anyMatch(d -> !AssessmentDimensionName.ALL.contains(d))) {
        throw new IllegalArgumentException("invalid focusDimensions");
    }
    if (dto.recommendedAction() == null) {
        throw new IllegalArgumentException("recommendedAction is required");
    }
    requireText(dto.recommendedAction().type(), "recommendedAction.type");
    requireText(dto.recommendedAction().title(), "recommendedAction.title");
    requireText(dto.rationaleSummary(), "rationaleSummary");
    requireList(dto.toolCalls(), "toolCalls");
}
```

AI-layer shape validation rules:

- `currentGoal`, action type/title, and `rationaleSummary` are nonblank.
- `focusDimensions` is non-null, has at most 3 items, and each item is in `AssessmentDimensionName.ALL`.
- action type is one of:

```text
confirmProfile
generateJobBrief
startAssessment
generateTrainingPlan
continueTraining
startMockInterview
reviewProgress
reviewReport
none
```

- `toolCalls` is non-null. Tool name/operation/reason are nonblank when present.

- [ ] **Step 7: Add deterministic test-only LocalPlatform output**

Return valid `agentDecision` JSON only for unit/CI tests. Do not treat this output as product acceptance.

- [ ] **Step 8: Document the prompt contract**

Add `Task: agentDecision` to `docs/ai/prompt-contracts.md`, including:

- Input is low-risk structured summaries only.
- Output is `AgentDecisionDto`.
- No chain-of-thought.
- No resume source text or answer source text.
- Product acceptance must use real AI.

- [ ] **Step 9: Run focused tests**

```bash
rtk mvn -q -f backend/pom.xml -Dtest=AiStructuredOutputServiceTest,AssessmentRealAiGateTest test
```

Expected: PASS.

- [ ] **Step 10: Stage, detect changes, and commit Task 35B**

Commit summary:

```text
feat(agent): 增加真实 AI AgentDecision 契约
```

### Task 35C: Build Agent Context, Runner, And After-Commit Dispatch

**Files:**

- Create: `backend/src/main/java/com/interviewcoach/agent/model/AgentContextSnapshot.java`
- Create: `backend/src/main/java/com/interviewcoach/agent/service/AgentContextService.java`
- Create: `backend/src/main/java/com/interviewcoach/agent/service/AgentPromptFactory.java`
- Create: `backend/src/main/java/com/interviewcoach/agent/service/AgentDecisionValidator.java`
- Create: `backend/src/main/java/com/interviewcoach/agent/service/AgentUserExecutionContext.java`
- Create: `backend/src/main/java/com/interviewcoach/agent/service/InterviewCoachAgentRunner.java`
- Create: `backend/src/main/java/com/interviewcoach/agent/service/AgentDecisionApplicationService.java`
- Create: `backend/src/main/java/com/interviewcoach/agent/service/CoachEventDispatcher.java`
- Create: `backend/src/main/java/com/interviewcoach/agent/config/AgentRuntimeConfig.java`
- Create: `backend/src/main/java/com/interviewcoach/agent/config/AgentRuntimeProperties.java`
- Modify: `backend/src/main/java/com/interviewcoach/agent/service/CoachEventService.java`
- Modify: `backend/src/main/resources/application.yml`
- Modify: `backend/src/test/resources/application-test.yml`
- Modify: `backend/src/test/resources/application-live-ai-test.yml`
- Test: `backend/src/test/java/com/interviewcoach/agent/InterviewCoachAgentRunnerTest.java`
- Test: `backend/src/test/java/com/interviewcoach/agent/AgentUserExecutionContextTest.java`

- [ ] **Step 1: Write failing runner and security-context tests**

Security context test:

```java
@Test
void callAsSetsUserForAiGatewayAndRestoresPreviousContext() {
    User user = savedUser("agent_provider_owner");

    String principalId = executionContext.callAs(user,
            () -> SecurityUtils.currentUser().getId().toString());

    assertEquals(user.getId().toString(), principalId);
    assertNull(SecurityContextHolder.getContext().getAuthentication());
}
```

Runner test:

```java
@Test
void successfulEventUpdatesAgentWithoutPersistingRawContext() {
    when(aiService.generateAgentDecision(any())).thenReturn(validDecision());

    runner.run(eventId);

    InterviewCoachAgent agent = agentRepository.findByTargetIdAndUserId(targetId, userId).orElseThrow();
    assertEquals("continueTraining", agent.getNextRecommendedActionType());
    assertEquals("succeeded", agent.getLastRunOutcome());
    assertFalse(agent.getLastDecisionSummary().contains("raw answer"));
}
```

Use `@BeforeEach` to persist one user, target, Agent, and pending event, assigning their IDs to `userId`, `targetId`, and `eventId`. Add a `validDecision()` helper that returns a decision with:

```text
currentGoal = "提升系统设计表达"
focusDimensions = ["systemThinking"]
recommendedAction = continueTraining / "继续专项训练"
rationaleSummary = "最近表现需要继续强化系统设计权衡。"
toolCalls = []
memoryUpdateRequired = false
planAdjustmentRequired = false
```

- [ ] **Step 2: Run tests to verify they fail**

```bash
rtk mvn -q -f backend/pom.xml -Dtest=InterviewCoachAgentRunnerTest,AgentUserExecutionContextTest test
```

Expected: FAIL because runner components do not exist.

- [ ] **Step 3: Implement low-risk context snapshot**

`AgentContextSnapshot` may contain:

```java
public record AgentContextSnapshot(
        String targetId,
        String targetTitle,
        String targetStatus,
        String currentStage,
        boolean hasConfirmedProfile,
        String confirmedProfileSummary,
        List<String> confirmedSkills,
        boolean hasJobBrief,
        String roleSummary,
        List<String> roleRiskAreas,
        boolean hasCompletedAssessment,
        String trainingPlanStatus,
        int completedTrainingTasks,
        int totalTrainingTasks,
        int completedMockInterviews,
        List<String> eventFactSummary,
        List<String> dimensionSummaries,
        List<String> trustedStrengths,
        List<String> trustedWeaknesses,
        List<String> trustedNextFocus,
        List<String> pendingVerificationLeads
) {
}
```

Do not include:

```text
InterviewTarget.jd
resumeText
projectRawText
Assessment answers
Training answers
Mock interview messages
prompt
completion
```

When reading CoachingMemory:

- Fact summaries may use only `confirmed`, `observed`, and `corrected`.
- `inferred` may be placed in a separate pending-verification list only when a later prompt explicitly treats it as a question lead.
- `rejected` must be excluded entirely.

Confirmed profile summary, confirmed skills, JobBrief role summary, and JobBrief risk areas may be used in the transient prompt context, but must never be copied into Agent state, logs, metrics, or event payloads.

`eventFactSummary` must be built only from structured completed facts:

```text
AssessmentResult dimensions, strengths, weaknesses, nextActions
TrainingFeedback score, problems, recommendedReviewPoints
Adaptive Training session summary and structured feedback
MockInterviewReport dimensionScores, strengths, weaknesses, likelyFollowUpPoints
Corrected CoachingMemory item after the correction has been saved
```

Never include the user's original answer or mock interview message text.

- [ ] **Step 4: Implement temporary user execution context**

```java
public <T> T callAs(User user, Supplier<T> action) {
    SecurityContext previous = SecurityContextHolder.getContext();
    try {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, List.of()));
        SecurityContextHolder.setContext(context);
        return action.get();
    } finally {
        SecurityContextHolder.setContext(previous);
    }
}
```

- [ ] **Step 5: Implement prompt factory**

The prompt must:

- State that the model is the persistent interview coach for one target.
- Require JSON matching `AgentDecisionDto`.
- Require at most 3 focus dimensions.
- Require only approved action types.
- State that tool calls are optional and will be validated.
- For the Task 35 implementation, require `toolCalls: []`, `memoryUpdateRequired: false`, and `planAdjustmentRequired: false`; later tasks update the contract when those capabilities exist.
- State that explanations must be short business summaries, not chain-of-thought.
- Forbid invented candidate experience and rejected facts.

`AgentDecisionValidator.validateFinalDecision(...)` must enforce the capabilities currently implemented:

- Until Task 38 lands, `toolCalls` must be empty.
- Until Task 39 lands, `memoryUpdateRequired` and `planAdjustmentRequired` must both be `false`.

- [ ] **Step 6: Implement runner**

Initial single-pass flow:

```java
public void run(UUID eventId) {
    CoachEvent event = eventService.claim(eventId);
    if (event == null) return;

    try {
        User user = userRepository.findById(event.getUserId()).orElseThrow();
        AgentContextSnapshot snapshot = contextService.build(event);
        AgentDecisionDto decision = executionContext.callAs(user,
                () -> aiService.generateAgentDecision(promptFactory.build(event, snapshot)));
        decisionValidator.validateFinalDecision(
                event.getEventType(), snapshot.currentStage(), decision);
        applicationService.applyFinalDecision(eventId, decision);
    } catch (Exception ex) {
        eventService.markFailed(eventId, ex.getClass().getSimpleName());
    }
}
```

`CoachEventService.claim(...)` must use a separate transaction to atomically change `pending` or `failed` to `processing`, increment attempts, and prevent concurrent duplicate processing. It must also set the Agent `lastRunOutcome` to `processing`.

`AgentDecisionApplicationService.applyFinalDecision(...)` must use one transaction to update Agent state and mark the event completed. It must never save raw prompt or completion. Task 39 extends this same transaction to include memory and plan updates.

- [ ] **Step 7: Implement AFTER_COMMIT dispatcher**

`CoachEventService.recordEvent(...)` publishes a lightweight record containing only `eventId`:

```java
public record CoachEventRecorded(UUID eventId) {
}
```

Dispatcher:

```java
@Async("coachAgentExecutor")
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
public void onRecorded(CoachEventRecorded event) {
    runner.run(event.eventId());
}
```

Configure a bounded executor; do not use an unbounded queue.

- [ ] **Step 8: Add initial runtime properties and deterministic test dispatch**

Create properties with these defaults:

```yaml
app:
  agent:
    async-enabled: true
    executor-core-pool-size: 2
    executor-max-pool-size: 4
    executor-queue-capacity: 100
    session-refresh-hours: 6
```

Bind them with:

```java
@ConfigurationProperties(prefix = "app.agent")
public class AgentRuntimeProperties {
    private boolean asyncEnabled = true;
    private int executorCorePoolSize = 2;
    private int executorMaxPoolSize = 4;
    private int executorQueueCapacity = 100;
    private int sessionRefreshHours = 6;
}
```

Enable the properties from `AgentRuntimeConfig` with `@EnableConfigurationProperties(AgentRuntimeProperties.class)`.

In `application-test.yml`, set `app.agent.async-enabled: false` so default tests use a synchronous executor and do not leak background Agent work between test methods. In `application-live-ai-test.yml`, keep async enabled and make live tests poll for Agent completion.

- [ ] **Step 9: Run focused tests and full backend tests**

```bash
rtk mvn -q -f backend/pom.xml -Dtest=InterviewCoachAgentRunnerTest,AgentUserExecutionContextTest test
rtk mvn -q -f backend/pom.xml test
```

Expected: PASS.

- [ ] **Step 10: Stage, detect changes, and commit Task 35C**

Commit summary:

```text
feat(agent): 运行提交后云端 Agent 决策
```

## 5. Task 36: Core Business Event Integration

### Task 36A: Record Target And Profile Events

**Files:**

- Modify: `backend/src/main/java/com/interviewcoach/target/service/InterviewTargetService.java`
- Modify: `backend/src/main/java/com/interviewcoach/profile/service/CandidateProfileService.java`
- Test: `backend/src/test/java/com/interviewcoach/agent/CoachEventIntegrationTest.java`

- [ ] **Step 1: Run GitNexus impact checks**

Use `gitnexus_impact` for:

```text
InterviewTargetService.createTarget
CandidateProfileService.confirmProfile
```

- [ ] **Step 2: Write failing integration tests**

```java
@Test
void targetCreationAndProfileConfirmationRecordOneEventEach() throws Exception {
    String token = loginAndGetToken("agent_event_profile");
    String targetId = createTarget(token);
    confirmProfile(token, targetId);

    assertEquals(1, eventRepository.countByTargetIdAndEventType(
            UUID.fromString(targetId), "TARGET_CREATED"));
    assertEquals(1, eventRepository.countByTargetIdAndEventType(
            UUID.fromString(targetId), "RESUME_SUMMARY_CONFIRMED"));
}
```

- [ ] **Step 3: Run test to verify it fails**

```bash
rtk mvn -q -f backend/pom.xml -Dtest=CoachEventIntegrationTest test
```

Expected: FAIL because services do not record events.

- [ ] **Step 4: Record events inside the same business transaction**

```java
eventService.recordEvent(user, target.getId(),
        "TARGET_CREATED", "target", target.getId());
```

```java
eventService.recordEvent(user, targetId,
        "RESUME_SUMMARY_CONFIRMED", "candidateProfile", profile.getId());
```

- [ ] **Step 5: Run focused tests**

```bash
rtk mvn -q -f backend/pom.xml -Dtest=CoachEventIntegrationTest,TargetControllerTest,ProfileLifecycleTest test
```

Expected: PASS.

- [ ] **Step 6: Stage, detect changes, and commit Task 36A**

Commit summary:

```text
feat(agent): 接入目标与摘要确认事件
```

### Task 36B: Record Assessment, Training, Mock, And Correction Events

**Files:**

- Modify: `backend/src/main/java/com/interviewcoach/assessment/service/AssessmentService.java`
- Modify: `backend/src/main/java/com/interviewcoach/training/service/TrainingService.java`
- Modify: `backend/src/main/java/com/interviewcoach/mockinterview/service/MockInterviewService.java`
- Modify: `backend/src/main/java/com/interviewcoach/coachingmemory/service/CoachingMemoryService.java`
- Test: `backend/src/test/java/com/interviewcoach/agent/CoachEventIntegrationTest.java`
- Test: `backend/src/test/java/com/interviewcoach/assessment/AssessmentControllerTest.java`
- Test: `backend/src/test/java/com/interviewcoach/training/TrainingControllerTest.java`
- Test: `backend/src/test/java/com/interviewcoach/training/TrainingAdaptiveActionTest.java`
- Test: `backend/src/test/java/com/interviewcoach/mockinterview/MockInterviewControllerTest.java`
- Test: `backend/src/test/java/com/interviewcoach/coachingmemory/CoachingMemoryControllerTest.java`

- [ ] **Step 1: Run GitNexus impact checks**

Use `gitnexus_impact` for:

```text
AssessmentService.finishAssessment
TrainingService.completeTask
TrainingService.completeAdaptiveSession
MockInterviewService.finishInterview
CoachingMemoryService.correctMemoryItem
```

- [ ] **Step 2: Add failing event integration tests**

Verify exactly one event for each source:

```text
ASSESSMENT_COMPLETED / assessment / assessmentSessionId
TRAINING_TASK_COMPLETED / trainingTask / taskId
TRAINING_SESSION_COMPLETED / trainingSession / sessionId
MOCK_INTERVIEW_COMPLETED / mockInterview / interviewId
MEMORY_CORRECTED / coachingMemory / memoryId
```

Repeat the same correction request and verify its hashed idempotency discriminator prevents duplicate pollution. Then change the correction content and verify a new event is recorded for the new correction.

- [ ] **Step 3: Run tests to verify they fail**

```bash
rtk mvn -q -f backend/pom.xml -Dtest=CoachEventIntegrationTest test
```

Expected: FAIL because event recording is absent.

- [ ] **Step 4: Record events after facts are saved**

Example:

```java
eventService.recordEvent(session.getUser(), session.getTarget().getId(),
        "ASSESSMENT_COMPLETED", "assessment", sessionId);
```

Do not remove existing coaching memory AI calls in Task 36. Task 36 is compatibility-first event integration.

- [ ] **Step 5: Run focused regression tests**

```bash
rtk mvn -q -f backend/pom.xml \
  -Dtest=CoachEventIntegrationTest,AssessmentControllerTest,TrainingControllerTest,MockInterviewControllerTest,CoachingMemoryControllerTest test
```

Expected: PASS.

- [ ] **Step 6: Stage, detect changes, and commit Task 36B**

Commit summary:

```text
feat(agent): 接入核心教练业务事件
```

## 6. Task 37: Unified Next Recommendation

### Task 37A: Derive Stage And Validate Recommended Action

**Files:**

- Create: `backend/src/main/java/com/interviewcoach/agent/service/AgentStageResolver.java`
- Create: `backend/src/main/java/com/interviewcoach/agent/service/AgentDeterministicDecisionPolicy.java`
- Modify: `backend/src/main/java/com/interviewcoach/agent/service/AgentContextService.java`
- Modify: `backend/src/main/java/com/interviewcoach/agent/service/AgentDecisionValidator.java`
- Modify: `backend/src/main/java/com/interviewcoach/agent/service/InterviewCoachAgentRunner.java`
- Modify: `backend/src/main/java/com/interviewcoach/agent/service/AgentDecisionApplicationService.java`
- Test: `backend/src/test/java/com/interviewcoach/agent/AgentStageResolverTest.java`
- Test: `backend/src/test/java/com/interviewcoach/agent/InterviewCoachAgentRunnerTest.java`

- [ ] **Step 1: Write failing stage/action compatibility tests**

```java
@ParameterizedTest
@CsvSource({
        "targetSetup,confirmProfile",
        "profileConfirmation,generateJobBrief",
        "assessment,startAssessment",
        "training,continueTraining",
        "mockInterview,startMockInterview",
        "review,reviewProgress"
})
void stageAllowsExpectedAction(String stage, String action) {
    assertDoesNotThrow(() -> validator.validateActionForStage(stage, action));
}
```

Also assert that `targetSetup + startMockInterview` is rejected.

Add tests that obvious prerequisites return a complete decision without invoking AI:

```text
no confirmed profile -> confirmProfile
confirmed profile and no JobBrief -> generateJobBrief
JobBrief exists and no completed assessment -> startAssessment
```

- [ ] **Step 2: Run tests to verify they fail**

```bash
rtk mvn -q -f backend/pom.xml -Dtest=AgentStageResolverTest,InterviewCoachAgentRunnerTest test
```

Expected: FAIL because stage resolver and compatibility validation do not exist.

- [ ] **Step 3: Implement deterministic stage resolution**

Resolve stage in code from facts, not from model claims:

```text
no confirmed profile -> targetSetup
profile confirmed and no job brief -> profileConfirmation
job brief exists and no completed assessment -> assessment
assessment completed and incomplete training work exists -> training
training baseline exists and no completed mock interview -> mockInterview
otherwise -> review
```

- [ ] **Step 4: Validate model action against stage**

The Agent runner must reject an action that conflicts with the derived stage. The model may choose between allowed actions for a stage, but cannot skip required product steps.

- [ ] **Step 5: Skip unnecessary model calls for deterministic prerequisites**

Before calling `AiStructuredOutputService.generateAgentDecision(...)`, ask `AgentDeterministicDecisionPolicy` whether the current fact snapshot has exactly one valid prerequisite action. If it returns a decision, apply that decision directly and do not call the model.

Do not use deterministic decisions for assessment weakness selection, training focus, mock interview review, memory updates, or plan adjustments.

- [ ] **Step 6: Run focused tests**

```bash
rtk mvn -q -f backend/pom.xml -Dtest=AgentStageResolverTest,InterviewCoachAgentRunnerTest test
```

Expected: PASS.

- [ ] **Step 7: Stage, detect changes, and commit Task 37A**

Commit summary:

```text
feat(agent): 统一校验教练阶段与下一步推荐
```

### Task 37B: Add App Session Start Refresh API

**Files:**

- Modify: `backend/src/main/java/com/interviewcoach/agent/controller/InterviewCoachAgentController.java`
- Modify: `backend/src/main/java/com/interviewcoach/agent/service/CoachEventService.java`
- Modify: `backend/src/main/java/com/interviewcoach/agent/service/InterviewCoachAgentService.java`
- Modify: `docs/api/openapi.yaml`
- Test: `backend/src/test/java/com/interviewcoach/agent/InterviewCoachAgentControllerTest.java`

- [ ] **Step 1: Write failing freshness and authentication tests**

```java
@Test
void sessionStartRecordsRefreshOnlyWhenAgentIsStale() throws Exception {
    String token = loginAndGetToken("agent_session_start");
    String targetId = createTarget(token);

    mockMvc.perform(post("/api/targets/" + targetId + "/coach-agent/session-start")
                    .header("Authorization", "Bearer " + token))
            .andExpect(status().isAccepted());

    mockMvc.perform(post("/api/targets/" + targetId + "/coach-agent/session-start")
                    .header("Authorization", "Bearer " + token))
            .andExpect(status().isAccepted());

    assertEquals(1, eventRepository.countByTargetIdAndEventType(
            UUID.fromString(targetId), "APP_SESSION_STARTED"));
}
```

- [ ] **Step 2: Run test to verify it fails**

```bash
rtk mvn -q -f backend/pom.xml -Dtest=InterviewCoachAgentControllerTest test
```

Expected: FAIL because endpoint does not exist.

- [ ] **Step 3: Implement freshness-gated session event**

Use a deterministic 6-hour freshness window from `AgentRuntimeProperties`. Do not call AI on every app open.

Build a deterministic source UUID from `targetId + sixHourTimeBucket` and pass the same time-bucket discriminator into `CoachEventService`. This allows one refresh per bucket while keeping repeated opens in the same bucket idempotent.

Endpoint:

```java
@PostMapping("/session-start")
public ResponseEntity<InterviewCoachAgentDto> sessionStart(@PathVariable UUID targetId) {
    User user = SecurityUtils.currentUser();
    agentService.recordSessionStartIfStale(user, targetId);
    return ResponseEntity.accepted().body(agentService.getByTarget(targetId, user.getId()));
}
```

- [ ] **Step 4: Update OpenAPI and run tests**

```bash
rtk mvn -q -f backend/pom.xml -Dtest=InterviewCoachAgentControllerTest test
```

Expected: PASS.

- [ ] **Step 5: Stage, detect changes, and commit Task 37B**

Commit summary:

```text
feat(agent): 增加持续教练会话刷新入口
```

## 7. Task 38: Whitelist Tool Orchestration And Budgets

### Task 38A: Add Tool Registry And Read-Only Tools

**Files:**

- Create: `backend/src/main/java/com/interviewcoach/agent/model/AgentToolContext.java`
- Create: `backend/src/main/java/com/interviewcoach/agent/model/AgentToolResult.java`
- Create: `backend/src/main/java/com/interviewcoach/agent/tool/CoachAgentTool.java`
- Create: `backend/src/main/java/com/interviewcoach/agent/tool/CoachAgentToolRegistry.java`
- Create: `backend/src/main/java/com/interviewcoach/agent/tool/AssessmentCoachAgentTool.java`
- Create: `backend/src/main/java/com/interviewcoach/agent/tool/TrainingPlanCoachAgentTool.java`
- Create: `backend/src/main/java/com/interviewcoach/agent/tool/AdaptiveTrainingCoachAgentTool.java`
- Create: `backend/src/main/java/com/interviewcoach/agent/tool/MockInterviewCoachAgentTool.java`
- Create: `backend/src/main/java/com/interviewcoach/agent/tool/ProgressAnalysisCoachAgentTool.java`
- Create: `backend/src/main/java/com/interviewcoach/agent/tool/CoachingMemoryCoachAgentTool.java`
- Test: `backend/src/test/java/com/interviewcoach/agent/CoachAgentToolRegistryTest.java`

- [ ] **Step 1: Write failing whitelist tests**

```java
@Test
void registryRejectsUnknownTool() {
    assertThrows(IllegalArgumentException.class,
            () -> registry.execute(context, new AgentToolCallDto(
                    "arbitraryHttp", "post", "not allowed")));
}
```

Verify every registered tool returns a low-risk summary and never returns raw user answers.

- [ ] **Step 2: Run tests to verify they fail**

```bash
rtk mvn -q -f backend/pom.xml -Dtest=CoachAgentToolRegistryTest test
```

Expected: FAIL because tool contracts do not exist.

- [ ] **Step 3: Implement tool contract**

```java
public interface CoachAgentTool {
    String name();
    Set<String> supportedOperations();
    AgentToolResult execute(AgentToolContext context, AgentToolCallDto call);
}
```

Registry:

```java
public AgentToolResult execute(AgentToolContext context, AgentToolCallDto call) {
    CoachAgentTool tool = toolsByName.get(call.toolName());
    if (tool == null || !tool.supportedOperations().contains(call.operation())) {
        throw new IllegalArgumentException("Unsupported Agent tool call");
    }
    return tool.execute(context, call);
}
```

- [ ] **Step 4: Implement six read-only tools**

Allowed initial operations:

```text
assessment.inspectLatestResult
trainingPlan.inspectCurrentPlan
trainingPlan.inspectLatestTaskFeedback
adaptiveTraining.inspectActiveSession
mockInterview.inspectRecentReports
progressAnalysis.readSummary
coachingMemory.readTrustedSummary
```

Tool summaries may include scores, statuses, dimension names, trusted memory summaries, and counts. They must not include raw answers, resume source text, prompt, or completion.

`trainingPlan.inspectCurrentPlan` may include pending task IDs, titles, descriptions, and day indexes so a later validated plan adjustment can identify tasks. These are AI-generated training artifacts, not user answer source text.

- [ ] **Step 5: Run focused tests**

```bash
rtk mvn -q -f backend/pom.xml -Dtest=CoachAgentToolRegistryTest test
```

Expected: PASS.

- [ ] **Step 6: Stage, detect changes, and commit Task 38A**

Commit summary:

```text
feat(agent): 增加白名单教练工具注册表
```

### Task 38B: Enforce Bounded Two-Pass Agent Loop

**Files:**

- Modify: `backend/src/main/java/com/interviewcoach/agent/config/AgentRuntimeProperties.java`
- Create: `backend/src/main/java/com/interviewcoach/agent/service/AgentMetrics.java`
- Create: `backend/src/main/java/com/interviewcoach/agent/service/CoachEventRetryScheduler.java`
- Modify: `backend/src/main/java/com/interviewcoach/agent/config/AgentRuntimeConfig.java`
- Modify: `backend/src/main/java/com/interviewcoach/agent/service/InterviewCoachAgentRunner.java`
- Modify: `backend/src/main/java/com/interviewcoach/agent/service/AgentPromptFactory.java`
- Modify: `backend/src/main/resources/application.yml`
- Modify: `docs/ai/provider-contracts.md`
- Test: `backend/src/test/java/com/interviewcoach/agent/InterviewCoachAgentRunnerTest.java`
- Test: `backend/src/test/java/com/interviewcoach/agent/AgentMetricsTest.java`

- [ ] **Step 1: Write failing budget, retry, and metrics tests**

Verify:

- More than 3 tool calls are rejected.
- A second decision with additional tool calls is rejected.
- A failed event remains retryable until 3 attempts.
- Metrics include only `eventType`, `toolName`, `operation`, and `outcome`.

- [ ] **Step 2: Run tests to verify they fail**

```bash
rtk mvn -q -f backend/pom.xml -Dtest=InterviewCoachAgentRunnerTest,AgentMetricsTest test
```

Expected: FAIL because budgets and metrics do not exist.

- [ ] **Step 3: Add runtime properties**

```yaml
app:
  agent:
    async-enabled: true
    executor-core-pool-size: 2
    executor-max-pool-size: 4
    executor-queue-capacity: 100
    max-model-calls-per-event: 2
    max-tool-calls-per-event: 3
    max-event-attempts: 3
    session-refresh-hours: 6
    retry-delay-ms: 60000
```

- [ ] **Step 4: Implement two-pass loop**

```java
AgentDecisionDto first = generateDecision(event, snapshot, List.of());
List<AgentToolCallDto> calls = first.toolCalls();
if (calls.isEmpty()) {
    decisionValidator.validateFinalDecision(
            event.getEventType(), snapshot.currentStage(), first);
    return first;
}
decisionValidator.validateInitialDecision(first);
if (calls.size() > properties.getMaxToolCallsPerEvent()) {
    throw new IllegalArgumentException("Agent tool call budget exceeded");
}
List<AgentToolResult> results = calls.stream()
        .map(call -> registry.execute(toolContext, call))
        .toList();
AgentDecisionDto second = generateDecision(event, snapshot, results);
decisionValidator.validateFinalDecision(
        event.getEventType(), snapshot.currentStage(), second);
return second;
```

Update `AgentDecisionValidator` and `AgentPromptFactory` so Task 38 permits only registered read-only tool calls within budget. Keep `memoryUpdateRequired=false` and `planAdjustmentRequired=false` mandatory until Task 39.

Split validation into:

```java
public void validateInitialDecision(AgentDecisionDto decision)
public void validateFinalDecision(String eventType, String currentStage, AgentDecisionDto decision)
```

The initial decision may request tools and must not carry memory or plan mutations. The final decision must request no additional tools and is the only decision that may later carry validated mutations.

- [ ] **Step 5: Add retry scheduler**

The scheduler selects pending/failed events below the attempt budget and dispatches them. Event claiming must prevent concurrent duplicate processing.

- [ ] **Step 6: Add low-risk metrics**

Use:

```text
agent.run.total{eventType,outcome}
agent.run.duration{eventType,outcome}
agent.tool.total{toolName,operation,outcome}
```

Do not add target ID, user ID, prompt, completion, or user text as metric labels.

- [ ] **Step 7: Update Provider documentation**

Document that Agent decisions are real-AI-required tasks and that Agent metrics contain only low-risk metadata.

- [ ] **Step 8: Run focused tests and full backend tests**

```bash
rtk mvn -q -f backend/pom.xml -Dtest=InterviewCoachAgentRunnerTest,AgentMetricsTest test
rtk mvn -q -f backend/pom.xml test
```

Expected: PASS.

- [ ] **Step 9: Stage, detect changes, and commit Task 38B**

Commit summary:

```text
feat(agent): 限制 Agent 模型与工具调用预算
```

## 8. Task 39: Consolidate AI Calls, Memory Updates, And Plan Adjustment

### Task 39A: Carry Structured Memory Update In AgentDecision

**Files:**

- Create: `backend/src/main/java/com/interviewcoach/common/api/AgentMemoryUpdateDto.java`
- Modify: `backend/src/main/java/com/interviewcoach/common/api/AgentDecisionDto.java`
- Modify: `backend/src/main/java/com/interviewcoach/agent/service/AgentDecisionValidator.java`
- Modify: `backend/src/main/java/com/interviewcoach/agent/service/AgentPromptFactory.java`
- Modify: `backend/src/main/java/com/interviewcoach/agent/service/InterviewCoachAgentRunner.java`
- Modify: `backend/src/main/java/com/interviewcoach/agent/service/AgentDecisionApplicationService.java`
- Modify: `backend/src/main/java/com/interviewcoach/coachingmemory/service/CoachingMemoryService.java`
- Modify: `backend/src/main/java/com/interviewcoach/ai/service/LocalPlatformAiClient.java`
- Modify: `docs/ai/prompt-contracts.md`
- Test: `backend/src/test/java/com/interviewcoach/agent/InterviewCoachAgentRunnerTest.java`
- Test: `backend/src/test/java/com/interviewcoach/coachingmemory/CoachingMemoryControllerTest.java`

- [ ] **Step 1: Run GitNexus impact checks**

Use `gitnexus_impact` for:

```text
CoachingMemoryService
AgentDecisionDto
InterviewCoachAgentRunner
```

- [ ] **Step 2: Write failing memory update tests**

```java
@Test
void finalDecisionCanPersistTrustedMemoryWithoutAnotherAiCall() {
    when(aiService.generateAgentDecision(any())).thenReturn(decisionWithMemoryUpdate());

    runner.run(eventId);

    CoachingMemory saved = memoryRepository
            .findByTargetIdAndUserIdOrderByCreatedAtDesc(targetId, userId)
            .get(0);
    assertEquals(event.getSourceType(), saved.getSourceType());
    assertEquals(event.getSourceId(), saved.getSourceId());
    verify(aiService, times(1)).generateAgentDecision(any());
    verify(aiService, never()).generateCoachingMemory(any());
}
```

`decisionWithMemoryUpdate()` must return a final decision with one valid `observedWeaknesses` item and one valid `recommendedNextFocus` item, both using nonblank content and allowed confidence values.

- [ ] **Step 3: Run tests to verify they fail**

```bash
rtk mvn -q -f backend/pom.xml -Dtest=InterviewCoachAgentRunnerTest,CoachingMemoryControllerTest test
```

Expected: FAIL because decision-carried memory does not exist.

- [ ] **Step 4: Add memory update DTO**

```java
public record AgentMemoryUpdateDto(
        List<CoachingMemoryItemDto> observedStrengths,
        List<CoachingMemoryItemDto> observedWeaknesses,
        List<CoachingMemoryItemDto> recurringProblems,
        List<CoachingMemoryItemDto> verifiedExperience,
        List<CoachingMemoryItemDto> unverifiedClaims,
        List<CoachingMemoryItemDto> recommendedNextFocus,
        List<CoachingMemoryItemDto> avoidRepeating
) {
}
```

Extend `AgentDecisionDto` with nullable `memoryUpdate`.

Task 39A relaxes only memory updates. Keep `planAdjustmentRequired=false` mandatory until Task 39C.

- [ ] **Step 5: Validate memory trust semantics**

Reuse the same item rules as `CoachingMemory`:

- source in `confirmed`, `observed`, or `inferred`.
- confidence in `high`, `medium`, `low`.
- content nonblank.
- `memoryUpdateRequired=true` requires non-null `memoryUpdate`.
- `memoryUpdateRequired=false` requires null `memoryUpdate`.

The model must never create `corrected` or `rejected` items. Those sources are reserved for explicit user correction actions.

Update the prompt and validator by event type:

```text
ASSESSMENT_COMPLETED -> memoryUpdateRequired must be true
TRAINING_TASK_COMPLETED -> memoryUpdateRequired must be true
TRAINING_SESSION_COMPLETED -> memoryUpdateRequired must be true
MOCK_INTERVIEW_COMPLETED -> memoryUpdateRequired must be true
TARGET_CREATED / RESUME_SUMMARY_CONFIRMED / MEMORY_CORRECTED / APP_SESSION_STARTED
  -> memoryUpdateRequired must be false
```

Apply these requirements only in `validateFinalDecision(...)`. An initial decision that requests tools must not create memory updates.

- [ ] **Step 6: Save memory without a second model call**

Add:

```java
@Transactional
public CoachingMemoryDto saveFromAgentDecision(
        User user,
        UUID targetId,
        String sourceType,
        UUID sourceId,
        AgentMemoryUpdateDto update)
```

This method maps validated DTO items to entity items and does not call `AiStructuredOutputService`.

- [ ] **Step 7: Apply memory update from runner**

Only apply the final decision after tool execution. Use event source type/source ID to preserve traceability. Extend `AgentDecisionApplicationService.applyFinalDecision(...)` so Agent state, event completion, and memory update commit atomically.

Update the test-only `LocalPlatformAiClient` Agent response so completed assessment/training/mock event prompts contain a valid memory update, while other event prompts keep `memoryUpdateRequired=false`. This remains default-test scaffolding only.

- [ ] **Step 8: Run focused tests**

```bash
rtk mvn -q -f backend/pom.xml -Dtest=InterviewCoachAgentRunnerTest,CoachingMemoryControllerTest test
```

Expected: PASS.

- [ ] **Step 9: Stage, detect changes, and commit Task 39A**

Commit summary:

```text
feat(agent): 由 AgentDecision 直接更新教练记忆
```

### Task 39B: Remove Direct Post-Result CoachingMemory AI Calls

**Files:**

- Modify: `backend/src/main/java/com/interviewcoach/assessment/service/AssessmentService.java`
- Modify: `backend/src/main/java/com/interviewcoach/training/service/TrainingService.java`
- Modify: `backend/src/main/java/com/interviewcoach/mockinterview/service/MockInterviewService.java`
- Modify: `backend/src/test/java/com/interviewcoach/assessment/AssessmentControllerTest.java`
- Modify: `backend/src/test/java/com/interviewcoach/training/TrainingControllerTest.java`
- Modify: `backend/src/test/java/com/interviewcoach/training/TrainingAdaptiveActionTest.java`
- Modify: `backend/src/test/java/com/interviewcoach/mockinterview/MockInterviewControllerTest.java`
- Modify: `backend/src/test/java/com/interviewcoach/coachingmemory/CoachingMemoryControllerTest.java`
- Test: `backend/src/test/java/com/interviewcoach/agent/AiCallConsolidationTest.java`

- [ ] **Step 1: Run GitNexus impact checks**

Use `gitnexus_impact` for:

```text
AssessmentService.finishAssessment
TrainingService.submitAnswer
TrainingService.completeAdaptiveSession
MockInterviewService.finishInterview
```

- [ ] **Step 2: Write failing call-consolidation tests**

The test must prove that a completed fact records an Agent event and does not directly call `generateCoachingMemory(...)`:

```java
verify(aiService, never()).generateCoachingMemory(any());
verify(eventService).recordEvent(
        any(User.class), eq(targetId), eq("ASSESSMENT_COMPLETED"),
        eq("assessment"), eq(sessionId));
```

- [ ] **Step 3: Run tests to verify they fail**

```bash
rtk mvn -q -f backend/pom.xml -Dtest=AiCallConsolidationTest test
```

Expected: FAIL because direct memory AI calls still exist.

- [ ] **Step 4: Remove direct memory generation calls one business slice at a time**

Order:

1. Assessment finish.
2. Mock interview finish.
3. Standard training task flow.
4. Adaptive training completion.

After each removal, run the corresponding controller/service tests before moving to the next slice.

- [ ] **Step 5: Update existing tests to await Agent-produced memory**

Tests that previously expected memory immediately after a result must poll the event/Agent outcome with a bounded timeout, then assert the same source traceability.

Do not weaken assertions to merely check that an event exists.

- [ ] **Step 6: Run focused and full backend tests**

```bash
rtk mvn -q -f backend/pom.xml \
  -Dtest=AiCallConsolidationTest,AssessmentControllerTest,TrainingControllerTest,MockInterviewControllerTest,CoachingMemoryControllerTest test
rtk mvn -q -f backend/pom.xml test
```

Expected: PASS.

- [ ] **Step 7: Stage, detect changes, and commit Task 39B**

Commit summary:

```text
refactor(agent): 收拢业务结果后的记忆 AI 调用
```

### Task 39C: Apply Safe Future Training Task Adjustments

**Files:**

- Create: `backend/src/main/java/com/interviewcoach/common/api/AgentTrainingPlanAdjustmentDto.java`
- Create: `backend/src/main/java/com/interviewcoach/common/api/AgentTrainingTaskAdjustmentDto.java`
- Modify: `backend/src/main/java/com/interviewcoach/common/api/AgentDecisionDto.java`
- Modify: `backend/src/main/java/com/interviewcoach/agent/service/AgentDecisionValidator.java`
- Modify: `backend/src/main/java/com/interviewcoach/agent/service/InterviewCoachAgentRunner.java`
- Modify: `backend/src/main/java/com/interviewcoach/agent/service/AgentDecisionApplicationService.java`
- Modify: `backend/src/main/java/com/interviewcoach/training/service/TrainingService.java`
- Modify: `backend/src/main/java/com/interviewcoach/ai/service/LocalPlatformAiClient.java`
- Modify: `docs/ai/prompt-contracts.md`
- Test: `backend/src/test/java/com/interviewcoach/training/TrainingAdaptiveActionTest.java`
- Test: `backend/src/test/java/com/interviewcoach/agent/InterviewCoachAgentRunnerTest.java`

- [ ] **Step 1: Run GitNexus impact check**

Use `gitnexus_impact` for:

```text
TrainingService
InterviewCoachAgentRunner
AgentDecisionDto
```

- [ ] **Step 2: Write failing adjustment safety tests**

Verify:

- Only pending tasks may be replaced.
- Completed and in-progress tasks are unchanged.
- Task ID, day index, and task count are unchanged.
- At most 3 replacements are accepted.
- Blank replacement title/description is rejected.

- [ ] **Step 3: Run tests to verify they fail**

```bash
rtk mvn -q -f backend/pom.xml -Dtest=TrainingAdaptiveActionTest,InterviewCoachAgentRunnerTest test
```

Expected: FAIL because plan adjustments do not exist.

- [ ] **Step 4: Add adjustment DTOs**

```java
public record AgentTrainingTaskAdjustmentDto(
        String taskId,
        String title,
        String description
) {
}
```

```java
public record AgentTrainingPlanAdjustmentDto(
        List<AgentTrainingTaskAdjustmentDto> replacements
) {
}
```

Extend `AgentDecisionDto` with nullable `planAdjustment`.

- [ ] **Step 5: Implement strict validation**

`planAdjustmentRequired=true` requires a non-null adjustment with 1-3 replacements. `false` requires null adjustment.

Allow plan adjustments only for `TRAINING_TASK_COMPLETED` and `TRAINING_SESSION_COMPLETED` events when a current plan exists. All other event types must return `planAdjustmentRequired=false`.

Apply these requirements only to the final decision after tool results are available.

- [ ] **Step 6: Implement safe task replacement**

```java
@Transactional
public void applyAgentPlanAdjustment(
        UUID targetId,
        UUID userId,
        AgentTrainingPlanAdjustmentDto adjustment)
```

Rules:

- Load current plan by `targetId + userId`.
- Resolve each task ID from that plan only.
- Require status `pending`.
- Replace only `title` and `description`.
- Never change `id`, `dayIndex`, `status`, task count, plan total days, or completed work.

- [ ] **Step 7: Apply adjustment from final Agent decision**

Apply plan adjustment inside the same `AgentDecisionApplicationService.applyFinalDecision(...)` transaction as Agent state, memory update, and event completion. If validation fails, roll back all decision-derived mutations, mark the event failed in a separate transaction, and do not partially mutate tasks.

- [ ] **Step 8: Run focused and full backend tests**

```bash
rtk mvn -q -f backend/pom.xml -Dtest=TrainingAdaptiveActionTest,InterviewCoachAgentRunnerTest test
rtk mvn -q -f backend/pom.xml test
```

Expected: PASS.

- [ ] **Step 9: Stage, detect changes, and commit Task 39C**

Commit summary:

```text
feat(agent): 受控调整后续训练任务
```

## 9. Task 40: iOS Persistent Coach Entry

### Task 40A: Add Agent DTO And Coach View

**Files:**

- Create: `ios/InterviewCoach/InterviewCoach/Core/API/DTO/InterviewCoachAgentDTO.swift`
- Create: `ios/InterviewCoach/InterviewCoach/Features/CoachAgent/CoachAgentView.swift`
- Modify: `ios/InterviewCoach/InterviewCoach.xcodeproj/project.pbxproj`

- [ ] **Step 1: Inspect current Xcode project references before editing**

Confirm that new Swift files require explicit `PBXFileReference`, group, build file, and Sources phase entries.

- [ ] **Step 2: Add DTO**

```swift
struct InterviewCoachAgentDTO: Decodable, Equatable {
    let id: String
    let targetId: String
    let status: String
    let currentStage: String
    let currentGoal: String
    let activeFocusDimensions: [String]
    let nextRecommendedAction: AgentRecommendedActionDTO
    let lastEventType: String?
    let lastDecisionSummary: String?
    let lastRunOutcome: String
    let lastErrorType: String?
    let lastRunAt: String?
    let version: Int
    let createdAt: String
    let updatedAt: String
}

struct AgentRecommendedActionDTO: Decodable, Equatable {
    let type: String
    let title: String
}
```

- [ ] **Step 3: Add coach view**

The view must show current goal, focus dimensions, next action, and last decision summary using a restrained `List` / `Section` layout.

```swift
struct CoachAgentView: View {
    let target: InterviewTargetDTO
    let authService: AuthService

    @State private var agent: InterviewCoachAgentDTO?
    @State private var isLoading = false
    @State private var errorMessage: String?
}
```

On `.task`:

1. POST `/api/targets/{id}/coach-agent/session-start`.
2. GET `/api/targets/{id}/coach-agent`.

Provide loading, error, empty, failed-Agent, and success states. Do not display Provider, Prompt, tool calls, or metrics.

- [ ] **Step 4: Register Swift files in Xcode project**

Add both files to the correct DTO and Features groups and to the application Sources phase.

- [ ] **Step 5: Build iOS project**

Run:

```bash
rtk xcodebuild -project ios/InterviewCoach/InterviewCoach.xcodeproj \
  -scheme InterviewCoach \
  -destination 'generic/platform=iOS Simulator' \
  build
```

Expected: `BUILD SUCCEEDED`.

- [ ] **Step 6: Stage, detect changes, and commit Task 40A**

Commit summary:

```text
feat(ios): 展示持续面试教练状态
```

### Task 40B: Connect Recommended Actions To Existing Flows

**Files:**

- Modify: `ios/InterviewCoach/InterviewCoach/Features/CoachAgent/CoachAgentView.swift`
- Modify: `ios/InterviewCoach/InterviewCoach/Features/Targets/TargetDetailView.swift`

- [ ] **Step 1: Add coach entry to target detail**

Use an icon-and-text command:

```swift
Button {
    showCoachAgent = true
} label: {
    Label("持续面试教练", systemImage: "sparkles.rectangle.stack")
}
```

- [ ] **Step 2: Map action types to existing destinations**

```text
confirmProfile -> ProfileInputView
generateJobBrief -> JobBriefView
startAssessment -> AssessmentView
generateTrainingPlan / continueTraining -> TrainingPlanView
startMockInterview -> MockInterviewView
reviewProgress -> ProgressDashboardView
reviewReport -> ReportListView
none -> no navigation
```

Unknown action types must not navigate and must show a recoverable unavailable state.

- [ ] **Step 3: Build iOS project**

```bash
rtk xcodebuild -project ios/InterviewCoach/InterviewCoach.xcodeproj \
  -scheme InterviewCoach \
  -destination 'generic/platform=iOS Simulator' \
  build
```

Expected: `BUILD SUCCEEDED`.

- [ ] **Step 4: Stage, detect changes, and commit Task 40B**

Commit summary:

```text
feat(ios): 连接 Agent 下一步教练行动
```

## 10. Task 41: Real AI Regression, Privacy, And Release Hardening

### Task 41A: Expand Default Reliability And Privacy Tests

**Files:**

- Test: `backend/src/test/java/com/interviewcoach/agent/InterviewCoachAgentControllerTest.java`
- Test: `backend/src/test/java/com/interviewcoach/agent/InterviewCoachAgentRunnerTest.java`
- Test: `backend/src/test/java/com/interviewcoach/agent/AgentMetricsTest.java`
- Test: `backend/src/test/java/com/interviewcoach/agent/AgentPrivacyTest.java`
- Test: `backend/src/test/java/com/interviewcoach/auth/controller/AuthControllerTest.java`

- [ ] **Step 1: Add privacy tests**

Assert Agent entity, event entity, logs captured during Agent run, and Micrometer tags do not contain:

```text
resumeText
rawResume
projectRawText
originalText
Authorization
apiKey
prompt
completion
known user answer fixture text
```

- [ ] **Step 2: Add failure-recovery tests**

Verify:

- Provider failure marks event failed.
- Business fact remains completed.
- Retry can later complete the event.
- Cross-user Agent query and event recording are rejected.
- Account deletion removes Agent and events.

- [ ] **Step 3: Run focused and full backend tests**

```bash
rtk mvn -q -f backend/pom.xml \
  -Dtest=InterviewCoachAgentControllerTest,InterviewCoachAgentRunnerTest,AgentMetricsTest,AgentPrivacyTest,AuthControllerTest test
rtk mvn -q -f backend/pom.xml test
```

Expected: PASS.

- [ ] **Step 4: Stage, detect changes, and commit Task 41A**

Commit summary:

```text
test(agent): 补齐 Agent 隔离与隐私回归
```

### Task 41B: Add Live AI Agent Acceptance

**Files:**

- Modify: `backend/src/test/java/com/interviewcoach/acceptance/AiLiveSmokeTest.java`
- Modify: `backend/src/test/java/com/interviewcoach/acceptance/AiContentQualityTest.java`
- Modify: `docs/ai/prompt-contracts.md`

- [ ] **Step 1: Add live smoke assertions**

Extend the existing real AI smoke journey to wait for and verify:

- Assessment completion produces a parseable Agent decision.
- Agent recommends a training-oriented next action.
- Training completion changes Agent reasoning based on latest performance.
- Mock interview completion produces a review or continued-training action.

Do not accept skipped Agent assertions as a pass.

- [ ] **Step 2: Add content-quality cases**

Add explicit Agent quality cases for:

- Java backend payment role.
- AI application / RAG Agent role.
- Data platform role.
- Rejected memory does not reappear as fact.
- Agent does not invent candidate experience or business metrics.
- Recommended action is compatible with derived product stage.

- [ ] **Step 3: Run live AI smoke**

Run:

```bash
rtk /bin/zsh -lc 'cd backend && set -a; source .env; set +a; mvn -q -Dtest=AiLiveSmokeTest test'
```

Expected: PASS with no skipped Agent product assertions.

- [ ] **Step 4: Run full live AI content quality test**

Run:

```bash
rtk /bin/zsh -lc 'cd backend && set -a; source .env; set +a; mvn -q -Dtest=AiContentQualityTest test'
```

Expected: PASS. If external service failure, quota, cost, or timeout prevents completion, final output must explicitly state: `未完成 AI 产品能力验收`.

- [ ] **Step 5: Stage, detect changes, and commit Task 41B**

Commit summary:

```text
test(agent): 增加真实 AI Agent 质量验收
```

### Task 41C: Update API, Provider, Privacy, And Release Documentation

**Files:**

- Modify: `docs/api/openapi.yaml`
- Modify: `docs/ai/prompt-contracts.md`
- Modify: `docs/ai/provider-contracts.md`
- Modify: `docs/privacy/data-policy.md`
- Modify: `README.md`
- Modify: `docs/product/vibecoding-development-plan.md`
- Modify: `CLAUDE.md`
- Modify: `AGENTS.md`

- [ ] **Step 1: Update documentation**

Document:

- Agent identity and endpoints.
- `agentDecision` cloud real AI requirement.
- Agent runtime budgets and retry behavior.
- Tool whitelist and low-risk metrics.
- Agent state/event privacy inventory.
- Live AI acceptance commands.
- Phase 4 Task 34-41 completion status only after all tests pass.

- [ ] **Step 2: Run documentation consistency checks**

Run:

```bash
rtk rg -n "Task 1-33|Task 34-41|Phase 4|agentDecision|coach-agent" \
  AGENTS.md CLAUDE.md README.md docs
rtk git diff --check
```

Expected: no stale statements that claim Task 1-33 is the latest approved work, and no whitespace errors.

- [ ] **Step 3: Run final backend and iOS verification**

```bash
rtk mvn -q -f backend/pom.xml test
rtk xcodebuild -project ios/InterviewCoach/InterviewCoach.xcodeproj \
  -scheme InterviewCoach \
  -destination 'generic/platform=iOS Simulator' \
  build
```

Expected: PASS and `BUILD SUCCEEDED`.

- [ ] **Step 4: Perform final completion audit**

For every Phase 4 acceptance item, identify authoritative evidence:

```text
Persistent Agent identity -> entity/repository/API tests
Cloud real AI decision -> gateway task gate + live AI tests
No local model product behavior -> provider contract + live AI acceptance
Event-driven updates -> integration tests and event records
Failure does not roll back facts -> recovery tests
Whitelist tools and budgets -> runner/tool tests and metrics
Memory trust semantics -> validator and rejected-memory live case
Safe plan adjustment -> training tests
iOS persistent coach experience -> successful build and manual navigation verification
Privacy -> AgentPrivacyTest and documentation inventory
```

Do not mark Phase 4 complete when any evidence is missing or indirect.

- [ ] **Step 5: Stage, run GitNexus change detection, and commit Task 41C**

Commit summary:

```text
docs(agent): 完成 Phase 4 发布约束同步
```

## 11. Final Verification Commands

Run after Task 41:

```bash
rtk mvn -q -f backend/pom.xml test
rtk xcodebuild -project ios/InterviewCoach/InterviewCoach.xcodeproj \
  -scheme InterviewCoach \
  -destination 'generic/platform=iOS Simulator' \
  build
rtk /bin/zsh -lc 'cd backend && set -a; source .env; set +a; mvn -q -Dtest=AiLiveSmokeTest test'
rtk /bin/zsh -lc 'cd backend && set -a; source .env; set +a; mvn -q -Dtest=AiContentQualityTest test'
```

Expected:

- Default backend tests pass.
- iOS build succeeds.
- Live AI smoke passes without skipped Agent product assertions.
- Full `AiContentQualityTest` passes, or final output explicitly states `未完成 AI 产品能力验收`.

## 12. Plan Self-Review

### 12.1 Specification Coverage

| Approved Requirement | Implementation Evidence Planned |
|----------------------|---------------------------------|
| One persistent Agent per `userId + targetId` | Task 34 entity uniqueness, creation lifecycle, query API, deletion tests |
| Cloud real AI reasoning, no local model product behavior | Task 35 `agentDecision` real-AI gate and Task 41 live acceptance |
| Event-driven updates across the coaching lifecycle | Task 35 persistent event record and Task 36 business integrations |
| Agent failure does not roll back business facts | AFTER_COMMIT dispatch, failed event status, retry tests |
| Correct user Provider in async execution | `AgentUserExecutionContext` and security-context tests |
| Unified current goal, focus dimensions, and next action | Task 37 stage resolver and recommendation validation |
| Whitelist-only tools and no infinite loop | Task 38 tool registry, two-pass loop, call budgets |
| Long-term trust semantics remain in CoachingMemory | Task 39 validated decision-carried memory and rejected-memory live case |
| Reduce unconscious duplicate AI calls | Task 39 removal of direct post-result memory AI calls |
| Safe training plan adjustment | Task 39 pending-task-only replacement tests |
| iOS persistent coach experience | Task 40 DTO, view, navigation, and build |
| Privacy and low-risk observability | Task 38 metrics and Task 41 privacy tests/docs |
| Customer-facing quality requires real AI | Task 41 live smoke and full `AiContentQualityTest` |

### 12.2 Key Risks Addressed

| Risk | Plan Mitigation |
|------|-----------------|
| In-memory event lost after business commit | Persist `CoachEvent` with retry status |
| AI failure rolls back completed work | Process only after business transaction commit |
| Async thread uses wrong Provider or platform fallback | Establish scoped user SecurityContext before `AiModelGateway` |
| Event deletion violates Agent foreign key | Delete `CoachEvent` before Agent and target/user |
| Repeated app opens create excessive AI cost | Six-hour deterministic session-start bucket |
| Obvious prerequisite steps waste cloud model calls | Deterministic decision policy before AI reasoning |
| Model asks for unknown tools or loops forever | Registry, two-pass limit, model/tool budgets |
| Early implementation silently ignores unsupported decision fields | Require empty tools and false mutation flags until corresponding tasks land |
| Decision-derived updates partially commit | Apply Agent state, memory, plan changes, and event completion in one transaction |
| Agent loses candidate understanding without raw answers | Use confirmed summaries and structured completed fact summaries only |
| AI-generated memory claims user correction authority | Reserve `corrected` and `rejected` for explicit user actions |

### 12.3 Review Result

- Spec coverage: complete.
- Placeholder scan: clean; every implementation step names concrete behavior, files, and verification.
- Type consistency: `AgentDecisionDto`, tool calls, memory updates, plan adjustments, and API DTO names are consistent across tasks.
- Scope: Task 34-41 remains one Phase 4 master plan, but each commit slice produces independently testable software and preserves the current coaching flow.
- Code implementation status: not started.

## 13. Implementation Handoff

Do not begin code implementation until this plan has been reviewed and approved.

Recommended execution mode after approval:

```text
superpowers:subagent-driven-development
```

Use one fresh implementation subagent per small commit slice, then perform requirements review and code-quality review before moving to the next slice.
