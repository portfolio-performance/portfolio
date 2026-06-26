package name.abuchen.portfolio.model.ledger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Portfolio;

/**
 * Links a persisted Ledger entry to a runtime legacy transaction projection.
 * This is internal Ledger model data. Projection references identify views; they are not a
 * second persisted transaction truth.
 */
public class LedgerProjectionRef
{
    private String uuid;
    private LedgerProjectionRole role;
    private Account account;
    private Portfolio portfolio;
    private String primaryPostingUUID;
    private String postingGroupUUID;
    private List<ProjectionMembership> memberships = new ArrayList<>();

    public LedgerProjectionRef()
    {
        this(UUID.randomUUID().toString());
    }

    public LedgerProjectionRef(String uuid)
    {
        this.uuid = Objects.requireNonNull(uuid);
    }

    public String getUUID()
    {
        return uuid;
    }

    public void setUUID(String uuid)
    {
        this.uuid = Objects.requireNonNull(uuid);
    }

    public LedgerProjectionRole getRole()
    {
        return role;
    }

    public void setRole(LedgerProjectionRole role)
    {
        this.role = role;
    }

    public Account getAccount()
    {
        return account;
    }

    public void setAccount(Account account)
    {
        this.account = account;
    }

    public Portfolio getPortfolio()
    {
        return portfolio;
    }

    public void setPortfolio(Portfolio portfolio)
    {
        this.portfolio = portfolio;
    }

    public String getPrimaryPostingUUID()
    {
        return primaryPostingUUID;
    }

    public void setPrimaryPostingUUID(String primaryPostingUUID)
    {
        this.primaryPostingUUID = primaryPostingUUID;
    }

    public void setPrimaryPostingTargetUUID(String primaryPostingUUID)
    {
        setPrimaryPostingUUID(primaryPostingUUID);
        setMembership(ProjectionMembershipRole.PRIMARY, primaryPostingUUID);
    }

    public void setPrimaryPosting(LedgerPosting posting)
    {
        setPrimaryPostingTargetUUID(Objects.requireNonNull(posting).getUUID());
    }

    public String getPostingGroupUUID()
    {
        return postingGroupUUID;
    }

    public void setPostingGroupUUID(String postingGroupUUID)
    {
        this.postingGroupUUID = postingGroupUUID;
    }

    public void setPostingGroupTargetUUID(String postingGroupUUID)
    {
        setPostingGroupUUID(postingGroupUUID);
        setMembership(ProjectionMembershipRole.GROUP_ANCHOR, postingGroupUUID);
    }

    public void setPostingGroup(LedgerPosting posting)
    {
        setPostingGroupTargetUUID(Objects.requireNonNull(posting).getUUID());
    }

    public List<ProjectionMembership> getMemberships()
    {
        return Collections.unmodifiableList(memberships());
    }

    public void addMembership(ProjectionMembership membership)
    {
        memberships().add(Objects.requireNonNull(membership));
    }

    public ProjectionMembership addMembership(String postingUUID, ProjectionMembershipRole role)
    {
        var membership = new ProjectionMembership(postingUUID, role);

        addMembership(membership);

        return membership;
    }

    public Optional<ProjectionMembership> getPrimaryMembership()
    {
        return memberships().stream().filter(membership -> membership.getRole() == ProjectionMembershipRole.PRIMARY)
                        .findFirst();
    }

    public List<ProjectionMembership> getMembershipsByRole(ProjectionMembershipRole role)
    {
        Objects.requireNonNull(role);
        return memberships().stream().filter(membership -> membership.getRole() == role).toList();
    }

    public boolean hasMembershipRole(ProjectionMembershipRole role)
    {
        Objects.requireNonNull(role);
        return memberships().stream().anyMatch(membership -> membership.getRole() == role);
    }

    public void removeMembershipsForPostingUUID(String postingUUID)
    {
        memberships().removeIf(membership -> Objects.equals(membership.getPostingUUID(), postingUUID));
    }

    private List<ProjectionMembership> memberships()
    {
        if (memberships == null)
            memberships = new ArrayList<>();

        return memberships;
    }

    private void setMembership(ProjectionMembershipRole role, String postingUUID)
    {
        Objects.requireNonNull(role);

        memberships().removeIf(membership -> membership.getRole() == role);

        if (postingUUID != null)
            addMembership(postingUUID, role);
    }
}
