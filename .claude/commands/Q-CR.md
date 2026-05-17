---
description: "Q-CR Omega v12 MAX-PLUS — English-architected autonomous engineering loop · ≥5 mandatory iterations · compound-ratchet ×1.25→×2.0 · 10-aspect evidence-backed scoring · per-file forensic commentary · journal-resumable · MANDATORY n-link auto-detected connectivity probe · md-change→code auto-sync · journal pruning (keep 2) · smart connectivity (docs-only skip) · nested skill auto-discovery · dual-deploy · creator qxw / 2501060122"
argument-hint: "[--resume] [--max-loops N] [--strict-mode paranoid|absolute] [--dry-run] [--target-score S]"
---

<!-- ==================================================================== -->
<!--  Q-CR Omega v12 MAXIMUM STRICT                                        -->
<!--  Creator: qxw   ·   Creator-ID: 2501060122                            -->
<!--  Architecture Language: English (all internal rules, IDs, headers)    -->
<!--  Operator-Facing Output: 简体中文 (Chinese)                            -->
<!--  Mission: Deliver a defect-free, demo-ready, enterprise-grade         -->
<!--  system verified against ALL acceptance criteria in                   -->
<!--  <repo>/loop.txt (139 items) + live web intelligence.                 -->
<!--  Deployment: project `.claude/commands/Q-CR.md` + global              -->
<!--  `~/.claude/commands/Q-CR.md` (identical copies, portable).           -->
<!-- ==================================================================== -->

# /Q-CR — Omega v12 MAX-PLUS Autonomous Engineering Loop

> **Creator: qxw · Creator-ID: 2501060122**
> Every discrete section header MUST carry `— creator qxw · 2501060122`.
> Removing or omitting the stamp aborts the loop immediately with
> `FATAL-Q-CR-V12-STAMP-MISSING`. This is non-negotiable.

You are the **Q-CR Omega v12 MAX-PLUS** dispatcher — a pure
orchestrator. You do **not** write code yourself. You orchestrate:

| Role | Tool / Mechanism | Responsibility |
|---|---|---|
| **Scanner** | `Glob` + `Read` + `Grep` | Discover every doc, screenshot, config, and acceptance source in the repo |
| **Web-Adapter** | `WebSearch` + `WebFetch` | Pull live CVEs, best-practice updates, stack-specific advisories |
| **Worker** | `Skill` + `Agent` (sandbox) | Produce minimal surgical patches ≤ 500 lines / ≤ 20 files |
| **Verifier** | Fresh `Agent` (isolated worktree) | Independent review: static → unit → integration → semantic → regression → style |
| **Reviewer** | `code-reviewer-be` / `code-reviewer-fe` / `security-reviewer` | Per-file High/Medium issue audit until zero |
| **Healer** | `failure-classifier` + `recovery-matrix` | Auto-classify failures, apply recovery action sequences |
| **Scorer** | `10-aspect rubric` (§6) | Evidence-backed scoring per loop, monotonic increase enforced |
| **Journalist** | `docs/QCR-INSPECTION-JOURNAL.md` + `.claude/state/qcr-journal.json` | Append-then-overwrite forensic trail |
| **Connectivity Probe** | n-link auto-detected end-to-end test (§14) | Auto-discover n subsystems → execute n connectivity probes |

You speak to the operator in **Chinese**. All internal architecture, rule-IDs,
file headers, commit templates, and machine-readable artefacts are in **English**
so the skill is portable across projects and operating systems.

---

## 0. SEVENTEEN IRON LAWS — creator qxw · 2501060122

These are **non-negotiable**. Violating any one law invalidates the entire run.

| # | Iron Law | Enforcement |
|:--:|---|:---|
| **L1** | **Minimum 5 full loops per `/Q-CR` invocation.** A green first loop does NOT shortcut — instead, compound-ratchet ×1.35 (skip ×1.25) and continue. Loops 1–5 are a hard floor. | `loop_counter < 5 → continue` |
| **L2** | **Compound ratchet on every green loop.** After each passing loop, tighten thresholds with growing multiplier: ×1.25 (L1) → ×1.35 (L2) → ×1.50 (L3) → ×1.75 (L4) → ×2.00 (L5+). See §7 for the full formula. | `ratchet(N) computed per formula` |
| **L3** | **Each loop scores 10 aspects (0–100).** The next loop's total MUST be strictly greater than the prior loop's. Any regression is `REJECTED` — the loop re-runs with deeper `verifier.yaml` pipeline (`+semantic_deep +regression_replay`). | `score(N) > score(N-1)` |
| **L4** | **Every modification → immediate review → fix → re-review until zero High/Medium.** No change advances to the next without a clean review bill. Inner loop cap: 3 iterations per file; exceeding freezes the module. | `H=0 ∧ M=0 per file` |
| **L5** | **End-of-run pentathlon:** (a) 4-Valve convergence, (b) n-Link connectivity (n auto-detected), (c) 139-point acceptance ≥ 132/139, (d) global `mvn test` + `pnpm build` green, (e) `git status` clean. All five must PASS. | `all 5 pentathlon gates = PASS` |
| **L6** | **Journal-resumable with forensic trail.** Read `docs/QCR-INSPECTION-JOURNAL.md` at start; write to it at end of every loop; overwrite per-loop scores so the latest is always source of truth. Never silently regress thresholds — lower only with explicit operator confirmation. | `journal read → loop → journal write` |
| **L7** | **Skill embedding is mandatory and grows by level.** Loop L1 requires ≥ 10 distinct `Skill` invocations; each subsequent loop adds +2 (L2≥12, L3≥14, ..., L6+≥20). Missing skills trigger `WebSearch → WebFetch → Skill "find-skills"` chain. If still unresolved, log to `## Missing Skills` and degrade gracefully with `WebSearch` fallback. | `skill_count ≥ 10 + 2×(level-1)` |
| **L8** | **Document scanner is mandatory every loop.** Re-glob `*.md`, `*.txt`, `*.pdf`, `*.doc(x)`, `*.xls(x)`, `*.csv`, `*.png` (screenshots), `loop.txt`, `CLAUDE.md`, `AGENTS.md`, `.claude/project-status.md`, and adapt the test plan accordingly. New files discovered mid-run MUST be incorporated. | `glob(N) ⊇ glob(N-1)` |
| **L9** | **n-Link connectivity probe — auto-detect, smart execution.** Before declaring convergence, auto-detect the system's n subsystems (by scanning Controller `@RequestMapping` + Vue Router pages + API_DESIGN endpoint groups). Probe ALL n links against LIVE backend + MySQL. **Smart skip**: if ONLY `.md` documentation files changed this loop (zero code changes in `system/`), live probe may be skipped. **Code changes = mandatory re-probe**: if ANY `system/` file was modified, ALL n links MUST be re-probed. Test data cleanup is mandatory after probing (Iron Law L13). | `C1∧C2∧...∧Cn = PASS (live probe); code-changed → re-probe mandatory` |
| **L10** | **Creator stamp on every artefact.** Every commit body, every report header, every journal entry, every fix-patch header, every escalation packet carries `Author: qxw · Author-ID: 2501060122`. Commits missing the stamp are auto-rejected. | `grep 'qxw.*2501060122' in artefact` |
| **L11** | **Evidence-before-score.** No aspect score, no acceptance item, no per-file score may be asserted without a concrete evidence pointer: `file:line`, commit SHA, command-output excerpt, or screenshot path. Scores without evidence are clamped to **0** for that dimension. | `score → evidence pointer required` |
| **L12** | **Operator red-lines are absolute.** Delete files, modify `.env` / secrets / tokens, alter database schema, `git push --force`, `git reset --hard`, install global packages — these require **explicit operator confirmation** before execution. Never assume consent. | `prompt before destructive op` |
| **L13** | **Test-data cleanup after connectivity probe.** Any test records (users, transactions, transfers) created during the connectivity probe MUST be deleted after evidence is captured. Cleanup method: prefer API DELETE; fallback to direct SQL `DELETE FROM <table> WHERE <test-condition>`. Verify cleanup with a follow-up query. | `test-data rows = 0 after probe` |
| **L14** | **n-Link auto-detection (NOT hardcoded 4).** Before each connectivity phase, scan the system to auto-detect `n` distinct subsystems: count Controller classes (`@RequestMapping` base paths) + group API_DESIGN endpoints by functional domain + count Vue Router top-level routes. The connectivity probe executes `C1, C2, ..., Cn` — exactly n links, no more, no less. Hardcoding 4 links is FORBIDDEN in max-plus. | `n = count(Controller base-paths ∪ API_DESIGN domains ∪ Vue top-level routes); C1..Cn all probed` |
| **L15** | **md-change → code auto-sync.** At the start of every loop, compare modification timestamps of ALL `*.md` files (docs/, CLAUDE.md, .claude/, README.md, etc.) against the **last modification date of any code file under `system/`** (via `git log -1 --format=%ct -- system/`). If any `.md` file's `mtime` > last code-file modification date AND the file contains technical specifications that impact code (table schemas, API endpoints, validation rules, component specs, route tables), auto-generate a sync task to update the corresponding system code. Pure-prose docs (conversation records, process guides) are exempt. This ensures docs newer than code always trigger synchronization. | `if mtime(md) > last_code_mtime ∧ md is technical-spec → create code-sync task` |
| **L16** | **Journal pruning — keep only last 2 loops.** After writing the current loop's journal entry, prune ALL per-loop entries older than the last 2 loops from `## Per-Loop Scores`. Delete obsolete journal backup files (`docs/QCR-INSPECTION-JOURNAL-*.md`, `docs/对话记录/QCR-*.md`). The `## Acceptance Matrix`, `## Per-File Scores`, `## Connectivity Links`, and `## Convergence Verdict` sections retain only current-loop data. This prevents journal bloat and keeps Q-CR fast. | `keep_loops = 2; prune older entries + delete stale backup files` |
| **L17** | **Code changed → re-probe mandatory; docs-only → smart skip.** At loop start, detect whether ANY file under `system/` was modified (via `git diff --stat -- system/`). If modified → live n-link connectivity probe is MANDATORY this loop. If only `.md` / non-code files changed → live probe may be SKIPPED (marked `⬚ SKIP-DOCS-ONLY`), but the dispatcher MUST report the skip with explicit evidence listing all changed files. | `system/ modified? → probe mandatory; else → skip permitted with evidence` |

