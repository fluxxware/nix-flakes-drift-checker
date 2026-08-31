package com.nix.flakedrift.app.report;

import com.nix.flakedrift.drift.domain.model.DriftType;
import com.nix.flakedrift.drift.dto.DriftMemberDto;
import com.nix.flakedrift.drift.dto.DriftReportDto;
import com.nix.flakedrift.drift.dto.FlakeGraphNodeDto;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** ANSI-colored tree renderer for a drift report.
 * all things in app module is straight vibe-coded (and of course a little curated) shi
 * since I got no patience for printing beautiful output into terminal
 * */
public final class ReportRenderer {
    private static final String GREEN = "\u001B[32m";
    private static final String RED = "\u001B[31m";
    private static final String YELLOW = "\u001B[33m";
    private static final String CYAN = "\u001B[36m";
    private static final String RESET = "\u001B[0m";
    private static final String BOLD = "\u001B[1m";
    private static final String DIM = "\u001B[2m";

    private ReportRenderer() {
    }

    public static void render(DriftReportDto report) {
        System.out.println();
        System.out.println(BOLD + "⟐ " + report.root.name + " (" + report.target + ")" + RESET);
        renderRootStatus(report.root);
        List<FlakeGraphNodeDto> children = report.root.children;
        for (int i = 0; i < children.size(); i++) {
            renderNode(children.get(i), "", i == children.size() - 1);
        }
        System.out.println();
        System.out.println("⚠️ Summary: " + CYAN + report.synced + RESET + " in sync, "
                + (report.drifted == 0 ? GREEN : RED) + report.drifted + RESET + " drifted, "
                + report.total + " total.");
    }

    private static void renderRootStatus(FlakeGraphNodeDto node) {
        Set<DriftType> types = node.driftTypes;
        if (types.isEmpty() || types.contains(DriftType.REMOTE)) {
            return;
        }
        for (DriftType t : types) {
            System.out.println("    " + icon(t) + " " + BOLD + node.name + RESET + " (" + label(t) + ")");
        }
        renderDetail(node, "    ");
    }

    private static void renderNode(FlakeGraphNodeDto node, String prefix, boolean last) {
        String connector = last ? "└── " : "├── ";
        String childPrefix = prefix + (last ? "    " : "│   ");

        Set<DriftType> types = node.driftTypes;
        if (types.isEmpty() || types.contains(DriftType.REMOTE)) {
            DriftType single = types.contains(DriftType.REMOTE) ? DriftType.REMOTE : DriftType.SYNC;
            System.out.println(prefix + connector + icon(single) + " " + BOLD + node.name + RESET + " (" + label(single) + ")");
        } else {
            int i = 0;
            for (DriftType t : types) {
                String p = (i == 0) ? (prefix + connector) : (prefix + "    ");
                System.out.println(p + icon(t) + " " + BOLD + node.name + RESET + " (" + label(t) + ")");
                i++;
            }
        }

        if (!types.isEmpty() && !types.contains(DriftType.REMOTE)) {
            renderDetail(node, childPrefix);
        }

        List<FlakeGraphNodeDto> children = node.children;
        for (int i = 0; i < children.size(); i++) {
            renderNode(children.get(i), childPrefix, i == children.size() - 1);
        }
    }

    private static void renderDetail(FlakeGraphNodeDto node, String childPrefix) {
        if (node.path != null) {
            System.out.println(childPrefix + "    " + DIM + "path: " + node.path + RESET);
        }
        if (node.lockHash != null) {
            System.out.println(childPrefix + "    " + DIM + "lock: " + node.lockHash + RESET);
        } else if (node.driftTypes.contains(DriftType.NARHASH_ABSENT)) {
            System.out.println(childPrefix + "    " + DIM + "lock: (absent — path-only entry)" + RESET);
        }
        if (node.diskHash != null) {
            System.out.println(childPrefix + "    " + DIM + "disk: " + node.diskHash + RESET);
        }
        System.out.println(childPrefix + "    " + DIM + "live present: " + node.livePresent + RESET);
        if (!node.unrefreshedMembers.isEmpty()) {
            String members = node.unrefreshedMembers.stream()
                    .map(ReportRenderer::formatMember)
                    .collect(Collectors.joining(", "));
            System.out.println(childPrefix + "    " + DIM + "unrefreshed: " + members + RESET);
        }
    }

    private static String formatMember(DriftMemberDto member) {
        String role = member.types().contains(DriftType.CHAIN_STALE_CAUSE) ? "CAUSE"
                : member.types().contains(DriftType.CHAIN_STALE_TRANSITIVE) ? "TRANSITIVE"
                : "";
        return member.name() + (role.isEmpty() ? "" : " (" + role + ")");
    }

    private static String icon(DriftType type) {
        return switch (type) {
            case SYNC -> GREEN + "[OK]" + RESET;
            case REMOTE -> CYAN + "[REM]" + RESET;
            case LOCAL_DRIFT -> YELLOW + "[DRIFT]" + RESET;
            case NARHASH_ABSENT -> RED + "[NO-NAR]" + RESET;
            case UNDEPLOYED -> RED + "[DOWN]" + RESET;
            case CHAIN_STALE_CAUSE -> YELLOW + "[CAUSE]" + RESET;
            case CHAIN_STALE_TRANSITIVE -> YELLOW + "[STALE-T]" + RESET;
        };
    }

    private static String label(DriftType type) {
        return switch (type) {
            case SYNC -> GREEN + "SYNC" + RESET;
            case REMOTE -> YELLOW + "REMOTE" + RESET;
            case LOCAL_DRIFT -> RED + "LOCAL DRIFT (un-locked)" + RESET;
            case NARHASH_ABSENT -> YELLOW + "NARHASH ABSENT (lock has no hash)" + RESET;
            case UNDEPLOYED -> RED + "UNDEPLOYED" + RESET;
            case CHAIN_STALE_CAUSE -> RED + "CHAIN STALE CAUSE (I'm the culprit)" + RESET;
            case CHAIN_STALE_TRANSITIVE -> RED + "CHAIN STALE TRANSITIVE (needs re-lock)" + RESET;
        };
    }
}
