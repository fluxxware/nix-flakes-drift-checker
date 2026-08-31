package com.nix.flakedrift.drift.service.impl;

import com.nix.flakedrift.drift.domain.model.DriftType;
import com.nix.flakedrift.drift.domain.model.FlakeDependencyGraph;
import com.nix.flakedrift.drift.domain.model.FlakeGraphNode;
import com.nix.flakedrift.drift.dto.UpdateCandidateDto;
import com.nix.flakedrift.drift.dto.UpdateResultDto;
import com.nix.flakedrift.drift.infra.INixCommandService;
import com.nix.flakedrift.drift.service.IDriftCompareService;
import com.nix.flakedrift.drift.service.IUpdateService;
import com.nix.flakedrift.drift.service.IWorkspaceGraphService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * {@link IUpdateService} — locates the stale chain (cause + transitive, via the
 * same classifier as the drift check, but without the live store probe) and
 * updates each local flake's lock, deepest-level first.
 *
 * <p><b>Lock lifecycle contract.</b> A local flake without a {@code flake.lock}
 * is never given one by this service: it tracks its upstream parent's lock (the
 * parent re-pins it via {@code nix flake lock --update-input <child>}), so
 * creating a lock for it would detach it from that pinning. Updating such a
 * candidate therefore means <i>re-pinning its parent</i>, not creating its own
 * lock. Correspondingly:
 *
 * <ul>
 *   <li>a <b>stale candidate that has its own lock</b> → full
 *       {@code nix flake update};</li>
 *   <li>a <b>candidate without its own lock</b> → skipped as a standalone
 *       update target, its parent (if it has a lock) gets a surgical
 *       {@code nix flake lock --update-input <child>} instead;</li>
 *   <li>any <b>ancestor with its own lock</b> of every updated candidate →
 *       surgical re-pin {@code nix flake lock <path> --update-input <direct child>},
 *       so the parent's lock snapshot tracks the now-updated descendant, e.g.
 *       {@code nixos-rebuild} sees a sub-flake's new inputs (a flake node whose
 *       parent-lock entry lacks a narHash stays a candidate until re-pinned);</li>
 *   <li>an <b>ancestor without its own lock</b> → never touched, following the
 *       same never-create-a-lock rule.</li>
 * </ul>
 *
 * <p>Ordering stays deepest-first: children are fully updated before their
 * parents are re-pinned, so a parent re-pin snapshots the final child state.
 */
public final class UpdateServiceImpl implements IUpdateService {
    private final IWorkspaceGraphService workspaceGraphService;
    private final IDriftCompareService driftCompareService;
    private final INixCommandService nixCommandService;

    public UpdateServiceImpl(
            IWorkspaceGraphService workspaceGraphService,
            IDriftCompareService driftCompareService,
            INixCommandService nixCommandService) {
        this.workspaceGraphService = workspaceGraphService;
        this.driftCompareService = driftCompareService;
        this.nixCommandService = nixCommandService;
    }

    @Override
    public List<UpdateCandidateDto> findUpdateCandidates(Path flakeRootAbsolutePath) {
        FlakeDependencyGraph tree = workspaceGraphService.buildDependencyGraph(flakeRootAbsolutePath);
        Map<FlakeGraphNode, Set<DriftType>> drift = driftCompareService.evaluate(tree, Map.of());
        return tree.allNodes().stream()
                .filter(n -> n.getPath() != null)
                .filter(n -> isStale(n, drift))
                .map(n -> new UpdateCandidateDto(n.getName(), n.getPath(), n.getDepth()))
                .sorted(Comparator.comparingInt(UpdateCandidateDto::depth).reversed()
                        .thenComparing(UpdateCandidateDto::name))
                .toList();
    }

