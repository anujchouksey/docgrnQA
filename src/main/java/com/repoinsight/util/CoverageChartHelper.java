package com.repoinsight.util;

import com.repoinsight.model.AnalysisResult;
import org.springframework.stereotype.Component;

/**
 * Thymeleaf helper for generating inline SVG coverage charts.
 */
@Component("coverageChartHelper")
public class CoverageChartHelper {

    /**
     * Generates a simple inline SVG donut chart for coverage distribution.
     */
    public String coverageDonut(AnalysisResult.CoverageSummaryStats stats) {
        if (stats == null) return "<p>No data</p>";

        int total = stats.getTotalFeatures();
        if (total == 0) return "<p>No features analyzed</p>";

        int covered  = stats.getCoveredCount();
        int partial  = stats.getPartiallyCoveredCount();
        int notCov   = stats.getNotCoveredCount();
        int unclear  = stats.getUnclearCount();

        double pct = Math.round(stats.getOverallCoveragePercent());

        // Build SVG donut chart
        double cx = 100, cy = 100, r = 80, inner = 50;
        double[] values = {covered, partial, notCov, unclear};
        String[] colors = {"#38a169", "#d69e2e", "#e53e3e", "#a0aec0"};

        StringBuilder svg = new StringBuilder();
        svg.append("<svg width='200' height='220' viewBox='0 0 200 220' xmlns='http://www.w3.org/2000/svg'>");

        double startAngle = -Math.PI / 2;
        for (int i = 0; i < values.length; i++) {
            if (values[i] <= 0) continue;
            double slice = (values[i] / total) * 2 * Math.PI;
            double endAngle = startAngle + slice;

            double x1 = cx + r * Math.cos(startAngle);
            double y1 = cy + r * Math.sin(startAngle);
            double x2 = cx + r * Math.cos(endAngle);
            double y2 = cy + r * Math.sin(endAngle);

            int largeArc = slice > Math.PI ? 1 : 0;
            svg.append(String.format(
                    "<path d='M%.1f,%.1f A%.1f,%.1f 0 %d,1 %.1f,%.1f L%.1f,%.1f Z' fill='%s'/>",
                    x1, y1, r, r, largeArc, x2, y2, cx, cy, colors[i]));

            startAngle = endAngle;
        }

        // Inner circle (donut hole)
        svg.append(String.format("<circle cx='%.1f' cy='%.1f' r='%.1f' fill='white'/>", cx, cy, inner));

        // Center label
        svg.append(String.format(
                "<text x='%.1f' y='%.1f' text-anchor='middle' dominant-baseline='middle' " +
                "font-size='20' font-weight='bold' fill='#2d3748'>%.0f%%</text>",
                cx, cy, pct));

        // Legend
        String[] labels = {"Covered", "Partial", "Not Covered", "Unclear"};
        int[] rawValues = {covered, partial, notCov, unclear};
        for (int i = 0; i < labels.length; i++) {
            int y = 200 + i * 0;
            // Horizontal legend
            double lx = 10 + i * 48.0;
            svg.append(String.format(
                    "<rect x='%.1f' y='190' width='12' height='12' fill='%s' rx='2'/>", lx, colors[i]));
            svg.append(String.format(
                    "<text x='%.1f' y='200' font-size='9' fill='#718096'>%d</text>", lx + 14, rawValues[i]));
        }

        svg.append("</svg>");
        return svg.toString();
    }
}
