package com.yuzhi.dts.wiki.service.wiki;

import com.yuzhi.dts.wiki.domain.User;
import com.yuzhi.dts.wiki.repository.UserRepository;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

/**
 * User directory for @mention candidates (E9). Only users visible to a reader of
 * the space are returned; login and display name only, never credentials.
 */
@Service
public class UserDirectoryService {

    public record MentionCandidate(String id, String login, String displayName) {}

    private final UserRepository userRepository;
    private final SpaceAccessService spaceAccessService;
    private final CurrentIdentityService identity;

    public UserDirectoryService(UserRepository userRepository, SpaceAccessService spaceAccessService, CurrentIdentityService identity) {
        this.userRepository = userRepository;
        this.spaceAccessService = spaceAccessService;
        this.identity = identity;
    }

    public List<MentionCandidate> mentionCandidates(String query, String spaceSlug) {
        if (spaceSlug == null || spaceSlug.isBlank()) { throw new IllegalArgumentException("A space is required"); }
        String authority = spaceAccessService.requiredReadAuthority(spaceSlug);
        String q = query == null ? "" : query.strip();
        if (q.length() > 100) { throw new IllegalArgumentException("Mention query is too long"); }
        if (q.isBlank()) return List.of();
        List<User> users = userRepository.findMentionDisplayCandidates(q, Pageable.ofSize(20));
        var result = new java.util.ArrayList<MentionCandidate>();
        for (User user : users) {
            var current = identity.lookup(user.getId());
            if (current.enabled() && (current.authorities().contains("ROLE_ADMIN") || current.authorities().contains(authority))) {
                result.add(new MentionCandidate(user.getId(), user.getLogin(), displayName(user)));
                if (result.size() == 20) break;
            }
        }
        return List.copyOf(result);
    }

    private static String displayName(User user) {
        String name = ((user.getFirstName() == null ? "" : user.getFirstName()) + (user.getLastName() == null ? "" : user.getLastName())).trim();
        return name.isEmpty() ? user.getLogin() : name;
    }
}
