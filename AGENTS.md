# AGENTS.md

## Repository workflow

- This is a personal Android app maintained by a single developer, not a team project.
- For routine changes, **commit and push directly to the `main` branch**. Do **not** create feature branches or pull requests by default.
- Create a separate branch or pull request only when the owner explicitly requests one.
- Before pushing, run appropriate build and tests when practical. After pushing, check GitHub Actions and fix any failures with additional commits to `main`.
- Never force-push or rewrite `main` history. Preserve existing user data, app signing configuration, and secrets.
