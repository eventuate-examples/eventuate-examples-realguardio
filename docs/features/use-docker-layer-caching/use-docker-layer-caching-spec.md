# Docker Layer Caching Implementation Specification

## Executive Summary

Implement Docker layer caching for GitHub Actions to significantly speed up the `test-compose-up` job by leveraging GitHub Actions Cache backend with Docker buildx bake. The implementation will be transparent to local development while providing automatic caching in CI environments.

---

## Goals and Success Criteria

### Primary Goals
1. Reduce GitHub Actions build times for the `test-compose-up` job
2. Maintain backward compatibility with local development workflows
3. Implement with minimal code changes
4. Ensure builds always succeed regardless of cache state

### Success Criteria
- ✅ Second consecutive build shows significant speedup (30-50%+ for unchanged code)
- ✅ Builds with only code changes show 20-40% speedup (dependency cache hit)
- ✅ Build logs show cache hits with `BUILDKIT_PROGRESS=plain`
- ✅ Local development workflow remains unchanged
- ✅ Minimal changes to `.github/workflows/test.yml` (reorder steps, add buildx, call mise task)

---

## Architecture Overview

### Technology Stack
- **Caching Backend**: GitHub Actions Cache (`type=gha`)
  - 10GB free storage per repository
  - Automatic 7-day expiration for unused caches
  - Native integration with GitHub Actions
- **Build Tool**: Docker buildx bake
  - Native docker-compose.yml conversion
  - Built-in cache management
  - Parallel build support
- **Build Orchestration**: mise.toml (environment-controlled)

### High-Level Architecture

```
┌─────────────────────────────────────────────────────────┐
│ GitHub Actions Workflow (.github/workflows/test.yml)   │
│                                                         │
│  1. Setup Docker Buildx                                │
│  2. Set GITHUB_ACTIONS=true (automatic)                │
│  3. Call mise task                                     │
└────────────────────┬────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────┐
│ mise.toml                                               │
│                                                         │
│  Detects: GITHUB_ACTIONS=true OR USE_DOCKER_CACHE=true│
│                                                         │
│  IF caching enabled:                                   │
│    Phase 1: docker buildx bake (with cache)            │
│    Phase 2: docker compose up --no-build               │
│  ELSE:                                                 │
│    docker compose up --build (standard)                │
└────────────────────┬────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────┐
│ Docker Buildx Bake                                      │
│                                                         │
│  • Converts docker-compose.yml on-the-fly              │
│  • Builds 6 services in parallel                       │
│  • Injects cache configuration via CLI flags           │
│  • cache-from: type=gha (read from GitHub cache)       │
│  • cache-to: type=gha,mode=max (write to GitHub cache) │
└─────────────────────────────────────────────────────────┘
```

---

## Detailed Design

### 1. Services and Build Targets

#### Services to Build (6 total)
All services will have caching enabled with identical configuration:

1. **realguardio-iam-service**
   - Context: `./realguardio-iam-service`
   - Dockerfile variants: `Dockerfile` (default)
   - Build args: `baseImageVersion`, `serviceImageVersion`

2. **realguardio-security-system-service**
   - Context: `./`
   - Dockerfile variants: `Dockerfile-multi-stage` (CI), `Dockerfile-local` (local dev)
   - Build args: `baseImageVersion`, `serviceImageVersion`

3. **realguardio-bff**
   - Context: `./realguardio-bff`
   - Dockerfile variants: `Dockerfile` (default)
   - Type: Next.js application

4. **realguardio-customer-service**
   - Context: `./`
   - Dockerfile variants: `Dockerfile-multi-stage` (CI), `Dockerfile-local` (local dev)
   - Build args: `baseImageVersion`, `serviceImageVersion`

5. **realguardio-orchestration-service**
   - Context: `./realguardio-orchestration-service`
   - Dockerfile variants: `Dockerfile-multi-stage` (CI), `Dockerfile-local` (local dev)
   - Build args: `baseImageVersion`, `serviceImageVersion`

