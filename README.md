# AI PR Reviewer POC

Policy-driven, generic, and extensible Java 21 CLI for AI-assisted pull request review.

This repository is designed as a clean starting point for a GitHub PR review proof of concept. It can review:

- a unified diff file
- a prepared sample scenario
- a GitHub pull request fetched through the GitHub API

The core idea is simple: review behavior is not hardcoded into a single prompt. Instead, the system composes review context from readable YAML policy files, an agent profile, and a prompt template, then normalizes the AI output into a stable domain model.

## Why This Exists

Most AI review experiments start as a prompt string buried inside a script. That works for a demo, but it becomes brittle the moment teams want to:

- tune review behavior per repository or domain
- change severity vocabulary
- swap AI providers
- move from local CLI usage to GitHub Actions

This project gives you a reusable baseline with explicit boundaries and small focused classes, while staying lightweight enough for a POC.

## Key Decisions

### Maven over Gradle

Maven was chosen because:

- it is ubiquitous in Java backend teams
- it keeps CI/bootstrap simple for a first repository commit
- it was already available in the target local environment
- it pairs well with a single-module CLI artifact and Maven Wrapper

### No Spring Boot

Spring Boot was intentionally not used in v1 because the primary runtime is a lightweight CLI / GitHub Action style workflow. For this stage, manual wiring keeps startup, dependencies, and cognitive load low while preserving a clean path to a future service wrapper.

### YAML for policy and agent files

YAML was chosen because:

- humans can read and edit it quickly
- nested structures stay compact
- it works well for policy layering
- it is more maintainable than embedding long prompts in Java strings

## Architecture Summary

The implementation is intentionally split by responsibility:

- `dev.prreviewer.diff`: diff input loading, scenario fixtures, diff generation, filtering
- `dev.prreviewer.config`: runtime config, agent profile loading, environment interpolation
- `dev.prreviewer.policy`: policy documents and merge logic
- `dev.prreviewer.ai`: provider abstraction, OpenAI integration, mock provider
- `dev.prreviewer.review`: prompt assembly, normalized review payload, orchestration
- `dev.prreviewer.output`: markdown and JSON formatters
- `demo/scenarios`: reviewable sample PR scenarios

High-level flow:

1. Input arrives from `--diff-file`, `--changes-manifest`, `--sample`, or `--github-pr`.
2. Runtime config and agent profile are loaded.
3. Policy files are merged into one resolved review policy set.
4. Prompt context is assembled from the policy set plus the diff.
5. The selected AI provider returns structured JSON.
6. The JSON is normalized into `ReviewReport`.
7. The selected formatter prints markdown or JSON output.

## Project Layout

```text
ai-pr-reviewer-poc/
├── config/
│   ├── application.example.yml
│   └── agents/default-reviewer.yml
├── policies/
│   ├── base/
│   ├── profiles/
│   └── overrides/
├── demo/scenarios/
├── src/main/java/dev/prreviewer/
├── src/main/resources/
│   ├── schemas/
│   └── templates/
├── scripts/
├── .github/workflows/
└── pom.xml
```

## Config Model

### Runtime config

`config/application.example.yml` contains:

- provider selection
- active agent profile path
- include/exclude file filters
- max file count and diff size guardrails
- OpenAI provider settings
- GitHub repository settings
- demo scenarios directory

You can either:

- use it directly for local POC runs, or
- copy it to `config/application.yml` and customize it

### Agent profile

`config/agents/default-reviewer.yml` defines:

- which prompt template to use
- which policy files are active
- review focus areas
- actionable-finding rules
- max findings and minimum confidence
- default formatter preference

### Policy files

The current policy set is layered like this:

- `policies/base/review-principles.yml`
- `policies/base/clean-code-rules.yml`
- `policies/base/severity-map.yml`
- `policies/base/output-contract.yml`
- `policies/profiles/java-backend.yml`

`policies/overrides/domain-template.yml` is a copy-ready template for project/domain-specific extensions.

## Secrets And Externalized Values

No real secret is committed.

The project expects these values when relevant:

- `OPENAI_API_KEY`: required only for the `openai` provider
- `OPENAI_MODEL`: optional override, defaults to `gpt-5.4-mini`
- `OPENAI_REASONING_EFFORT`: optional override, defaults to `low`
- `GITHUB_TOKEN`: required only for `--github-pr`
- `GITHUB_REPO_OWNER`: required only for `--github-pr`
- `GITHUB_REPO_NAME`: required only for `--github-pr`
- `GITHUB_BASE_BRANCH`: optional metadata default for future GitHub workflows

`.env.example` documents the expected environment variables and `.gitignore` excludes `.env` and `config/application.yml`.

## How To Run

### 1. Verify build

```bash
./mvnw test
```

### 2. Package a runnable jar

```bash
./mvnw -DskipTests package
```

### 3. Run with a built-in sample

```bash
java -jar target/ai-pr-reviewer-poc.jar review --sample naming-problem
```

The default config uses the `mock` provider so the sample flow works without an API key.

### 4. Review a diff file

```bash
java -jar target/ai-pr-reviewer-poc.jar review --diff-file /absolute/path/to/pr.diff
```

### 5. Review a GitHub PR with OpenAI

```bash
cp config/application.example.yml config/application.yml
```

Then set secrets in `.env` or the shell:

```bash
export OPENAI_API_KEY=...
export GITHUB_TOKEN=...
export GITHUB_REPO_OWNER=your-org
export GITHUB_REPO_NAME=your-repo
```

Run:

