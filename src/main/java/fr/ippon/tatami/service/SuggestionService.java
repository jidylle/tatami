package fr.ippon.tatami.service;

import fr.ippon.tatami.domain.Group;
import fr.ippon.tatami.domain.User;
import fr.ippon.tatami.repository.UserGroupRepository;
import fr.ippon.tatami.service.util.DomainUtil;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.*;

import static fr.ippon.tatami.service.util.AnalysisUtil.*;

/**
 * Analyses data to find suggestions of users, groups... for the current user.
 */
@Service
public class SuggestionService {

    private static final int SAMPLE_SIZE = 20;

    private static final int SUB_SAMPLE_SIZE = 100;

    @Inject
    private FriendshipService friendshipService;

    @Inject
    private UserGroupRepository userGroupRepository;

    @Inject
    private UserService userService;

    @Inject
    private GroupService groupService;

    /**
     * Size of the sample data used to find the suggestions.
     */

    @Cacheable("suggest-users-cache")
    public Collection<User> suggestUsers(String login) {
        Map<String, Integer> userCount = new HashMap<String, Integer>();
        List<String> friendIds = friendshipService.getFriendIdsForUser(login);
        friendIds = reduceCollectionSize(friendIds, SAMPLE_SIZE);
        for (String friendId : friendIds) {
            List<String> friendsOfFriend = friendshipService.getFriendIdsForUser(friendId);
            friendsOfFriend = reduceCollectionSize(friendsOfFriend, SUB_SAMPLE_SIZE);
            for (String friendOfFriend : friendsOfFriend) {
                if (!friendIds.contains(friendOfFriend) && !friendOfFriend.equals(login)) {
                    incrementKeyCounterInMap(userCount, friendOfFriend);
                }
            }
        }
        List<String> mostFollowedUsers = findMostUsedKeys(userCount);
        Collection<User> suggestions = new ArrayList<User>();
        for (String mostFollowedUser : mostFollowedUsers) {
            User suggestion = userService.getUserByLogin(mostFollowedUser);
            suggestions.add(suggestion);
        }
        return suggestions;
    }

    @Cacheable("suggest-groups-cache")
    public Collection<Group> suggestGroups(String login) {
        Map<String, Integer> groupCount = new HashMap<String, Integer>();
        List<String> groupIds = userGroupRepository.findGroups(login);
        List<String> friendIds = friendshipService.getFriendIdsForUser(login);
        friendIds = reduceCollectionSize(friendIds, SAMPLE_SIZE);
        for (String friendId : friendIds) {
            List<String> groupsOfFriend = userGroupRepository.findGroups(friendId);
            for (String groupOfFriend : groupsOfFriend) {
                if (!groupIds.contains(groupOfFriend)) {
                    incrementKeyCounterInMap(groupCount, groupOfFriend);
                }
            }
        }
        List<String> mostFollowedGroups = findMostUsedKeys(groupCount);
        Collection<Group> suggestions = new ArrayList<Group>();
        String domain = DomainUtil.getDomainFromLogin(login);
        for (String mostFollowedGroup : mostFollowedGroups) {
            Group suggestion = groupService.getGroupById(domain, mostFollowedGroup);
            suggestions.add(suggestion);
        }
        return suggestions;
    }


}
