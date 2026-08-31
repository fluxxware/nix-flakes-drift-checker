package com.nix.flakedrift.drift.infra.impl;

import com.nix.flakedrift.drift.domain.model.FlakeInputReference;
import com.nix.flakedrift.drift.infra.IFlakeDefinitionService;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * {@link IFlakeDefinitionService} extracting input declarations from flake.nix.
 *
 * <p>Two passes: first the {@code inputs = { ... }} attrset is located via
 * brace-matching (string/comment aware), then input URLs are matched only inside
 * that block — avoiding false positives from {@code url = "..."} elsewhere.
 */
public final class FlakeDefinitionService implements IFlakeDefinitionService {
    private static final Pattern INPUTS_START = Pattern.compile("\\binputs\\s*=\\s*\\{");
    /** Matches both {@code name.url = "..."} and {@code name = { url = "..."; };} forms. */
    private static final Pattern INPUT_URL =
            Pattern.compile("([A-Za-z0-9_\\-]+)\\s*(?:\\.\\s*url|\\s*=\\s*\\{\\s*url)\\s*=\\s*\"([^\"]+)\"");

    @Override
    public List<FlakeInputReference> parseInputs(Path flakeNixPath) {
        List<FlakeInputReference> refs = new ArrayList<>();
        if (flakeNixPath == null || !Files.isRegularFile(flakeNixPath)) {
            return refs;
        }
        try {
            String text = Files.readString(flakeNixPath, StandardCharsets.UTF_8);
            String inputsBlock = extractInputsBlock(text);
            if (inputsBlock == null) {
                return refs;
            }
            Matcher m = INPUT_URL.matcher(inputsBlock);
            while (m.find()) {
                refs.add(new FlakeInputReference(m.group(1), m.group(2)));
            }
        } catch (IOException e) {
            throw new IllegalStateException("cannot read flake.nix: " + flakeNixPath, e);
        }
        return refs;
    }

    /** Returns the text of the {@code inputs = { ... }} attrset, or {@code null}. */
    private static String extractInputsBlock(String text) {
        Matcher start = INPUTS_START.matcher(text);
        if (!start.find()) {
            return null;
        }
        int open = text.indexOf('{', start.start());
        if (open < 0) {
            return null;
        }
        int depth = 0;
        boolean inString = false;
        for (int i = open; i < text.length(); i++) {
            char c = text.charAt(i);
            if (inString) {
                if (c == '"' && (i == 0 || text.charAt(i - 1) != '\\')) {
                    inString = false;
                }
                continue;
            }
            if (c == '"') {
                inString = true;
            } else if (c == '#') {
                while (i < text.length() && text.charAt(i) != '\n') {
                    i++;
                }
            } else if (c == '{') {
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0) {
                    return text.substring(open, i + 1);
                }
            }
        }
        return null;
    }
}
