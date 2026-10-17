# ADR-0011 — Keep dependencies current with Dependabot

- **Status:** Accepted
- **Date:** 2026-10-16
- **Decided by:** Diego

## Context — why this became a decision

The API started on current versions of everything: Maven dependencies, base images, GitHub
Actions. Left alone they age silently — security fixes included — until an upgrade spans several
majors at once.

## Decision

`.github/dependabot.yml` checks weekly: Maven (minor and patch grouped in one pull request),
GitHub Actions (grouped), the `Dockerfile` base images and the images in the compose files. Major
versions arrive as separate pull requests. Every update goes through CI (ADR-0009) like any change.

## Why this one and not the others

| Alternative | Why not |
|---|---|
| Renovate | More configurable, but an external app to install; Dependabot ships with GitHub |
| Manual updates | Happen when something breaks, not before |
| One pull request per dependency | Floods review; minor and patch updates rarely need separate attention |

## Pros

- Security fixes and base images arrive without anyone remembering to look.
- CI decides whether an update is safe before anyone reviews it.

## Cons

- Weekly pull requests to review and merge.
- Only works once the repository has a GitHub remote.
- Versions pinned outside manifests (a tool run through `npx` in the front) are not covered.
