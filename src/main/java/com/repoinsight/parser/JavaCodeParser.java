package com.repoinsight.parser;

import com.repoinsight.model.EvidenceLink;
import com.repoinsight.model.Feature;
import com.repoinsight.util.PathUtil;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extracts candidate features / capabilities from Java source files.
 * Uses heuristic pattern matching – not a full AST parser.
 */
@Component
public class JavaCodeParser {

    // Controller / handler patterns
    private static final Pattern REST_MAPPING = Pattern.compile(
            "@(Get|Post|Put|Delete|Patch|Request)Mapping\\s*\\(\\s*(?:value\\s*=\\s*)?\"([^\"]+)\"",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern CLASS_NAME = Pattern.compile(
            "public\\s+(?:abstract\\s+)?class\\s+(\\w+)");
    private static final Pattern INTERFACE_NAME = Pattern.compile(
            "public\\s+interface\\s+(\\w+)");
    private static final Pattern PUBLIC_METHOD = Pattern.compile(
            "public\\s+(?:\\S+\\s+)?(\\w+)\\s*\\([^)]*\\)\\s*(?:throws\\s+\\S+\\s*)?\\{",
            Pattern.MULTILINE);
    private static final Pattern SERVICE_ANNOTATION = Pattern.compile(
            "@(Service|Component|Repository|Controller|RestController|Bean)");
    private static final Pattern SWAGGER_OP = Pattern.compile(
            "@Operation\\s*\\(\\s*summary\\s*=\\s*\"([^\"]+)\"");
    private static final Pattern SWAGGER_TAG = Pattern.compile(
            "@Tag\\s*\\(\\s*name\\s*=\\s*\"([^\"]+)\"");

    /**
     * Parses a Java source file and returns candidate features it exposes.
     */
    public List<Feature> parse(Path file, Path repoRoot, String content) {
        List<Feature> features = new ArrayList<>();
        String relativePath = PathUtil.normalise(repoRoot.relativize(file).toString());
        String fileName = file.getFileName().toString().replace(".java", "");
        String module = inferModule(relativePath, fileName);

        // Skip non-functional files
        if (isTestFile(relativePath, fileName)) return features;

        // REST controller – extract endpoints
        Matcher restMatcher = REST_MAPPING.matcher(content);
        boolean isController = content.contains("@RestController") || content.contains("@Controller");

        if (isController) {
            while (restMatcher.find()) {
                String method = restMatcher.group(1).toUpperCase();
                String path = restMatcher.group(2);
                Feature f = new Feature();
                f.setId(PathUtil.newId());
                f.setName(method + " " + path);
                f.setDescription("REST endpoint: " + method + " " + path + " in " + fileName);
                f.setModule(module);
                f.setInterfaceType("API");
                f.setConfidence(0.95);
                f.setSeverity(Feature.Severity.HIGH);
                f.setRisk(Feature.Risk.MEDIUM);
                int lineNum = getLineNumber(content, restMatcher.start());
                EvidenceLink ev = new EvidenceLink(file.toString(), relativePath, lineNum, EvidenceLink.EvidenceType.ENDPOINT);
                f.setEvidenceLinks(List.of(ev));
                features.add(f);
            }

            // OpenAPI summary annotations
            Matcher opMatcher = SWAGGER_OP.matcher(content);
            while (opMatcher.find()) {
                Feature f = new Feature();
                f.setId(PathUtil.newId());
                f.setName(opMatcher.group(1).trim());
                f.setDescription("API operation: " + opMatcher.group(1).trim() + " in " + fileName);
                f.setModule(module);
                f.setInterfaceType("API");
                f.setConfidence(0.9);
                f.setSeverity(Feature.Severity.HIGH);
                f.setRisk(Feature.Risk.MEDIUM);
                int lineNum = getLineNumber(content, opMatcher.start());
                EvidenceLink ev = new EvidenceLink(file.toString(), relativePath, lineNum, EvidenceLink.EvidenceType.METHOD);
                f.setEvidenceLinks(List.of(ev));
                features.add(f);
            }
        }

        // Service class – extract public business methods as features
        boolean isService = SERVICE_ANNOTATION.matcher(content).find() && !isController;
        if (isService && features.isEmpty()) {
            Matcher methodMatcher = PUBLIC_METHOD.matcher(content);
            int featureCount = 0;
            while (methodMatcher.find() && featureCount < 10) {
                String methodName = methodMatcher.group(1);
                if (isBoilerplateMethod(methodName)) continue;
                Feature f = new Feature();
                f.setId(PathUtil.newId());
                f.setName(splitCamelCase(methodName) + " (" + fileName + ")");
                f.setDescription("Service method: " + fileName + "#" + methodName);
                f.setModule(module);
                f.setInterfaceType("SERVICE");
                f.setConfidence(0.7);
                f.setSeverity(Feature.Severity.MEDIUM);
                f.setRisk(Feature.Risk.MEDIUM);
                int lineNum = getLineNumber(content, methodMatcher.start());
                EvidenceLink ev = new EvidenceLink(file.toString(), relativePath, lineNum, EvidenceLink.EvidenceType.METHOD);
                f.setEvidenceLinks(List.of(ev));
                features.add(f);
                featureCount++;
            }
        }

        return features;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String inferModule(String relativePath, String fileName) {
        if (relativePath.contains("controller")) return "Controller";
        if (relativePath.contains("service"))    return "Service";
        if (relativePath.contains("repository")) return "Repository";
        if (relativePath.contains("model") || relativePath.contains("entity")) return "Model";
        if (relativePath.contains("config"))     return "Config";
        if (relativePath.contains("security") || relativePath.contains("auth")) return "Security";
        if (relativePath.contains("payment") || relativePath.contains("billing")) return "Payment";
        if (relativePath.contains("notification") || relativePath.contains("email")) return "Notification";
        if (relativePath.contains("admin"))      return "Admin";
        if (relativePath.contains("report"))     return "Reporting";
        String[] parts = relativePath.split("/");
        return parts.length >= 3 ? parts[parts.length - 3] : "Core";
    }

    private boolean isTestFile(String relativePath, String fileName) {
        return relativePath.contains("/test/") || relativePath.contains("\\test\\")
                || fileName.endsWith("Test") || fileName.endsWith("Spec")
                || fileName.endsWith("Tests") || relativePath.contains("src/test");
    }

    private boolean isBoilerplateMethod(String name) {
        return name.startsWith("get") || name.startsWith("set") || name.startsWith("is")
                || name.equals("toString") || name.equals("hashCode") || name.equals("equals")
                || name.equals("main") || name.startsWith("lambda");
    }

    private String splitCamelCase(String name) {
        return name.replaceAll("([a-z])([A-Z])", "$1 $2")
                   .replaceAll("([A-Z]+)([A-Z][a-z])", "$1 $2");
    }

    private int getLineNumber(String content, int charIndex) {
        if (charIndex < 0 || charIndex >= content.length()) return 1;
        int line = 1;
        for (int i = 0; i < charIndex; i++) {
            if (content.charAt(i) == '\n') line++;
        }
        return line;
    }
}
