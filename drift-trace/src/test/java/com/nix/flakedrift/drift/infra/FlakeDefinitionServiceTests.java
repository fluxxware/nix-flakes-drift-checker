package com.nix.flakedrift.drift.infra;

import com.nix.flakedrift.drift.domain.model.FlakeInputReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static com.nix.flakedrift.drift.infra.FlakeDefinitionServiceTestData.ABSOLUTE_PATH;
import static com.nix.flakedrift.drift.infra.FlakeDefinitionServiceTestData.FALSE_POSITIVES;
import static com.nix.flakedrift.drift.infra.FlakeDefinitionServiceTestData.FOLLOWS;
import static com.nix.flakedrift.drift.infra.FlakeDefinitionServiceTestData.OUTSIDE_BLOCK;
import static com.nix.flakedrift.drift.infra.FlakeDefinitionServiceTestData.SIMPLE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Parsing tests for {@link com.nix.flakedrift.drift.infra.impl.FlakeDefinitionService}. */
class FlakeDefinitionServiceTests {

    @Test
    void givenFlakeWithBothUrlForms_whenParsingInputs_thenAllAreParsed(@TempDir Path dir) throws IOException {
        int expectedInputCount = 3;
        String expectedUpstreamUrl = "github:NixOS/nixpkgs/nixos-25.11";
        String expectedHomeManagerUrl = "github:nix-community/home-manager";
        String expectedAggregatorName = "aggregator";
        String expectedAggregatorPath = "./aggregator";

        List<FlakeInputReference> parsedInputs = new FlakeDefinitionServiceTestBuilder(dir)
                        .withFlakeNix(SIMPLE)
                        .build();

        assertEquals(expectedInputCount, parsedInputs.size());

        FlakeInputReference upstreamInput = parsedInputs.get(0);
        FlakeInputReference homeManagerInput = parsedInputs.get(1);
        FlakeInputReference aggregatorInput = parsedInputs.get(2);

        assertEquals(expectedUpstreamUrl, upstreamInput.url());
        assertEquals(expectedHomeManagerUrl, homeManagerInput.url());
        assertEquals(expectedAggregatorName, aggregatorInput.name());
        assertTrue(aggregatorInput.isPath(), "aggregator must be a path input");
        assertEquals(expectedAggregatorPath, aggregatorInput.pathValue());
    }

    @Test
    void givenInputWithFollows_whenParsingInputs_thenFollowsIsNotFakeInput(@TempDir Path dir) throws IOException {
        int expectedInputCount = 2;
        String expectedHomeManagerName = "home-manager";

        List<FlakeInputReference> parsedInputs = new FlakeDefinitionServiceTestBuilder(dir)
                        .withFlakeNix(FOLLOWS)
                        .build();

        assertEquals(expectedInputCount, parsedInputs.size());

        FlakeInputReference homeManagerInput = parsedInputs.get(1);
        assertEquals(expectedHomeManagerName, homeManagerInput.name());
    }

    @Test
    void givenAbsolutePathInput_whenParsingInputs_thenTreatedAsPath(@TempDir Path dir) throws IOException {
        String expectedFixturePath = "/fixtures/leaf-module";

        FlakeInputReference onlyInput = new FlakeDefinitionServiceTestBuilder(dir)
                        .withFlakeNix(ABSOLUTE_PATH)
                        .build()
                        .get(0);

        assertTrue(onlyInput.isPath(), "absolute path input must be treated as a path");
        assertEquals(expectedFixturePath, onlyInput.pathValue());
    }

    @Test
    void givenBracesInStringsAndComments_whenParsingInputs_thenIgnoresFalsePositives(@TempDir Path dir) throws IOException {
        int expectedInputCount = 2;
        String expectedUpstreamName = "nixpkgs";
        String expectedAppName = "app";

        List<FlakeInputReference> parsedInputs = new FlakeDefinitionServiceTestBuilder(dir)
                        .withFlakeNix(FALSE_POSITIVES)
                        .build();

        assertEquals(expectedInputCount, parsedInputs.size());

        FlakeInputReference upstreamInput = parsedInputs.get(0);
        FlakeInputReference appInput = parsedInputs.get(1);
        assertEquals(expectedUpstreamName, upstreamInput.name());
        assertEquals(expectedAppName, appInput.name());
    }

    @Test
    void givenUrlOutsideInputsBlock_whenParsingInputs_thenIgnored(@TempDir Path dir) throws IOException {
        int expectedInputCount = 1;
        String expectedAggregatorName = "aggregator";

        List<FlakeInputReference> parsedInputs = new FlakeDefinitionServiceTestBuilder(dir)
                        .withFlakeNix(OUTSIDE_BLOCK)
                        .build();

        assertEquals(expectedInputCount, parsedInputs.size());

        FlakeInputReference aggregatorInput = parsedInputs.get(0);
        assertEquals(expectedAggregatorName, aggregatorInput.name());
    }

    @Test
    void givenNoFlakeNix_whenParsingInputs_thenEmpty(@TempDir Path dir) throws IOException {
        List<FlakeInputReference> parsedInputs = new FlakeDefinitionServiceTestBuilder(dir).build();
        assertTrue(parsedInputs.isEmpty());
    }
}