6. **realguardio-oso-integration-service**
   - Context: `./realguardio-oso-integration-service`
   - Dockerfile variants: `Dockerfile-multi-stage` (CI), `Dockerfile-local` (local dev)
   - Build args: `baseImageVersion`, `serviceImageVersion`

#### Services to Pull Only (7 total)
These services use pre-built images and are not part of the build phase:
- `customer-service-db`
- `security-system-service-db`
- `orchestration-service-db`
- `jaeger`
- `kafka`
- `oso-service`
- `cdc`

### 2. Cache Strategy

#### Cache Key Structure
```
cache-key = docker-<service-name>-<dockerfile-variant>-<branch>-<dependency-hash>-<source-hash>
```

**Components:**
- `service-name`: Service name from docker-compose.yml
- `dockerfile-variant`: `multi-stage` or `local`
- `branch`: Git branch name (from `GITHUB_REF_NAME`)
- `dependency-hash`: Hash of dependency files
- `source-hash`: Hash of source files

#### Cache Scope and Lifecycle
- **Scope**: Per-branch isolation
- **Expiration**: 7 days of inactivity (GitHub Actions default)
- **Storage**: Up to 10GB per repository (GitHub Actions free tier)
- **Retention**: Automatic cleanup by GitHub Actions

#### Cache Invalidation Triggers

**Dependency Layer** (cached longest):
- Java services:
  - `build.gradle`
  - `gradle.properties`
  - `settings.gradle`
- Next.js service:
  - `package.json`
  - `package-lock.json`

**Source Code Layer** (invalidated more frequently):
- All `.java` files
- All `.ts`, `.tsx`, `.js`, `.jsx` files
- Configuration files (`application.yml`, etc.)
- Dockerfile changes

#### Layered Caching Model
```
┌─────────────────────────────────────────┐
│ Base Image Layer                        │ ← Rarely changes
├─────────────────────────────────────────┤
│ Dependency Installation Layer           │ ← Changes when deps change
│ (Gradle deps / npm modules)             │
├─────────────────────────────────────────┤
│ Source Code Compilation Layer           │ ← Changes frequently
├─────────────────────────────────────────┤
│ Final Image Layer                       │
└─────────────────────────────────────────┘

Cache Strategy:
- cache-from: Reads all layers from GitHub cache
- cache-to: Writes all layers (mode=max) to GitHub cache
```

### 3. Build Process Flow

#### Phase 1: Build with Caching
```bash
docker buildx bake \
  -f docker-compose.yml \
  --set '*.cache-from=type=gha,scope=<service>-<variant>-<branch>' \
  --set '*.cache-to=type=gha,mode=max,scope=<service>-<variant>-<branch>' \
  realguardio-iam-service \
  realguardio-security-system-service \
  realguardio-bff \
  realguardio-customer-service \
  realguardio-orchestration-service \
  realguardio-oso-integration-service
```

**Key Options:**
- `-f docker-compose.yml`: Use docker-compose.yml as input
- `--set '*.cache-from=...'`: Read cache from GitHub Actions Cache
- `--set '*.cache-to=...,mode=max'`: Write all layers to cache (not just final image)
- Service list: Explicit targets to build (excludes pull-only services)

#### Phase 2: Start Services
```bash
docker compose up -d --wait --no-build
```

**Key Options:**
- `-d`: Detached mode
- `--wait`: Wait for services to be healthy
- `--no-build`: Use images built in Phase 1, pull pre-built images

#### Standard Build (No Caching)
```bash
docker compose up -d --build --wait
```

Used when neither `GITHUB_ACTIONS` nor `USE_DOCKER_CACHE` is set.

### 4. Environment Detection

#### Environment Variables

**Auto-Detected (GitHub Actions):**
```bash
GITHUB_ACTIONS=true           # Automatically set by GitHub Actions
GITHUB_REF_NAME=<branch>      # Branch name for cache scoping
```

**Manual Override (Local Testing):**
```bash
USE_DOCKER_CACHE=true         # Opt-in to caching for local testing
```