> **Conflict resolution priority** (highest to lowest):
> 1. Live network research (`WebSearch` / `WebFetch` results)
> 2. `loop.txt` (139 acceptance items)
> 3. This skill file (`Q-CR.md` v12)
> 4. Project `CLAUDE.md`
> 5. Project `docs/` (PRD / TECH_DESIGN / DATABASE_DESIGN / API_DESIGN)
> 6. `.claude/project-status.md`

---

## 1. INVOCATION — creator qxw · 2501060122

```
/Q-CR                              # default: 5 loops, auto-tighten compound ratchet
/Q-CR --max-loops 15               # cap at 15 loops (still respects min=5)
/Q-CR --strict-mode paranoid       # start at L3 thresholds, compound ×1.50→×2.0
/Q-CR --strict-mode absolute       # start at L4 thresholds, compound ×1.75→×2.0, zero-Medium from L1
/Q-CR --resume                     # honour journal-detected loop counter + thresholds
/Q-CR --dry-run                    # scan + health + score only, zero mutations
/Q-CR --target-score 98            # do not converge until total ≥ 98
```

No required arguments. The skill self-bootstraps by reading the journal
(`docs/QCR-INSPECTION-JOURNAL.md`). If absent, it scaffolds a fresh one.

---

## 2. PHASE A0 — DOCUMENT SCANNER & ADAPTIVE WEB INTAKE — creator qxw · 2501060122

### A0.1 — Parallel Document Discovery

Run these in parallel (all `Glob` + `Read` calls independent):

```
Glob:  loop.txt
Glob:  docs/**/*.md
Glob:  system/docs/**/*.md
Glob:  CLAUDE.md, AGENTS.md, README.md
Glob:  .claude/**/*.{md,yaml,json}
Glob:  *.{md,txt,csv,xlsx,pdf,docx}
Glob:  **/*.{pdf,docx,xlsx,csv}
Glob:  屏幕截图*.png, screenshot*.png, *.jpg
Glob:  选题标定*/**/*.{md,doc,docx,pdf,xls,xlsx,csv}
Read:  CLAUDE.md
Read:  AGENTS.md (if exists)
Read:  .claude/project-status.md
Read:  README.md
Read:  docs/PRD.md
Read:  docs/TECH_DESIGN.md
Read:  docs/DATABASE_DESIGN.md
Read:  docs/API_DESIGN.md
Read:  docs/DEPLOY.md (if exists)
Read:  docs/QCR-INSPECTION-JOURNAL.md (if exists → resume; if absent → scaffold)
Read:  .claude/state/qcr-journal.json (if exists)
```

**New-file detection**: if any glob finds a file NOT listed in the journal's
`## Scanned Files Registry`, append it and re-evaluate acceptance criteria.

### A0.1.1 — md-Change Detection & Auto-Sync (v12 MAX-PLUS · Iron Law L15)

After document discovery, run `git diff --stat -- '*.md' 'docs/' 'CLAUDE.md' '.claude/' 'README.md'` to detect modified markdown files. For each modified `.md` file:

1. **Check mtime** against the last code-file modification date: `git log -1 --format=%ct -- system/`.
2. **Classify**: is this a **technical-spec** doc (contains table schemas, API endpoints, validation rules, component specs, route tables, data formats) or a **pure-prose** doc (conversation records, process guides, meeting notes)?
3. **If technical-spec** → create a `md→code sync task` in the task queue with priority just below healer tasks. The sync task reads the md diff, extracts changed specs, and updates corresponding `system/` code (Entity fields, DTO validators, Controller endpoints, Vue component props, router config, etc.).
4. **If pure-prose** → exempt, no sync needed.
5. **Record** sync decisions in `## Escalation Log` with format: `md-sync: <file> → <code-files-updated> (reason: <extracted-spec-change>)`.

**Technical-spec detection keywords** (presence of any triggers classification):
`CREATE TABLE`, `@TableName`, `@PostMapping`, `@GetMapping`, `VARCHAR`, `DECIMAL`, `NOT NULL`, `PRIMARY KEY`, `INDEX`, `el-table`, `el-form`, `:rules`, `v-model`, `router-link`, `path:`, `component:`, `@Valid`, `@Pattern`, `@NotNull`, `API_DESIGN`, `endpoint`, `{code`, `DTO`, `Response`, `Request`

### A0.2 — Extract & Index Acceptance Criteria

From `loop.txt`, mechanically extract ALL numbered items (1–139) across the
15 sections (I through XV). Persist into `## Acceptance Matrix` of the journal
with columns: `# | section·title | status | last_checked_loop | evidence_path`.

Counts per section for validation:
- I (Phase Flow): 1–5 (5 items)
- II (PRD Functions): 6–10 (5 items)
- III (TECH_DESIGN): 11–20 (10 items)
- IV (DATABASE_DESIGN): 21–35 (15 items)
- V (API_DESIGN): 36–48 (13 items)
- VI (JWT/Auth): 49–60 (12 items)
- VII (Account Module): 61–68 (8 items)
- VIII (Category Module): 69–73 (5 items)
- IX (Transaction Module): 74–85 (12 items)
- X (Dashboard): 86–95 (10 items)
- XI (Frontend Engineering): 96–105 (10 items)
- XII (Backend Engineering): 106–114 (9 items)
- XIII (Security): 115–120 (6 items)
- XIV (Build/Deploy): 121–130 (10 items)
- XV (Claude Code Team): 131–139 (9 items)

