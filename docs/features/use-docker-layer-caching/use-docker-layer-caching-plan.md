# Docker Layer Caching Implementation Plan

## Overview

This implementation plan follows the Steel Thread methodology to implement Docker layer caching for GitHub Actions. Each thread represents a narrow end-to-end flow that delivers incremental value.

**Key Principles:**
- Each steel thread must deliver testable value
- Build on previous threads progressively
- Follow TDD approach where applicable
- Mark each checkbox `[x]` when completed
- All tests must pass before committing

---

## Steel Thread 1: Add Docker Buildx Setup to GitHub Actions

**Goal:** Establish the foundation for Docker layer caching by adding buildx support to both GitHub Actions jobs.

**Value Delivered:** CI environment ready for caching without changing build behavior yet.

### Tasks

#### [x] Task 1.0: Add explicit image tags to all services in docker-compose.yaml

**Prompt:**
```
Add explicit image tags to all services in docker-compose.yaml that have build sections.

Context:
- Currently only realguardio-iam-service has an explicit image tag (line 64)
- Other services rely on auto-generated image names
- For buildx bake and docker compose to work together reliably, we need explicit image tags
- This ensures consistent image naming between buildx and compose commands

Steps:
- [ ] Add image tags to the following services in docker-compose.yaml:

  realguardio-security-system-service (after line 92):
  ```yaml
  realguardio-security-system-service:
    image: eventuate-examples-realguardio-realguardio-security-system-service:latest
    build:
      context: ./
  ```

  realguardio-bff (after line 122):
  ```yaml
  realguardio-bff:
    image: eventuate-examples-realguardio-realguardio-bff:latest
    build:
      context: ./realguardio-bff
  ```

  realguardio-customer-service (after line 148):
  ```yaml
  realguardio-customer-service:
    image: eventuate-examples-realguardio-realguardio-customer-service:latest
    build:
      context: ./
  ```

  realguardio-orchestration-service (after line 177):
  ```yaml
  realguardio-orchestration-service:
    image: eventuate-examples-realguardio-realguardio-orchestration-service:latest
    build:
      context: ./realguardio-orchestration-service
  ```

  realguardio-oso-integration-service (after line 204):
  ```yaml
  realguardio-oso-integration-service:
    image: eventuate-examples-realguardio-realguardio-oso-integration-service:latest
    build:
      context: ./realguardio-oso-integration-service
  ```

- [ ] Verify the image names follow the pattern: eventuate-examples-realguardio-<service-name>:latest
- [ ] Test that docker compose still works:
  ```bash
  docker compose build realguardio-iam-service
  docker images | grep eventuate-examples-realguardio
  ```
- [ ] Verify all 6 custom service images are listed with :latest tag
- [ ] Commit with message: "Add explicit image tags to all services for buildx compatibility - Written by Claude Code"

Acceptance Criteria:
- All services with build sections have explicit image: tags
- Image naming is consistent across all services
- Local docker compose build still works
- Images are tagged with expected names
```

#### [x] Task 1.1: Add buildx setup to `test` job

**Prompt:**
```
Update .github/workflows/test.yml to add Docker Buildx setup to the 'test' job.

Context:
- The test job currently builds the IAM service container at line 41 using 'mise iam-service-compose-build'
- We need buildx for layer caching support
- Add the buildx setup step after the mise-action setup (after line 26)

Steps:
- [ ] Add the following step after the 'uses: jdx/mise-action@v2' step (after line 26):
  ```yaml
  - name: Set up Docker Buildx
    uses: docker/setup-buildx-action@v3
  ```
- [ ] Verify the YAML syntax is correct
- [ ] Commit with message: "Add Docker Buildx setup to test job - Written by Claude Code"

Acceptance Criteria:
- Buildx setup step is added after mise-action setup
- YAML is valid
- No behavioral changes (builds work the same way)
```

#### [x] Task 1.2: Reorder steps and add buildx to `test-compose-up` job

