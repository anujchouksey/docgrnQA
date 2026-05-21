package com.repoinsight.service.analysis;

import com.repoinsight.config.AppProperties;
import com.repoinsight.model.*;
import com.repoinsight.util.FileScanner;
import com.repoinsight.util.PathUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Analyzes a repository to produce a {@link RepositoryInsight} report.
 */
@Service
public class RepositoryUnderstandingService {

    private static final Logger log = LoggerFactory.getLogger(RepositoryUnderstandingService.class);

    private static final Pattern PACKAGE_PATTERN  = Pattern.compile("^package\\s+([\\w.]+);", Pattern.MULTILINE);
    private static final Pattern IMPORT_PATTERN   = Pattern.compile("^import\\s+(?:static\\s+)?([\\w.]+)(?:\\*)?;", Pattern.MULTILINE);
    private static final Pattern CLASS_ANNOTATION = Pattern.compile("@(RestController|Controller|Service|Repository|Component|Entity|Configuration)");
    private static final Pattern SPRING_BOOT_APP  = Pattern.compile("@SpringBootApplication");
    private static final Pattern MERMAID_MODULE   = Pattern.compile("[A-Z][\\w]+");

    private final AppProperties properties;
    private final FileScanner   fileScanner;

    public RepositoryUnderstandingService(AppProperties properties, FileScanner fileScanner) {
        this.properties  = properties;
        this.fileScanner = fileScanner;
    }

    /**
     * Analyzes the repository and returns a rich {@link RepositoryInsight}.
     */
    public RepositoryInsight analyze(RepoSource source) {
        RepositoryInsight insight = new RepositoryInsight();
        insight.setRepoName(source.getResolvedName() != null ? source.getResolvedName() : "Unknown Repository");
        insight.setRepoPath(source.getPath());
        insight.setTotalFiles(source.getTotalFiles());

        if (!source.isValid()) {
            insight.setExecutiveSummary("Repository could not be analyzed: " + source.getValidationError());
            insight.setSystemPurpose("Unknown – source validation failed");
            return insight;
        }

        Path root = Paths.get(source.getPath());
        AppProperties.Analysis cfg = properties.getAnalysis();

        try {
            List<Path> files = fileScanner.scanFiles(
                    root, cfg.getIncludeExtensions(), cfg.getExcludeDirs(),
                    cfg.getMaxFileSizeKb(), cfg.getMaxFilesPerRepo());

            log.info("Understanding analysis: scanning {} files", files.size());

            // Categorize files
            Map<String, List<Path>> byExtension = files.stream()
                    .collect(Collectors.groupingBy(f -> PathUtil.extension(f.getFileName().toString())));

            // Detect dominant language
            String lang = detectDominantLanguage(byExtension);
            insight.setDominantLanguage(lang);

            // Extract modules
            List<RepositoryInsight.ArchitectureNode> modules = extractModules(files, root);
            insight.setModules(modules);

            // External integrations
            insight.setExternalIntegrations(detectExternalIntegrations(files, root));

            // Dependencies
            insight.setDependencies(detectDependencies(root));

            // Security-sensitive areas
            insight.setSecuritySensitiveAreas(detectSecurityAreas(files, root));

            // High-risk areas
            insight.setHighRiskChangeAreas(detectHighRiskAreas(files, root));

            // Missing documentation
            insight.setMissingDocumentation(detectMissingDocs(files, root));

            // Build summaries
            buildSummaries(insight, modules, lang, source);

            // Generate Mermaid diagrams
            insight.setMermaidArchitectureDiagram(generateArchitectureDiagram(modules));
            insight.setMermaidFlowDiagram(generateFlowDiagram(modules));

            // Gherkin suggestions
            insight.setSuggestedGherkinScenarios(generateGherkinSuggestions(modules));

            // Edge cases
            insight.setEdgeCases(generateEdgeCases(insight));

            // Assumptions and limitations
            insight.setAssumptions(buildAssumptions(lang));
            insight.setLimitations(buildLimitations());

            log.info("Understanding analysis complete for {}", source.getResolvedName());

        } catch (Exception e) {
            log.error("Error analyzing repository: {}", e.getMessage(), e);
            insight.setExecutiveSummary("Analysis encountered an error: " + e.getMessage());
        }

        return insight;
    }

    // ── Module extraction ─────────────────────────────────────────────────────