**Dockerfile Variant Control:**
```bash
DOCKERFILE_SUFFIX=-multi-stage    # Default in CI
DOCKERFILE_SUFFIX=-local          # Used by mise.toml for local dev
```

#### Detection Logic in mise.toml
```toml
[tasks.compose-up]
run = '''
  if [ "$GITHUB_ACTIONS" = "true" ] || [ "$USE_DOCKER_CACHE" = "true" ]; then
    # Phase 1: Build with caching
    docker buildx bake -f docker-compose.yml \
      --set '*.cache-from=type=gha,scope=...' \
      --set '*.cache-to=type=gha,mode=max,scope=...' \
      [service list]

    # Phase 2: Start all services
    docker compose up -d --wait --no-build
  else
    # Standard build without caching
    docker compose up -d --build --wait
  fi
'''
```

---

## Implementation Details

### File Changes

#### 1. `.github/workflows/test.yml`

Both jobs need buildx setup since both build Docker images.

**Job 1: `test` job (line 11)**

**Current order (lines 14-41):**
```yaml
    - name: Checkout code
      uses: actions/checkout@v4

    - name: Cache npm dependencies
      uses: actions/setup-node@v4

    - uses: jdx/mise-action@v2
      with:
        version: ${{ env.MISE_VERSION }}

    # ... other steps ...

    - name: Build IAM service container
      run: mise iam-service-compose-build  # ← Builds Docker image
```

**Add after mise-action setup (after line 26):**
```yaml
    - name: Set up Docker Buildx
      uses: docker/setup-buildx-action@v3
```

**No other changes needed** - `mise iam-service-compose-build` will use caching automatically.

---

**Job 2: `test-compose-up` job (line 103)**

**Current order (lines 107-113):**
```yaml
    - name: Checkout code
      uses: actions/checkout@v4

    - name: Compose up
      run: docker compose up -d --build --wait  # ← Direct call, not using mise

    - uses: jdx/mise-action@v2
      with:
        version: ${{ env.MISE_VERSION }}
```

**Required changes:**

1. **Move mise setup before compose up**
2. **Add buildx setup**
3. **Replace direct docker compose call with mise task**

**New order:**
```yaml
    - name: Checkout code
      uses: actions/checkout@v4

    - uses: jdx/mise-action@v2
      with:
        version: ${{ env.MISE_VERSION }}

    - name: Set up Docker Buildx
      uses: docker/setup-buildx-action@v3

    - name: Compose up
      run: mise run compose-up
```

**Summary of changes to test.yml:**
- **test job**: Add buildx setup after mise-action (line ~27)
- **test-compose-up job**: Move mise-action before compose up, add buildx setup, change command to `mise run compose-up`

#### 2. `mise.toml`

**Tasks to Update:**

Only tasks that are directly or indirectly called by test.yml need caching support:

1. **`compose-up`** (line 45) - Builds all 6 services
   - Current: `DOCKERFILE_SUFFIX=-local docker compose up -d ${COMPOSE_OPTS:---build}`
   - **Called by test.yml**:
     - test job line 81: `mise go` → calls `mise compose-up`
     - test-compose-up job line 111: will change to `mise run compose-up`

2. **`iam-service-compose-build`** (line 70) - Builds IAM service only
   - Current: `DOCKERFILE_SUFFIX=-local docker compose build realguardio-iam-service`
   - **Called by test.yml**:
     - test job line 41: `mise iam-service-compose-build` (direct)

3. **`iam-service-compose-up`** (line 67) - Builds and starts IAM service
   - Current: `DOCKERFILE_SUFFIX=-local docker compose up -d ${COMPOSE_OPTS:---build} realguardio-iam-service`
   - **Called by test.yml**:
     - test job line 75: `mise go-local` → depends on `iam-service-compose-up`

4. **`security-system-service-compose-up`** (line 80) - Builds and starts security system service
   - Current: `DOCKERFILE_SUFFIX=-local docker compose up -d ${COMPOSE_OPTS:---build} realguardio-security-system-service`
   - **Called by test.yml**:
     - test job line 75: `mise go-local` → calls `mise security-system-service-compose-up`