**Prompt:**
```
Update .github/workflows/test.yml to prepare the 'test-compose-up' job for caching.

Context:
- Currently the test-compose-up job runs 'docker compose up' directly (line 111)
- We need to move mise-action setup before the compose command
- We need to add buildx setup
- We'll change to use mise task in the next thread

Steps:
- [ ] Reorder the steps in test-compose-up job (starting at line 107):
  1. Checkout code (keep as-is)
  2. Move 'uses: jdx/mise-action@v2' to be second (move from line 113)
  3. Add 'Set up Docker Buildx' step after mise-action
  4. Keep 'Cache npm dependencies' in current position
  5. Keep 'Compose up' step (will modify in next thread)
  6. Keep remaining steps as-is

- [ ] The new order should be:
  ```yaml
  - name: Checkout code
    uses: actions/checkout@v4

  - uses: jdx/mise-action@v2
    with:
      version: ${{ env.MISE_VERSION }}

  - name: Set up Docker Buildx
    uses: docker/setup-buildx-action@v3

  - name: Cache npm dependencies
    uses: actions/setup-node@v4
    with:
      cache: 'npm'
      cache-dependency-path: 'realguardio-bff/package.json'

  - name: Compose up
    run: docker compose up -d --build --wait
  ```

- [ ] Verify YAML syntax
- [ ] Commit with message: "Reorder steps and add buildx to test-compose-up job - Written by Claude Code"

Acceptance Criteria:
- mise-action is before compose up command
- Buildx setup is added
- All other steps remain functional
- YAML is valid
```

#### [ ] Task 1.3: Verify buildx is available in CI

**Prompt:**
```
Create a temporary verification step to confirm Docker Buildx is working in both jobs.

Steps:
- [ ] Add a verification step after buildx setup in both jobs:
  ```yaml
  - name: Verify Docker Buildx
    run: docker buildx version
  ```
- [ ] Push the changes to a test branch
- [ ] Monitor the GitHub Actions run to confirm:
  - [ ] Both jobs start successfully
  - [ ] Buildx version is printed in logs
  - [ ] Existing builds still work
- [ ] Remove the verification steps once confirmed
- [ ] Commit with message: "Verify buildx setup works in CI - Written by Claude Code"

Acceptance Criteria:
- Buildx is available in both jobs
- Existing builds continue to work
- Verification steps removed after confirmation
```

---

## Steel Thread 2: Implement Basic Caching in mise.toml

**Goal:** Add conditional Docker caching logic to mise.toml with automatic detection in CI.

**Value Delivered:** Docker builds will use layer caching in GitHub Actions, speeding up builds.

### Tasks

#### [x] Task 2.1: Create docker-build-helper task with caching logic

**Prompt:**
```
Create a reusable helper task in mise.toml that implements conditional Docker caching.

Context:
- We want caching to be automatic in CI (GITHUB_ACTIONS=true)
- Optional for local testing (USE_DOCKER_CACHE=true)
- Must support building all services or individual services
- Must support different actions: build, up, up-wait

Implementation:
- [ ] Add the following new task to mise.toml after the existing tasks:

```toml
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
  echo "🚀 Building with Docker layer caching (branch: $BRANCH_NAME)..."

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
      echo "✅ Build complete (images cached)"
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
      echo "🚀 Starting services and waiting..."
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

- [ ] Verify the syntax is correct (run: mise tasks to list tasks)
- [ ] Test the helper locally without caching:
  ```bash
  mise run docker-build-helper "realguardio-iam-service" build
  ```
- [ ] Verify it builds using the standard docker compose method
- [ ] Commit with message: "Add docker-build-helper with conditional caching - Written by Claude Code"

Acceptance Criteria:
- Helper task is added to mise.toml
- Task accepts SERVICE and ACTION parameters
- Conditional logic detects GITHUB_ACTIONS or USE_DOCKER_CACHE
- Local builds work without caching (default behavior)
- Script uses proper error handling (set -euo pipefail)
```

#### [x] Task 2.2: Update compose-up task to use helper

**Prompt:**
```
Update the compose-up task to use the new docker-build-helper.

