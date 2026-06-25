# Inference Repair Prototype

This package contains prototype infrastructure for repairing source code when inference produces
unsatisfiable constraints.

## Pipeline

The repair loop is intentionally validation-driven:

1. Run inference and collect constraints.
2. Extract unsatisfiable constraint contexts.
3. Expand those contexts to nearby repair-relevant constraints.
4. Plan ranked repair candidates from the constraint contexts.
5. Resolve each candidate to a concrete source span.
6. Generate concrete replacement edits.
7. Apply one edit to a temporary source copy.
8. Rerun inference on the repaired source.
9. If inference succeeds, insert inferred annotations with AFU.
10. Run the checker on the inserted source.

An edit is accepted only if the final inference, AFU insertion, and checker typecheck all pass.

## Experiment Reports

`InferenceRepairExperimentRunner` is the evaluation entry point for research experiments. For each
source file, it records:

- initial inference outcome and constraint counts,
- unsat and repair-context counts,
- generated repair candidates,
- validation attempts,
- whether repair solved inference,
- whether the repaired source passed AFU insertion and final checker typecheck.

`InferenceRepairExperimentResult` and `InferenceRepairExperimentBatchResult` serialize these
metrics as JSON so benchmark runs can be compared across repair configurations and ablations.

The Gradle entry point writes a report for one or more source files:

```text
./gradlew repairExperimentReport \
  -PrepairExperimentMode=NO_REPAIR \
  -PrepairExperimentSources=testdata/repair/InferenceUnsatAssignment.java \
  -PrepairExperimentOutput=build/inference-repair-experiments/report.json
```

Use `-PrepairExperimentSourceList=path/to/sources.txt` for larger batches.
Use `-PrepairExperimentSourceRoots=src/main/java,src/test/java` to recursively discover Java
sources. Project metadata can be attached with `-PrepairExperimentProjectName`,
`-PrepairExperimentProjectRoot`, and `-PrepairExperimentRevision`.

For benchmark checkouts, first write a stable source list and reuse it across runs:

```text
./gradlew repairSourceList \
  -PrepairSourceListRoots=path/to/project/src/main/java,path/to/project/src/test/java \
  -PrepairSourceListFilter=NULLNESS_RELEVANT \
  -PrepairSourceListOutput=build/inference-repair-source-lists/project.txt
```

Compare multiple experiment reports as a CSV summary:

```text
./gradlew repairExperimentCompare \
  -PrepairExperimentReports=build/inference-repair-experiments/no-repair.json,build/inference-repair-experiments/unsat-guided.json \
  -PrepairExperimentComparisonOutput=build/inference-repair-experiments/comparison.csv
```

Use `-PrepairExperimentReportList=path/to/reports.txt` when comparing a larger benchmark matrix.

For a single inspectable case study, generate a Markdown trace:

```text
./gradlew repairCaseStudyTrace \
  -PrepairCaseStudySource=testdata/repair/InferenceUnsatMethodCall.java \
  -PrepairCaseStudyOutput=build/inference-repair-case-study/method-call-trace.md
```

## Localization Pilot Study

Before implementing the full repair-aware constraint system, use the localization study runner to
sample real CFI nullability UNSAT cases and annotate whether solver-derived MCS locations cover the
human repair. The runner writes both raw JSON and spreadsheet-friendly CSV:

```text
./gradlew repairLocalizationStudyReport \
  -PrepairLocalizationSourceList=build/inference-repair-source-lists/project.txt \
  -PrepairLocalizationSampleSize=100 \
  -PrepairLocalizationTopK=5 \
  -PrepairLocalizationValidateSourceRepairPlans=true \
  -PrepairLocalizationOutput=build/inference-localization-study/report.json \
  -PrepairLocalizationCsvOutput=build/inference-localization-study/report.csv
```

For the NullRepair artifact, the artifact README says evaluated projects live under `benchmarks/`.
If the full artifact is available locally, a broad first pilot is:

