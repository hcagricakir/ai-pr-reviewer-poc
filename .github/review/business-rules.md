# Repository Business Rules

Use these repository-specific rules after the shared common and profile rules.

- Prioritize findings that can break the review engine's CLI behavior, GitHub review publishing flow, or prompt construction.
- Flag compatibility risks for configuration keys, environment variable names, and output formats that callers or workflows rely on.
- Call out changes that can silently weaken review quality, such as dropping findings, losing file/line anchors, or skipping critical diff context.