**Note on DOCKERFILE_SUFFIX:**
- These tasks use `DOCKERFILE_SUFFIX=-local` (for local dev with pre-built JARs)
- In CI, may want to use multi-stage variant instead
- The caching logic should respect DOCKERFILE_SUFFIX

**Implementation Pattern:**

Create a helper function that can be reused across all tasks:

```toml
# Helper function for Docker builds with optional caching
[tasks.docker-build-helper]
run = '''
#!/usr/bin/env bash
set -euo pipefail

# Arguments:
# $1 = SERVICE (or empty for all services)
# $2 = ACTION (build, up, up-wait)

SERVICE="${1:-}"
ACTION="${2:-up}"

# Detect if caching should be enabled
USE_CACHE=false
if [ "${GITHUB_ACTIONS:-false}" = "true" ] || [ "${USE_DOCKER_CACHE:-false}" = "true" ]; then
  USE_CACHE=true
fi

# Get branch name for cache scoping
BRANCH_NAME="${GITHUB_REF_NAME:-$(git branch --show-current)}"

# Build command options
COMPOSE_OPTS="${COMPOSE_OPTS:---build}"
DOCKERFILE_SUFFIX="${DOCKERFILE_SUFFIX:--local}"

# Services to build (when SERVICE is empty)
ALL_SERVICES=(
  "realguardio-iam-service"
  "realguardio-security-system-service"
  "realguardio-bff"
  "realguardio-customer-service"
  "realguardio-orchestration-service"
  "realguardio-oso-integration-service"
)

if [ "$USE_CACHE" = "true" ]; then
  echo "🚀 Building with Docker layer caching..."

  # Determine services to build
  if [ -z "$SERVICE" ]; then
    SERVICES=("${ALL_SERVICES[@]}")
  else
    SERVICES=("$SERVICE")
  fi

  # Phase 1: Build with caching
  docker buildx bake \
    -f docker-compose.yml \
    --set '*.cache-from=type=gha,scope=build-'"${BRANCH_NAME}" \
    --set '*.cache-to=type=gha,mode=max,scope=build-'"${BRANCH_NAME}" \
    "${SERVICES[@]}"

  # Phase 2: Start services based on action
  case "$ACTION" in
    build)
      # Just build, don't start
      ;;
    up)
      echo "🚀 Starting services..."
      if [ -z "$SERVICE" ]; then
        docker compose up -d --no-build
      else
        docker compose up -d --no-build "$SERVICE"
      fi
      ;;
    up-wait)
      echo "🚀 Starting services..."
      if [ -z "$SERVICE" ]; then
        docker compose up -d --wait --no-build
      else
        docker compose up -d --wait --no-build "$SERVICE"
      fi
      ;;
  esac
else
  echo "🚀 Building without caching (local development)..."

  # Standard docker compose commands
  case "$ACTION" in
    build)
      if [ -z "$SERVICE" ]; then
        DOCKERFILE_SUFFIX=$DOCKERFILE_SUFFIX docker compose build
      else
        DOCKERFILE_SUFFIX=$DOCKERFILE_SUFFIX docker compose build "$SERVICE"
      fi
      ;;
    up)
      if [ -z "$SERVICE" ]; then
        DOCKERFILE_SUFFIX=$DOCKERFILE_SUFFIX docker compose up -d $COMPOSE_OPTS
      else
        DOCKERFILE_SUFFIX=$DOCKERFILE_SUFFIX docker compose up -d $COMPOSE_OPTS "$SERVICE"
      fi
      ;;
    up-wait)
      if [ -z "$SERVICE" ]; then
        DOCKERFILE_SUFFIX=$DOCKERFILE_SUFFIX docker compose up -d --wait $COMPOSE_OPTS
      else
        DOCKERFILE_SUFFIX=$DOCKERFILE_SUFFIX docker compose up -d --wait $COMPOSE_OPTS "$SERVICE"
      fi
      ;;
  esac
fi
'''
```