```text
./gradlew repairBenchmarkSourceLists \
  -PrepairBenchmarkRoot=path/to/nullrepair-artifact/benchmarks \
  -PrepairBenchmarkSourceListFilter=NULLNESS_RELEVANT \
  -PrepairBenchmarkIncludeProjects=retrofit,conductor,libgdx \
  -PrepairBenchmarkSourceListOutputDir=build/inference-repair-source-lists

./gradlew repairLocalizationStudyReport \
  -PrepairLocalizationProjectName=<project> \
  -PrepairLocalizationProjectRoot=path/to/nullrepair-artifact/benchmarks/<project> \
  -PrepairLocalizationSourceList=build/inference-repair-source-lists/<project>.txt \
  -PrepairLocalizationSampleSize=20 \
  -PrepairLocalizationTopK=5 \
  -PrepairLocalizationTimeoutSeconds=30 \
  -PrepairLocalizationValidateSourceRepairPlans=true \
  -PrepairLocalizationOutput=build/inference-localization-study/<project>.json \
  -PrepairLocalizationCsvOutput=build/inference-localization-study/<project>.csv
```

For a tighter real-case pilot, use NullRepair's manual-inspection table instead of broad source
discovery. The table rows identify concrete checker errors, source locations, and reviewer scores.
Generate one source list per benchmark from those rows:

```text
./gradlew repairNullRepairManualSourceLists \
  -PrepairNullRepairManualInspectionTsv=path/to/results_evaluation/gpt5.1/manual_inspection/manual_inspection_scoring.tsv \
  -PrepairNullRepairManualIncludeProjects=eureka,glide,libgdx,litiengine \
  -PrepairNullRepairManualMaxBestScore=1 \
  -PrepairNullRepairManualSourceListOutputDir=build/inference-repair-source-lists/nullrepair-manual
```

`MaxBestScore=1` keeps rows where at least one inspected patch received the best manual score. Drop
that filter to include every manually inspected error. The generated source paths are relative to
each benchmark project root, so they can be fed directly into `repairLocalizationMatrixFile`.

If the full artifact is not cloned locally, materialize only the selected source files from the
anonymous artifact raw-file endpoint:

```text
./gradlew repairNullRepairManualMaterialize \
  -PrepairNullRepairManualInspectionTsv=path/to/results_evaluation/gpt5.1/manual_inspection/manual_inspection_scoring.tsv \
  -PrepairNullRepairManualIncludeProjects=eureka,glide,libgdx,litiengine \
  -PrepairNullRepairManualMaxBestScore=1 \
  -PrepairNullRepairManualUseDefaultArtifactUrl=true \
  -PrepairNullRepairManualMaterializedRoot=build/nullrepair-manual-materialized/benchmarks \
  -PrepairNullRepairManualSourceListOutputDir=build/nullrepair-manual-materialized/companion-source-lists \
  -PrepairNullRepairManualTargetSourceListOutputDir=build/nullrepair-manual-materialized/target-source-lists \
  -PrepairNullRepairManualCaseManifestOutput=build/nullrepair-manual-materialized/case-manifest.csv \
  -PrepairNullRepairManualClosureDepth=1
```

For an already-cloned artifact, replace `-PrepairNullRepairManualUseDefaultArtifactUrl=true` with:

```text
-PrepairNullRepairManualBenchmarkRoot=path/to/nullrepair-artifact/benchmarks
```

The materializer writes two source-list flavors when
`-PrepairNullRepairManualTargetSourceListOutputDir` is set:

- `target-source-lists/<project>.txt`: only the manually inspected NullRepair error files; use this
  as the localization study's sampled source list.
- `companion-source-lists/<project>.txt`: the target files plus materialized closure files; use this
  as a companion source list for javac context.

