package com.nix.flakedrift.app;

import com.nix.flakedrift.app.cli.CheckCommand;
import com.nix.flakedrift.app.cli.HistoryCommand;
import com.nix.flakedrift.app.cli.UpdateCommand;
import com.nix.flakedrift.drift.configuration.AppConfiguration;
import picocli.CommandLine;

/** flakes-drift-checker CLI entrypoint. */
@CommandLine.Command(
        name = "flakes-drift-checker",
        mixinStandardHelpOptions = true,
        versionProvider = Main.VersionProvider.class,
        description = "Detect drift between a deployed nix target and the local monorepo.",
        subcommands = {CheckCommand.class, UpdateCommand.class, HistoryCommand.class})
public final class Main implements Runnable {
    @Override
    public void run() {
        CommandLine.usage(this, System.out);
    }

    public static void main(String[] args) {
        int exit = new CommandLine(new Main()).execute(args);
        System.exit(exit);
    }

    /** Resolves the tool version from the build-generated resource (single source: root gradle.properties). */
    public static final class VersionProvider implements CommandLine.IVersionProvider {
        @Override
        public String[] getVersion() {
            return new String[]{"flakes-drift-checker " + new AppConfiguration().version()};
        }
    }
}