**Then update each task to use the helper:**

```toml
[tasks.compose-up]
depends = ["assemble", "bff-dev-stop"]
run = "mise run docker-build-helper '' up"

[tasks.iam-service-compose-build]
run = "mise run docker-build-helper realguardio-iam-service build"

[tasks.iam-service-compose-up]
run = "mise run docker-build-helper realguardio-iam-service up"

[tasks.security-system-service-compose-up]
depends = ["assemble"]
run = "mise run docker-build-helper realguardio-security-system-service up"
```

**Alternative: Inline implementation for compose-up only**

If you prefer not to create a helper, update each task individually. Here's the compose-up example:

```toml
[tasks.compose-up]
depends = ["assemble", "bff-dev-stop"]
run = '''
#!/usr/bin/env bash
set -euo pipefail

USE_CACHE=false
if [ "${GITHUB_ACTIONS:-false}" = "true" ] || [ "${USE_DOCKER_CACHE:-false}" = "true" ]; then
  USE_CACHE=true
fi

BRANCH_NAME="${GITHUB_REF_NAME:-$(git branch --show-current)}"
DOCKERFILE_SUFFIX="${DOCKERFILE_SUFFIX:--local}"
COMPOSE_OPTS="${COMPOSE_OPTS:---build}"

ALL_SERVICES=(
  "realguardio-iam-service"
  "realguardio-security-system-service"
  "realguardio-bff"
  "realguardio-customer-service"
  "realguardio-orchestration-service"
  "realguardio-oso-integration-service"
)

if [ "$USE_CACHE" = "true" ]; then
  docker buildx bake \
    -f docker-compose.yml \
    --set '*.cache-from=type=gha,scope=build-'"${BRANCH_NAME}" \
    --set '*.cache-to=type=gha,mode=max,scope=build-'"${BRANCH_NAME}" \
    "${ALL_SERVICES[@]}"
  docker compose up -d --no-build
else
  DOCKERFILE_SUFFIX=$DOCKERFILE_SUFFIX docker compose up -d $COMPOSE_OPTS
fi
'''
```

**Key Implementation Details:**
- Helper function approach: Reusable, DRY, easier to maintain
- Inline approach: Simpler, no dependencies between tasks
- Both respect DOCKERFILE_SUFFIX for local dev
- Both respect COMPOSE_OPTS for --build flag control
- Simplified cache scope (per-branch, not per-service)

### 3. Dockerfile Variants

**No changes needed** - Both variants work with the existing approach:
- `Dockerfile-multi-stage`: Used in CI (default `DOCKERFILE_SUFFIX`)
- `Dockerfile-local`: Used locally (set via `DOCKERFILE_SUFFIX=-local` in other mise tasks)

The `${DOCKERFILE_SUFFIX:--multi-stage}` substitution in docker-compose.yml handles this automatically.

---

## Cache Configuration Details

### GitHub Actions Cache Settings

**Cache Type:** `type=gha`
- Uses GitHub Actions cache API
- Automatically managed by GitHub
- No additional authentication needed

**Cache Mode:** `mode=max`
- Exports all intermediate layers (not just final image)
- Maximizes cache reuse
- Larger cache size but better performance

**Cache Scope Pattern:**
```
build-<branch-name>
```

**Simplified Scope Rationale:**
- Single cache per branch for all services
- Reduces cache key complexity
- Sufficient for most use cases
- Can be refined later if needed

### Cache Size Estimation

**Per Service Estimate:**
- Java multi-stage builds: ~500MB-1GB per service
- Next.js build: ~200-500MB
- Total: ~3-5GB for all 6 services

**With 10GB GitHub Cache Limit:**
- Sufficient for 2-3 branches actively building
- 7-day expiration prevents bloat
- Automatic LRU eviction if limit reached

---

## Error Handling and Fallback

### Failure Scenarios

#### 1. Cache Miss (First Build)
**Behavior:** Full build from scratch
**Impact:** Same build time as before caching
**Resolution:** Automatic - cache will be populated for next build

