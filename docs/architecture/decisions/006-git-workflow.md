# ADR-006 — Git: rama protegida, PR obligatorio y squash

- **Estado:** Aceptado
- **Fecha:** 2026-11-21
- **Decisores:** Architect

## Contexto

El repositorio es público. Hasta ahora era posible empujar directamente a `Master`, existía un ruleset con bypass permanente para el propietario, el CI filtraba `main` (rama que no existe) y estaban permitidos merge commits, rebase y squash. Además, el working tree acumulaba cambios de varias fases sin integrar.

Riesgos concretos:

- Cambios sin revisión ni CI en la rama que publica la imagen y el historial del portfolio.
- Historial no lineal con merges que dificultan el `git bisect` y la lectura del proyecto.
- `git push` accidental con secretos o con build roto.

## Decisión

1. **Ruleset activo sobre la rama por defecto (`Master`)**, sin actores de bypass:
   - prohibido borrar la rama y hacer `force push`;
   - `pull_request` obligatorio con historial lineal;
   - solo **squash merge**;
   - resolución de conversaciones obligatoria;
   - `required_approving_review_count = 0` (ver §Consecuencias).
2. **Configuración del repositorio**: squash habilitado; merge commits y rebase deshabilitados; borrado automático de la rama tras el merge.
3. **Flujo de trabajo**:
   - ramas `feat/…`, `fix/…`, `chore/…`, `docs/…`, `ci/…`;
   - Conventional Commits (`feat:`, `fix:`, `build(deps):`, `ci:`, `docs:`);
   - un PR por fase o por cambio significativo, con el template `.github/pull_request_template.md`;
   - releases etiquetadas `vX.Y.Z` (SemVer) cuando exista algo desplegable.
4. **Checks requeridos**: `unit tests`, `integration tests` y `CodeQL (java-kotlin)`
   están activos en el ruleset desde que los workflows existen en `Master`. Un check
   requerido que nunca se ejecuta bloquea todos los PRs, por eso la activación fue
   posterior al primer merge. `trivy image scan (non-blocking)` no es requerido.

## Consecuencias

- (+) La rama por defecto solo recibe cambios revisados por CI y CodeQL.
- (+) Historial lineal y legible; cada PR es una unidad de cambio contable.
- (+) El portfolio demuestra un flujo de trabajo profesional, no solo código.
- (−) **Sin segundo mantenedor no hay revisión independiente**: 0 aprobaciones es la única configuración que no bloquea al propietario (GitHub no permite autoaprobarse). Al incorporar un colaborador, subir a 1 aprobación y añadir `CODEOWNERS`.
- (−) Todo cambio, incluso trivial, requiere rama + PR; se acepta el coste por disciplina de historial.
- Riesgo residual: el propietario conserva permisos de administrador y podría desactivar el ruleset; se documenta como control de gobernanza, no técnico.

## Verificación

```bash
gh api repos/pyckzon/CASL/rulesets/25761862 --jq '{rules:[.rules[].type], bypass:.bypass_actors}'
gh api repos/pyckzon/CASL --jq '{squash:.allow_squash_merge, merge:.allow_merge_commit, rebase:.allow_rebase_merge, del:.delete_branch_on_merge}'
```
