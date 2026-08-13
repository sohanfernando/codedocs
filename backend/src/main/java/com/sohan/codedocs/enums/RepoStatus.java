package com.sohan.codedocs.enums;

public enum RepoStatus {
    PENDING, CLONING, CHUNKING, EMBEDDING, READY, FAILED;

    public boolean isTerminal(){
        return this == READY || this == FAILED;
    }
}