#### 2. Cache Unavailable (GitHub API Issue)
**Behavior:** Buildx automatically falls back to full build
**Impact:** Build succeeds but slower
**Resolution:** Automatic - no manual intervention needed

#### 3. Cache Corruption
**Behavior:** Build fails with cache error
**Impact:** Rare, but possible
**Resolution:** Buildx retries without cache, or manual cache clear

#### 4. Build Failure (Unrelated to Cache)
**Behavior:** Build fails as it would without caching
**Impact:** Same as before
**Resolution:** Fix the actual build issue

### Error Handling Strategy

**Philosophy:** Cache is an optimization, not a requirement.

**Implementation:**
- No try-catch needed - buildx handles failures gracefully
- Cache errors are logged but don't fail the build
- Natural degradation to full build if cache unavailable

**Monitoring:**
- Use `BUILDKIT_PROGRESS=plain` for verbose logging
- GitHub Actions automatically displays cache hit/miss statistics
- Monitor build times to verify effectiveness

---

## Testing Strategy

### Pre-Implementation Testing

#### 1. Local Testing (Optional)
Test caching behavior locally before CI deployment:

```bash
# Enable caching locally
export USE_DOCKER_CACHE=true

# First build (cold cache)
mise compose-up

# Second build (warm cache) - should be faster
mise compose-up

# Change a source file
touch realguardio-iam-service/src/main/java/SomeFile.java

# Third build (partial cache hit) - dependencies cached
mise compose-up
```

**Expected Results:**
- First build: Normal speed, cache populated
- Second build: Significantly faster (all cache hits)
- Third build: Faster (dependency cache hit, source rebuild)

#### 2. Verify mise.toml Logic
```bash
# Test detection logic
GITHUB_ACTIONS=true mise compose-up          # Should use cache
USE_DOCKER_CACHE=true mise compose-up        # Should use cache
mise compose-up                               # Should NOT use cache
```

### Post-Implementation Testing

#### 1. GitHub Actions Validation

**Test Case 1: Cache Population**
1. Create a new branch
2. Push changes to trigger CI
3. First build should succeed (cache miss)
4. Verify cache is created in GitHub Actions UI

**Test Case 2: Cache Reuse**
1. Push another commit to same branch (no code changes)
2. Second build should be significantly faster
3. Check logs for cache hit messages

**Test Case 3: Partial Cache Hit**
1. Modify only source code (no dependency changes)
2. Push commit
3. Build should be faster (dependency layers cached)
4. Verify only source layers rebuild

**Test Case 4: Full Cache Miss**
1. Modify dependency files (build.gradle or package.json)
2. Push commit
3. Build should rebuild from dependencies onward
4. Verify new cache is created

#### 2. Verification Checklist

- [ ] Buildx setup step added to test.yml
- [ ] mise.toml updated with caching logic
- [ ] First build succeeds (cache miss)
- [ ] Second build succeeds and is faster (cache hit)
- [ ] Build logs show cache operations
- [ ] Local development workflow unchanged (no caching)
- [ ] Local caching works when `USE_DOCKER_CACHE=true` set
- [ ] All 6 services build successfully
- [ ] Docker compose up starts all services
- [ ] Existing tests pass

---

## Rollback Plan

### Quick Rollback
If caching causes issues, rollback is simple:

**Option 1: Disable via Environment Variable**
```yaml
# In .github/workflows/test.yml
env:
  USE_DOCKER_CACHE: false  # Override detection
```

**Option 2: Revert mise.toml**
```bash
git revert <commit-hash>
```

**Option 3: Comment Out Logic**
Comment out the buildx bake section in mise.toml, keeping standard docker compose.

### Gradual Rollout Strategy
If preferred, implement in stages:

1. **Stage 1**: Add logic to mise.toml, but don't enable in CI (test locally only)
2. **Stage 2**: Enable for a single non-critical branch
3. **Stage 3**: Enable for all branches
4. **Stage 4**: Monitor and optimize cache keys if needed

---

## Performance Expectations

