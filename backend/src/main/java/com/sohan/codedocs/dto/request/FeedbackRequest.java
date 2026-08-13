package com.sohan.codedocs.dto.request;

import com.sohan.codedocs.enums.Feedback;

/** Null vote clears any existing feedback — same endpoint handles "un-voting". */
public record FeedbackRequest(Feedback vote) {}
