package com.yuzhi.dts.wiki.service.wiki.content;

import com.yuzhi.dts.wiki.service.wiki.content.ContentAnalysis.FieldError;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/** STRICT-mode frontmatter rejection (HTTP 422, errorKey FRONTMATTER_INVALID). */
@ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
public class FrontmatterInvalidException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final List<FieldError> errors;

    public FrontmatterInvalidException(List<FieldError> errors) {
        super("Frontmatter invalid: " + errors);
        this.errors = errors;
    }

    public List<FieldError> getErrors() {
        return errors;
    }
}
