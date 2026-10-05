package com.yuzhi.dts.wiki.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Forwards frontend routes to {@code index.html} so the React application
 * (verified by {@code build.sh} and bundled by Maven) handles
 * them. API, management, authentication and documentation paths are excluded.
 *
 * Routes are maintained with the React router; API paths are never forwarded.
 */
@Controller
public class SpaForwardController {

    @GetMapping(
        value = {
            "/",
            "/s/**",
            "/p/**",
            "/docs/**",
            "/worklog/**",
            "/sandbox/**",
            "/search",
            "/conflicts/**",
            "/admin/**",
        }
    )
    public String forward() {
        return "forward:/index.html";
    }
}