    private List<RepositoryInsight.ArchitectureNode> extractModules(List<Path> files, Path root) {
        Map<String, RepositoryInsight.ArchitectureNode> modules = new LinkedHashMap<>();

        for (Path file : files) {
            String rel = PathUtil.normalise(root.relativize(file).toString());
            String[] parts = rel.split("/");
            if (parts.length < 2) continue;

            String content = fileScanner.readSafely(file);
            String layer = inferLayer(rel, content);
            if (layer == null) continue;

            modules.computeIfAbsent(layer, k -> {
                RepositoryInsight.ArchitectureNode node = new RepositoryInsight.ArchitectureNode();
                node.setName(k);
                node.setRole(layerRole(k));
                node.setDescription(layerDescription(k));
                return node;
            });

            // Add evidence
            RepositoryInsight.ArchitectureNode node = modules.get(layer);
            EvidenceLink ev = new EvidenceLink(file.toString(), rel, 1, EvidenceLink.EvidenceType.FILE);
            if (node.getEvidenceLinks().size() < 5) {
                node.getEvidenceLinks().add(ev);
            }
        }

        // Set dependencies between layers
        List<String> layerOrder = List.of("Controller", "Service", "Repository", "Model", "Security", "Config");
        for (int i = 0; i < layerOrder.size() - 1; i++) {
            RepositoryInsight.ArchitectureNode node = modules.get(layerOrder.get(i));
            if (node != null && i + 1 < layerOrder.size()) {
                String dep = layerOrder.get(i + 1);
                if (modules.containsKey(dep)) {
                    node.getDependencies().add(dep);
                }
            }
        }

        return new ArrayList<>(modules.values());
    }

    private String inferLayer(String rel, String content) {
        if (rel.contains("/controller") || rel.contains("/controllers")) return "Controller";
        if (rel.contains("/service") || rel.contains("/services"))       return "Service";
        if (rel.contains("/repository") || rel.contains("/repositories") || rel.contains("/dao")) return "Repository";
        if (rel.contains("/model") || rel.contains("/entity") || rel.contains("/domain")) return "Model";
        if (rel.contains("/security") || rel.contains("/auth"))          return "Security";
        if (rel.contains("/config") || rel.contains("/configuration"))   return "Config";
        if (rel.contains("/util") || rel.contains("/helper"))            return "Utility";
        if (rel.contains("/dto") || rel.contains("/request") || rel.contains("/response")) return "DTO";
        if (rel.contains("/event"))  return "Events";
        if (rel.contains("/job") || rel.contains("/scheduler")) return "Scheduler";
        if (rel.endsWith(".feature")) return "BDD Tests";
        if (content != null && content.contains("@RestController")) return "Controller";
        if (content != null && content.contains("@Service"))        return "Service";
        if (content != null && content.contains("@Repository"))     return "Repository";
        if (content != null && content.contains("@Entity"))         return "Model";
        return null;
    }

    private String layerRole(String layer) {
        return switch (layer) {
            case "Controller"  -> "HTTP Request Handler";
            case "Service"     -> "Business Logic";
            case "Repository"  -> "Data Access";
            case "Model"       -> "Domain Entities";
            case "Security"    -> "Auth & Authorization";
            case "Config"      -> "Application Configuration";
            case "Utility"     -> "Helper Utilities";
            case "DTO"         -> "Data Transfer Objects";
            case "Events"      -> "Event / Message Handling";
            case "Scheduler"   -> "Scheduled Jobs";
            case "BDD Tests"   -> "Automated BDD Tests";
            default            -> "Application Layer";
        };
    }

    private String layerDescription(String layer) {
        return switch (layer) {
            case "Controller"  -> "Handles incoming HTTP requests, routes them to services, and returns responses.";
            case "Service"     -> "Contains the core business logic. Coordinates between controllers and repositories.";
            case "Repository"  -> "Manages data persistence – reads and writes to the database or data store.";
            case "Model"       -> "Defines the core domain entities and their attributes.";
            case "Security"    -> "Implements authentication, authorization, and security filters.";
            case "Config"      -> "Provides application-wide configuration beans and settings.";
            case "Utility"     -> "Reusable helper classes and common utilities.";
            case "DTO"         -> "Plain data objects used for transferring data between layers.";
            case "Events"      -> "Handles domain events and message processing.";
            case "Scheduler"   -> "Background and scheduled tasks.";
            case "BDD Tests"   -> "Behaviour-Driven Development test scenarios.";
            default            -> "General application component.";
        };
    }

    // ── Dependency / integration detection ────────────────────────────────────