Context:
- Current compose-up (line 43-45) runs docker compose directly
- Need to maintain dependencies on assemble and bff-dev-stop
- Need to call helper with empty SERVICE and 'up' action

Steps:
- [ ] Replace the compose-up task implementation (lines 43-45):

  OLD:
  ```toml
  [tasks.compose-up]
  depends = ["assemble", "bff-dev-stop"]
  run = "DOCKERFILE_SUFFIX=-local docker compose up -d ${COMPOSE_OPTS:---build}"
  ```

  NEW:
  ```toml
  [tasks.compose-up]
  depends = ["assemble", "bff-dev-stop"]
  run = "mise run docker-build-helper '' up"
  ```

- [ ] Test locally that compose-up still works:
  ```bash
  mise compose-up
  ```
- [ ] Verify services start normally
- [ ] Check that no caching is used (should see standard build messages)
- [ ] Commit with message: "Update compose-up to use docker-build-helper - Written by Claude Code"

Acceptance Criteria:
- compose-up uses the helper
- Local builds work without caching
- Dependencies are preserved
- Services start successfully
```

#### [x] Task 2.3: Update iam-service-compose-build to use helper

**Prompt:**
```
Update the iam-service-compose-build task to use the docker-build-helper.

Context:
- Current task (line 70) builds IAM service only
- Used by test job in GitHub Actions (line 41)
- Should use 'build' action (no startup)

Steps:
- [ ] Replace iam-service-compose-build task (line 70):

  OLD:
  ```toml
  [tasks.iam-service-compose-build]
  run = "DOCKERFILE_SUFFIX=-local docker compose build realguardio-iam-service"
  ```

  NEW:
  ```toml
  [tasks.iam-service-compose-build]
  run = "mise run docker-build-helper realguardio-iam-service build"
  ```

- [ ] Test locally:
  ```bash
  mise iam-service-compose-build
  ```
- [ ] Verify IAM service builds without starting
- [ ] Commit with message: "Update iam-service-compose-build to use helper - Written by Claude Code"

Acceptance Criteria:
- Task builds IAM service only
- No services are started
- Builds successfully locally
```

#### [ ] Task 2.4: Update iam-service-compose-up to use helper

**Prompt:**
```
Update the iam-service-compose-up task to use the docker-build-helper.

Context:
- Current task (line 67) builds and starts IAM service
- Used by bff-e2e-test task
- Should use 'up' action (build and start)

Steps:
- [ ] Replace iam-service-compose-up task (line 67):

  OLD:
  ```toml
  [tasks.iam-service-compose-up]
  run = "DOCKERFILE_SUFFIX=-local docker compose up -d ${COMPOSE_OPTS:---build} realguardio-iam-service"
  ```

  NEW:
  ```toml
  [tasks.iam-service-compose-up]
  run = "mise run docker-build-helper realguardio-iam-service up"
  ```

- [ ] Test locally:
  ```bash
  mise iam-service-compose-up
  ```
- [ ] Verify IAM service builds and starts
- [ ] Verify service is healthy
- [ ] Commit with message: "Update iam-service-compose-up to use helper - Written by Claude Code"

Acceptance Criteria:
- Task builds and starts IAM service
- Service is accessible
- Builds successfully locally
```

#### [ ] Task 2.5: Update security-system-service-compose-up to use helper

**Prompt:**
```
Update the security-system-service-compose-up task to use the docker-build-helper.

Context:
- Current task (line 79-80) builds and starts security system service
- Has dependency on assemble
- Used by go-local task

Steps:
- [ ] Replace security-system-service-compose-up task (lines 79-80):

  OLD:
  ```toml
  [tasks.security-system-service-compose-up]
  depends = ["assemble"]
  run = "DOCKERFILE_SUFFIX=-local docker compose up -d ${COMPOSE_OPTS:---build} realguardio-security-system-service"
  ```

  NEW:
  ```toml
  [tasks.security-system-service-compose-up]
  depends = ["assemble"]
  run = "mise run docker-build-helper realguardio-security-system-service up"
  ```

- [ ] Test locally:
  ```bash
  mise security-system-service-compose-up
  ```
