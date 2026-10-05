package com.yuzhi.dts.wiki.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Forwards frontend routes to {@code index.html} so the React application
 * (built into {@code target/classes/static} by frontend-maven-plugin) handles
 * them. API, management, authentication and documentation paths are excluded.
 *
 * DTS-WIKI: hand-written (Sprint-6 design 03 S1, 00 D14); application-owned routing
 * is generated with {@code skipClient}.
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
