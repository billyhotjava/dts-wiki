package com.yuzhi.dts.wiki.service.wiki;

import com.yuzhi.dts.wiki.domain.Comment;
import com.yuzhi.dts.wiki.domain.User;
import com.yuzhi.dts.wiki.repository.CommentRepository;
import com.yuzhi.dts.wiki.security.SecurityUtils;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WikiCommentService {
    public record CommentView(long id, Long parentId, String bodyMd, String authorLogin, String authorName, Instant createdAt,
        Instant updatedAt, boolean deleted, boolean resolved, boolean canEdit, List<CommentView> replies) {}
    public record Comments(List<CommentView> items, long total) {}
    private final CommentRepository comments;
    private final WikiPersonalService personal;
    private final WikiNotificationIntents intents;
    public WikiCommentService(CommentRepository comments, WikiPersonalService personal, WikiNotificationIntents intents) {
        this.comments = comments; this.personal = personal; this.intents = intents;
    }
    @Transactional(readOnly = true)
    public Comments list(long pageId, int page, int size) {
        personal.visible(pageId);
        if (page < 0 || page > 10000 || size < 1 || size > 50) throw new IllegalArgumentException("Invalid comment pagination");
        var roots = comments.findThreads(pageId, PageRequest.of(page, size));
        return new Comments(roots.map(root -> view(root, comments.findReplies(root.getId()).stream().map(reply -> view(reply, List.of())).toList())).getContent(), roots.getTotalElements());
    }
    @Transactional
    public CommentView create(long pageId, String body, Long parentId) {
        validateBody(body); var page = personal.visible(pageId); User actor = personal.current(); Comment parent = null;
        if (parentId != null) {
            parent = comments.findForUpdate(parentId).orElseThrow(() -> new SpaceNotVisibleException("comment"));
            if (!parent.getPage().getId().equals(pageId) || parent.getDeletedAt() != null || parent.getParent() != null) throw new IllegalArgumentException("Reply requires a live root comment on the same page");
            if (comments.countByParentId(parentId) >= 200) throw new IllegalArgumentException("A thread is limited to 200 replies");
        }
        Comment comment = comments.saveAndFlush(new Comment().page(page).author(actor).parent(parent).bodyMd(body).createdAt(Instant.now()));
        intents.comment(page, comment.getId(), body, actor, parent == null ? null : parent.getAuthor());
        return view(comment, List.of());
    }
    @Transactional
    public CommentView update(long id, String body, Boolean resolved) {
        Comment comment = mutable(id);
        if (body != null) { validateBody(body); comment.setBodyMd(body); }
        if (resolved != null) {
            if (comment.getParent() != null) throw new IllegalArgumentException("Only threads can be resolved");
            comment.setResolvedAt(resolved ? Instant.now() : null);
        }
        comment.setUpdatedAt(Instant.now()); comments.save(comment);
        if (body != null) intents.comment(comment.getPage(), comment.getId(), body, personal.current(), comment.getParent() == null ? null : comment.getParent().getAuthor());
        return view(comment, List.of());
    }
    @Transactional
    public void delete(long id) { Comment comment = mutable(id); comment.setDeletedAt(Instant.now()); comments.save(comment); }
    private Comment mutable(long id) {
        Comment comment = comments.findForUpdate(id).filter(value -> value.getDeletedAt() == null).orElseThrow(() -> new SpaceNotVisibleException("comment"));
        personal.visible(comment.getPage().getId()); if (!owner(comment)) throw new AccessDeniedException("Only the author or administrator can change a comment"); return comment;
    }
    private boolean owner(Comment comment) {
        return SecurityUtils.hasCurrentUserThisAuthority("ROLE_ADMIN") || SecurityUtils.getCurrentUserLogin().filter(comment.getAuthor().getLogin()::equals).isPresent();
    }
    private CommentView view(Comment comment, List<CommentView> replies) {
        boolean deleted = comment.getDeletedAt() != null; User author = comment.getAuthor();
        String name = ((author.getFirstName() == null ? "" : author.getFirstName()) + " " + (author.getLastName() == null ? "" : author.getLastName())).strip();
        return new CommentView(comment.getId(), comment.getParent() == null ? null : comment.getParent().getId(), deleted ? "" : comment.getBodyMd(),
            author.getLogin(), name.isBlank() ? author.getLogin() : name, comment.getCreatedAt(), comment.getUpdatedAt(), deleted,
            comment.getResolvedAt() != null, !deleted && owner(comment), replies);
    }
    private static void validateBody(String body) { if (body == null || body.isBlank() || body.length() > 20000) throw new IllegalArgumentException("Comment must contain 1–20000 characters"); }
}
