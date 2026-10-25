# JetBrains Junie GitHub Automation

This document outlines the setup, architecture, and usage of the
[JetBrains Junie GitHub Action](https://github.com/JetBrains/junie-github-action) in this repository.

## Overview

Junie is JetBrains' AI coding agent for GitHub workflows. In this repository, Junie is integrated with
**OpenRouter** using BYOK (Bring Your Own Key) authentication to provide:

1. **Interactive Coding Assistance**: Implement features from GitHub issues, make adjustments on PRs, and resolve
   merge conflicts when tagged with `@junie-agent`.
2. **Automated Pull Request Code Reviews**: Review PR changes against repository conventions (Jetpack Compose patterns,
   `explicitApi()` mode, `applicationContext` usage, Main-thread WebView execution, KDoc requirements, and test
   coverage) and provide actionable feedback in a single updated comment thread.

Source repository: [JetBrains/junie-github-action](https://github.com/JetBrains/junie-github-action)

---

## Configuration & Authentication

Junie is authenticated via OpenRouter using repository secrets:

| Secret / Variable | Description | Example / Required Format |
|---|---|---|
| `OPENROUTER_API_KEY` *(Secret)* | Your OpenRouter API key | `sk-or-v1-...` |
| `OPENROUTER_MODEL_NAME` *(Secret or Variable)* | Junie model identifier (optional, defaults to `claude-sonnet-5-5`) | `claude-sonnet-5-5` |

> [!IMPORTANT]
> **Model Naming Requirement:**  
> Junie CLI requires model names **without** provider prefixes. Do **not** use `anthropic/claude-sonnet-5.5` or
> `openai/gpt-4o`. Always use Junie's supported identifiers (e.g. `claude-sonnet-5-5`).

### Supported Model Names

Junie maps internal model identifiers directly to the appropriate LLM endpoints via OpenRouter. Below is the
complete list of supported models in Junie CLI:

#### Anthropic Claude

| Model Identifier | Notes |
|---|---|
| `claude-sonnet-5-5` | **Recommended** - Best balance of code reasoning, speed, and cost |
| `claude-sonnet-5` | Previous generation Sonnet |
| `claude-sonnet-4-6` | Fast coding model |
| `claude-haiku-5-5` | Ultra-fast, lightweight coding model |
| `claude-opus-5-5` | High-depth reasoning for complex tasks |
| `claude-opus-5` | Previous generation Opus |
| `claude-opus-4-8`, `claude-opus-4-7`, `claude-opus-4-6` | Legacy Opus models |
| `claude-fable-5-1`, `claude-fable-5` | Fable series |
| `sonnet`, `opus`, `fable` | Standard family aliases |

#### Google Gemini

| Model Identifier | Notes |
|---|---|
| `gemini-4-argon` | Next-generation Gemini model |
| `gemini-3.8-flash` | Ultra-fast multimodal model |
| `gemini-3.7-flash`, `gemini-3.6-flash`, `gemini-3.5-flash-lite`, `gemini-3.1-flash-lite` | Flash series |
| `gemini-3.1-pro-preview`, `gemini-3-flash-preview` | Preview releases |
| `gemini-pro`, `gemini-flash` | Standard aliases |

#### OpenAI GPT

| Model Identifier | Notes |
|---|---|
| `gpt-5.5` | Flagship OpenAI model |
| `gpt-5.4`, `gpt-5.3-codex`, `gpt-5.2-2025-12-11`, `gpt-5-2025-08-07` | GPT-5 family |
| `gpt-6.1-sol`, `gpt-6-sol`, `gpt-6-luna`, `gpt-6-astra` | GPT-6 series |
| `gpt-5.6-sol`, `gpt-5.6-luna`, `gpt-5.6-terra` | GPT-5.6 series |
| `gpt`, `gpt-sol`, `gpt-luna`, `gpt-astra`, `gpt-terra`, `gpt-codex` | Standard aliases |

#### xAI Grok

| Model Identifier | Notes |
|---|---|
| `grok-4.7`, `grok-4.6`, `grok-4.5`, `grok-4.3` | Grok series |
| `grok` | Standard Grok alias |

#### Meta / Muse

| Model Identifier | Notes |
|---|---|
| `muse-spark-1.3`, `muse-spark-1.2`, `muse-spark-1.1` | Meta Muse Spark models |

#### JetBrains Internal

| Model Identifier | Notes |
|---|---|
| `junie-lite` | Lightweight internal model |

#### How to Find the Updated List Anytime

If JetBrains updates the Junie action version (`junie_version` in `action.yml`), you can check newly added models by either:

1. Providing an unrecognized model parameter (e.g. `junie-args: --model="help"`) in a PR comment - Junie will output
   the complete valid model list in the GitHub Actions step log.
2. Checking the [JetBrains Junie GitHub Action Release Notes](https://github.com/JetBrains/junie-github-action/releases).

---

### Custom Model Profiles (`.junie/models/`)

To use arbitrary OpenRouter model IDs (such as `deepseek/deepseek-v4.1-flash`, `minimax/minimax-01`, or
`qwen/qwen-2.5-coder-32b-instruct`), define a custom model profile in `.junie/models/`.

Junie automatically loads profile definitions from `.junie/models/*.json` in the repository root.

#### 1. Create a Model Definition File

Create `.junie/models/<model-name>.json` (for example, `.junie/models/deepseek-v4.json`):

```json
{
  "id": "deepseek/deepseek-v4.1-flash",
  "displayName": "DeepSeek V4.1 Flash",
  "baseUrl": "https://openrouter.ai/api/v1",
  "apiKey": "${OPENROUTER_API_KEY}",
  "apiType": "OpenAICompletion"
}
```

Key fields:
- `id`: The exact OpenRouter model ID.
- `displayName`: Human-readable label for logs and UI.
- `baseUrl`: OpenRouter endpoint (`https://openrouter.ai/api/v1`).
- `apiKey`: `${OPENROUTER_API_KEY}` (automatically resolved from the environment).
- `apiType`: `"OpenAICompletion"` (standard for OpenRouter and OpenAI-compatible endpoints).

#### 2. Select the Custom Model

Junie registers custom models under the `custom:<filename>` prefix:

- In workflow configuration (`.github/workflows/junie.yml`):
  Set `model: "custom:deepseek-v4"`.
- Dynamically in an issue or PR comment:
  ```markdown
  @junie-agent Add unit tests for ThemeParser
  junie-args: --model="custom:deepseek-v4"
  ```

---

## Active Workflows

### 1. Interactive Agent (`.github/workflows/junie.yml`)

- **Triggers**: Mentioning `@junie-agent` in:
  - Issues (title or description)
  - Issue comments
  - Pull Request review comments
  - Pull Request reviews
- **Permissions**: `contents: write`, `pull-requests: write`, `issues: write`
- **Security & Authorization**: Restricted to trusted users (`OWNER`, `MEMBER`, or `COLLABORATOR`) via
  `author_association` filter. Mentions from untrusted external users are skipped before provisioning a runner,
  protecting CI minutes and API balance.
- **Behavior**:
  - Clones the repository and loads project context from `.junie/guidelines.md`.
  - Implements the requested change, commits to a branch, and opens/updates a PR.

### 2. Automated PR Code Review (`.github/workflows/junie-code-review.yml`)

- **Triggers**: Opened, updated (synchronized), or marked ready for review (`ready_for_review`) Pull Requests
  targeting `main` (skips drafts).
- **Permissions**: `contents: read`, `pull-requests: write`, `issues: write`
- **Behavior**:
  - Automatically fetches the PR diff against `main`.
  - Evaluates code against Kotlin/Compose patterns, explicit API requirements, testing guidelines, and security.
  - Posts structured review feedback in a single, sticky PR comment.

---

## Usage Guide & Examples

### Implementing Features from Issues

Mention `@junie-agent` in an issue body or comment:

```markdown
@junie-agent Add an example for custom theme configuration in the sample app
```

Junie will create a branch and open a pull request with the implementation.

### Quick Adjustments on PRs

Request minor changes on an existing PR without re-explaining context:

```markdown
@junie-agent minor-fix update KDoc description for rememberHighlightedCode
```

### Resolving Merge Conflicts

If a PR has merge conflicts with `main`:

```markdown
@junie-agent resolve conflicts
```

### Dynamic Model Override (Per Request)

You can override the model on any prompt using the `junie-args:` syntax:

```markdown
@junie-agent optimize ThemeParser to reduce unnecessary allocations
junie-args: --model="claude-opus-5-5"
```

Or for a custom model configured in `.junie/models/deepseek-v4.json`:

```markdown
@junie-agent optimize ThemeParser to reduce unnecessary allocations
junie-args: --model="custom:deepseek-v4"
```

---

## Project Guidelines Integration

Junie reads project guidelines from `.junie/guidelines.md`. In this repository, `.junie/guidelines.md` is a
relative symlink to [`.github/copilot-instructions.md`](https://github.com/hossain-khan/android-compose-highlight/blob/main/.github/copilot-instructions.md).

This ensures a **single source of truth** across:

- GitHub Copilot (`.github/copilot-instructions.md`)
- Antigravity / AI Coding Assistants (`AGENTS.md` -> `.github/copilot-instructions.md`)
- JetBrains Junie (`.junie/guidelines.md` -> `.github/copilot-instructions.md`)
