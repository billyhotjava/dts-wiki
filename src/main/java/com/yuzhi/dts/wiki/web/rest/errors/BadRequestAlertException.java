package com.yuzhi.dts.wiki.web.rest.errors;
import java.io.Serial;
import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

public class BadRequestAlertException extends ErrorResponseException {
    @Serial private static final long serialVersionUID = 1L;
    private final String entityName;
    private final String errorKey;
    public BadRequestAlertException(String message, String entityName, String errorKey) {
        this(ErrorConstants.DEFAULT_TYPE, message, entityName, errorKey);
    }
    public BadRequestAlertException(URI type, String message, String entityName, String errorKey) {
        super(HttpStatus.BAD_REQUEST, problem(type, message, entityName, errorKey), null);
        this.entityName = entityName;
        this.errorKey = errorKey;
    }
    private static ProblemDetail problem(URI type, String message, String entityName, String errorKey) {
        ProblemDetail detail = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        detail.setType(type);
        detail.setTitle(message);
        detail.setProperty("message", "error." + errorKey);
        detail.setProperty("params", entityName);
        return detail;
    }
    public String getEntityName() { return entityName; }
    public String getErrorKey() { return errorKey; }
}
