# Agent Operating Guide

This repository is for the RadPhoneCamera Android app described in
`BUILD_PLAN.md`. Agents working here should be as autonomous as possible while
staying inside platform safety rules and the product limitations in the build
plan.

## Core Behavior

- Treat `BUILD_PLAN.md` as the source of truth for product intent, safety
  language, implementation order, and acceptance criteria.
- Do not ask avoidable questions. Use repo state, docs, tests, and reasonable
  engineering defaults to keep moving.
- Prefer coding and verification over planning-only work once the next task is
  clear.
- Complete the largest coherent batch of build-plan tasks that can be done
  safely in the current run.
- Leave the repo in a buildable, documented state whenever possible.
- Respect required tool, platform, filesystem, network, credential, and
  destructive-action approval flows. Do not bypass safeguards.

## When To Ask

Ask the user only when the work depends on:

- destructive changes,
- credentials, accounts, stores, signing keys, or paid services,
- real radiation-source validation or field-test decisions,
- unavailable hardware choices that materially affect implementation,
- major scope changes outside `BUILD_PLAN.md`,
- ambiguous product or safety decisions that cannot be resolved from the docs.

For ordinary implementation details, choose the simplest approach that fits the
existing repo and the build plan.

## Coding-First Run Protocol

At the start of each implementation run:

1. Read `AGENTS.md`.
2. Read `BUILD_PLAN.md`.
3. Read `IMPLEMENTATION_LOG.md` if it exists.
4. Inspect the current repo state.
5. Pick the next incomplete build-plan task or a coherent batch of adjacent
   tasks.

During the run:

- Make real code changes when feasible.
- Prefer vertical slices over scattered placeholders.
- Keep product claims conservative and aligned with the safety limitations.
- Keep camera/radiation behavior deterministic before adding ML.
- Use local-only processing and privacy-preserving defaults.
- Add or update tests for new logic where practical.
- Run the most relevant build, test, or static check available.
- Avoid unrelated refactors.

For early implementation runs, default to Phase 0 unless the repo state shows it
is already complete:

- Android Kotlin project setup.
- Camera2 camera discovery.
- Camera capability display.
- Single-camera YUV capture plumbing.
- Manual exposure, ISO, and focus lock attempts.
- Frame brightness and variance metrics.
- Basic debug UI.

Then continue through later phases in build-plan order unless dependencies make
a different order clearly better.

## Documentation Updates After Each Run

After completing work, update GitHub Markdown docs before stopping when
possible:

- Update `IMPLEMENTATION_LOG.md` with date, summary, completed build-plan
  tasks, tests or builds run, files changed, blockers, and recommended next
  tasks.
- Update the progress/status area in `BUILD_PLAN.md`.
- Mark tasks as completed, in progress, or blocked without rewriting the
  product strategy.
- Do not weaken safety limitations, privacy constraints, or marketing
  guardrails unless the user explicitly asks.

If a run cannot update docs because of a tool or network blocker, state that in
the final response and make the code changes still stand on their own.

## Definition Of Done For A Run

A run is complete when:

- the selected task batch is implemented or explicitly blocked,
- relevant tests or checks were run, or the reason they could not run is known,
- `IMPLEMENTATION_LOG.md` is updated,
- `BUILD_PLAN.md` progress is updated when task status changed,
- the final response names what changed, what was verified, and the next
  recommended build-plan task.
