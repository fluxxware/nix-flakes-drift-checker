package com.nix.flakedrift.drift.service;

import com.nix.flakedrift.drift.domain.model.FlakeDependencyGraph;

import java.nio.file.Path;

/** Builds the workspace dependency DAG (root -> nested path: inputs). */
public interface IWorkspaceGraphService {
    FlakeDependencyGraph buildDependencyGraph(Path rootDir);
}