    @Override
    public List<UpdateResultDto> updateAll(Path flakeRootAbsolutePath) {
        FlakeDependencyGraph tree = workspaceGraphService.buildDependencyGraph(flakeRootAbsolutePath);
        Map<FlakeGraphNode, Set<DriftType>> drift = driftCompareService.evaluate(tree, Map.of());
        List<FlakeGraphNode> candidates = tree.allNodes().stream()
                .filter(n -> n.getPath() != null)
                .filter(n -> isStale(n, drift))
                .sorted(Comparator.comparingInt(FlakeGraphNode::getDepth).reversed()
                        .thenComparing(FlakeGraphNode::getName))
                .toList();
        Map<FlakeGraphNode, FlakeGraphNode> parent = parentMap(tree);

        List<UpdateResultDto> results = new ArrayList<>(candidates.size());

        for (FlakeGraphNode candidate : candidates) {
            if (!hasOwnLock(candidate)) {
                continue; // no lock file -> it tracks the parent lock, nothing to update here.
            }
            updateLock(candidate, results);
        }

        // Surgical re-pin of non-stale ancestors whose lock must now snapshot the
        // changed descendant, deepest ancestor first so a parent snapshots the
        // already-re-pinned child. Candidates themselves are handled above.
        List<RePin> rePins = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (FlakeGraphNode candidate : candidates) {
            FlakeGraphNode current = candidate;
            while (parent.containsKey(current)) {
                FlakeGraphNode ancestor = parent.get(current);
                if (!isStale(ancestor, drift) && hasOwnLock(ancestor)) {
                    String key = ancestor.getPath() + ":" + current.getName();
                    if (seen.add(key)) {
                        rePins.add(new RePin(ancestor, current.getName(), ancestor.getDepth()));
                    }
                }
                current = ancestor;
            }
        }
        rePins.sort(Comparator.comparingInt(RePin::depth).reversed().thenComparing(r -> r.childName));
        for (RePin rePin : rePins) {
            updateRePin(rePin, results);
        }
        return results;
    }

    private void updateLock(FlakeGraphNode node, List<UpdateResultDto> results) {
        Path lock = node.getPath().resolve("flake.lock");
        String before = fingerprint(lock);
        String output = nixCommandService.run(List.of("nix", "flake", "update", "--flake", node.getPath().toString()));
        String after = fingerprint(lock);
        results.add(new UpdateResultDto(
                node.getName(),
                node.getDepth(),
                !Objects.equals(before, after),
                before,
                after,
                output));
    }

    private void updateRePin(RePin rePin, List<UpdateResultDto> results) {
        Path lock = rePin.ancestor.getPath().resolve("flake.lock");
        String before = fingerprint(lock);
        String output = nixCommandService.run(List.of(
                "nix", "flake", "lock", rePin.ancestor.getPath().toString(), "--update-input", rePin.childName));
        String after = fingerprint(lock);
        results.add(new UpdateResultDto(
                rePin.ancestor.getName(),
                rePin.ancestor.getDepth(),
                !Objects.equals(before, after),
                before,
                after,
                output));
    }

    private static Map<FlakeGraphNode, FlakeGraphNode> parentMap(FlakeDependencyGraph tree) {
        Map<FlakeGraphNode, FlakeGraphNode> parent = new HashMap<>();
        walk(parent, tree.getRoot(), null);
        return parent;
    }

    private static void walk(Map<FlakeGraphNode, FlakeGraphNode> parent, FlakeGraphNode node, FlakeGraphNode parentNode) {
        if (parentNode != null) {
            parent.put(node, parentNode);
        }
        for (FlakeGraphNode child : node.getChildren()) {
            walk(parent, child, node);
        }
    }

    private static boolean hasOwnLock(FlakeGraphNode node) {
        return node.getPath() != null && Files.isRegularFile(node.getPath().resolve("flake.lock"));
    }

    private static String fingerprint(Path lockFile) {
        try {
            if (lockFile == null || !Files.isRegularFile(lockFile)) {
                return null; // no lock file — invalid caller state, no fingerprint to record.
            }
            byte[] content = Files.readAllBytes(lockFile);
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(content);
            return "sha256-" + Base64.getEncoder().encodeToString(hash);
        } catch (IOException | NoSuchAlgorithmException e) {
            throw new IllegalStateException("cannot fingerprint lock file: " + lockFile, e);
        }
    }

    private static boolean isStale(FlakeGraphNode node, Map<FlakeGraphNode, Set<DriftType>> drift) {
        Set<DriftType> types = drift.get(node);
        if (types == null) {
            return false;
        }
        if (types.contains(DriftType.CHAIN_STALE_CAUSE) || types.contains(DriftType.CHAIN_STALE_TRANSITIVE)) {
            return true;
        }
        return node.getPath() != null && types.contains(DriftType.NARHASH_ABSENT);
    }

    private record RePin(FlakeGraphNode ancestor, String childName, int depth) {
    }
}