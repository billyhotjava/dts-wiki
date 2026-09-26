package com.yuzhi.dts.wiki.service.wiki;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/** The page has an unresolved sync conflict: plain saves are rejected (HTTP 409). */
@ResponseStatus(HttpStatus.CONFLICT)
public class PageSyncConflictException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public PageSyncConflictException(Long pageId) {
        super("Page has an unresolved sync conflict: " + pageId);
    }
}