It also writes `case-manifest.csv`, which preserves the manual-inspection row metadata for each
selected case: benchmark, ID, diagnostic type/message, line, relative source path, triggering
expression, and reviewer scores. Use that manifest to connect localization-study rows back to the
concrete NullRepair case and expected human-reviewed repair.

`ClosureDepth=1` fetches likely same-project companion sources using package/import declarations and
same-package type references. Missing closure candidates are ignored.

Generate a matrix that samples only target files while passing companion files to javac:

```text
./gradlew repairLocalizationMatrixFile \
  -PrepairLocalizationMatrixSourceListDir=build/nullrepair-manual-materialized/target-source-lists \
  -PrepairLocalizationMatrixCompanionSourceListDir=build/nullrepair-manual-materialized/companion-source-lists \
  -PrepairLocalizationMatrixBenchmarkRoot=build/nullrepair-manual-materialized/benchmarks \
  -PrepairLocalizationMatrixOutput=build/nullrepair-manual-materialized/matrix.csv
```

The matrix format is backward-compatible:

```text
# projectName,sourceList,projectRoot[,companionSourceList]
eureka,target-source-lists/eureka.txt,benchmarks/eureka,companion-source-lists/eureka.txt
```

Summarize a pilot before spending manual annotation effort:

```text
./gradlew repairLocalizationStudySummary \
  -PrepairLocalizationSummaryCsv=build/inference-localization-study/<project>.csv \
  -PrepairLocalizationSummaryOutput=build/inference-localization-study/<project>-summary.md
```

Use the summary to triage benchmark suitability: prioritize projects with enough UNSAT rows, low
timeout rates, and high candidate yield among UNSAT rows.

When a project has many run errors, extract the dependency blockers before interpreting the
localization numbers:

```text
./gradlew repairLocalizationDependencyReport \
  -PrepairLocalizationDependencyCsv=build/inference-localization-study/<project>.csv \
  -PrepairLocalizationDependencyOutput=build/inference-localization-study/<project>-dependencies.csv
```

The dependency report is intended to separate inference/repair failures from incomplete benchmark
context, such as missing project classes, generated sources, or third-party classpath entries.

For quick benchmark triage, generate minimal source stubs for missing class/interface/enum symbols:

```text
./gradlew repairLocalizationStubSources \
  -PrepairLocalizationStubCsv=build/inference-localization-study/<project>.csv \
  -PrepairLocalizationStubProjectRoot=path/to/benchmarks/<project> \
  -PrepairLocalizationStubOutputDir=build/inference-localization-study/<project>-stubs \
  -PrepairLocalizationStubSourceListOutput=build/inference-localization-study/<project>-stubs.txt
```

The task also accepts cumulative reports when one stub pass exposes the next compiler layer:

```text
./gradlew repairLocalizationStubSources \
  -PrepairLocalizationStubCsvs=first-pass.csv,second-pass.csv \
  -PrepairLocalizationStubProjectRoot=path/to/benchmarks/<project> \
  -PrepairLocalizationStubOutputDir=build/inference-localization-study/<project>-merged-stubs \
  -PrepairLocalizationStubSourceListOutput=build/inference-localization-study/<project>-merged-stubs.txt
```

These stubs are only intended to unblock the next compiler diagnostics during benchmark
exploration. They should be replaced by real project classpaths or checked source dependencies when
running final experiments.

For multiple benchmark projects, create a matrix CSV with one project per line:

```text
# projectName,sourceList,projectRoot
retrofit,build/inference-repair-source-lists/retrofit.txt,path/to/benchmarks/retrofit
conductor,build/inference-repair-source-lists/conductor.txt,path/to/benchmarks/conductor
```

If the source lists are named after projects, generate that matrix file automatically:

```text
./gradlew repairLocalizationMatrixFile \
  -PrepairLocalizationMatrixSourceListDir=build/inference-repair-source-lists \
  -PrepairLocalizationMatrixBenchmarkRoot=path/to/benchmarks \
  -PrepairLocalizationMatrixIncludeProjects=retrofit,conductor,libgdx \
  -PrepairLocalizationMatrixOutput=build/inference-repair-source-lists/matrix.csv
```