- [ ] Verify security system service builds and starts
- [ ] Verify service is healthy
- [ ] Commit with message: "Update security-system-service-compose-up to use helper - Written by Claude Code"

Acceptance Criteria:
- Task builds and starts security system service
- Dependencies are preserved
- Service is accessible
```

#### [ ] Task 2.6: Test all updated tasks locally

**Prompt:**
```
Verify all updated tasks work correctly locally (without caching).

Steps:
- [ ] Clean up any existing containers:
  ```bash
  mise compose-down
  ```

- [ ] Test iam-service-compose-build:
  ```bash
  mise iam-service-compose-build
  docker images | grep realguardio-iam-service
  ```
  Expected: Image is built, no containers running

- [ ] Test iam-service-compose-up:
  ```bash
  mise iam-service-compose-up
  docker ps | grep realguardio-iam-service
  ```
  Expected: Service is running

- [ ] Clean up:
  ```bash
  mise compose-down
  ```

- [ ] Test security-system-service-compose-up:
  ```bash
  mise assemble
  mise security-system-service-compose-up
  docker ps | grep realguardio-security-system-service
  ```
  Expected: Service is running

- [ ] Clean up:
  ```bash
  mise compose-down
  ```

- [ ] Test compose-up (all services):
  ```bash
  mise compose-up
  docker ps
  ```
  Expected: All 6 custom services are running

- [ ] Verify logs show "Building without caching (local development)"

- [ ] Document test results in commit message
- [ ] Commit with message: "Verify all mise tasks work locally without caching - Written by Claude Code"

Acceptance Criteria:
- All tasks build successfully
- Services start and are healthy
- No caching is used locally
- Logs confirm local development mode
```

---

## Steel Thread 3: Enable Caching in test-compose-up Job

**Goal:** Enable Docker layer caching for the test-compose-up job in GitHub Actions.

**Value Delivered:** Faster CI builds for the test-compose-up job with measurable speedup.

### Tasks

#### [ ] Task 3.1: Update test-compose-up job to use mise task

**Prompt:**
```
Update the test-compose-up job to use the mise compose-up task instead of direct docker compose command.

Context:
- Currently runs 'docker compose up -d --build --wait' directly (line 111)
- Need to use mise task which will automatically enable caching in CI
- The mise task will detect GITHUB_ACTIONS=true and use buildx with caching

Steps:
- [ ] Update the 'Compose up' step in test-compose-up job (line 110-111):

  OLD:
  ```yaml
  - name: Compose up
    run: docker compose up -d --build --wait
  ```

  NEW:
  ```yaml
  - name: Compose up with caching
    run: COMPOSE_OPTS="--quiet-pull" mise compose-up
  ```

  Note: We don't need --wait because the next step already runs 'mise wait-all'

- [ ] Verify YAML syntax
- [ ] Commit with message: "Use mise compose-up in test-compose-up job for caching - Written by Claude Code"

Acceptance Criteria:
- Job uses mise compose-up command
- YAML is valid
- Ready for CI testing
```

#### [ ] Task 3.2: Test caching in CI - First Build (Cache Population)

**Prompt:**
```
Push changes to a test branch and verify the first build (cache miss scenario).

Steps:
- [ ] Create a new test branch:
  ```bash
  git checkout -b test/docker-layer-caching
  git push -u origin test/docker-layer-caching
  ```

- [ ] Monitor GitHub Actions workflow
- [ ] Check test-compose-up job logs for:
  - [ ] "Building with Docker layer caching" message appears
  - [ ] buildx bake command is executed
  - [ ] Cache export messages appear
  - [ ] All 6 services build successfully
  - [ ] Services start successfully
  - [ ] E2E tests pass

- [ ] Note the build time for test-compose-up job
- [ ] Verify cache is created in GitHub Actions cache UI
- [ ] Document findings

Acceptance Criteria:
- First build completes successfully
- Cache is populated (check GitHub Actions cache)
- All tests pass
- Build time is noted for comparison
```

#### [ ] Task 3.3: Test caching in CI - Second Build (Cache Hit)

**Prompt:**
```
Trigger a second build to verify cache is being used effectively.