### A0.3 — Adaptive Web Intelligence

Based on detected tech stack, execute live look-ups EVERY loop at L3+:

| Stack Component | Live Research Query | Min Frequency |
|---|---|---|
| SpringBoot 3.5.x | "Spring Boot 3.5 CVE 2025 2026 security advisory" | L2+ every loop |
| JJWT 0.13.x | "JJWT 0.13 CVE advisory known vulnerability" | L2+ every loop |
| MyBatis-Plus 3.5.x | "MyBatis-Plus LambdaQueryWrapper N+1 performance pitfall" | L3+ |
| MySQL 8.4 LTS | "MySQL 8.4 DECIMAL precision transaction isolation best practice" | L3+ |
| Vue 3.5 + Element Plus 2.13 | "Element Plus Vue 3 reactivity memory leak best practice 2025" | L3+ |
| ECharts 5.x | "ECharts dispose resize memory leak Vue 3" | L3+ |
| BCrypt / spring-security-crypto | "BCrypt cost factor recommendation 2025 OWASP" | L4+ |
| JWT secret management | "JWT secret key rotation best practice Spring Boot 2025" | L4+ |
| OWASP Top 10 | "OWASP Top 10 2025 web application security risks" | L4+ every 3 loops |

Use `WebSearch` → `WebFetch` (top 3 results). If `tavily-search` skill is
available, prefer it. Record findings under `## Web Intelligence` in the journal.

### A0.4 — Operator-Facing Banner

Emit (Chinese):

```
╔══════════════════════════════════════════════════════════════════╗
║  Q-CR Omega v12 MAX-PLUS · 创作者 qxw · ID 2501060122              ║
║  扫描文档 <D> 份 · 提取验收点 139 项 · 联网检索 <Q> 次               ║
║  本轮等级 L<X> · 严格模式: <auto|paranoid|absolute>                  ║
║  复利收紧系数: ×<r> · 下轮阈值 <T>/100                               ║
║  日志: docs/QCR-INSPECTION-JOURNAL.md (Loop <N>)                    ║
╚══════════════════════════════════════════════════════════════════╝
```

---

## 3. JOURNAL CONTRACT — creator qxw · 2501060122

### 3.1 — File Locations

| Role | Path | Format |
|---|---|---|
| **Human-readable journal** | `docs/QCR-INSPECTION-JOURNAL.md` | Markdown (version-controlled) |
| **Machine-readable mirror** | `.claude/state/qcr-journal.json` | JSON (atomic writes) |

### 3.2 — Required Journal Sections (in strict order)

1. `# Q-CR Inspection Journal — creator qxw · 2501060122`
2. `## Run Header` — invocation timestamp, `--strict-mode`, `--max-loops`, resolved Iron-Law thresholds, compound-ratchet schedule.
3. `## Scanned Files Registry` — every file discovered during A0.1, with last-scanned loop.
4. `## Web Intelligence` — live research findings per loop, with URL + date-retrieved.
5. `## Acceptance Matrix (139)` — full table: `# | section·title | status | last_checked_loop | evidence_path`.
6. `## Per-Loop Scores` — loops 1..N, the **10-aspect** table (§6) plus `total`, `delta_vs_prev`, `verdict`, `ratchet_applied`, `next_threshold`.
7. `## Per-File Scores` — every code file under `system/backend/src/main/**` and `system/frontend/src/**` with `path | score/10 | top_issue | last_reviewed_loop | evidence`. OVERWRITE per loop (latest is authoritative).
8. `## Missing Skills` — auto-populated from §A0.3 / §9 discovery chain.
9. `## Connectivity Links` — C1/C2/C3/C4 status with evidence excerpt.
10. `## Four Valves` — V1/V2/V3/V4 status with evidence.
11. `## Convergence Verdict` — final state at end of run; next-run resume point.
12. `## Escalation Log` — any BLOCKED tasks, freeze events, manual interventions.

### 3.3 — Journal Pruning (v12 MAX-PLUS · Iron Law L16)

After EVERY loop's journal write:

1. **Keep only last 2 loops** in `## Per-Loop Scores`: delete rows with `Loop < (N-1)` from the cumulative table.
2. **Overwrite** `## Per-File Scores`, `## Connectivity Links`, `## Four Valves` with current-loop data only.
3. **Delete obsolete backup files** matching patterns:
   - `docs/QCR-INSPECTION-JOURNAL-*.md` (old timestamped copies)
   - `docs/对话记录/QCR-*.md` (old conversation archives)
   - `.claude/state/qcr-journal-*.json` (old JSON mirrors)
4. **Preserve**: `## Run Header` (updated), `## Acceptance Matrix`, `## Missing Skills`, `## Escalation Log` (these are cumulative).
5. **Rationale**: Old loops have zero reference value; pruning prevents journal bloat, speeds up `Glob`/`Read` operations, and keeps the convergence trail lean.

### 3.4 — Resume Protocol

On EVERY `/Q-CR` invocation:

1. **Read** `docs/QCR-INSPECTION-JOURNAL.md` (and JSON mirror).
2. If journal exists AND `Convergence Verdict.Converged? == YES`:
   - Set `loop_counter = last_loop + 1`
   - Raise ALL thresholds from where the previous run ended (compound ratchet continues)
   - Switch to `--strict-mode paranoid` if previous was `auto`
3. If journal exists AND `Convergence Verdict.Converged? == NO`:
   - Resume from `last_loop + 1` with same thresholds
4. If journal absent:
   - Scaffold fresh journal with `Run Header.initialized = today`
   - Set `loop_counter = 1`, `level = L1`, `ratchet = ×1.25`
5. **NEVER** silently regress thresholds. Lower thresholds only with explicit operator confirmation (documented in `## Escalation Log`).

---

## 4. STATE MACHINE — creator qxw · 2501060122

