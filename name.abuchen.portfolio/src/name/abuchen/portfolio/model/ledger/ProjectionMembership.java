package name.abuchen.portfolio.model.ledger;

import java.util.Objects;

/**
 * Links one Ledger posting to a projection with an explicit projection-local role.
 */
public class ProjectionMembership
{
    private String postingUUID;
    private ProjectionMembershipRole role;

    public ProjectionMembership(String postingUUID, ProjectionMembershipRole role)
    {
        this.postingUUID = Objects.requireNonNull(postingUUID);
        this.role = Objects.requireNonNull(role);
    }

    public String getPostingUUID()
    {
        return postingUUID;
    }

    public void setPostingUUID(String postingUUID)
    {
        this.postingUUID = Objects.requireNonNull(postingUUID);
    }

    public ProjectionMembershipRole getRole()
    {
        return role;
    }

    public void setRole(ProjectionMembershipRole role)
    {
        this.role = Objects.requireNonNull(role);
    }
}
