package com.yuzhi.dts.wiki.web.rest.wiki;

import com.yuzhi.dts.wiki.service.wiki.PageSyncConflictException;
import com.yuzhi.dts.wiki.service.wiki.PageVersionConflictException;
import com.yuzhi.dts.wiki.service.wiki.SpaceNotVisibleException;
import java.net.URI;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Translates wiki business errors to RFC 7807 ProblemDetail with an {@code errorKey}
 * (design 03 S4). Spring Security translates {@link AccessDeniedException} to 403 itself;
 * {@link SpaceNotVisibleException} and the 409 pair carry {@code @ResponseStatus} as well,
 * this advice only enriches the body.
 */
@RestControllerAdvice(basePackages = "com.yuzhi.dts.wiki.web.rest.wiki")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class WikiExceptionHandler {

    @ExceptionHandler(SpaceNotVisibleException.class)
    public ProblemDetail notVisible(SpaceNotVisibleException e) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
        problem.setType(URI.create("https://yuzhicloud.com/problems/space-not-visible"));
        problem.setTitle("Space or page not found");
        problem.setProperty("errorKey", "SPACE_NOT_VISIBLE");
        return problem;
    }

    @ExceptionHandler(PageVersionConflictException.class)
    public ProblemDetail versionConflict(PageVersionConflictException e) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
        problem.setType(URI.create("https://yuzhicloud.com/problems/page-version-conflict"));
        problem.setTitle("Page changed since base version");
        problem.setProperty("errorKey", "PAGE_VERSION_CONFLICT");
        problem.setProperty("currentVersionNo", e.getCurrentVersionNo());
        return problem;
    }

    @ExceptionHandler(PageSyncConflictException.class)
    public ProblemDetail syncConflict(PageSyncConflictException e) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
        problem.setType(URI.create("https://yuzhicloud.com/problems/page-sync-conflict"));
        problem.setTitle("Page has an unresolved sync conflict");
        problem.setProperty("errorKey", "PAGE_SYNC_CONFLICT");
        return problem;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail badRequest(IllegalArgumentException e) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
        problem.setType(URI.create("https://yuzhicloud.com/problems/bad-request"));
        problem.setTitle("Bad request");
        problem.setProperty("errorKey", "BAD_REQUEST");
        return problem;
    }
}