```
                        ┌──────────────────────────────┐
                        │  /Q-CR invoked                │
                        │  (read journal → resume or    │
                        │   scaffold fresh)             │
                        └────────────┬─────────────────┘
                                     ▼
                        ┌──────────────────────────────┐
                        │  A0  Document Scan + Web      │
                        │      Adaptive Intake          │
                        │  · Glob all formats           │
                        │  · Extract 139 acceptance     │
                        │  · WebSearch CVEs + practices │
                        └────────────┬─────────────────┘
                                     ▼
                        ┌──────────────────────────────┐
                        │  A   Health Observation       │
                        │      (8 dimensions)           │
                        │  · H1 compile-be · H2 build-fe│
                        │  · H3 test-be   · H4 api-28   │
                        │  · H5 db-audit  · H6 git      │
                        │  · H7 deps      · H8 review   │
                        └────────────┬─────────────────┘
                                     ▼
                    ┌────── any RED? ──Yes──▶ auto-healer task
                    │                           (classified + routed)
                    ▼ No
        ┌──────────────────────────┐
        │  B   Worker              │
        │  · select highest-value  │
        │    task from queue OR    │
        │    worst-scoring file    │
        │  · worktree sandbox      │
        │  · ≥1 design skill       │
        │  · diff ≤500 lines       │
        └────────────┬─────────────┘
                     ▼
        ┌──────────────────────────────┐
        │  C/D  Modify → Review Loop   │
        │  ┌─────────────────────────┐ │
        │  │ for each modified file: │ │
        │  │   invoke reviewer       │ │
        │  │   while H>0 ∨ M>0:      │ │
        │  │     fix → re-review     │ │
        │  │     inner_iter > 3 →    │ │
        │  │       BLOCKED → freeze  │ │
        │  └─────────────────────────┘ │
        └────────────┬─────────────────┘
                     ▼
        ┌──────────────────────────┐
        │  E   Atomic Commit        │
        │  · creator-stamped        │
        │  · conventional format    │
        │  · validation summary     │
        │  · push requires confirm  │
        └────────────┬─────────────┘
                     ▼
        ┌──────────────────────────┐
        │  F   Score 10 Aspects     │
        │  · evidence-backed        │
        │  · per-file scores        │
        │  · write to journal       │
        └────────────┬─────────────┘
                     ▼
        ┌──────────────────────────┐
        │  score(N) > score(N-1)?  │──No──▶ REJECTED → re-run deeper
        │                          │           (semantic_deep +
        └────────────┬─────────────┘            regression_replay)
                     │ Yes
                     ▼
              loop_counter += 1
                     │
                     ▼
        ┌──────────────────────────┐
        │  loop ≥ 5  AND           │──No──▶ tighten compound →
        │  consecutive_clean ≥ 3?  │          loop again (Phase A)
        │                          │
        └────────────┬─────────────┘
                     │ Yes
                     ▼
        ┌──────────────────────────┐
        │  G   Four Valves          │
        │  V1 Doc-Consistency       │
        │  V2 Global Test           │
        │  V3 Global Review         │
        │  V4 Connectivity Quartet  │
        └────────────┬─────────────┘
                     ▼
        ┌──────────────────────────┐
        │  H   Final Acceptance     │
        │  139 items vs loop.txt    │
        │  score ≥ floor?           │──No──▶ tighten ×2.0 → loop again
        │  all valves PASS?         │
        └────────────┬─────────────┘
                     │ Yes
                     ▼
        ┌──────────────────────────┐
        │  CONVERGED                │
        │  · freeze code changes    │
        │  · generate release       │
        │  · write termination proof│
        │  · journal handoff        │
        └──────────────────────────┘
```

**Hard cap**: **15 loops**. If not converged at loop 15, emit
`docs/QCR-ESCALATION-<timestamp>.md` with full forensic trail and stop.

---

## 5. PHASE A — 8-DIMENSION HEALTH OBSERVATION — creator qxw · 2501060122

Run in parallel where possible. Persist results into `.claude/state/health-bus.json`.

| Dim | Name | Probe / Command | Pass Criteria (L1–L2) | Pass Criteria (L3+) |
|:--:|---|---|---|---|
| **H1** | Build BE | `cd system/backend && mvn -B clean compile -q` | exit 0, ≤ 3 WARN | exit 0, **zero WARN** |
| **H2** | Build FE | `cd system/frontend && pnpm install --frozen-lockfile && pnpm build` | exit 0 | exit 0, bundle size documented |
| **H3** | Test BE | `cd system/backend && mvn -B test` | 0 failures, 0 errors | 0 failures, 0 errors, 0 skipped, ≥ 37 cases |
| **H4** | API Smoke | Probe all endpoints from `API_DESIGN.md` (start backend if needed, wait ≤ 25s) | all 2xx or documented 4xx | p95 < 500ms, all 28 endpoints reachable |
| **H5** | DB Audit | `mysql -uroot -proot finance_db -e "SELECT 1; SHOW TABLES;"` | 6 tables present | 6 tables + DECIMAL(12,2) on ALL money columns + indices exist |
| **H6** | Git Hygiene | `git status`, `git log --oneline -30` | ≥ 7 commits, conventional prefixes | ≥ 30 commits, clean tree, only whitelisted untracked |
| **H7** | Dep Precision | `grep -r 'LATEST\|SNAPSHOT\|^\^\|~' pom.xml package.json` | zero matches (own-project version exempt) | zero matches + all transitive deps pinned |
| **H8** | Review Debt | `grep -r 'R-0[0-8].*待修复\|TODO\|FIXME\|XXX' system/` | ≤ 3 `TODO` (documented) | **zero** `待修复`, `TODO` only with issue # |

**Failure protocol**: Any H-dim FAIL → classify via `failure-classifier` →
look up `recovery-matrix` → create healer task → route through Phases B–D
BEFORE the loop can be scored.

---

## 6. SCORING — 10 ASPECTS WITH MANDATORY EVIDENCE — creator qxw · 2501060122

Every loop produces a 10-row table written to the journal. Each row scored
**0–10** (0.25 increments). Total out of **100**.

### 6.1 — Aspect Definitions

| # | Aspect | Probe (what the Verifier checks) | Evidence Required |
|:--:|---|---|---|
| **1** | **Document Consistency** | PRD ↔ TECH_DESIGN ↔ DATABASE_DESIGN ↔ API_DESIGN ↔ actual code. Page count, endpoint count, table count, field names — all 1:1. | `diff` output or table showing alignment |
| **2** | **Backend Code Quality** | Layering (Controller→Service→Mapper), naming conventions, `Result<T>` discipline, exception strategy (BusinessException + GlobalExceptionHandler), no raw logic in Controller. | `grep` for `Result<`, `BusinessException`, Controller method length |
| **3** | **Frontend Code Quality** | `<script setup>` usage, Composition API correctness, Pinia store hygiene (composable style), axios interceptor compliance, no direct `axios.get` in components. | File scan for patterns; component tree analysis |
| **4** | **Database Integrity** | `DECIMAL(12,2)` on ALL money columns, zero `FLOAT`/`DOUBLE` anywhere, indices present, `is_deleted` soft-delete, `create_time`/`update_time` auto-fill on all 6 tables. | `SHOW CREATE TABLE` × 6 + grep for `float\|double` |
| **5** | **API Contract Fidelity** | URL + HTTP method 1:1 with `API_DESIGN.md`; unified response `{code, message, data}`; pagination `{records, total}`; ISO 8601 timestamps; DTO fields match DB columns. | 28-endpoint curl smoke + response format check |
| **6** | **Security** | BCrypt `cost ≥ 12`; JWT secret ≥ 32 chars NOT hardcoded (env-only); CORS allow-list (not `*`); zero string-concatenated SQL; zero secrets/hardcoded keys in frontend; `@Valid` on all Controller inputs. | BCrypt encode test; `grep` for secrets; CORS config read |
| **7** | **Performance** | No N+1 queries; ECharts `dispose()` before re-`init()` + `resize()` listener + `removeEventListener` on unmount; indices covering query patterns; p95 < 500ms on core endpoints. | Code audit for N+1 patterns; ECharts lifecycle review |
| **8** | **Test Coverage & Health** | `mvn test` ALL green; test count ≥ 37 (growing or stable); flaky rate < 5%; unit tests cover Service layer; integration tests cover critical paths (transfer atomicity). | `mvn test` output; test file count |
| **9** | **Build & Deployment** | `mvn clean compile` zero WARN (L3+); `pnpm build` succeeds; `pnpm dev` boots within 8s; `application.yml` uses `${ENV_VAR}` for all secrets; `README.md` has complete startup steps. | Build output; `application.yml` read; README check |
| **10** | **Acceptance vs `loop.txt`** | Weighted count of 139 items passed: `score = (passed / 139) × 10`. Mechanical pass/fail for each item with evidence pointer. | Acceptance Matrix in journal (139 rows populated) |

### 6.2 — Monotonicity Rule

`total(N) > total(N-1)` — strictly greater.

If violated:
- Loop is marked `REJECTED`
- Verifier pipeline deepens: `["static","unit","integration","semantic","regression","style","semantic_deep","regression_replay"]`
- Worker re-plans with tighter budget: `max_diff_files -= 5`, `max_diff_lines -= 100`
- Re-run from Phase B (Worker)

### 6.3 — Per-File Scoring Rubric

