package com.yuzhi.dts.wiki.service.wiki;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class GitPageReadOnlyException extends RuntimeException {
    public GitPageReadOnlyException() { super("Git-managed content is read-only; edit it in its source repository"); }
}