### Build Time Estimates

**Current State (No Caching):**
- Full build: ~10-15 minutes (estimate)
- All 6 services build from scratch every time

**Expected with Caching:**

**Scenario 1: No Changes (Cache Hit)**
- Build time: ~2-3 minutes (80-85% reduction)
- All layers served from cache
- Only image pulling and validation

**Scenario 2: Source Code Changes Only**
- Build time: ~5-7 minutes (50-60% reduction)
- Dependency layers cached
- Only source compilation and later stages rebuild

**Scenario 3: Dependency Changes**
- Build time: ~8-12 minutes (20-30% reduction)
- Base image layers cached
- Dependencies and source rebuild

**Scenario 4: First Build (Cache Miss)**
- Build time: ~10-15 minutes (0-10% slower)
- Slight overhead from cache write operations
- Acceptable for cache population

### Cache Hit Rate Targets
- **Target**: 70%+ cache hit rate across all builds
- **Measurement**: Monitor GitHub Actions cache statistics
- **Optimization**: Adjust cache keys if hit rate is lower

---

## Monitoring and Observability

### Build Logs
Enable detailed logging in mise.toml:
```bash
export BUILDKIT_PROGRESS=plain
```

**Log Indicators:**
- `CACHED [stage X/Y]` - Cache hit
- `[stage X/Y] COPY ...` with download indicators - Cache miss
- Cache export/import messages

### GitHub Actions UI
- Cache size displayed in Actions tab
- Cache hit/miss statistics
- Cache creation/expiration events

### Metrics to Track
1. **Build Duration**: Overall job time
2. **Cache Hit Rate**: Percentage of builds using cache
3. **Cache Size**: Total storage used
4. **Build Failure Rate**: Ensure caching doesn't increase failures

---

## Future Enhancements

### Potential Optimizations

#### 1. Per-Service Cache Scopes
More granular cache keys:
```
scope=<service>-<dockerfile-variant>-<branch>-<dependency-hash>
```

**Pros:**
- Better cache isolation per service
- Finer-grained invalidation

**Cons:**
- More complex cache key management
- Higher cache storage usage

#### 2. Cross-Branch Cache Sharing
Allow feature branches to use main branch cache as fallback:
```bash
--set '*.cache-from=type=gha,scope=build-'"${BRANCH_NAME}" \
--set '*.cache-from=type=gha,scope=build-main' \
```

**Pros:**
- Faster first build on new branches

**Cons:**
- More complex cache configuration

#### 3. Dependency-Only Pre-Warming
Separate workflow to pre-build and cache dependencies:

**Pros:**
- Even faster builds (dependencies always cached)

**Cons:**
- More complex workflow management

#### 4. Remote Cache Backend
Switch from GHA cache to registry cache:
```bash
--set '*.cache-to=type=registry,ref=ghcr.io/...'
```

**Pros:**
- Unlimited storage (if using own registry)
- Faster cache transfer

**Cons:**
- Requires registry authentication
- Additional infrastructure

---

## Security Considerations

### Cache Isolation
- **Per-Repository**: Cache is isolated per repository (GitHub limitation)
- **Per-Branch**: Current design isolates cache per branch
- **No Cross-Repo Access**: Cache cannot be accessed by other repositories

### Sensitive Data
- **Risk**: Build arguments or environment variables in cached layers
- **Mitigation**:
  - Don't pass secrets as build arguments
  - Use multi-stage builds to exclude secrets from final image
  - Review Dockerfiles for hardcoded credentials

### Cache Poisoning
- **Risk**: Malicious code in cached layers
- **Mitigation**:
  - Cache is per-branch (isolated from PRs)
  - Standard PR review process applies
  - Cache automatically expires after 7 days

---

## Documentation Updates

### Files to Update

#### 1. README.md
Add section on Docker build caching:
```markdown
## Building with Docker

### Local Development
```bash
mise compose-up  # Standard build, no caching
```

### Testing with Cache (Optional)
```bash
USE_DOCKER_CACHE=true mise compose-up
```

### CI/CD
Caching is automatically enabled in GitHub Actions for faster builds.
```

