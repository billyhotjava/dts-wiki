package com.yuzhi.dts.wiki.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties("application.wiki.mcp")
public class WikiMcpProperties {
    private boolean enabled;
    private String resourceUri = "http://localhost:8080/mcp";
    private List<String> allowedOrigins = List.of();
    private List<String> clientIds = List.of("dts-wiki-agent");
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getResourceUri() { return resourceUri; }
    public void setResourceUri(String value) { resourceUri = value; }
    public List<String> getAllowedOrigins() { return allowedOrigins; }
    public void setAllowedOrigins(List<String> value) { allowedOrigins = List.copyOf(value); }
    public List<String> getClientIds() { return clientIds; }
    public void setClientIds(List<String> value) { clientIds = List.copyOf(value); }
    public String metadataUri() {
        var uri = java.net.URI.create(resourceUri);
        return uri.getScheme() + "://" + uri.getRawAuthority() + "/.well-known/oauth-protected-resource/mcp";
    }
}
