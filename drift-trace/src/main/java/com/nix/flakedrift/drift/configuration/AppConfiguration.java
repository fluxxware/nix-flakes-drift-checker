package com.nix.flakedrift.drift.configuration;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Composed root of app-level configuration — read-only defaults, molded after
 * Spring {@code @ConfigurationProperties} minus the framework: an outer bean
 * container exposing each config section via an accessor, and immutable nested
 * value objects holding plain defaults. No setters, no runtime mutation; the
 * only customization path is {@link #withStore(Path, boolean)} which returns a
 * NEW config instance (copy semantics) for the mock-store/dev scenario.
 */
public class AppConfiguration {

    private final String version;
    private final StoreConfiguration store;
    private final WorkspaceConfiguration workspace;
    private final HistoryConfiguration history;

    public AppConfiguration() {
        this(
                resolveVersion(),
                new StoreConfiguration(),
                new WorkspaceConfiguration(),
                new HistoryConfiguration());
    }

    private AppConfiguration(
            String version,
            StoreConfiguration store,
            WorkspaceConfiguration workspace,
            HistoryConfiguration history) {
        this.version = version;
        this.store = store;
        this.workspace = workspace;
        this.history = history;
    }

    /** Copy with a different store probing setup (mock-store/dev), original untouched. */
    public AppConfiguration withStore(Path root, boolean mock) {
        return new AppConfiguration(version, new StoreConfiguration(root, mock), workspace, history);
    }

    /**
     * Tool version — read from the build-generated {@code version.properties}
     * resource (single source: root {@code gradle.properties}). Falls back to
     * {@code unknown} when the resource is absent (e.g. bare IDE unit runs).
     */
    public String version() {
        return version;
    }

    private static String resolveVersion() {
        try (InputStream in = AppConfiguration.class.getClassLoader().getResourceAsStream("version.properties")) {
            if (in == null) {
                return "unknown";
            }
            Properties props = new Properties();
            props.load(in);
            return props.getProperty("version", "unknown");
        } catch (IOException e) {
            return "unknown";
        }
    }

    public StoreConfiguration store() {
        return store;
    }

    public WorkspaceConfiguration workspace() {
        return workspace;
    }

    public HistoryConfiguration history() {
        return history;
    }

    /** Where deployed source objects are probed (real /nix/store or a simulated deployment dir). */
    public static class StoreConfiguration {
        private final Path root;
        private final boolean mock;

        public StoreConfiguration() {
            this(Path.of("/nix/store"), false);
        }

        public StoreConfiguration(Path root, boolean mock) {
            this.root = root;
            this.mock = mock;
        }

        public Path root() {
            return root;
        }

        /** {@code true} = store objects are plain dirs hashed with {@code nix hash path}; {@code false} = real store via {@code nix path-info}. */
        public boolean mock() {
            return mock;
        }
    }

    /** Default root flake directory scanned by {@code check}/{@code update} when {@code --root} is omitted. */
    public static class WorkspaceConfiguration {
        private final Path defaultRoot = Path.of("/etc/nixos");

        public Path defaultRoot() {
            return defaultRoot;
        }
    }

    /** Update-history audit log — one run file per {@code update} run, read back by {@code history}.
     * <br> falls back to /tmp when user.home is not set (somehow)
     * */
    public static class HistoryConfiguration {
        private final Path historyDirectory = Path.of(
                System.getProperty("user.home", "/tmp"), ".flake-drift");

        public Path historyDirectory() {
            return historyDirectory;
        }
    }
}