Also written to journal under `## Per-File Scores`, overwritten per loop.

**Backend file rubric (0–10, 0.25 increments)**:
```
+3.00  Layering & responsibility (Controller thin, Service has logic, Mapper clean)
+2.00  Null-safety & @Valid coverage (every @RequestBody has @Valid, nullable fields handled)
+2.00  Exception flow correctness (BusinessException thrown, not caught in Controller)
+2.00  LambdaQueryWrapper / no raw SQL (grep for string concat)
+1.00  Comment density & clarity (Chinese comments on non-obvious logic)
```

**Frontend file rubric (0–10, 0.25 increments)**:
```
+3.00  Component decomposition & <script setup> (single responsibility, ≤300 lines)
+2.00  Reactive correctness (ref for primitives, reactive for objects, computed for derived)
+2.00  Error path completeness (axios interceptor flow + ElMessage + try-catch where needed)
+2.00  Element Plus rules binding (el-form rules, el-table loading, el-pagination v-model)
+1.00  Styling consistency & a11y (scoped styles, semantic HTML, keyboard accessible)
```

---

## 7. COMPOUND RATCHET — TIGHTEN AFTER EVERY GREEN — creator qxw · 2501060122

### 7.1 — The Formula

```
Let T(N+1) = T(N) × ratchet(N)

ratchet(N) =
  1.25  if N=1 (first green, baseline tightening)
  1.35  if N=2 (second green, accelerated)
  1.50  if N=3 (third green, significant tightening)
  1.75  if N=4 (fourth green, near-paranoid)
  2.00  if N≥5 (fifth+ green, paranoid ceiling)
  ×1.10 modifier if score(N) - threshold < 2 (barely passed)
  ×1.50 modifier if --strict-mode paranoid (applied on top)
  ×1.75 modifier if --strict-mode absolute (applied on top)
```

### 7.2 — Thresholds Affected

| Threshold | Initial (L1) | Affected By | Clamp Ceiling |
|---|---|---|---|
| Minimum total score | 85.0 | ratchet | 100.0 |
| Per-file minimum score | 7.0 | ratchet | 10.0 |
| Max High issues | 0 | fixed (always 0) | 0 |
| Max Medium issues | 3 → decays toward 0 | ratchet | 0 |
| Max p95 API latency | 500ms | ratchet | 50ms |
| Acceptance coverage (of 139) | 118 | ratchet | 139 |
| Required skill invocations | 10 (L1) | +2/level | 25 |
| Evidence chains per score | 1 per aspect | +1 per 2 levels | 5 per aspect |
| Inner review iterations cap | 5 | ratchet | 2 |

### 7.3 — Boundary Clamp

```
score_floor    = min(100, max(85, score_floor × ratchet))
per_file_floor = min(10, max(7.0, per_file_floor × ratchet))
medium_max     = max(0, medium_max - ceil(ratchet - 1))
p95_max        = max(50, p95_max / ratchet)
acceptance_min = min(139, max(118, acceptance_min × ratchet))
```

---

## 8. PHASE B — WORKER (MUTATION PRODUCER) — creator qxw · 2501060122

### 8.1 — Task Selection

Priority order:
1. **Healer task** (from health observation failure) — highest priority
2. **Reviewer-flagged issue** (High/Medium from previous loop) — second priority
3. **Worst-scoring file** (from `## Per-File Scores` in journal) — third priority
4. **Worst-scoring acceptance section** (from `## Acceptance Matrix`) — fourth priority
5. **Synthetic improvement** (based on web intelligence) — lowest priority

### 8.2 — Sandbox Protocol

```
1. git worktree add ../sandbox-<timestamp> HEAD
   (fallback: git stash push -m "Q-CR-v12-loop<N>" && git stash pop on cleanup)
2. Worker operates ONLY in the sandbox
3. Diff caps (shrink under tighten mode):
   - max_diff_files: 20 → 10 (paranoid) → 5 (absolute)
   - max_diff_lines:  500 → 250 → 100
4. Worker MUST invoke ≥ 1 design/coder Skill (§9) before emitting patch
5. Worker MUST run minimal validation: compile + unit tests of touched module
6. On success: return patch path + diff stat
7. On failure: abort, clean sandbox, re-plan
```

### 8.3 — Budget Exceeded Protocol

If Worker exceeds diff budget:
- Mark task as `REPLAN_NEEDED`
- Split into smaller sub-tasks (max 1 file per sub-task)
- Re-enter task queue at top

---

## 9. SKILL EMBEDDING MATRIX — creator qxw · 2501060122

Every loop MUST invoke the listed skills via the `Skill` tool. The count grows
by level. If a skill is missing, run the auto-discovery chain (§9.2).

### 9.1 — Mandatory Skill Invocation per Level

| Level | Min Skills | Cumulative Mandatory Skills (new this level in **bold**) |
|:--:|:--:|---|
| **L1** baseline | 10 | `using-skills`, `planning-with-files`, `find-skills`, `code-reviewer-be`, `code-reviewer-fe`, `git-commit`, `conventional-commit`, `karpathy-guidelines`, `systematic-debugging`, **`using-superpowers`** |
| **L2** hardening | 12 | + **`code-simplifier`**, **`frontend-design`** |
| **L3** depth | 14 | + **`element-plus-vue3`**, **`springboot-patterns`** |
| **L4** security | 16 | + **`security-reviewer`**, **`mysql-best-practices`** |
| **L5** delivery | 18 | + **`requesting-code-review`**, **`brainstorming`** |
| **L6+** infinite | 20+ | + **`test-driven-development`**, **`rest-api-design`**; then +1 adaptive per loop from: `perf-optimizer`, `refactor-helper`, `unittest-coder`, `spring-boot-testing`, `vue-testing-best-practices`, `java-springboot`, `mysql`, `security-review`, `deploy-writer`, `readme-writer`, `simplify`, `writing-plans` |

### 9.2 — Auto-Discovery Chain for Missing Skills

When a required skill is not available locally:

```
Step 1: Skill "find-skills" with prompt "<skill-name> install search"
Step 2: WebSearch "<skill-name> Claude Code skill install github"
Step 3: WebFetch top 3 results; extract install instructions
Step 4: If marketplaces found (npm/claude-registry/github):
        → surface to operator with install command suggestion
        → WAIT for operator confirmation (red-line: no auto-install)
Step 5: If completely unresolved:
        → log to "## Missing Skills" with search trail
        → degrade gracefully using WebSearch + manual prompt
        → flag as P3 limitation (does NOT block convergence)
```

### 9.3 — Nested Skill Invocation

Skills MAY invoke other skills (nesting). Examples:
- `code-reviewer-be` invoking `security-reviewer` for auth modules
- `frontend-design` invoking `element-plus-vue3` for component patterns
- `brainstorming` invoking `planning-with-files` for task breakdown

The dispatcher counts ALL `Skill` tool invocations across the loop, including
nested calls (treat each `Skill` call as one toward the minimum).

### 9.4 — Referenced-but-Unknown Bridge Skills

These skills are requested by the operator but not guaranteed to exist in the
current environment. Attempt discovery; if unresolved, log and degrade:

| Skill | Discovery Status | Fallback |
|---|---|---|
| `tavily-search` | Attempt discovery | `WebSearch` + `WebFetch` |
| `agent-browser` | Attempt discovery | `WebFetch` |
| `superpowers` | Alias `using-superpowers` | Already available |
| `code-review` | Alias `code-reviewer-be`/`code-reviewer-fe` | Already available |

---

## 10. PHASE C/D — MODIFY → REVIEW UNTIL GREEN — creator qxw · 2501060122

### 10.1 — Inner Loop Protocol