Steps:
- [ ] Make a trivial change (e.g., update a comment in a README)
- [ ] Push to the same test branch:
  ```bash
  echo "# Cache test" >> README.md
  git add README.md
  git commit -m "Test cache reuse"
  git push
  ```

- [ ] Monitor GitHub Actions workflow
- [ ] Check test-compose-up job logs for:
  - [ ] "Building with Docker layer caching" message appears
  - [ ] Cache import messages appear (CACHED markers in buildx output)
  - [ ] Significantly faster build time than first build
  - [ ] All services start successfully
  - [ ] E2E tests pass

- [ ] Compare build times:
  - [ ] First build time: ___ minutes
  - [ ] Second build time: ___ minutes
  - [ ] Speedup: ___% faster

- [ ] Document the speedup achieved
- [ ] Verify success criteria met (30-50% speedup expected)

Acceptance Criteria:
- Second build completes successfully
- Cache is used (logs show CACHED layers)
- Build is significantly faster (30%+ speedup target)
- All tests pass
```

#### [ ] Task 3.4: Test caching in CI - Partial Cache Hit (Source Changes)

**Prompt:**
```
Test cache behavior when source code changes but dependencies remain the same.

Steps:
- [ ] Make a source code change in one service:
  ```bash
  # Add a comment to a Java file
  echo "// Cache test comment" >> realguardio-iam-service/src/main/java/com/realguardio/iam/IamServiceApplication.java
  git add .
  git commit -m "Test partial cache hit with source change"
  git push
  ```

- [ ] Monitor GitHub Actions workflow
- [ ] Check test-compose-up job logs for:
  - [ ] Cache import messages for dependency layers
  - [ ] Rebuild only for source code layers
  - [ ] Moderate speedup (20-40% expected)
  - [ ] All services start successfully
  - [ ] E2E tests pass

- [ ] Compare build times:
  - [ ] Baseline (first build): ___ minutes
  - [ ] Source change build: ___ minutes
  - [ ] Speedup: ___% faster

- [ ] Document findings

Acceptance Criteria:
- Build completes successfully
- Dependency layers cached, source layers rebuilt
- Build is moderately faster (20-40% speedup target)
- All tests pass
```

#### [ ] Task 3.5: Test caching in CI - Cache Miss (Dependency Changes)

**Prompt:**
```
Test cache behavior when dependencies change.

Steps:
- [ ] Make a dependency change:
  ```bash
  # Update a dependency version in one service
  # Or add a new dependency to build.gradle
  git add .
  git commit -m "Test cache with dependency change"
  git push
  ```

- [ ] Monitor GitHub Actions workflow
- [ ] Check test-compose-up job logs for:
  - [ ] Base layers still cached
  - [ ] Dependency layers rebuilt
  - [ ] Source layers rebuilt
  - [ ] Smaller speedup (10-20% expected)
  - [ ] All services start successfully
  - [ ] E2E tests pass

- [ ] Compare build times
- [ ] Document findings
- [ ] Revert the dependency change if it was only for testing

Acceptance Criteria:
- Build completes successfully
- Only base image layers cached
- Build shows expected rebuild pattern
- All tests pass
```

---

## Steel Thread 4: Enable Caching in test Job

**Goal:** Enable Docker layer caching for the main test job in GitHub Actions.

**Value Delivered:** Faster CI builds for the comprehensive test job.

### Tasks

#### [ ] Task 4.1: Verify test job uses updated mise tasks

**Prompt:**
```
Confirm that the test job already benefits from caching through existing mise task calls.

Context:
- test job calls 'mise iam-service-compose-build' (line 41)
- test job calls 'mise go-local' which depends on iam-service-compose-up (line 75)
- test job calls 'mise go' which calls compose-up (line 81)
- All these tasks now use docker-build-helper with caching

