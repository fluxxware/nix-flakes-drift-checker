package com.nix.flakedrift.drift.testutil;

import com.nix.flakedrift.drift.domain.model.DeploymentTarget;
import com.nix.flakedrift.drift.domain.model.DriftType;
import com.nix.flakedrift.drift.domain.model.FlakeDependencyGraph;
import com.nix.flakedrift.drift.domain.model.FlakeGraphNode;
import com.nix.flakedrift.drift.dto.DriftMemberDto;
import com.nix.flakedrift.drift.dto.DriftReportDto;
import com.nix.flakedrift.drift.dto.FlakeGraphNodeDto;
import com.nix.flakedrift.drift.service.IDriftCompareService;
import com.nix.flakedrift.drift.service.impl.DriftCompareServiceImpl;
import com.nix.flakedrift.drift.service.mapper.DriftReportMapper;
import com.nix.flakedrift.drift.testutil.DriftFixtureLoader.Loaded;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Dataset-driven tests: every JSON in {@code src/test/resources/datasets} is loaded,
 * evaluated and assembled, then asserted against the dataset's expected statuses,
 * unrefreshed members and totals.
 */
class DriftDatasetTest {

    private final IDriftCompareService compare = new DriftCompareServiceImpl();
    private final DriftReportMapper mapper = new DriftReportMapper();
    private final DriftFixtureLoader loader = new DriftFixtureLoader();

    @Test
    void allDatasetsMatchExpected(@TempDir Path tempDir) throws IOException, URISyntaxException {
        List<Path> datasets = datasetFiles();
        assertTrue(datasets.size() >= 5, "expected at least 5 datasets, found " + datasets.size());

        for (Path dataset : datasets) {
            assertDataset(dataset);
        }
    }

    private void assertDataset(Path dataset) throws IOException {
        Loaded loaded = loader.load(dataset);
        FlakeDependencyGraph graph = loaded.graph();
        List<FlakeGraphNode> nodes = graph.allNodes();

        Map<FlakeGraphNode, Set<DriftType>> drift = compare.evaluate(graph, loaded.live());

        for (FlakeGraphNode node : nodes) {
            assertEquals(loaded.expectedStatuses().get(node.getName()),
                    drift.get(node),
                    dataset.getFileName() + " :: statuses of " + node.getName());
        }

        DriftReportDto report = mapper.assemble(graph, loaded.live(), drift, DeploymentTarget.local());

        List<FlakeGraphNodeDto> dtos = flatten(report.root);
        for (int i = 0; i < nodes.size(); i++) {
            List<String> expectedUnrefreshed = loaded.expectedUnrefreshed().get(nodes.get(i).getName());
            List<String> actualUnrefreshed = dtos.get(i).unrefreshedMembers.stream()
                    .map(DriftMemberDto::name)
                    .toList();
            assertEquals(expectedUnrefreshed, actualUnrefreshed,
                    dataset.getFileName() + " :: unrefreshed of " + nodes.get(i).getName());
        }

        assertEquals(loaded.expectedTotals().total(), report.total, dataset.getFileName() + " :: total");
        assertEquals(loaded.expectedTotals().synced(), report.synced, dataset.getFileName() + " :: synced");
        assertEquals(loaded.expectedTotals().drifted(), report.drifted, dataset.getFileName() + " :: drifted");
    }

    private List<Path> datasetFiles() throws URISyntaxException, IOException {
        Path dir = Path.of(getClass().getClassLoader().getResource("datasets").toURI());
        try (var stream = Files.list(dir)) {
            return stream.filter(p -> p.getFileName().toString().endsWith(".json"))
                    .sorted()
                    .toList();
        }
    }

    private static List<FlakeGraphNodeDto> flatten(FlakeGraphNodeDto dto) {
        List<FlakeGraphNodeDto> out = new ArrayList<>();
        out.add(dto);
        for (FlakeGraphNodeDto child : dto.children) {
            out.addAll(flatten(child));
        }
        return out;
    }
}
