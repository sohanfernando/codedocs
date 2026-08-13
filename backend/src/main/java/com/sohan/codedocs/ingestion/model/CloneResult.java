package com.sohan.codedocs.ingestion.model;

import java.nio.file.Path;

public record CloneResult(Path path, String commitSha, String branch) {}
