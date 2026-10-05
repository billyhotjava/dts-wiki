package com.yuzhi.dts.wiki.service.wiki.sync;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class SpaceManifestParserTest {

    private final SpaceManifestParser parser = new SpaceManifestParser();

    private static String manifest(String slug, String root) {
        return "version: 1\nspaces:\n  - slug: " + slug + "\n    name: Knowledge\n    role: space-team-readers\n    roots: [" + root + "]\n";
    }

    @Test
    void keepsInventoryRolesAndRootsWithoutProductAssumptions() {
        var value = parser.parse(manifest("team-notes", "docs/team"));
        assertThat(value.spaces()).hasSize(1);
        assertThat(value.spaces().getFirst().role()).isEqualTo("space-team-readers");
        assertThat(value.spaces().getFirst().roots()).containsExactly("docs/team");
    }

    @Test
    void rejectsSchemaDriftAndUnknownVersions() {
        assertThatThrownBy(() -> parser.parse(manifest("team-notes", "docs/team").replace("version: 1", "version: 2")))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> parser.parse(manifest("team-notes", "docs/team") + "outbound: true\n"))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsTraversalOverlapsAndDuplicateSlugs() {
        assertThatThrownBy(() -> parser.parse(manifest("team-notes", "docs/../secrets"))).isInstanceOf(IllegalArgumentException.class);
        String entry = manifest("team-notes", "docs/team").substring("version: 1\nspaces:\n".length());
        assertThatThrownBy(() -> parser.parse(manifest("team-notes", "docs/team") + entry)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> parser.parse(manifest("team-notes", "docs/team") + entry.replace("team-notes", "other-team").replace("docs/team", "docs/team/child")))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsDuplicateYamlKeysAndObjectTags() {
        assertThatThrownBy(() -> parser.parse(manifest("team-notes", "docs/team") + "version: 1\n"))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> parser.parse("!!java.lang.ProcessBuilder {}"))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