#### 2. CONTRIBUTING.md
Add note about cache behavior:
```markdown
## Docker Build Caching

GitHub Actions uses Docker layer caching to speed up CI builds.
No action required - it works automatically.

To test caching behavior locally:
```bash
export USE_DOCKER_CACHE=true
mise compose-up
```
```

---

## Appendix

### A. Command Reference

**Standard Build (Local Development):**
```bash
docker compose up -d --build --wait
```

**Cached Build (CI):**
```bash
# Phase 1: Build
docker buildx bake -f docker-compose.yml \
  --set '*.cache-from=type=gha,scope=build-main' \
  --set '*.cache-to=type=gha,mode=max,scope=build-main' \
  realguardio-iam-service realguardio-security-system-service \
  realguardio-bff realguardio-customer-service \
  realguardio-orchestration-service realguardio-oso-integration-service

# Phase 2: Start
docker compose up -d --wait --no-build
```

**Clear Local Cache:**
```bash
docker buildx prune --all
```

**View Cache Usage:**
```bash
docker buildx du --verbose
```

### B. Troubleshooting

#### Problem: Cache not being used
**Symptoms:** Every build takes full time
**Diagnosis:**
1. Check if buildx is set up: `docker buildx version`
2. Verify GITHUB_ACTIONS is set: `echo $GITHUB_ACTIONS`
3. Check logs for cache messages

**Solution:**
- Ensure buildx setup step in test.yml
- Verify cache scope matches between builds
- Check GitHub cache API status

#### Problem: Build fails with cache error
**Symptoms:** Error mentioning cache or buildkit
**Solution:**
- Retry the build (may be transient GitHub API issue)
- Clear cache: Settings → Actions → Management → Delete cache
- Check if 10GB cache limit exceeded

#### Problem: Cache not speeding up builds
**Symptoms:** Cache hits shown but build still slow
**Diagnosis:**
- Check which layers are being cached
- Verify dependency files haven't changed
- Review Dockerfile layer structure

**Solution:**
- Optimize Dockerfile layer ordering
- Ensure dependency installation is separate from source compilation
- Consider per-service cache scopes

### C. References

**Docker Buildx Documentation:**
- https://docs.docker.com/build/cache/backends/gha/
- https://docs.docker.com/build/bake/

**GitHub Actions:**
- https://docs.github.com/en/actions/using-workflows/caching-dependencies-to-speed-up-workflows

**mise Documentation:**
- https://mise.jdx.dev/

---

## Implementation Checklist

### Pre-Implementation
- [ ] Review specification with team
- [ ] Confirm GitHub Actions cache quota (10GB)
- [ ] Identify services to build (6 confirmed)
- [ ] Review Dockerfiles for optimization opportunities

### Implementation Phase
- [ ] Add Docker Buildx setup to `.github/workflows/test.yml`
- [ ] Update `mise.toml` with caching logic
- [ ] Test locally with `USE_DOCKER_CACHE=true`
- [ ] Verify conditional logic works (cache vs non-cache paths)
- [ ] Test with both `DOCKERFILE_SUFFIX` variants

### Testing Phase
- [ ] Push to test branch, verify first build succeeds
- [ ] Push again, verify second build is faster
- [ ] Make code-only change, verify partial cache hit
- [ ] Make dependency change, verify appropriate rebuild
- [ ] Verify local development workflow unchanged
- [ ] Run existing test suite to ensure no regressions

### Deployment Phase
- [ ] Merge to main branch
- [ ] Monitor first main branch build
- [ ] Monitor subsequent builds for cache effectiveness
- [ ] Document cache hit rates
- [ ] Update team documentation

### Post-Deployment
- [ ] Monitor build times over 1 week
- [ ] Collect feedback from team
- [ ] Optimize cache keys if needed
- [ ] Document lessons learned

---

**Specification Version:** 1.0
**Last Updated:** 2025-11-10
**Status:** Ready for Implementation
