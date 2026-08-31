package com.nix.flakedrift.drift.dto;

/**
 * Result of a drift check, ready for --json or tree rendering.
 */
public class DriftReportDto {
    public FlakeGraphNodeDto root;
    public String target;
    public int total;
    public int synced;
    public int drifted;

    public static DriftReportDto empty(String target) {
        DriftReportDto d = new DriftReportDto();
        d.target = target;
        return d;
    }
}