    private List<String> detectExternalIntegrations(List<Path> files, Path root) {
        Set<String> integrations = new LinkedHashSet<>();
        Pattern dbP   = Pattern.compile("(?i)(jdbc|datasource|jpa|hibernate|mongo|redis|elasticsearch|cassandra|dynamo)");
        Pattern mqP   = Pattern.compile("(?i)(kafka|rabbitmq|activemq|sqs|pubsub|nats)");
        Pattern httpP = Pattern.compile("(?i)(resttemplate|webclient|feign|httpclient|okhttp|retrofit)");
        Pattern awsP  = Pattern.compile("(?i)(s3|sns|sqs|dynamodb|lambda|cognito|secretsmanager)");

        for (Path file : files) {
            String content = fileScanner.readSafely(file);
            if (dbP.matcher(content).find())   integrations.add("Database (SQL/NoSQL)");
            if (mqP.matcher(content).find())   integrations.add("Message Queue");
            if (httpP.matcher(content).find()) integrations.add("HTTP Client (external APIs)");
            if (awsP.matcher(content).find())  integrations.add("AWS Cloud Services");
            if (content.toLowerCase().contains("oauth") || content.toLowerCase().contains("jwt"))
                integrations.add("OAuth / JWT Authentication");
            if (content.toLowerCase().contains("smtp") || content.toLowerCase().contains("email"))
                integrations.add("Email Service");
            if (content.toLowerCase().contains("twilio") || content.toLowerCase().contains("sms"))
                integrations.add("SMS / Notification Service");
        }
        return new ArrayList<>(integrations);
    }

    private List<String> detectDependencies(Path root) {
        List<String> deps = new ArrayList<>();
        // Maven
        Path pom = root.resolve("pom.xml");
        if (java.nio.file.Files.exists(pom)) {
            String content = fileScanner.readSafely(pom);
            Pattern depP = Pattern.compile("<artifactId>([^<]+)</artifactId>");
            Matcher m = depP.matcher(content);
            Set<String> seen = new LinkedHashSet<>();
            while (m.find() && seen.size() < 30) seen.add(m.group(1));
            deps.addAll(seen);
        }
        // Gradle
        Path gradle = root.resolve("build.gradle");
        if (java.nio.file.Files.exists(gradle)) {
            String content = fileScanner.readSafely(gradle);
            Pattern depP = Pattern.compile("['\"]([\\w.]+:[\\w.-]+:[\\w.]+)['\"]");
            Matcher m = depP.matcher(content);
            while (m.find() && deps.size() < 30) deps.add(m.group(1));
        }
        // package.json
        Path packageJson = root.resolve("package.json");
        if (java.nio.file.Files.exists(packageJson)) {
            String content = fileScanner.readSafely(packageJson);
            Pattern depP = Pattern.compile("\"([^\"]+)\"\\s*:\\s*\"[^\"]+\"");
            Matcher m = depP.matcher(content);
            while (m.find() && deps.size() < 30) deps.add(m.group(1));
        }
        return deps;
    }

    private List<String> detectSecurityAreas(List<Path> files, Path root) {
        Set<String> areas = new LinkedHashSet<>();
        for (Path file : files) {
            String rel     = PathUtil.normalise(root.relativize(file).toString());
            String content = fileScanner.readSafely(file);
            if (content.contains("@PreAuthorize") || content.contains("@Secured"))
                areas.add(rel + " (method-level security)");
            if (content.toLowerCase().contains("password") || content.toLowerCase().contains("bcrypt"))
                areas.add(rel + " (password handling)");
            if (content.toLowerCase().contains("jwt") || content.toLowerCase().contains("token"))
                areas.add(rel + " (token handling)");
            if (areas.size() >= 10) break;
        }
        return new ArrayList<>(areas);
    }

    private List<String> detectHighRiskAreas(List<Path> files, Path root) {
        Set<String> areas = new LinkedHashSet<>();
        for (Path file : files) {
            String rel = PathUtil.normalise(root.relativize(file).toString());
            String content = fileScanner.readSafely(file);
            if (content.contains("@Transactional"))   areas.add(rel + " (transactional)");
            if (content.toLowerCase().contains("delete") && content.toLowerCase().contains("cascade"))
                areas.add(rel + " (cascade delete)");
            if (content.toLowerCase().contains("schedule"))
                areas.add(rel + " (scheduled task)");
            if (content.toLowerCase().contains("payment") || content.toLowerCase().contains("billing"))
                areas.add(rel + " (payment / billing)");
            if (areas.size() >= 10) break;
        }
        return new ArrayList<>(areas);
    }

