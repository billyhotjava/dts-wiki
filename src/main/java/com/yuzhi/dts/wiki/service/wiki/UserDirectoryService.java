package com.yuzhi.dts.wiki.service.wiki;

import com.yuzhi.dts.wiki.domain.User;
import com.yuzhi.dts.wiki.repository.UserRepository;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * User directory for @mention candidates (E9). Only users visible to a reader of
 * the space are returned; login and display name only, never credentials.
 */
@Service
@Transactional(readOnly = true)
public class UserDirectoryService {

    public record MentionCandidate(String login, String name) {}

    private final UserRepository userRepository;
    private final SpaceAccessService spaceAccessService;

    public UserDirectoryService(UserRepository userRepository, SpaceAccessService spaceAccessService) {
        this.userRepository = userRepository;
        this.spaceAccessService = spaceAccessService;
    }

    public List<MentionCandidate> mentionCandidates(String query, String spaceSlug) {
        if (spaceSlug == null || spaceSlug.isBlank()) { throw new IllegalArgumentException("A space is required"); }
        String authority = spaceAccessService.requiredReadAuthority(spaceSlug);
        String q = query == null ? "" : query.strip();
        if (q.length() > 100) { throw new IllegalArgumentException("Mention query is too long"); }
        List<User> users = userRepository.findMentionCandidates(authority, q, Pageable.ofSize(20));
        return users.stream().map(u -> new MentionCandidate(u.getLogin(), displayName(u))).toList();
    }

    private static String displayName(User user) {
        String name = ((user.getFirstName() == null ? "" : user.getFirstName()) + (user.getLastName() == null ? "" : user.getLastName())).trim();
        return name.isEmpty() ? user.getLogin() : name;
    }
}