```bash
java -jar target/ai-pr-reviewer-poc.jar review --github-pr 123 --provider openai
```

### 5.a Publish inline review comments to GitHub

When you want the CLI to publish actionable findings directly onto PR lines:

```bash
java -jar target/ai-pr-reviewer-poc.jar \
  review \
  --github-pr 123 \
  --provider openai \
  --publish-github-review-comments \
  --output-format markdown
```

This uses the GitHub pull request review comments API and requires:

- `GITHUB_TOKEN`
- repository `pull-requests: write` permission for the workflow/job token

### 6. Print the assembled prompt without calling AI

```bash
java -jar target/ai-pr-reviewer-poc.jar review --sample performance-smell --dry-run
```

## Output Contract

The normalized review output supports:

- `severity`
- `title`
- `problem`
- `whyItMatters`
- `recommendation`
- `confidence`
- `filePath`
- `startLine`
- `endLine`

Output formatters included:

- `markdown`
- `json`

## Demo / Sample Scenarios

Prepared scenarios live under `demo/scenarios`:

- `clean-implementation`
- `naming-problem`
- `null-edge-case`
- `responsibility-violation`
- `performance-smell`

These fixtures are isolated from the main source set, so they stay reviewable without polluting the production code path.

### Materialize a scenario into a sandbox branch

Use the helper script to copy either the `base` or `head` variant of a scenario into a target directory:

```bash
./scripts/materialize-demo-scenario.sh naming-problem base /absolute/path/to/sandbox
./scripts/materialize-demo-scenario.sh naming-problem head /absolute/path/to/sandbox
```

Recommended PR rehearsal flow after the repository is created:

1. Create a sandbox branch from `main`.
2. Materialize a `base` scenario into a small sample app area and commit it.
3. Create a second branch from that base branch.
4. Materialize the corresponding `head` scenario and commit it.
5. Open a PR between the two branches.

Suggested scenario branch names:

- `demo/clean-implementation`
- `demo/naming-problem`
- `demo/null-edge-case`
- `demo/responsibility-violation`
- `demo/performance-smell`

## How To Extend

### Add a new review policy

1. Create a YAML file under `policies/base`, `policies/profiles`, or `policies/overrides`.
2. Add it to `activePolicies` in the chosen agent profile.
3. Keep rules focused on observable review behavior, not provider-specific prompting tricks.

### Add a new provider

1. Implement `AiReviewProvider`.
2. Register it in `AiProviderFactory`.
3. Keep the provider output aligned with `src/main/resources/schemas/review-report.schema.json`.

### Add a new output formatter

1. Implement `ReviewOutputFormatter`.
2. Register it in `OutputFormatterFactory`.

### Move toward GitHub Action or service mode

The current design already separates:

- GitHub PR input loading
- policy assembly
- AI calling
- normalized output rendering

That means the next step can be:

- a GitHub Action wrapper
- a REST service wrapper
- a background worker

without rewriting the core review flow.

## GitHub Integration Next Steps

The repository includes an active workflow at:

- `.github/workflows/pr-review-poc.yml`

Current workflow behavior:

1. builds the CLI
2. reviews the target PR
3. prints the normalized markdown report into the action log
4. publishes inline PR review comments for findings that have valid file and line anchors

Required repository setup:

1. add `OPENAI_API_KEY` as a repository secret
2. allow the workflow job token to use `pull-requests: write`
3. open PRs from branches inside the same repository if you want secrets to be available

## Known Limitations

- The GitHub input path now posts inline review comments, but only for findings that have a file path plus line anchor.
- Re-runs on the same commit avoid duplicating previously published bot comments, but older comments on previous commits are not cleaned up automatically.
- Only `mock` and `openai` providers are wired in v1.
- There is no token-aware chunking yet; the current guardrail is file count plus total diff character limit.
- The mock provider is heuristic-only and exists to validate the pipeline, not to replace real model review.
- Output is normalized, but line references still depend on what is visible in the diff.

## Roadmap / Next Steps

- add PR summary reviews or check-run output in addition to inline comments
- add provider-specific retry and rate-limit handling
- add diff chunking and multi-pass review for large PRs
- add richer project/domain override packs
- support multi-agent or rule-based prefilters before expensive AI calls
- add evaluation fixtures and golden outputs

## Repo Opening Guide

After you create the GitHub repository:

1. initialize the remote and push the prepared project
2. create `.env` from `.env.example`
3. optionally copy `config/application.example.yml` to `config/application.yml`
4. run `./mvnw test`
5. run one sample review locally
6. enable the workflow template only after secrets are configured

### Suggested first push sequence

```bash
git init
git add .
git commit -m "chore: bootstrap policy-driven AI PR reviewer POC"
git branch -M main
git remote add origin <your-new-repo-url>
git push -u origin main
```

### Recommended repository secrets

- `OPENAI_API_KEY`
- `GITHUB_TOKEN` if you plan to call GitHub outside the default Actions token
- optionally `OPENAI_MODEL` and `OPENAI_REASONING_EFFORT` as repository variables

### Suggested branch and release conventions

- long-lived branch: `main`
- feature branches: `feature/*`
- demo rehearsal branches: `demo/*`
- release tags: `v0.1.0`, `v0.2.0`, ...

## First Commit Scope

The current repository state already includes:

- Maven project skeleton and wrapper
- Java source code for the full POC pipeline
- example runtime config and `.env.example`
- agent profile and policy files
- prompt template and JSON schema
- sample scenarios
- tests
- active GitHub workflow with inline review comment publishing
- helper script for demo scenario materialization

That is the intended first commit boundary.