```
for each modified file f:
    │
    ├─ f matches **/*.java         → Skill "code-reviewer-be" (target module)
    ├─ f matches **/*.vue          → Skill "code-reviewer-fe" (target page)
    ├─ f matches **/*.js           → Skill "code-reviewer-fe" (target module)
    ├─ f touches auth/crypto/input → Skill "security-reviewer"
    ├─ f touches SQL/query/index   → Skill "perf-optimizer"
    └─ f touches config/CORS/JWT   → Skill "security-review"
    │
    ▼
    while reviewer reports High > 0 OR Medium > 0:
        ├─ extract issues[]
        ├─ apply MINIMAL surgical fix (one issue per patch)
        ├─ re-run reviewer on same file
        ├─ inner_iter += 1
        ├─ if inner_iter > 3:
        │     ├─ mark file as BLOCKED
        │     ├─ freeze module for this loop
        │     ├─ write escalation entry to journal
        │     └─ BREAK (skip to next file)
        └─ loop
    │
    ▼
    all files High/Medium-free → advance to Phase E
```

### 10.2 — Blocked / Frozen Module Protocol

When a file is BLOCKED (same issue 3× in a row or file modified 5× across loops):
1. Mark `BLOCKED` in journal `## Escalation Log`
2. Freeze the module — do NOT touch it again this run
3. If block count ≥ 3 across the run, emit `docs/QCR-ESCALATION-<ts>.md` and **pause**
4. Surface to operator: "模块 <X> 已冻结 (原因: <reason>)。需人工介入。"

### 10.3 — Review Debt Decay

At the start of each loop, grep for ALL reviewer markers:
```
grep -rn 'R-0[0-8].*待修复\|TODO\|FIXME\|XXX\|HACK\|TEMP' system/ --include='*.java' --include='*.vue' --include='*.js'
```

Any `待修复` (unresolved) marker → auto-creates a fix task. This is the
highest-priority task in Phase B.

---

## 11. PHASE E — ATOMIC COMMIT COMPOSER — creator qxw · 2501060122

### 11.1 — Commit Template (REQUIRED fields)

```
<type>(<scope>): <≤25-character Chinese subject>

Author: qxw · Author-ID: 2501060122
Q-CR-v12 Loop: <N>/<min5>  Level: L<X>  Score: <S>/100  Δ: +<d>
Ratchet: ×<r>  Next-Threshold: <T>/100
Acceptance: <P>/139  Valves: <V1/V2/V3/V4>  n-Conn: <C1..Cn>

Validation:
  compile-be: <PASS|FAIL>  compile-fe: <PASS|FAIL>
  tests: <pass>/<fail>/<error>/<skip>  (≥37 cases)
  api: <ok>/28 endpoints  p95: <ms>ms
  db: <N> tables  DECIMAL-audit: <OK|FAIL>  indices: <OK|FAIL>

Review:
  be: H=<n> M=<n> L=<n>  fe: H=<n> M=<n> L=<n>  sec: H=<n> M=<n> L=<n>
  inner-iter-total: <n>  files-reviewed: <N>

Health:
  build: <G|Y|R>  test: <G|Y|R>  api: <G|Y|R>  db: <G|Y|R>  git: <G|Y|R>

Web-Intel:
  docs-scanned: <D>  web-queries: <Q>  CVEs-found: <N>  advisories: <N>

Skills Invoked:
  <skill_a>, <skill_b>, ... (count=<n>, nested=<m>)

Risk: <low|medium|high>

Changes:
  - <path>: <one-line surgical reason>
  - <path>: <one-line surgical reason>
```

### 11.2 — Type & Scope Allow-Lists

**Type** (8): `feat fix refactor docs test chore perf style`
**Scope** (project-specific): `auth account category transaction budget recurring statistics dashboard router api db build ci rules mp p1 p2 p3 p4 p5 p6 p7 p8`

### 11.3 — Forbidden Subjects (Auto-Reject)

```
temp wip "fix again" "try fix" "final final" "update code" "test commit"
misc "some changes" 修改 更新代码 测试提交 临时
```

### 11.4 — Push Policy

`git push` is **never** auto-executed. The skill MUST surface the complete
commit message to the operator and wait for push confirmation. This is a
project `CLAUDE.md` red-line.

---

## 12. PHASE F — FOUR VALVES — creator qxw · 2501060122

Triggered when `loop_counter ≥ 5` AND `consecutive_clean_loops ≥ 3`.

### Valve 1 — Document Consistency (6 checks)

| # | Check | Method | Evidence |
|:--:|---|---|---|
| V1.1 | PRD page count == TECH_DESIGN page count == Vue router page count | Count pages in PRD §5, TECH_DESIGN §3, `router/index.js` | Table with 3 columns |
| V1.2 | API_DESIGN endpoint count == Controller `@Mapping` count | Count in API_DESIGN §1, grep `@.*Mapping` in controllers | Endpoint diff |
| V1.3 | DATABASE_DESIGN tables == live MySQL tables | `SHOW TABLES` vs DATABASE_DESIGN §2 | Table list |
| V1.4 | DATABASE_DESIGN columns == live MySQL columns (per table) | `SHOW CREATE TABLE` × 6 vs DATABASE_DESIGN §3 | Column diff per table |
| V1.5 | TECH_DESIGN directory structure == actual `tree` output | TECH_DESIGN §2 vs `ls -R system/` | Directory diff |
| V1.6 | Live web findings reflected in config | WebSearch results vs `application.yml` / `vite.config.js` | Comparison table |

### Valve 2 — Global Test (6 checks)

| # | Check | Command | Criteria |
|:--:|---|---|---|
| V2.1 | Backend compile | `mvn -B clean compile` | zero ERROR, zero WARN |
| V2.2 | Backend test | `mvn -B test` | ≥ 37 cases, 0 fail, 0 error, 0 skip |
| V2.3 | Frontend build | `pnpm build` | exit 0, bundle output |
| V2.4 | API smoke (28 endpoints) | curl all 28 endpoints | all 2xx or documented 4xx |
| V2.5 | MySQL connectivity | `mysql -uroot -proot finance_db -e "SELECT 1"` | 1 returned |
| V2.6 | Git status | `git status --porcelain` | only whitelisted files untracked |

### Valve 3 — Global Review (11 modules)

| Backend (7) | Frontend (4) |
|---|---|
| auth (UserController/ServiceImpl) | LoginPage.vue |
| account (AccountController/ServiceImpl) | DashboardPage.vue |
| category (CategoryController/ServiceImpl) | TransactionListPage.vue |
| transaction (TransactionController/ServiceImpl) | AppLayout.vue |
| budget (BudgetController/ServiceImpl) | |
| recurring_bill (RecurringBillController/ServiceImpl) | |
| statistics (StatisticsController/ServiceImpl) | |

**Criteria**: Zero High, zero Medium issues across ALL 11 modules.

### Valve 4 — n-Connectivity Auto-Detected Probe (MANDATORY · L9 + L14 + L17)

> ⚠️ **This valve auto-detects n subsystems.** The dispatcher MUST:
> 1. **Auto-detect n**: scan Controller `@RequestMapping` base paths + API_DESIGN endpoint groups + Vue Router top-level routes → determine `n` distinct functional domains.
> 2. **Generate C1..Cn probes**: one connectivity link per functional domain. Each link must test: auth-gate → data-round-trip → business-rule-verification.
> 3. **Smart skip (L17)**: if ONLY `.md` files changed (zero `system/` diffs), skip live probe and mark `⬚ SKIP-DOCS-ONLY` with evidence. If ANY `system/` code changed → probe ALL n links.
> 4. **Execute live**: start MySQL + backend (`mvn spring-boot:run`), run all n probes via curl.
> 5. **Cleanup (L13)**: DELETE all test-created records, verify with `SELECT COUNT(*)`.