Use `-PrepairLocalizationMatrixExcludeProjects=repair-fixtures,scratch` when the source-list
directory contains local test lists that should not enter a benchmark pilot.

Then run pilot reports and summaries for every row:

```text
./gradlew repairLocalizationPilotMatrix \
  -PrepairLocalizationMatrix=build/inference-repair-source-lists/matrix.csv \
  -PrepairLocalizationMatrixSampleSize=20 \
  -PrepairLocalizationMatrixTopK=5 \
  -PrepairLocalizationMatrixTimeoutSeconds=30 \
  -PrepairLocalizationMatrixValidateSourceRepairPlans=true \
  -PrepairLocalizationMatrixOutputDir=build/inference-localization-study/matrix
```

The matrix task writes `<project>.json`, `<project>.csv`, `<project>-summary.md`, and an aggregate
`matrix-summary.csv` for quickly ranking projects before the larger 100-case manual study. The
aggregate includes `sourceRealizableYield`, the fraction of UNSAT rows with at least one legal
source repair unit, and `sourceRealizableCandidateYield`, the fraction of UNSAT rows whose top-1
MCS candidate maps to at least one legal source repair unit.

When validation is enabled, the aggregate also includes a repair funnel:

- `repairPlanMaterialized`: annotation repair plans that were materialized into a source file.
- `repairPlanAttempted`: any source-repair validation attempt, including direct expression repair.
- `repairPlanInferenceSolved`: validation attempts whose repaired source made inference SAT.
- `repairPlanVerified`: validation attempts that also passed final checker verification.
- `repairFollowUpAttempted`: cases where annotation repair solved inference but left residual
  checker diagnostics and triggered source repair.
- `repairFollowUpVerified`: residual diagnostic repairs that passed checker verification.

In JSON, `sourceRepairPlanValidation.stages` records the ordered path for each case. The current
stage names are:

- `annotation-materialization`
- `inference-rerun`
- `post-inference-typecheck`
- `diagnostic-source-repair`
- `inference-source-repair`

Each case records:

- unsat core source locations,
- repair-aware source units with provenance, source-realizability, and legal edit domains,
- source-realizability counts for measuring the gap between logical MCS and legal edits,
- top-k weighted MCS location sets,
- the oracle used for MCS enumeration,
- whether the MCS universe was truncated,
- blank manual annotation fields.

`sourceRepairUnits` intentionally separates logical diagnosis from legal source edits. A raw MCS
may mention non-editable typing rules or constants; those appear as non-realizable logical
evidence. Only repair units marked `sourceRealizable: true` have an edit domain that can become a
candidate Java source patch. `sourceRepairPlans` ranks concrete non-identity edits selected from
those legal edit domains. The first edit grammar supports annotation sites and expression repair
sites:

- `Identity`
- `InsertQualifier(q)`
- `ReplaceQualifier(q)`
- `RemoveQualifier`
- `RepairExpression`

Manual annotation fields:

- `humanRepairLocations`: source locations touched by the manually chosen reasonable repair.
- `humanRepairUnitIds`: source-realizable repair unit ids touched by the manually chosen repair.
- `humanRepairSchemas`: one or more labels from the repair schema list below.
- `humanRepairNotes`: short rationale, especially when the repair requires semantic judgment.
- `top1Hit`, `top3Hit`, `top5Hit`: `yes`, `partial`, `no`, or `unclear`.
- `sourceRealizableTop1Hit`, `sourceRealizableTop3Hit`, `sourceRealizableTop5Hit`: whether the
  corresponding top-k MCS candidates include a legal source repair unit matching the manual repair.

Repair schema labels:

