# Docker Layer Caching for GitHub Actions - Discussion

## Idea Summary
Speed up GitHub Actions workflow by implementing Docker Layer Caching for the multi-service application build process.

## Current Architecture
- **CI/CD**: GitHub Actions workflow
- **Build orchestration**: mise.toml task runner
- **Build command**: `docker compose up --build`
- **Services to build** (6 total):
  1. realguardio-iam-service
  2. realguardio-security-system-service (multi-stage Dockerfile)
  3. realguardio-bff (Next.js)
  4. realguardio-customer-service (multi-stage Dockerfile)
  5. realguardio-orchestration-service (multi-stage Dockerfile)
  6. realguardio-oso-integration-service (multi-stage Dockerfile)

## Q&A Session

### Q1: Workflow structure clarification
**Question**: How are the builds triggered in your current setup?
**Answer**: GitHub Actions workflow → invokes mise → executes `docker compose up --build`

### Q2: Docker layer caching backend
**Question**: Which Docker layer caching backend should we use?
**Answer**: A - GitHub Actions Cache (`cache-from/cache-to: type=gha`)
**Rationale**: Best balance of simplicity, cost (free 10GB storage), and performance. No additional infrastructure needed.

### Q3: Multiple Dockerfile variants strategy
**Question**: How should we handle the multiple Dockerfile variants?
**Answer**: B - Cache both variants separately
**Details**:
- `Dockerfile-multi-stage`: For users without Java installed (full build in Docker)
- `Dockerfile-local`: For users with Java installed (uses locally built artifacts)
- Both variants need separate cache keys for optimal performance
**Rationale**: Demo app needs to support both user scenarios with fast builds

### Q4: Cache key strategy
**Question**: What should the cache key strategy be based on?
**Answer**: C - Layered strategy with separate caches for dependencies vs. source code
**Implementation**:
- **Base/dependency layer cache**: Invalidated only when dependency files change
  - Java services: `build.gradle`, `gradle.properties`, `settings.gradle`
  - Next.js service: `package.json`, `package-lock.json`
- **Source code layer cache**: Invalidated on any code change
- **Benefits**: Maximizes cache reuse since dependencies change less frequently than source code
**Rationale**: Most efficient for multi-stage builds with distinct dependency installation and compilation phases

### Q5: Integration with existing build process
**Question**: How should Docker layer caching integrate with the existing mise/docker compose workflow?
**Answer**: A - Modify GitHub Actions workflow only
**Implementation approach**:
- Add Docker Buildx setup step in GitHub Actions
- Configure caching via buildx bake to convert docker-compose.yml on-the-fly
- Inject cache configuration through GitHub Actions, keeping docker-compose.yml clean
- Local development workflow remains unchanged
**Benefits**:
- Minimal code changes
- Cache configuration lives in CI where it belongs
- No impact on developer experience
**Rationale**: Separation of concerns - CI optimizations shouldn't clutter local development setup

### Q6: Service prioritization for caching
**Question**: Which services should be prioritized for caching optimization?
**Answer**: A - Apply caching uniformly to all 6 services
**Services covered**:
1. realguardio-iam-service
2. realguardio-security-system-service
3. realguardio-bff (Next.js)
4. realguardio-customer-service
5. realguardio-orchestration-service
6. realguardio-oso-integration-service
**Rationale**: Simplest to implement and maintain. All services benefit equally with consistent cache configuration.

### Q7: Cache scope and retention policy
**Question**: What should the cache scope and retention policy be?
**Answer**: A - Per-branch caching with 7-day expiration
**Implementation**:
- Each branch maintains its own cache
- Caches automatically expire after 7 days of inactivity
- Cache key pattern: `docker-<service>-<dockerfile-variant>-<branch>-<hash>`
**Trade-offs**:
- Isolated caches per branch (no cross-contamination)
- May use more cache storage initially
- Automatic cleanup prevents long-term bloat
**Rationale**: Simplest model, predictable behavior, good for short-lived feature branches

### Q8: Concurrent service builds strategy
**Question**: How should we handle concurrent service builds?
**Answer**: B - Parallel builds with docker buildx bake
**Implementation**:
- Use `docker buildx bake` to build all 6 services simultaneously
- Buildx bake natively converts docker-compose.yml format
- Handles cache injection automatically
**Expected benefits**:
- Maximum time savings through parallelization
- GitHub Actions runners (2 CPU cores, 7GB RAM) can handle 6 parallel builds
- Reduces total build time significantly
**Rationale**: Fastest option with no additional complexity in orchestration