Steps:
- [ ] Review the test job workflow steps
- [ ] Identify all mise commands that build Docker images:
  - Line 41: `mise iam-service-compose-build`
  - Line 75: `mise go-local` (depends on iam-service-compose-up and security-system-service-compose-up)
  - Line 81: `mise go` (calls compose-up)

- [ ] Confirm all these tasks now use docker-build-helper
- [ ] No code changes needed for this task
- [ ] Document the analysis

Acceptance Criteria:
- All Docker build commands in test job use updated mise tasks
- Caching will automatically work due to buildx setup from Thread 1
```

#### [ ] Task 4.2: Test caching in test job - First Build

**Prompt:**
```
Test the test job with caching enabled.

Steps:
- [ ] Push a commit to the test branch to trigger full workflow
- [ ] Monitor the 'test' job in GitHub Actions
- [ ] Check logs for caching behavior in these steps:
  - [ ] Build IAM service container (line 41)
  - [ ] Run E2E Tests with real service (line 75 - calls go-local)
  - [ ] Run E2E Tests with containers (line 81 - calls go)

- [ ] For each Docker build operation, verify:
  - [ ] "Building with Docker layer caching" message appears
  - [ ] buildx bake is used
  - [ ] Cache is being populated

- [ ] Note the total test job duration
- [ ] Document observations

Acceptance Criteria:
- Test job completes successfully
- Caching is used for all Docker builds
- All tests pass
```

#### [ ] Task 4.3: Test caching in test job - Second Build

**Prompt:**
```
Verify cache reuse in the test job.

Steps:
- [ ] Trigger another build (push trivial change or re-run workflow)
- [ ] Monitor the 'test' job
- [ ] Compare build times for Docker build steps:
  - First run vs. second run
  - Document speedup for each step

- [ ] Verify:
  - [ ] Cache is being reused (CACHED markers in logs)
  - [ ] Significant speedup observed
  - [ ] All tests still pass

- [ ] Compare total job duration:
  - [ ] First run: ___ minutes
  - [ ] Second run: ___ minutes
  - [ ] Speedup: ___%

Acceptance Criteria:
- Cache is reused effectively
- Docker build steps are faster
- All tests pass
- Measurable overall speedup
```

---

## Steel Thread 5: Documentation and Cleanup

**Goal:** Document the implementation and clean up temporary test artifacts.

**Value Delivered:** Clear documentation for team members and maintainability.

### Tasks

#### [ ] Task 5.1: Add documentation to README

**Prompt:**
```
Add documentation about Docker layer caching to the project README.

Steps:
- [ ] Add a new section to README.md about Docker builds:

```markdown
## Building with Docker

### Local Development

For local development, Docker builds use the standard process without caching:

\`\`\`bash
mise compose-up  # Builds and starts all services
\`\`\`

### Testing with Cache Locally (Optional)

To test the caching behavior locally:

\`\`\`bash
export USE_DOCKER_CACHE=true
mise compose-up
\`\`\`

Note: Local caching uses your local Docker buildx cache, not GitHub Actions cache.

### CI/CD

Docker layer caching is automatically enabled in GitHub Actions for faster builds:
- First build: Populates cache (normal build time)
- Subsequent builds: Reuse cached layers (30-85% faster depending on changes)
- Cache is scoped per branch and expires after 7 days of inactivity

Individual services can also be built:
\`\`\`bash
mise iam-service-compose-build        # Build only
mise iam-service-compose-up           # Build and start
\`\`\`
\`\`\`

- [ ] Verify markdown formatting
- [ ] Commit with message: "Add Docker caching documentation to README - Written by Claude Code"

Acceptance Criteria:
- Documentation is clear and accurate
- Examples are correct
- Markdown formatting is proper
```

#### [ ] Task 5.2: Add CONTRIBUTING notes about caching

**Prompt:**
```
Add notes about Docker caching to CONTRIBUTING.md (create if it doesn't exist).

Steps:
- [ ] Check if CONTRIBUTING.md exists
- [ ] Add or update the Docker section:

```markdown
## Docker Build Caching

### For Developers

Docker layer caching is automatically enabled in GitHub Actions. No action is required on your part.

### Cache Behavior

