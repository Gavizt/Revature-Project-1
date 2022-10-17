package com.revature.model;

import java.util.Objects;

/**
 * ReimbursementTicket class for tickets in the system.
 *      Each has a unique id and is associated to a particular User.
 *      Status is automatically set to "Pending" when created.
 *
 * @author johnainsworth
 */
public class ReimbursementTicket {
    private long id;
    private int amount;
    private String status;
    private long associatedUserId;
    private String description;


    public ReimbursementTicket() {
        super();
    }

    public ReimbursementTicket(long id, int amount, String status, long associatedUserId, String description) {
        this.id = id;
        this.amount = amount;
        this.status = status;
        this.associatedUserId = associatedUserId;
        this.description = description;
    }


    public long getId() {
        return this.id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public int getAmount() {
        return this.amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    public String getStatus() {
        return this.status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public long getAssociatedUserId() {
        return this.associatedUserId;
    }

    public void setAssociatedUserId(long associatedUserId) {
        this.associatedUserId = associatedUserId;
    }

    public String getDescription() {
        return this.description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    /*
    Objects are differed by their unique id.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ReimbursementTicket that = (ReimbursementTicket) o;
        return id == that.id;
    }

    /*
    Generate hashcode with object's id.
     */
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "ReimbursementTicket {" +
                "id=\t" + id +
                ", amount=\t" + amount +
                ", status=\t'" + status + '\'' +
                ", associatedUserId=\t" + associatedUserId +
                "\n\tdescription=\t'" + description + '\'' +
                '}';
    }
}
