package com.nix.flakedrift.drift.infra.impl;
import com.nix.flakedrift.drift.infra.INixCommandService;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;

/** {@link INixCommandService} backed by {@link ProcessBuilder}. */
public final class NixCommandService implements INixCommandService {
    @Override
    public String hashPath(Path dir) {
        return run(List.of("nix", "hash", "path", dir.toString()));
    }

    @Override
    public String pathInfoJsonMany(List<String> storePaths) {
        List<String> cmd = new java.util.ArrayList<>(List.of("nix", "path-info", "--json"));
        cmd.addAll(storePaths);
        return run(cmd);
    }

    @Override
    public String run(List<String> args) {
        try {
            Process p = new ProcessBuilder(args).start();
            StringBuilder out = new StringBuilder();
            StringBuilder err = new StringBuilder();
            // Both streams are drained CONCURRENTLY (never blocking one while the
            // other fills its pipe buffer). stderr is ALSO streamed to the console
            // live — `nix flake update` writes all its progress ("Updated input ...",
            // "Downloading ...") to stderr, so the user sees it as it happens. On
            // success the combined stdout+stderr is returned (that is what lands in
            // the update-history `nixOutput`); on failure stderr is the error.
            Thread stdoutThread = drain(p.getInputStream(), out, false);
            Thread stderrThread = drain(p.getErrorStream(), err, true);
            int code = p.waitFor();
            stdoutThread.join();
            stderrThread.join();
            if (code != 0) {
                throw new IllegalStateException("command failed (" + code + "): " + args + " -> " + err.toString().trim());
            }
            String combined = out + "\n" + err;
            return combined.trim();
        } catch (IOException | InterruptedException e) {
            throw new IllegalStateException("command error: " + args, e);
        }
    }

    private static Thread drain(InputStream input, StringBuilder buffer, boolean streamToConsole) {
        Thread t = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    buffer.append(line).append('\n');
                    if (streamToConsole) {
                        System.err.println(line);
                    }
                }
            } catch (IOException ignored) {
                // subprocess stream closed — nothing to do.
            }
        });
        t.setDaemon(true);
        t.start();
        return t;
    }
}