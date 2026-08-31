package com.nix.flakedrift.drift.infra;

import com.nix.flakedrift.drift.domain.model.FlakeLockEntry;

import java.nio.file.Path;
import java.util.Map;

/** Reads and parses a flake.lock into its locked input entries. */
public interface ILockFileService {
    /** node name -> lock entry. Empty map if the lock file is missing. */
    Map<String, FlakeLockEntry> readLockFile(Path flakeLockPath);
}