- **Local Development**: No caching by default (standard docker compose behavior)
- **GitHub Actions**: Automatic caching enabled via buildx
- **Cache Scope**: Per branch (isolated caching for each branch)
- **Cache Expiration**: 7 days of inactivity

### Testing Cache Locally

If you want to test caching behavior before pushing:

\`\`\`bash
export USE_DOCKER_CACHE=true
mise compose-up
\`\`\`

### Troubleshooting

If builds seem slow in CI:
1. Check GitHub Actions logs for cache hit/miss messages
2. Verify buildx is set up (should see "Building with Docker layer caching")
3. Check GitHub cache usage in repository Settings → Actions → Caches

To clear your local Docker cache:
\`\`\`bash
docker buildx prune --all
\`\`\`
\`\`\`

- [ ] Commit with message: "Add Docker caching notes to CONTRIBUTING - Written by Claude Code"

Acceptance Criteria:
- CONTRIBUTING.md has caching documentation
- Troubleshooting tips are included
- Format is consistent with existing docs
```

#### [ ] Task 5.3: Document cache performance metrics

**Prompt:**
```
Create a document with the observed cache performance metrics.

Steps:
- [ ] Create docs/features/use-docker-layer-caching/performance-metrics.md
- [ ] Document the actual speedups observed during testing:

```markdown
# Docker Layer Caching Performance Metrics

## Test Environment
- Runner: ubuntu-8-core
- Branch: test/docker-layer-caching
- Date: [Fill in date]

## test-compose-up Job

### Build Time Comparison

| Scenario | Build Time | Speedup | Notes |
|----------|-----------|---------|-------|
| Before caching | XX:XX | baseline | Original implementation |
| First build (cache miss) | XX:XX | -X% | Cache population overhead |
| Second build (full cache hit) | XX:XX | +X% | No code changes |
| Source code change | XX:XX | +X% | Dependencies cached |
| Dependency change | XX:XX | +X% | Base layers cached |

### Detailed Observations

[Document specific observations from testing]

## test Job

### Build Time Comparison

| Step | Before | After | Speedup |
|------|--------|-------|---------|
| Build IAM service | XX:XX | XX:XX | +X% |
| Build all services (go) | XX:XX | XX:XX | +X% |
| Total job time | XX:XX | XX:XX | +X% |

## Cache Statistics

- Cache size: ~X GB per branch
- Cache hit rate: X%
- Average speedup: X%

## Conclusions

[Summarize whether success criteria were met]
\`\`\`

- [ ] Fill in actual metrics from testing
- [ ] Commit with message: "Document Docker caching performance metrics - Written by Claude Code"

Acceptance Criteria:
- Metrics document is created
- Actual test results are recorded
- Analysis confirms success criteria met (30-50% speedup target)
```

#### [ ] Task 5.4: Clean up test branch and merge to main

**Prompt:**
```
Merge the Docker layer caching implementation to main branch.

Steps:
- [ ] Ensure all tasks are completed and checked off
- [ ] Run final verification:
  ```bash
  git checkout test/docker-layer-caching
  mise compose-down
  mise compose-up
  docker ps  # Verify all services running
  mise compose-down
  ```

- [ ] Create pull request from test/docker-layer-caching to main
- [ ] PR Description should include:
  - Summary of changes
  - Link to performance metrics
  - Before/after build time comparison
  - Testing performed

- [ ] Request review if required
- [ ] After approval, merge to main
- [ ] Monitor first build on main branch
- [ ] Verify caching works on main branch
- [ ] Delete test branch after successful merge

Acceptance Criteria:
- All tests pass locally
- PR is created with complete description
- Code is merged to main
- Caching works on main branch
- Test branch is cleaned up
```

---

## Steel Thread 6: Advanced Cache Optimization (Optional)

**Goal:** Implement advanced caching strategies for even better performance.

**Value Delivered:** Cross-branch cache sharing and per-service cache scopes.

**Note:** This thread is optional and can be implemented later if basic caching proves successful.

### Tasks

#### [ ] Task 6.1: Implement cross-branch cache fallback

