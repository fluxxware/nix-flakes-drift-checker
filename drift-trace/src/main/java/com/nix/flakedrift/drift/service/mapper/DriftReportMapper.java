package com.nix.flakedrift.drift.service.mapper;

import com.nix.flakedrift.drift.domain.model.DeploymentTarget;
import com.nix.flakedrift.drift.domain.model.DriftType;
import com.nix.flakedrift.drift.domain.model.FlakeDependencyGraph;
import com.nix.flakedrift.drift.domain.model.FlakeGraphNode;
import com.nix.flakedrift.drift.dto.DriftMemberDto;
import com.nix.flakedrift.drift.dto.DriftReportDto;
import com.nix.flakedrift.drift.dto.FlakeGraphNodeDto;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Single mapping pass: tree + live + drift values {@literal ->} {@link DriftReportDto}.
 * No classification logic — statuses and members are read from the drift map.
 */
public final class DriftReportMapper {

    public DriftReportDto assemble(FlakeDependencyGraph tree,
                                   Map<FlakeGraphNode, Boolean> live,
                                   Map<FlakeGraphNode, Set<DriftType>> drift,
                                   DeploymentTarget target) {
        DriftReportDto report = DriftReportDto.empty(describeTarget(target));
        report.root = toDto(tree.getRoot(), live, drift);
        count(tree, drift, report);
        return report;
    }

    private static void count(FlakeDependencyGraph tree,
                              Map<FlakeGraphNode, Set<DriftType>> drift,
                              DriftReportDto report) {
        for (FlakeGraphNode node : tree.allNodes()) {
            report.total++;
            if (isDrifted(drift.get(node))) {
                report.drifted++;
            } else {
                report.synced++;
            }
        }
    }

    private static boolean isDrifted(Set<DriftType> types) {
        return types != null && !types.isEmpty() && !types.contains(DriftType.REMOTE);
    }

    private FlakeGraphNodeDto toDto(FlakeGraphNode node,
                                    Map<FlakeGraphNode, Boolean> live,
                                    Map<FlakeGraphNode, Set<DriftType>> drift) {
        FlakeGraphNodeDto dto = new FlakeGraphNodeDto();
        dto.name = node.getName();
        dto.path = node.getPath() == null ? null : node.getPath().toString();
        dto.diskHash = node.getDiskHash();
        dto.lockHash = node.getLockHash();
        dto.livePresent = live.getOrDefault(node, false);
        dto.driftTypes = drift.getOrDefault(node, Set.of());

        for (FlakeGraphNode child : node.getChildren()) {
            dto.children.add(toDto(child, live, drift));
        }

        dto.unrefreshedMembers = collectUnrefreshedMembers(dto.children);
        return dto;
    }

    private static List<DriftMemberDto> collectUnrefreshedMembers(List<FlakeGraphNodeDto> children) {
        return children.stream()
                .filter(c -> c.driftTypes.contains(DriftType.CHAIN_STALE_CAUSE)
                        || c.driftTypes.contains(DriftType.CHAIN_STALE_TRANSITIVE))
                .map(c -> new DriftMemberDto(c.name, c.driftTypes))
                .toList();
    }

    private static String describeTarget(DeploymentTarget deploymentTarget) {
        return deploymentTarget.kind() == DeploymentTarget.TargetKind.LOCAL ? "local" : deploymentTarget.host();
    }
}