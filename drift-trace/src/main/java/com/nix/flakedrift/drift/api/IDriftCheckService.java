package com.nix.flakedrift.drift.api;

import com.nix.flakedrift.drift.dto.DriftCheckRequest;
import com.nix.flakedrift.drift.dto.DriftReportDto;

/** Main use-case: check a workspace against a live target and produce a report. */
public interface IDriftCheckService {
    DriftReportDto checkForDrift(DriftCheckRequest request);
}
