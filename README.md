# AI PR Reviewer Engine

Reusable Java 21 engine for AI-assisted pull request review.

This repository owns the review engine only:

- diff and pull request input loading
- policy and prompt assembly
- OpenAI provider integration
- normalized review output
- optional GitHub pull request review publishing

Shared GitHub Actions orchestration lives in `hcagricakir/ai-pr-review-workflows`. This repository is meant to be built and executed by that reusable workflow or by local tooling, not to own a repository-specific review workflow itself.

## Repository Role

Treat this repository as the engine layer:

- reusable from local development and GitHub Actions
- configurable through YAML and environment variables
- independent from one repository's workflow ownership
- suitable for multiple consuming repositories through a shared workflow

## Architecture Summary

Key packages:

- `dev.prreviewer.diff`: diff loading, GitHub PR input loading, filtering, and line mapping
- `dev.prreviewer.config`: application config, environment interpolation, and agent profiles
- `dev.prreviewer.policy`: policy parsing and merge logic
- `dev.prreviewer.review`: prompt assembly, orchestration, and normalized review payload handling
- `dev.prreviewer.ai`: provider abstraction and OpenAI implementation
- `dev.prreviewer.output`: markdown and JSON output formatting
- `dev.prreviewer.github`: pull request review publishing through the GitHub reviews API

High-level flow:

1. Load one review source: diff file, scenario manifest, sample, or GitHub PR.
2. Load runtime config and agent profile.
3. Merge YAML policies into one resolved review policy set.
4. Optionally append supplemental markdown guidance from `--extra-rules-file`.
5. Build system and user prompts.
6. Run the selected AI provider.
7. Normalize findings into a stable review report.
8. Print markdown or JSON output.
9. Optionally publish inline GitHub PR comments.

## Engine Inputs

Supported review source options:

- `--diff-file`
- `--changes-manifest`
- `--sample`
- `--github-pr`

Shared workflow integration option:

- `--extra-rules-file`

`--extra-rules-file` is intentionally generic. A reusable workflow can combine shared conventions and repository-specific business guidance into one markdown file and pass it into the engine without coupling workflow logic into this repository.

## Configuration

`config/application.example.yml` contains:

- provider selection
- agent profile path
- diff/file guardrails
- OpenAI settings
- GitHub API settings
- demo scenario path

Important environment variables:

- `OPENAI_API_KEY`
- `OPENAI_MODEL`
- `OPENAI_REASONING_EFFORT`
- `GITHUB_TOKEN`
- `GITHUB_REPO_OWNER`
- `GITHUB_REPO_NAME`
- `GITHUB_BASE_BRANCH`

Model selection is expected to come from environment or YAML configuration. The default example config resolves `OPENAI_MODEL` and falls back explicitly to `gpt-5.4-mini`.

## Local Usage

Run the test suite:

```bash
./mvnw test
```

Build the CLI jar:

```bash
./mvnw -DskipTests package
```

Run against a sample:

```bash
java -jar target/ai-pr-reviewer.jar review --sample naming-problem
```

Run against a pull request:

```bash
java -jar target/ai-pr-reviewer.jar review --github-pr 123 --provider openai
```

Run with merged markdown rules:

```bash
java -jar target/ai-pr-reviewer.jar \
  review \
  --github-pr 123 \
  --provider openai \
  --extra-rules-file /absolute/path/to/combined-rules.md
```

Publish GitHub PR comments:

```bash
java -jar target/ai-pr-reviewer.jar \
  review \
  --github-pr 123 \
  --provider openai \
  --publish-github-review-comments \
  --output-format markdown
```

## Shared Workflow Consumption

The reusable workflow repository is expected to:

1. checkout the caller repository
2. checkout this engine repository
3. build the jar
4. merge shared and repository-specific markdown rules
5. run the engine with `--extra-rules-file`
6. publish review comments back to GitHub

This repository includes a caller workflow under `.github/workflows/pr-review.yml` so the engine can consume the shared workflow as a normal repository would.