    private List<String> detectMissingDocs(List<Path> files, Path root) {
        List<String> missing = new ArrayList<>();
        boolean hasReadme = java.nio.file.Files.exists(root.resolve("README.md"))
                || java.nio.file.Files.exists(root.resolve("README.txt"))
                || java.nio.file.Files.exists(root.resolve("README.rst"));
        if (!hasReadme) missing.add("No README file found");

        long controllers = files.stream()
                .filter(f -> PathUtil.normalise(root.relativize(f).toString()).contains("controller"))
                .count();
        long withJavadoc = files.stream()
                .filter(f -> PathUtil.normalise(root.relativize(f).toString()).contains("controller"))
                .filter(f -> fileScanner.readSafely(f).contains("/**"))
                .count();
        if (controllers > 0 && withJavadoc < controllers / 2)
            missing.add("Many controller methods lack Javadoc comments");

        boolean hasOpenApi = files.stream().anyMatch(f -> {
            String name = f.getFileName().toString().toLowerCase();
            return name.contains("openapi") || name.contains("swagger");
        });
        if (!hasOpenApi && controllers > 0) missing.add("No OpenAPI / Swagger documentation found");

        return missing;
    }

    // ── Language detection ────────────────────────────────────────────────────

    private String detectDominantLanguage(Map<String, List<Path>> byExt) {
        Map<String, Integer> counts = new HashMap<>();
        byExt.forEach((ext, paths) -> counts.put(ext, paths.size()));
        return counts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(e -> switch (e.getKey()) {
                    case ".java" -> "Java";
                    case ".ts"   -> "TypeScript";
                    case ".js"   -> "JavaScript";
                    case ".py"   -> "Python";
                    case ".rb"   -> "Ruby";
                    case ".go"   -> "Go";
                    case ".cs"   -> "C#";
                    case ".kt"   -> "Kotlin";
                    default      -> e.getKey().replace(".", "");
                })
                .orElse("Unknown");
    }

    // ── Summary building ──────────────────────────────────────────────────────

    private void buildSummaries(RepositoryInsight insight,
                                 List<RepositoryInsight.ArchitectureNode> modules,
                                 String lang, RepoSource source) {
        String name = insight.getRepoName();
        int modCount = modules.size();
        int fileCount = insight.getTotalFiles();

        insight.setSystemPurpose("Heuristic analysis of '" + name + "' – a " + lang
                + " application with " + fileCount + " files and " + modCount + " detected architecture layers.");

        insight.setDomainContext(
                "Based on file structure and naming conventions, this appears to be a backend application. "
                + "Domain entities, REST APIs, and business logic layers were detected. "
                + "(This is a heuristic inference – AI enrichment can provide deeper domain understanding.)");

        insight.setArchitectureOverview(
                "The repository follows a layered architecture with the following detected layers: "
                + modules.stream().map(RepositoryInsight.ArchitectureNode::getName)
                         .collect(Collectors.joining(", ")) + ". "
                + "This is consistent with a standard " + lang + " application structure.");

        insight.setRequestLifecycle(
                "Incoming HTTP requests are handled by Controller layer → delegated to Service layer for business logic "
                + "→ Repository layer manages persistence → responses are returned to callers. "
                + "Cross-cutting concerns (security, validation, logging) are applied at appropriate layers.");

        insight.setCoreLogicExplanation(
                "The core business logic resides in the Service layer. "
                + "Data entities are managed by the Repository layer using ORM or data access patterns. "
                + "The Controller layer provides the HTTP API surface.");

        insight.setRecommendedQaStrategy(
                "1. Prioritize integration tests for Service layer covering both happy and error paths. "
                + "2. Add BDD feature files for each REST endpoint identified in Controllers. "
                + "3. Include security tests for authentication/authorization boundaries. "
                + "4. Test boundary conditions for all data validation rules. "
                + "5. Add contract tests for external integrations.");

        insight.setExecutiveSummary(
                "RepoInsight has analyzed '" + name + "' and identified " + modCount
                + " architecture layers across " + fileCount + " files. "
                + "The system appears to be a " + lang + " backend application. "
                + "Key areas requiring QA attention include: security, transactional logic, and external integrations. "
                + "Detailed findings are in the sections below.");

        insight.setStakeholderExplanation(
                "This repository contains the source code for '" + name + "'. "
                + "It is built using " + lang + " and follows standard software engineering patterns. "
                + "The analysis identified key functional areas, dependencies, and recommended test priorities.");

        insight.setTechnicalExplanation(
                "Static analysis of " + fileCount + " files identified " + modCount + " architectural layers. "
                + "The analysis used file-path heuristics, annotation detection, and pattern matching. "
                + "No AI enrichment was applied – enable AI mode for deeper semantic analysis.");
    }

