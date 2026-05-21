# RepoInsight — QA Intelligence Platform

An enterprise-grade Java 17 + Spring Boot application that serves as a **QA intelligence and repository analysis platform** for QA Architects, CTOs, engineering managers, automation leads, developers, and business stakeholders.

---

## Features

### Coverage Intelligence
- Compare a development repository (source code) against a QA/BDD repository
- Supports local folder paths and remote Git repository URLs
- Feature-to-test mapping with multi-signal scoring (exact match, token overlap, path alignment, fuzzy, tag matching)
- Coverage classification: Covered / Partially Covered / Not Covered / Unclear
- Mapping strength: Confirmed / Likely / Possible / Not Mapped
- Gap analysis with source evidence links
- Prioritised recommendations (P0–P3) with business impact
- HTML and JSON report exports

### Repository Understanding
- Analyze a single repository or local folder
- Architecture layer detection (Controller, Service, Repository, Model, Security, etc.)
- External integration detection
- Security-sensitive area identification
- High-risk change area detection
- Mermaid architecture and flow diagrams
- Suggested Gherkin test scenarios
- Edge case enumeration
- Stakeholder and technical explanations
- HTML and JSON report exports

### Analysis Modes
- **Non-AI deterministic mode** — fully heuristic, works without any AI configuration
- **AI-assisted mode** — optional enrichment via configurable AI provider (OpenAI compatible)
- AI-generated content is clearly labeled with `[AI-assisted]`

---

## Tech Stack

- Java 17 (compatible; designed for Java 21 semantics)
- Spring Boot 3.2
- Spring MVC + Thymeleaf
- JGit (remote repository cloning)
- Apache Commons Text (fuzzy matching)
- Jackson (JSON)
- JUnit 5 + AssertJ (tests)
- Bootstrap-style custom CSS

---

## Quick Start

### Prerequisites
- Java 17+ JDK
- Maven 3.8+

### Run Locally

```bash
git clone https://github.com/anujchouksey/docgrnQA.git
cd docgrnQA
mvn spring-boot:run
```

Open your browser at: **http://localhost:8080/**

### Build

```bash
mvn clean package
java -jar target/repoinsight-1.0.0-SNAPSHOT.jar
```

### Run Tests

```bash
mvn test
```

---

## Configuration

Edit `src/main/resources/application.yml` to configure the platform.

### Key configuration options:

```yaml
repoinsight:
  workspace:
    base-dir: /tmp/repoinsight-workspace   # where cloned repos are stored
    cleanup-on-exit: true

  analysis:
    max-file-size-kb: 512
    max-files-per-repo: 5000

  coverage:
    scoring:
      exact-match-weight: 1.0
      token-overlap-weight: 0.8
      min-covered-threshold: 0.7
      min-partial-threshold: 0.3

  ai:
    enabled: false           # set to true to enable AI enrichment
    provider: none           # use: openai
    # openai:
    #   api-key: your-key-here
    #   model: gpt-4o
```

### Enable AI Enrichment

```yaml
repoinsight:
  ai:
    enabled: true
    provider: openai
    openai:
      api-key: sk-...
      model: gpt-4o
      base-url: https://api.openai.com/v1
      max-tokens: 2048
      temperature: 0.3
```

The application works fully without AI configured. AI enrichment enhances executive summaries, domain context, and Gherkin scenarios when enabled.

---

## Project Structure

```
src/main/java/com/repoinsight/
├── RepoInsightApplication.java
├── config/
│   ├── AppProperties.java          # typed config binding
│   └── WebConfig.java              # static resource config
├── controller/
│   ├── HomeController.java
│   ├── CoverageController.java
│   ├── UnderstandingController.java
│   ├── ReportController.java       # HTML/JSON export + status API
│   └── SettingsController.java
├── service/
│   ├── AnalysisOrchestrationService.java
│   ├── ai/
│   │   ├── AiProvider.java         # provider interface
│   │   ├── NoOpAiProvider.java     # no-op fallback
│   │   └── AiEnrichmentService.java
│   ├── analysis/
│   │   ├── DevRepoAnalyzerService.java
│   │   ├── QaRepoAnalyzerService.java
│   │   └── RepositoryUnderstandingService.java
│   ├── coverage/
│   │   ├── CoverageMappingService.java
│   │   └── CoverageScoringService.java
│   ├── recommendation/
│   │   └── RecommendationEngine.java
│   ├── reporting/
│   │   └── ReportingService.java
│   └── source/
│       ├── SourceIngestionService.java
│       ├── LocalSourceService.java
│       └── RemoteSourceService.java
├── model/                          # domain models
├── dto/                            # request/response DTOs
├── parser/                         # .feature, Java code parsers
└── util/                           # similarity, file scanner, path utils
src/main/resources/
├── application.yml
├── templates/
│   ├── pages/                      # Thymeleaf UI pages
│   └── reports/                    # standalone export templates
└── static/
    ├── css/                        # main.css, report.css
    └── js/                         # main.js, charts.js
```

---

## Usage Guide

### Coverage Intelligence Analysis

1. Navigate to **Coverage Analysis** from the home page
2. Enter the path or URL for your **development repository** (local or remote Git)
3. Enter the path or URL for your **QA/BDD repository**
4. Click **Start Coverage Analysis**
5. Watch progress in real-time; report opens automatically when complete
6. Export as HTML or JSON using the buttons in the report header

### Repository Understanding Analysis

1. Navigate to **Repository Understanding** from the home page
2. Enter the path or URL for the repository you want to understand
3. Click **Analyze Repository**
4. The report will show architecture layers, Mermaid diagrams, Gherkin suggestions, and QA strategy

---

## AI vs Non-AI Mode

| Feature | Non-AI Mode | AI Mode |
|---------|-------------|---------|
| Coverage mapping | ✅ Heuristic | ✅ + Semantic |
| Architecture detection | ✅ Pattern-based | ✅ + AI narration |
| Executive summary | ✅ Template-based | ✅ AI-generated |
| Gherkin suggestions | ✅ Template-based | ✅ AI-enhanced |
| Domain understanding | ✅ Inferred | ✅ AI-described |
| Works without API key | ✅ Yes | ❌ Requires key |

---

## Acceptance Criteria — Status

- [x] Project builds and runs locally
- [x] Local and remote repo ingestion work with graceful validation
- [x] Coverage mapping and classification work
- [x] Repository understanding reports work
- [x] App runs without AI configured
- [x] AI output clearly labeled when enabled
- [x] HTML and JSON exports work
- [x] Key unit tests included (35 tests passing)
- [x] README and sample configuration included

---

## License

Internal engineering tool — all rights reserved.