### Q9: DOCKERFILE_SUFFIX handling in CI
**Question**: How should we handle the DOCKERFILE_SUFFIX environment variable in CI?
**Answer**: A - Use multi-stage variant (default behavior in test-compose-up job)
**Current state**:
- `test-compose-up` job (line 111): `docker compose up -d --build --wait` → defaults to `-multi-stage`
- `mise.toml` tasks set `DOCKERFILE_SUFFIX=-local` for local development
**Caching target**:
- Focus caching on `test-compose-up` job which uses multi-stage Dockerfiles
- This job is self-contained and builds everything from scratch
**Changes required**:
- Line 111 in test.yml must change from direct `docker compose` call to `mise run compose-up`
- mise-action step must be moved before the compose-up step
- buildx setup step must be added
**Rationale**: Existing workflow uses the correct variant for CI, but needs to call mise task for caching logic.

### Q10: Implementation location for caching logic
**Question**: Where should the Docker layer caching be implemented?
**Answer**: D - Environment variable controlled in mise.toml
**Implementation approach**:
- mise.toml detects GitHub Actions environment (or custom env var)
- When caching enabled: use `docker buildx bake` with cache configuration
- When caching disabled (local dev): use standard `docker compose up --build`
- test.yml sets environment variable to enable caching before calling mise tasks
**Benefits**:
- Single source of truth in mise.toml
- No code duplication
- Transparent to developers (local builds work as before)
- CI gets caching automatically
**Rationale**: Keeps build logic centralized while allowing environment-specific optimization

### Q11: Docker-compose to buildx bake conversion method
**Question**: How should docker-compose.yml be converted to buildx bake format?
**Answer**: A - Direct conversion with runtime cache injection
**Implementation**:
- Use `docker buildx bake -f docker-compose.yml` directly
- Inject cache configuration via CLI flags: `--set '*.cache-from=...' --set '*.cache-to=...'`
- No additional configuration files needed
**Benefits**:
- Single source of truth (docker-compose.yml)
- No file duplication or drift
- Cache config applied at runtime only when needed
**Rationale**: Simplest approach with minimal maintenance overhead

### Q12: Handling services that only pull images
**Question**: How should services that only pull pre-built images be handled?
**Answer**: A - Build only with buildx bake, then compose up separately
**Implementation**:
- **Phase 1 - Build**: `docker buildx bake -f docker-compose.yml [6 custom services]` with caching
- **Phase 2 - Start**: `docker compose up -d --wait --no-build` starts full stack
  - Uses already-built images from Phase 1
  - Pulls pre-built images (postgres, Kafka, Jaeger, OSO, CDC)
**Services in each phase**:
- Build phase (6): realguardio-iam-service, realguardio-security-system-service, realguardio-bff, realguardio-customer-service, realguardio-orchestration-service, realguardio-oso-integration-service
- Pull phase (7): customer-service-db, security-system-service-db, orchestration-service-db, jaeger, kafka, oso-service, cdc
**Rationale**: Clean separation of build vs run, explicit control over what gets cached

### Q13: Error handling and fallback strategy
**Question**: What should the error handling and fallback strategy be?
**Answer**: D - Cache is always optional
**Implementation**:
- Docker buildx bake naturally degrades to full build if cache unavailable
- No special error handling required
- Builds always succeed regardless of cache state
- Cache improves performance but isn't required for correctness
**Visibility**:
- Use `BUILDKIT_PROGRESS=plain` to log cache hits/misses
- Monitor build times to verify caching effectiveness
**Rationale**: Caching is an optimization, not a requirement. Builds must always work.

### Q14: Success measurement and validation
**Question**: How should we measure success and validate the caching is working?
**Answer**: D - Simple validation test
**Validation approach**:
- Run the build twice in succession after implementation
- First build (cold cache): Establishes cache
- Second build (warm cache): Should be significantly faster
**Success criteria**:
- Build completes successfully both times
- Second build shows visible speedup compared to first
- Build logs show cache hits (visible with BUILDKIT_PROGRESS=plain)
**Simple test**:
- Make a small code change
- Run build again
- Verify dependency layers are cached, only changed code layers rebuild
**Rationale**: Practical validation without complex metrics infrastructure

