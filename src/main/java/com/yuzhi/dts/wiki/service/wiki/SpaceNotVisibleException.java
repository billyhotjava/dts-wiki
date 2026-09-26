package com.yuzhi.dts.wiki.service.wiki;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when the current user has no read access to a space (or the space does not exist).
 * Translated to HTTP 404 so space existence is never leaked (design 03 S3).
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class SpaceNotVisibleException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public SpaceNotVisibleException(String spaceSlug) {
        super("Space not visible: " + spaceSlug);
    }
}
