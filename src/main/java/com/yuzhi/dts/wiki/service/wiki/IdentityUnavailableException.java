package com.yuzhi.dts.wiki.service.wiki;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
public class IdentityUnavailableException extends RuntimeException {
    public IdentityUnavailableException() { super("Current identity permissions are unavailable; retry later"); }
}
