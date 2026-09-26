package com.yuzhi.dts.wiki.service.wiki;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/** Optimistic-concurrency conflict: the page changed since {@code baseVersionNo} (HTTP 409). */
@ResponseStatus(HttpStatus.CONFLICT)
public class PageVersionConflictException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final int currentVersionNo;

    public PageVersionConflictException(int currentVersionNo) {
        super("Page version conflict, current version: " + currentVersionNo);
        this.currentVersionNo = currentVersionNo;
    }

    public int getCurrentVersionNo() {
        return currentVersionNo;
    }
}