### Q15: Environment variable for controlling caching
**Question**: What should the environment variable name be for controlling caching?
**Answer**: D - Combination approach
**Implementation**:
- mise.toml checks: `GITHUB_ACTIONS=true` OR `USE_DOCKER_CACHE=true`
- If either is true: Use buildx bake with caching
- If neither is true: Use standard docker compose
**Usage scenarios**:
- **GitHub Actions**: Automatic (GITHUB_ACTIONS=true already set)
- **Local testing**: Set `USE_DOCKER_CACHE=true` to test caching behavior
- **Default local dev**: Standard docker compose (no caching overhead)
**Benefits**:
- Zero changes needed to test.yml
- Developers can opt-in to test caching locally
- Future-proof for other CI systems
**Rationale**: Most flexible approach with minimal configuration

### Q16: Additional constraints and concerns
**Question**: Are there any specific constraints, concerns, or edge cases we should address?
**Answer**: E - No additional concerns
**Status**: All requirements and edge cases have been covered. Ready to finalize specification.

---

## Final Specification Summary

### Overview
Implement Docker layer caching for GitHub Actions using buildx bake with GitHub Actions Cache backend to speed up the `test-compose-up` job build times.

### Architecture Decisions

#### Core Technology Stack
- **Caching Backend**: GitHub Actions Cache (type=gha)
- **Build Tool**: Docker buildx bake
- **Cache Strategy**: Layered (separate caches for dependencies vs source code)
- **Build Parallelization**: All 6 services build simultaneously

#### Implementation Approach
- **Logic Location**: mise.toml (environment-controlled)
- **Trigger**: Auto-enabled when `GITHUB_ACTIONS=true` OR `USE_DOCKER_CACHE=true`
- **Conversion Method**: Direct buildx bake with runtime cache injection via CLI flags
- **Build Phases**:
  1. Build 6 custom services with buildx bake + caching
  2. Start full stack with docker compose up --no-build

#### Services Covered
**Build phase (6 services, all with cache)**:
1. realguardio-iam-service
2. realguardio-security-system-service
3. realguardio-bff (Next.js)
4. realguardio-customer-service
5. realguardio-orchestration-service
6. realguardio-oso-integration-service

**Both Dockerfile variants cached separately**:
- `Dockerfile-multi-stage` (used in CI, default)
- `Dockerfile-local` (future use, same cache strategy)

#### Cache Configuration
**Scope**: Per-branch with 7-day expiration
**Key Pattern**: `docker-<service>-<dockerfile-variant>-<branch>-<hash>`
**Invalidation triggers**:
- Base/dependency layer: Changes to build.gradle, gradle.properties, settings.gradle, package.json, package-lock.json
- Source code layer: Any code changes

#### Error Handling
- Cache is always optional
- Builds succeed regardless of cache state
- Natural degradation to full build if cache unavailable
- Progress logging via BUILDKIT_PROGRESS=plain

#### Validation
- Run build twice to verify caching works
- Second build should be significantly faster
- Monitor cache hits in build logs
- Test with code-only changes to verify dependency cache persistence

### Implementation Tasks

#### 1. Modify mise.toml
- Update tasks that call `docker compose up --build`
- Add conditional logic: if GITHUB_ACTIONS or USE_DOCKER_CACHE, use buildx bake
- Implement two-phase build:
  - Phase 1: `docker buildx bake -f docker-compose.yml [6 services] --set '*.cache-from=...' --set '*.cache-to=...'`
  - Phase 2: `docker compose up -d --wait --no-build`
- Generate cache keys based on service name, Dockerfile variant, branch, and content hashes

#### 2. Modify .github/workflows/test.yml
- Move mise-action step (currently line 113) to before compose up step
- Add Docker Buildx setup step after mise setup
- Replace line 111 `docker compose up -d --build --wait` with `mise run compose-up`
- GITHUB_ACTIONS already set automatically (no env var changes needed)

#### 3. Testing & Validation
- Run test-compose-up job twice on same branch
- Verify cache creation and reuse
- Test with code-only changes
- Test with dependency changes

### Success Criteria
- ✅ Builds complete successfully with and without cache
- ✅ Second build shows significant speedup
- ✅ Build logs show cache hits
- ✅ Local development workflow unchanged
- ✅ Minimal changes to .github/workflows/test.yml (reorder steps, add buildx, call mise task)

### Files to Modify
1. `mise.toml` - Add caching logic to compose-up task
2. `.github/workflows/test.yml` - Reorder steps, add buildx setup, call mise task

### Files NOT Modified
- `docker-compose.yml` - Remains unchanged
- No new configuration files needed
- Local development commands work as before

---

**Specification Complete** ✅
Ready for implementation.

