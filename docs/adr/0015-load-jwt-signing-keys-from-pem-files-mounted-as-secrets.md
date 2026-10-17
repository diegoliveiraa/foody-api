# ADR-0015 — Load JWT signing keys from PEM files mounted as secrets

- **Status:** Accepted
- **Date:** 2026-10-16
- **Decided by:** Diego

## Context — why this became a decision

ADR-0002 signs access tokens with RS256 and keeps the keys out of the repository. Local runs,
tests, CI and the deploy stack all need a key pair, the image is built to carry nothing secret
(ADR-0004), and the container runs as a non-root user on a read-only filesystem.

## Decision

- `foody.security.jwt.public-key` and `foody.security.jwt.private-key` are resource locations
  (or inline PEM) bound straight to `RSAPublicKey` and `RSAPrivateKey` by Spring Security's key
  converters: X.509 `PUBLIC KEY` and PKCS#8 `PRIVATE KEY`. `foody.security.jwt.*` also holds the
  issuer (`foody-api`) and the lifetimes: access 15 minutes, refresh 7 days, family 30 days.
- **Local:** `bash scripts/generate-jwt-keys.sh` writes `.secrets/jwt-private.pem` and
  `.secrets/jwt-public.pem` (RSA 2048, `chmod 644`) and refuses to overwrite them without
  `--force`. `.secrets/` is git-ignored and outside the Docker build context;
  `application.properties` points at those files.
- **Deploy:** `compose.deploy.yaml` mounts the PEMs as compose `secrets` (sources
  `JWT_PRIVATE_KEY_FILE` and `JWT_PUBLIC_KEY_FILE`, `.secrets/` by default) and sets
  `FOODY_SECURITY_JWT_PRIVATEKEY=file:/run/secrets/jwt_private_key` and its public twin.
- **CI** generates a throwaway pair before starting the image. **Tests** generate one pair per JVM
  in temporary files through a `ContextCustomizerFactory`. No key is ever committed.
- A missing or unreadable key stops the API at startup.

## Why this one and not the others

| Alternative | Why not |
|---|---|
| PEM content in environment variables | Multi-line values break easily and show up in `docker inspect` and the process environment |
| Key baked into the image | Whoever pulls the image can sign tokens |
| A key generated at startup | Every restart logs everyone out, and two instances would disagree |
| A KMS, Vault or JWKS endpoint | Infrastructure Foody does not run yet |

## Pros

- Nothing secret in git, the image or the environment.
- The same property works locally, in CI and in deploy — only the file behind it changes.
- Rotating is replacing two files and restarting.

## Cons

- The files are world-readable (`644`) because compose bind-mounts them with host permissions and
  the container user is not their owner: the host directory itself must be protected.
- One key at a time: rotating rejects every access token already issued (at most 15 minutes).
- Each developer generates their own pair; tokens are not portable between machines.
