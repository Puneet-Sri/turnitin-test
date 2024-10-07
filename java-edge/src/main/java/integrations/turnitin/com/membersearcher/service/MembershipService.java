package integrations.turnitin.com.membersearcher.service;

import integrations.turnitin.com.membersearcher.client.MembershipBackendClient;
import integrations.turnitin.com.membersearcher.model.Membership;
import integrations.turnitin.com.membersearcher.model.MembershipList;
import integrations.turnitin.com.membersearcher.model.User;
import integrations.turnitin.com.membersearcher.model.UserList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Service
public class MembershipService {
	@Autowired
	private MembershipBackendClient membershipBackendClient;

	/**
	 * Method to fetch all memberships with their associated user details included.
	 * This method calls out to the php-backend service and fetches all memberships,
	 * it then calls to fetch the user details for each user individually and
	 * associates them with their corresponding membership.
	 *
	 * @return A CompletableFuture containing a fully populated MembershipList object.
	 */
	public CompletableFuture<MembershipList> fetchAllMembershipsWithUsers() throws ExecutionException, InterruptedException {
		// Fetching userList before hand, to avoid multiple calls to the backEnd for each user.
		UserList userList =  membershipBackendClient.fetchUsers().join();
		return membershipBackendClient.fetchMemberships()
				.thenCompose(members -> {
					CompletableFuture<?>[] userCalls = members.getMemberships().stream()
							.map(member -> getMemberWithUserPopulated(member,userList))
							.toArray(CompletableFuture<?>[]::new);
					return CompletableFuture.allOf(userCalls)
							.thenApply(nil -> members);
				});
	}

	private CompletableFuture<?> getMemberWithUserPopulated(Membership member, UserList userList) {
		Optional<User> optionalUser = userList.getUsers().stream().filter(user -> user.getId().equals(member.getUserId())).findFirst();
        optionalUser.ifPresent(member::setUser);
		return CompletableFuture.completedFuture(member);
	}


}
