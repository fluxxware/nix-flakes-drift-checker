package com.nix.flakedrift.drift.domain.model;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A single flake in the workspace dependency DAG.
 *
 * <ul>
 *   <li>{@link #diskHash} — {@code nix hash path} of the current on-disk content;</li>
 *   <li>{@link #lockHash} — narHash recorded for this input in the parent's flake.lock;</li>
 * </ul>
 */
public class FlakeGraphNode {
    private final String name;
    private final Path path;
    private int depth;
    private String diskHash;
    private String lockHash;
    private final List<FlakeGraphNode> children = new ArrayList<>();

    /** @param path may be {@code null} for remote (non-path) inputs. */
    public FlakeGraphNode(String name, Path path) {
        this.name = Objects.requireNonNull(name);
        this.path = path;
    }

    public String getName() {
        return name;
    }

    public Path getPath() {
        return path;
    }

    public int getDepth() {
        return depth;
    }

    public void setDepth(int depth) {
        this.depth = depth;
    }

    public String getDiskHash() {
        return diskHash;
    }

    public void setDiskHash(String diskHash) {
        this.diskHash = diskHash;
    }

    public String getLockHash() {
        return lockHash;
    }

    public void setLockHash(String lockHash) {
        this.lockHash = lockHash;
    }

    public List<FlakeGraphNode> getChildren() {
        return children;
    }

    public void addChild(FlakeGraphNode child) {
        children.add(child);
    }

    @Override
    public String toString() {
        return "FlakeNode{" + name + '}';
    }
}