**Prompt:**
```
Enable feature branches to fall back to main branch cache when branch-specific cache is not available.

Context:
- Currently each branch has isolated cache
- New feature branches start with cold cache
- Can improve first build time by using main branch cache as fallback

Steps:
- [ ] Update docker-build-helper in mise.toml to add fallback cache source:

  In the buildx bake command, change:
  ```bash
  --set '*.cache-from=type=gha,scope=build-'"${BRANCH_NAME}" \
  ```

  To:
  ```bash
  --set '*.cache-from=type=gha,scope=build-'"${BRANCH_NAME}" \
  --set '*.cache-from=type=gha,scope=build-main' \
  ```

- [ ] Test by creating a new feature branch and verifying it uses main cache
- [ ] Commit with message: "Add cross-branch cache fallback to main - Written by Claude Code"

Acceptance Criteria:
- Feature branches can use main branch cache
- Branch-specific cache still takes precedence
- First build on new branches is faster
```

#### [ ] Task 6.2: Implement per-service cache scopes

**Prompt:**
```
Refine cache scoping to be per-service for better granularity.

Context:
- Currently using single scope for all services
- Per-service scopes allow finer-grained invalidation
- May improve cache efficiency

Steps:
- [ ] Update docker-build-helper to generate per-service cache scopes
- [ ] Modify buildx bake to use service-specific scopes
- [ ] Test and measure impact on cache hit rate
- [ ] Compare cache storage usage
- [ ] Decide if the added complexity is worth the benefit
- [ ] Document decision and rationale

Acceptance Criteria:
- Per-service scoping implemented
- Performance impact measured
- Decision documented
```

---

## Testing Checklist

### Pre-Merge Verification

Before merging to main, verify:

- [ ] All tasks marked as complete
- [ ] Local development workflow unchanged (mise compose-up works without caching)
- [ ] Local caching works when USE_DOCKER_CACHE=true
- [ ] test-compose-up job uses caching and is faster
- [ ] test job uses caching and is faster
- [ ] All 6 services build successfully with caching
- [ ] E2E tests pass with cached builds
- [ ] Cache is created and reused in GitHub Actions
- [ ] Documentation is complete and accurate
- [ ] Performance metrics are documented
- [ ] Success criteria met (30-50%+ speedup)

### Post-Merge Monitoring

After merging to main, monitor:

- [ ] First build on main populates cache
- [ ] Subsequent builds use cache effectively
- [ ] No increase in build failures
- [ ] Cache size is within GitHub limits (< 10GB)
- [ ] Team feedback is positive

---

## Rollback Plan

If issues arise after merging:

### Quick Disable (No Code Changes)

Set environment variable in GitHub Actions:
```yaml
env:
  USE_DOCKER_CACHE: false
```

### Revert Commits

```bash
git revert <commit-range>
git push
```

### Gradual Re-enable

1. Re-enable for test-compose-up job only
2. Monitor for 1 week
3. Re-enable for test job
4. Monitor and optimize

---

## Success Criteria

### Must Have
- ✅ test-compose-up job is 30%+ faster on cache hits
- ✅ All existing tests continue to pass
- ✅ Local development workflow unchanged
- ✅ No increase in CI failure rate
- ✅ Documentation complete

### Should Have
- ✅ test job shows measurable speedup
- ✅ Cache hit rate > 70%
- ✅ Source-only changes show 20-40% speedup

### Nice to Have
- ✅ Cross-branch cache sharing implemented
- ✅ Per-service cache scopes evaluated

---

## Change History

### Update 1: Add Explicit Image Tags Task
- **Date:** 2025-11-10
- **Author:** Claude Code
- **Changes:** Added Task 1.0 to add explicit image tags to all services in docker-compose.yaml
- **Rationale:** Without explicit image tags, docker buildx bake and docker compose rely on auto-generated names which can be fragile. Explicit tags ensure robust integration between buildx and compose commands.

### Initial Plan
- **Date:** 2025-11-10
- **Author:** Claude Code
- **Changes:** Initial implementation plan created based on specification
