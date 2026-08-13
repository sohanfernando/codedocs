package com.sohan.codedocs.util;

import com.sohan.codedocs.dto.response.SourceRef;
import com.sohan.codedocs.entity.IndexedRepo;
import com.sohan.codedocs.repository.projection.ChunkSearchResult;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.stream.Collectors;

@Component
public class SourceLinkBuilder {

    public SourceRef toSourceRef(IndexedRepo repo, ChunkSearchResult hit, int index) {
        String url = "%s/blob/%s/%s#L%d-L%d".formatted(
                stripTrailingSlash(repo.getRemoteUrl()),
                repo.getCommitSha(),
                encodePath(hit.getFilePath()),
                hit.getStartLine(),
                hit.getEndLine());

        return new SourceRef(index, hit.getFilePath(), hit.getStartLine(),
                hit.getEndLine(), url, round(hit.getSimilarity()));
    }

    /** Encode each segment so spaces or unicode in filenames cannot break the URL. */
    private String encodePath(String path) {
        return Arrays.stream(path.split("/"))
                .map(segment -> UriUtils.encodePathSegment(segment, StandardCharsets.UTF_8))
                .collect(Collectors.joining("/"));
    }

    private String stripTrailingSlash(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    private double round(double value) {
        return Math.round(value * 10_000d) / 10_000d;
    }
}