    // ── Diagram generation ────────────────────────────────────────────────────

    private String generateArchitectureDiagram(List<RepositoryInsight.ArchitectureNode> modules) {
        if (modules.isEmpty()) return "graph LR\n    A[No modules detected]";
        StringBuilder sb = new StringBuilder("graph TD\n");
        for (RepositoryInsight.ArchitectureNode m : modules) {
            sb.append("    ").append(sanitize(m.getName())).append("[\"")
              .append(m.getName()).append("\\n").append(m.getRole()).append("\"]\n");
        }
        sb.append("\n");
        for (RepositoryInsight.ArchitectureNode m : modules) {
            for (String dep : m.getDependencies()) {
                sb.append("    ").append(sanitize(m.getName()))
                  .append(" --> ").append(sanitize(dep)).append("\n");
            }
        }
        return sb.toString();
    }

    private String generateFlowDiagram(List<RepositoryInsight.ArchitectureNode> modules) {
        StringBuilder sb = new StringBuilder("sequenceDiagram\n");
        sb.append("    participant Client\n");
        for (RepositoryInsight.ArchitectureNode m : modules) {
            sb.append("    participant ").append(sanitize(m.getName())).append("\n");
        }
        sb.append("\n    Client->>Controller: HTTP Request\n");
        sb.append("    Controller->>Service: Invoke business logic\n");
        sb.append("    Service->>Repository: Query / persist data\n");
        sb.append("    Repository-->>Service: Data result\n");
        sb.append("    Service-->>Controller: Processed result\n");
        sb.append("    Controller-->>Client: HTTP Response\n");
        return sb.toString();
    }

    private String sanitize(String name) {
        return name.replaceAll("[^a-zA-Z0-9]", "_");
    }

    // ── Gherkin suggestions ────────────────────────────────────────────────────

    private List<String> generateGherkinSuggestions(List<RepositoryInsight.ArchitectureNode> modules) {
        List<String> scenarios = new ArrayList<>();
        for (RepositoryInsight.ArchitectureNode module : modules) {
            if (module.getName().equals("Controller") || module.getName().equals("Service")) {
                scenarios.add("""
                        Feature: %s Functionality
                          Scenario: Successful operation
                            Given the system is operational
                            When a valid request is made to %s
                            Then the response should be successful with status 200
                          
                          Scenario: Invalid input handling
                            Given the system is operational
                            When an invalid request is made to %s
                            Then the response should return an error with status 400
                        """.formatted(module.getName(), module.getName(), module.getName()));
            }
        }
        if (scenarios.isEmpty()) {
            scenarios.add("""
                    Feature: Core Application Functionality
                      Scenario: Application starts successfully
                        Given the application is configured correctly
                        When the application starts
                        Then it should be available and healthy
                    """);
        }
        return scenarios;
    }

    private List<String> generateEdgeCases(RepositoryInsight insight) {
        List<String> edges = new ArrayList<>();
        edges.add("Empty or null inputs to all API endpoints");
        edges.add("Concurrent requests to transactional operations");
        edges.add("Database connection failures and retry behaviour");
        edges.add("Large payload handling (file uploads, bulk data)");
        edges.add("Timezone and locale edge cases in date/time handling");
        edges.add("Authentication token expiry during active sessions");
        edges.add("Cascade delete side effects on related entities");
        edges.add("Third-party service timeouts and circuit-breaking");
        if (!insight.getExternalIntegrations().isEmpty()) {
            edges.add("External integration failures (graceful degradation)");
        }
        if (!insight.getSecuritySensitiveAreas().isEmpty()) {
            edges.add("SQL injection / XSS attempts on user-facing inputs");
            edges.add("Privilege escalation attempts");
        }
        return edges;
    }

    private List<String> buildAssumptions(String lang) {
        return List.of(
                "Analysis is based on static heuristics applied to file names and content patterns.",
                "Module classification is derived from directory naming conventions for " + lang + " projects.",
                "Feature extraction uses annotation and route pattern detection, not a full AST.",
                "Confidence scores reflect heuristic certainty, not semantic correctness.",
                "External integrations are inferred from import statements and configuration files."
        );
    }

    private List<String> buildLimitations() {
        return List.of(
                "Non-AI mode cannot understand business semantics or domain terminology.",
                "Dynamic routing patterns (reflection, runtime config) may not be detected.",
                "Minified, obfuscated, or generated code is excluded from analysis.",
                "Very large repositories (>5000 files) are analysed with a file cap.",
                "AI enrichment can significantly improve domain understanding and recommendations."
        );
    }
}