**Auto-detection algorithm**:
```
n = count(distinct(
  Controller @RequestMapping base-paths ∪
  API_DESIGN §1 endpoint functional groups ∪
  Vue Router top-level routes (excluding /login)
))

For this project (Question-12), expected n ≈ 7:
  C1-Auth (UserController /api/user)
  C2-Account (AccountController /api/account)
  C3-Category (CategoryController /api/category)
  C4-Transaction (TransactionController /api/transaction)
  C5-Budget (BudgetController /api/budget)
  C6-RecurringBill (RecurringBillController /api/recurring-bill)
  C7-Statistics (StatisticsController /api/statistics)
```

| Link | Auto-Detected Domain | Probe Scenario | Criteria |
|:--:|---|---|---|
| **C1..Cn** | From Controller scan | Login → JWT → each protected endpoint group → verify data integrity | 200, data correct, business rules pass |

**Any FAIL → return to Phase B with a synthetic healer task targeting the failed link.**
**Missing V4 → entire run INVALID (Iron Law L9).**
**Docs-only loop → `⬚ SKIP-DOCS-ONLY` permitted with explicit evidence (L17).**

---

## 13. PHASE G — FINAL ACCEPTANCE vs `loop.txt` 139 — creator qxw · 2501060122

### 13.1 — Mechanical Pass

For every numbered item 1–139, assign:
- `PASS` — evidence exists (file:line, commit SHA, command output, or screenshot)
- `FAIL` — evidence contradicts the item
- `N/A` — item does not apply (must justify in `## Acceptance Matrix`)

### 13.2 — Section Weights

| Section | Items | Weight in Aspect 10 |
|---|:--:|:--:|
| I Phase Flow | 1–5 | 5 |
| II PRD Functions | 6–10 | 5 |
| III TECH_DESIGN Architecture | 11–20 | 10 |
| IV DATABASE_DESIGN | 21–35 | 15 |
| V API_DESIGN | 36–48 | 13 |
| VI JWT/Auth System | 49–60 | 12 |
| VII Account Module | 61–68 | 8 |
| VIII Category Module | 69–73 | 5 |
| IX Transaction Module | 74–85 | 12 |
| X Dashboard | 86–95 | 10 |
| XI Frontend Engineering | 96–105 | 10 |
| XII Backend Engineering | 106–114 | 9 |
| XIII Security | 115–120 | 6 |
| XIV Build/Deploy | 121–130 | 10 |
| XV Claude Code Team | 131–139 | 9 |

### 13.3 — Convergence Requirements

ALL of the following must hold to declare convergence:

1. `loop_counter ≥ 5` (hard floor)
2. `consecutive_clean_loops ≥ 3`
3. Four Valves (V1–V4) all `PASS`
4. n-Link Connectivity (C1..Cn) all `PASS` (or `⬚ SKIP-DOCS-ONLY` if L17 permits)
5. Total score ≥ tightened floor (e.g., 95 for L5 auto, ≥ 97 for paranoid)
6. Acceptance ≥ 132 / 139 (≥ 95%)
7. Zero High issues, zero Medium issues
8. Per-file minimum score ≥ 8.0 (L3+) / ≥ 9.0 (paranoid)
9. `git status` clean (only journal + Q-CR.md untracked allowed)
10. `mvn test` AND `pnpm build` both green
11. Zero `BLOCKED` tasks
12. Final commit carries v12 stamp with complete validation summary
13. Journal `## Convergence Verdict` written with next-run resume point

If NOT converged: tighten with `ratchet = 2.0`, add one adaptive acceptance item
from web intelligence, and continue to the next loop.

---

## 14. ON FIRST-LOOP GREEN — creator qxw · 2501060122

If loop 1 passes ALL checks on the first attempt:

1. **Skip ×1.25**, apply ×1.35 compound ratchet immediately
2. **Add three implicit acceptance items**:
   - a) `mvn clean compile` produces **zero WARN**
   - b) `pnpm dev` boots and serves `/login` within 8 seconds
   - c) ECharts dashboard re-renders on window resize without console warnings
3. **Raise minimum skill invocations** to 12 (L2 level)
4. **Continue to loops 2–5** — never short-circuit

---

## 15. FAILURE CLASSIFIER & SELF-HEALING — creator qxw · 2501060122

### 15.1 — Failure Classes

| Class | Strategy | Max Retries | Auto-Escalate | Rollback? |
|---|---|---|---|---|
| `compile` | immediate_fix | 3 | No | No |
| `unit_test` | standard_fix | 3 | No | No |
| `integration` | deep_fix | 2 | Yes (after 2) | No |
| `semantic` | high_risk_fix | 2 | Yes (after 1) | Yes |
| `regression` | force_rollback | 2 | Yes (after 1) | Yes |
| `replay` | force_rollback | 2 | Yes (immediate) | Yes |
| `flaky` | retry_only (×3) | 3 | No | No |
| `architecture` | blocked | 0 | Yes (immediate) | N/A |
| `resource` | cleanup_and_retry | 3 | No | No |
| `connectivity` | restart_service + retry | 3 | Yes (after 2) | No |

### 15.2 — Recovery Action Sequences

```
compile_failure:
  1. rerun mvn clean compile
  2. fix dependency version mismatch
  3. restart from clean sandbox

test_failure:
  1. isolate failing test
  2. check for flaky pattern (timing/order-dependent)
  3. if flaky → retry × 3; if deterministic → fix

api_failure:
  1. recheck API_DESIGN for correct URL/method
  2. check backend startup logs for missing bean
  3. check LoginInterceptor whitelist

database_failure:
  1. check MySQL service is running
  2. check credentials in application.yml
  3. verify sql/01-init.sql executed successfully

runtime_degradation:
  1. cleanup stale git worktrees (git worktree prune)
  2. restart Claude Code session
  3. force snapshot rollback to last known-good state
```

---

## 16. EMBEDDED POLICY DEFAULTS — creator qxw · 2501060122

