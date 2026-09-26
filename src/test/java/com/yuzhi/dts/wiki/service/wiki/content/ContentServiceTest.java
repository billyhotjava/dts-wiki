package com.yuzhi.dts.wiki.service.wiki.content;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * ContentService unit tests (design 10 S3.1): every doc type, valid + invalid,
 * YAML errors, missing frontmatter, CRLF, duplicate doc_id, title derivation.
 */
class ContentServiceTest {

    private final ContentService service = new ContentService(new ContentSchemaRegistry(), new ObjectMapper());

    @Test
    void noFrontmatterIsLenientPage() {
        ContentAnalysis analysis = service.analyze("# Hello\n\ntext\n", ContentService.Mode.STRICT);
        assertThat(analysis.docType()).isEqualTo("page");
        assertThat(analysis.valid()).isTrue();
        assertThat(analysis.title()).isEqualTo("Hello");
        assertThat(analysis.plainText()).contains("Hello").contains("text");
        assertThat(analysis.headings()).containsExactly("Hello");
    }

    @Test
    void crlfFrontmatterSplits() {
        ContentAnalysis analysis = service.analyze(
            "---\r\ntype: task\r\nid: S6/F4/T03\r\nfeature: S6/F5\r\ntitle: T\r\nstatus: READY\r\n---\r\n# T\r\n",
            ContentService.Mode.STRICT
        );
        assertThat(analysis.body()).isEqualTo("# T\r\n");
        assertThat(analysis.valid()).isTrue();
    }

    @Test
    void yamlErrorIsInvalid() {
        ContentAnalysis analysis = service.analyze("---\ntype: [unclosed\n---\nbody\n", ContentService.Mode.LENIENT);
        assertThat(analysis.valid()).isFalse();
        assertThat(analysis.errors()).extracting(ContentAnalysis.FieldError::path).contains("$");
        assertThatThrownBy(() -> service.analyze("---\ntype: [unclosed\n---\nbody\n", ContentService.Mode.STRICT)).isInstanceOf(
            FrontmatterInvalidException.class
        );
    }

    @Test
    void sprintValidAndInvalid() {
        String ok = "---\ntype: sprint\nid: sprint-6\ntitle: Wiki\nstatus: IN_PROGRESS\ntimebox: {start: 2026-10-12, end: 2026-11-20}\ngoal: ship\n---\n# Wiki\n";
        assertThat(service.analyze(ok, ContentService.Mode.STRICT).valid()).isTrue();
        assertThat(service.analyze(ok.replace("id: sprint-6", "id: bad"), ContentService.Mode.LENIENT).valid()).isFalse();
        assertThat(service.analyze(ok.replace("IN_PROGRESS", "WRONG"), ContentService.Mode.LENIENT).valid()).isFalse();
        assertThat(service.analyze(ok.replace("goal: ship\n", ""), ContentService.Mode.LENIENT).valid()).isFalse();
    }

    @Test
    void featureValidAndInvalid() {
        String ok = "---\ntype: feature\nid: S6/F5\nsprint: sprint-6\ntitle: Sync\nstatus: DONE\n---\n# Sync\n";
        assertThat(service.analyze(ok, ContentService.Mode.STRICT).docId()).isEqualTo("S6/F5");
        assertThat(service.analyze(ok.replace("S6/F5", "F5"), ContentService.Mode.LENIENT).valid()).isFalse();
        assertThat(service.analyze(ok.replace("status: DONE", "status: DONE\npriority: P9"), ContentService.Mode.LENIENT).valid()).isFalse();
        assertThat(service.analyze("---\ntype: feature\nid: S6/F5\ntitle: No sprint\nstatus: DONE\n---\n", ContentService.Mode.LENIENT).valid()).isFalse();
    }

    @Test
    void taskValidAndInvalid() {
        String ok = "---\ntype: task\nid: S6/F4/T03\nfeature: S6/F5\ntitle: Inbound\nstatus: READY\ndepends: [S6/F4/T02]\n---\n# Inbound\n";
        ContentAnalysis analysis = service.analyze(ok, ContentService.Mode.STRICT);
        assertThat(analysis.depends()).containsExactly("S6/F4/T02");
        assertThat(analysis.valid()).isTrue();
        assertThat(service.analyze(ok.replace("S6/F4/T03", "T3"), ContentService.Mode.LENIENT).valid()).isFalse();
        assertThat(service.analyze(ok.replace("READY", "MAYBE"), ContentService.Mode.LENIENT).valid()).isFalse();
        assertThat(service.analyze(ok.replace("depends: [S6/F4/T02]", "depends: oops"), ContentService.Mode.LENIENT).valid()).isFalse();
    }

    @Test
    void adrValidAndInvalid() {
        String ok = "---\ntype: adr\nid: W-ADR-7\ntitle: Sync\nstatus: Accepted\ndate: 2026-09-27\n---\n# Sync\n";
        assertThat(service.analyze(ok, ContentService.Mode.STRICT).valid()).isTrue();
        assertThat(service.analyze(ok.replace("Accepted", "Maybe"), ContentService.Mode.LENIENT).valid()).isFalse();
        assertThat(service.analyze(ok.replace("date: 2026-09-27", "date: 27/09/2026"), ContentService.Mode.LENIENT).valid()).isFalse();
        assertThat(service.analyze(ok.replace("title: Sync\n", ""), ContentService.Mode.LENIENT).valid()).isFalse();
    }

    @Test
    void evidenceValidAndInvalid() {
        String ok = "---\ntype: evidence\nid: S6/IT-03\ncovers: [S6/F4/T03]\nresult: PASS\n---\n# IT\n";
        assertThat(service.analyze(ok, ContentService.Mode.STRICT).valid()).isTrue();
        assertThat(service.analyze(ok.replace("covers: [S6/F4/T03]", "covers: []"), ContentService.Mode.LENIENT).valid()).isFalse();
        assertThat(service.analyze(ok.replace("PASS", "OK"), ContentService.Mode.LENIENT).valid()).isFalse();
        assertThat(service.analyze(ok.replace("covers: [S6/F4/T03]\n", ""), ContentService.Mode.LENIENT).valid()).isFalse();
    }

    @Test
    void duplicateDocIdFlagged() {
        String md = "---\ntype: task\nid: S6/F4/T03\nfeature: S6/F5\ntitle: Dup\nstatus: READY\n---\n# Dup\n";
        ContentAnalysis analysis = service.analyze(md, ContentService.Mode.LENIENT, docId -> Optional.of("other/path.md"));
        assertThat(analysis.valid()).isFalse();
        assertThat(analysis.errors()).extracting(ContentAnalysis.FieldError::path).contains("id");
    }

    @Test
    void titleFallsBackToPageTitle() {
        ContentAnalysis analysis = service.analyze("no frontmatter here\n", ContentService.Mode.STRICT);
        assertThat(analysis.title()).isNull();
        assertThat(analysis.docType()).isEqualTo("page");
    }

    @Test
    void unknownTypeTreatedAsPage() {
        ContentAnalysis analysis = service.analyze("---\ntype: whatever\n---\n# X\n", ContentService.Mode.STRICT);
        assertThat(analysis.docType()).isEqualTo("page");
        assertThat(analysis.valid()).isTrue();
    }
}
