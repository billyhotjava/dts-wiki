package com.yuzhi.dts.wiki.service.wiki.content;

import java.util.ArrayList;
import java.util.List;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.node.AbstractVisitor;
import org.commonmark.node.Heading;
import org.commonmark.node.Node;
import org.commonmark.node.Text;
import org.commonmark.parser.Parser;

/**
 * Markdown body to plain text + headings (design 10 S3.1, B6).
 * Code block text is kept, link URLs are dropped (link text kept).
 */
public final class MarkdownText {

    private MarkdownText() {}

    public record DocText(String plainText, List<String> headings) {}

    private static final Parser PARSER = Parser.builder().extensions(List.of(TablesExtension.create())).build();

    public static DocText analyze(String body) {
        if (body == null || body.isBlank()) {
            return new DocText("", List.of());
        }
        Node document = PARSER.parse(body);
        StringBuilder text = new StringBuilder();
        List<String> headings = new ArrayList<>();
        document.accept(new AbstractVisitor() {
            @Override
            protected void visitChildren(Node parent) {
                Node node = parent.getFirstChild();
                while (node != null) {
                    Node next = node.getNext();
                    if (node instanceof Text textNode) {
                        text.append(textNode.getLiteral()).append(' ');
                    } else if (node instanceof Heading heading) {
                        StringBuilder title = new StringBuilder();
                        collectText(heading, title);
                        headings.add(title.toString().strip());
                    }
                    visitChildren(node);
                    node = next;
                }
            }
        });
        return new DocText(text.toString().replaceAll("\\s+", " ").strip(), headings);
    }

    private static void collectText(Node node, StringBuilder out) {
        Node child = node.getFirstChild();
        while (child != null) {
            if (child instanceof Text textNode) {
                out.append(textNode.getLiteral());
            }
            collectText(child, out);
            child = child.getNext();
        }
    }
}