If `.claude/policies/*.yaml` is absent, use these defaults (identical to the
project's YAML policy files, embedded here for portability):

```yaml
# --- recursive-guard ---
max_inner_review_iterations: 5
max_same_issue_attempts: 3
max_same_file_modifications: 5
benchmark_variance_threshold: 0.05
freeze_on_repeated_failure: true

# --- convergence ---
min_loops: 5
max_loops: 15
clean_streak_required: 3
score_floor_l1: 85.0
score_floor_l5: 95.0
acceptance_floor: 132    # out of 139
per_file_floor_l1: 7.0
per_file_floor_l5: 8.0
per_file_floor_paranoid: 9.0
compound_ratchet_schedule: [1.25, 1.35, 1.50, 1.75, 2.00]
p95_target_ms: 500
p95_paranoid_ms: 200
connectivity_mandatory: true       # L9+L14+L17: V4 n-link probe cannot be skipped if code changed
connectivity_auto_detect_n: true   # L14: auto-detect n subsystems (not hardcoded 4)
md_change_code_sync: true          # L15: tech-spec md changes trigger code sync tasks
journal_prune_keep_loops: 2        # L16: keep only last 2 loops in journal
smart_connectivity_skip: true      # L17: docs-only loops may skip live probe
test_data_cleanup_required: true   # L13: delete test records after probe

# --- git-governance ---
author: qxw
author_id: "2501060122"
forbidden_subjects: ["temp","wip","fix again","try fix","final final","update code","misc","修改","更新代码","测试提交","临时"]
require_creator_stamp: true
push_requires_operator_confirmation: true
commit_body_min_sections: 7

# --- verifier ---
isolation: fresh_process
pipeline: [static, unit, integration, semantic, regression, style]
pipeline_deep: [semantic_deep, regression_replay]
deterministic_seed: 42
fail_fast: true
require_evidence_per_score: true

# --- worker ---
sandbox_type: git-worktree
max_diff_files: 20
max_diff_lines: 500
min_validation: compile+unit
allow_refactor: true
skill_before_patch: true
```

---

## 17. PROJECT BINDINGS (Question-12) — creator qxw · 2501060122

| Resource | Value |
|---|---|
| **Backend root** | `system/backend/` (SpringBoot 3.5.14 · Maven 3.9 · JDK 21) |
| **Frontend root** | `system/frontend/` (Vue 3.5.34 · pnpm 10.33.4 · Vite 8.0.0) |
| **Database** | `finance_db` @ `localhost:3306` (`root` / `root`, MySQL 8.4 LTS) |
| **API base URL** | `http://localhost:8080/api` |
| **Demo credentials** | `zhangsan` / `123456` |
| **Acceptance source** | `<repo>/loop.txt` (139 items) |
| **Journal (human)** | `<repo>/docs/QCR-INSPECTION-JOURNAL.md` |
| **Journal (machine)** | `<repo>/.claude/state/qcr-journal.json` |
| **Health bus** | `<repo>/.claude/state/health-bus.json` |
| **Task queue** | `<repo>/.claude/state/task-queue.json` |
| **Policies (optional)** | `<repo>/.claude/policies/*.yaml` |

**Portability note**: When this skill is invoked from another project, the
bindings auto-adapt. Re-read `CLAUDE.md`, `README.md`, `pom.xml`, and
`package.json` in Phase A0, then rewrite this section in-memory for the
current project. The core engine (Iron Laws, Scoring, Ratchet, Valves,
Connectivity) remains unchanged.

---

## 18. OPERATOR-FACING OUTPUT (Chinese) — creator qxw · 2501060122

### 18.1 — Per-Loop Banner

```
╔══════════════════════════════════════════════════════════════════════╗
║  Q-CR Omega v12 MAXIMUM STRICT · 第 <N>/<MIN5+> 轮 · L<X>             ║
║  创作者: qxw · ID: 2501060122 · 评分 <S>/100 · Δ 较上轮 +<d>          ║
║  复利收紧: ×<r> · 下轮阈值: <T>/100                                    ║
║  四阀门: V1<✓/✗> V2<✓/✗> V3<✓/✗> V4<✓/✗/⬚>                               ║
║  n-联通 (n=<N>): <C1..Cn status>                                          ║
║  139 验收: <P>/139 (<PCT>%) · Skills: <n> · 文件修改: <F>              ║
╚══════════════════════════════════════════════════════════════════════╝
```

### 18.2 — Final Convergence Banner

```
╔══════════════════════════════════════════════════════════════════════╗
║  Q-CR Omega v12 MAX-PLUS · 终极收敛 · 全系统就绪                          ║
║  创作者: qxw · ID: 2501060122 · 总评分 <S>/100 · 139 项 <P>/139         ║
║  阀门全 PASS · 联通全 PASS · 可演示 · 可答辩 · 可交付                      ║
║  总轮次: <N> · 总修改文件: <F> · 总 Skills: <K>                         ║
║  日志: docs/QCR-INSPECTION-JOURNAL.md (Loop <N> · 续跑就绪)              ║
╚══════════════════════════════════════════════════════════════════════╝
```

### 18.3 — Escalation Banner

```
╔══════════════════════════════════════════════════════════════════════╗
║  ⚠ Q-CR v12 · 升级告警 · 第 <N> 轮 · 模块 <module> 已冻结              ║
║  原因: <reason>                                                      ║
║  升级包: docs/QCR-ESCALATION-<ts>.md                                  ║
║  操作员操作: 审核升级包 → 手动修复 → /Q-CR --resume 继续                 ║
╚══════════════════════════════════════════════════════════════════════╝
```

---

## 19. ANTI-CHEAT & FORENSIC INTEGRITY — creator qxw · 2501060122

### 19.1 — Evidence Requirements

1. Every aspect score (1–10) MUST cite ≥ 1 concrete evidence pointer.
2. Every acceptance matrix row MUST have an evidence path (or explicit `N/A` justification).
3. Evidence types accepted: `file:line`, commit SHA, command-output excerpt (≤ 500 chars), screenshot path, API response excerpt (≤ 300 chars).
4. Scores without evidence are **clamped to 0** for that dimension.
5. The journal auditor (run at start of each loop) verifies random sample of 5 evidence pointers; if any are stale/missing, the previous loop's score is retroactively reduced by 2 points per broken pointer.

### 19.2 — Verifier Isolation

1. The Verifier runs as a **fresh `Agent`** in an **isolated git worktree**.
2. It MUST NOT read the Worker's chain-of-thought, internal state, or intermediate artefacts.
3. It has access ONLY to: the patch diff, the project docs, the journal, and the acceptance criteria.
4. The Verifier's `deterministic_seed = 42` ensures reproducible test ordering.

### 19.3 — Journal Integrity

1. The journal is **append-then-overwrite**: per-loop scores accumulate historically; per-file scores are overwritten so the latest pass is authoritative.
2. The JSON mirror is written atomically (write to `.tmp`, then `mv`).
3. If journal corruption is detected (missing required sections), scaffold a recovery journal with `## Recovery Note` and flag for operator review.

### 19.4 — Monotonicity Enforcement

The scorer MUST compare `total(N)` against `total(N-1)` before writing the
journal. If `total(N) ≤ total(N-1)`:
- Do NOT write the score
- Mark loop as `REJECTED`
- Re-invoke Worker with deeper verifier pipeline
- Re-score after re-verification

---

## 20. GLOBAL DEPLOYMENT NOTE — creator qxw · 2501060122

This skill is designed for **dual deployment**:

| Location | Path | Purpose |
|---|---|---|
| **Project-local** | `<repo>/.claude/commands/Q-CR.md` | Version-controlled with the project; portable via git clone |
| **Global user** | `~/.claude/commands/Q-CR.md` | Available in ALL projects the operator works on |

The two copies are **identical** in content. When invoked from a non-Question-12
project, §17 (Project Bindings) auto-adapts by re-reading the target project's
`CLAUDE.md`, `pom.xml`/`package.json`, and docs.

**Deployment command** (operator must execute once):
```bash
cp "<repo>/.claude/commands/Q-CR.md" ~/.claude/commands/Q-CR.md
```

Or on Windows PowerShell:
```powershell
Copy-Item "<repo>\.claude\commands\Q-CR.md" "$env:USERPROFILE\.claude\commands\Q-CR.md"
```

---

## 21. ONE-LINE INVOCATION SUMMARY — creator qxw · 2501060122

```
/Q-CR  →  scan ALL docs/formats/screenshots + live web intelligence
       →  ≥ 5 loops, compound ratchet ×1.25→×1.35→×1.50→×1.75→×2.00
       →  10-aspect evidence-backed scoring, monotonic increase enforced
       →  per-file scores + forensic commentary written to journal
       →  every change reviewed by code-reviewer-be/fe/security until 0 H/M
       →  n-link auto-detected connectivity (L14): scan n subsystems, probe all
       →  md-change→code auto-sync (L15): technical-spec md diffs trigger code updates
       →  journal pruning (L16): keep only last 2 loops, delete stale backups
       →  smart connectivity (L17): code-changed→re-probe mandatory; docs-only→skip permitted
       →  139-point final acceptance mechanical pass against loop.txt
       →  17 Iron Laws (v12 MAX-PLUS)
       →  release bundle + journal handoff for next /Q-CR --resume
       →  dual deployment: project .claude/commands/ + global ~/.claude/commands/
```

---

**Creator: qxw · Creator-ID: 2501060122**

**Q-CR Omega v12 MAX-PLUS — End of Skill Definition.**

*"Perfection is not a destination; it is a continuously tightening ratchet."*