- `ChangeQualifier`: change a field, parameter, return, local, or type-argument qualifier.
- `InsertNullGuard`: insert an explicit null check with guarded non-null use.
- `InsertRequireNonNull`: add fail-fast non-null assertion.
- `InsertDefaultValue`: substitute a project-appropriate default value.
- `InitializeField`: add or move field initialization.
- `PropagateNullable`: make nullability flow through callers/callees and repair downstream uses.
- `ChangeReturnContract`: change a method return contract and related implementation/callers.
- `ChangeParameterContract`: change a parameter contract and related implementation/callers.
- `AddPrecondition`: add caller-side or method-entry precondition.
- `AddPostcondition`: add method contract/postcondition so the checker can know a value is non-null.
- `SuppressOrCast`: use suppression or unchecked/non-null cast; treat as high-risk fallback.
- `NoValidRepair`: no reasonable source repair is apparent.
- `Unknown`: insufficient context to classify.

Scope labels may be added in `humanRepairSchemas` or notes when useful:

- `single-site`, `multi-site`, `intra-method`, `inter-method`, `public-api-change`,
  `library-or-stub-change`, `semantic-hole-needed`.

Supported experiment modes:

- `UNSAT_CORE_GUIDED`: plan repair from the unsat core plus related constraints that touch unsat
  slots.
- `UNSAT_CORE_ONLY`: plan repair only from the solver's unsat constraint contexts.
- `NO_REPAIR`: run inference and report residual unsat data without attempting repair.

Supported edit-provider modes:

- `DETERMINISTIC_ONLY`: use only built-in repair edit generation.
- `AI_ONLY`: use only the configured AI repair client.
- `DETERMINISTIC_THEN_AI`: try deterministic edits first, then AI edits.

## Extension Points

`InferenceRepairCandidatePlanner` proposes repair candidates from inference constraint contexts.
`SimpleNninfRepairPlanner` is the first nninf-specific planner.

`InferenceRepairEditProvider` proposes concrete edits for a resolved source target.
`InferenceRepairEditGenerator` is the deterministic provider used by default.

`CompositeInferenceRepairEditProvider` combines multiple providers in priority order. This is the
intended path for mixing deterministic, checker-specific, and AI-backed edit proposals.

`RepairUsageIndex` retrieves bounded safe and unsafe usage examples for the target expression.
`RepairUsageContextExtractor` is the first source-backed implementation. It derives lookup keys
from the inference repair target, including simple identifiers, full method-call/member expressions,
method names, and receivers.

## AI Providers

AI-backed providers implement `AiInferenceRepairEditProvider`.

The provider receives a `RepairPromptContext` containing:

- the allowed source replacement span,
- the original target text,
- bounded surrounding source,
- bounded safe and unsafe usage examples from configured project source files,
- the repair kind and qualifier,
- the inferred expected Java type when available,
- the unsat constraint and target slot summaries.

The AI provider should return candidate replacement source strings only, not a full file and not a
patch. `AiRepairEditProvider` adapts those strings into `InferenceRepairEdit`s and limits how many
AI proposals enter validation.

AI is only a proposal source. The validator remains the acceptance oracle:

```text
AI proposal -> apply edit -> rerun inference -> AFU insert -> final checker typecheck
```

This means unsafe, irrelevant, or syntactically invalid AI proposals should be rejected by the same
pipeline that validates deterministic repairs.

## Current Limitations

The current target-type resolver is source-based and intentionally conservative. It handles simple
assignments, field assignments, and direct method-call arguments. It should eventually be replaced
or backed by parsed AST and symbol/type information.

The current usage-context extractor is also source-based. It classifies examples with conservative
null-check and require-non-null string patterns. The `RepairUsageIndex` seam should be backed by
a real parsed AST/symbol index before benchmark-scale repair experiments.

The prototype currently focuses on nninf/nullness-style repairs. Other checkers should plug in their
own candidate planners and deterministic edit providers before relying on AI proposals.

The experiment runner currently handles source-file inputs directly. A project-level harness should
add source discovery, build/test command execution, and ablation flags before large benchmark
runs.
