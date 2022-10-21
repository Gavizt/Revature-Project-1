package com.revature.model;

import java.util.Objects;

/**
 * ReimbursementTicket class for tickets in the system.
 *
 * @author johnainsworth
 */
public class ReimbursementTicket {
    private long id;
    private long associatedUserId;
    private String status;
    private double amount;
    private String description;


    public ReimbursementTicket() {
        super();
    }

    public ReimbursementTicket(long id, long associatedUserId, String status, double amount, String description) {
        this.id = id;
        this.associatedUserId = associatedUserId;
        this.status = status;
        this.amount = amount;
        this.description = description;
    }


    public long getId() {
        return this.id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getAssociatedUserId() {
        return this.associatedUserId;
    }

    public void setAssociatedUserId(long associatedUserId) {
        this.associatedUserId = associatedUserId;
    }

    public String getStatus() {
        return this.status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public double getAmount() {
        return this.amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getDescription() {
        return this.description;
    }

    public void setDescription(String description) {
        this.description = description;
    }


    // Objects are differed by their unique id.
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ReimbursementTicket that = (ReimbursementTicket) o;
        return id == that.id;
    }

    // Generate hashcode with object's id.
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Ticket: " +
                "id= " + id +
                ",\tuserId= " + associatedUserId +
                ",\tstatus= '" + status + '\'' +
                ",\t$" + amount +
                "\n\t  description= '" + description + '\'';
    }
}
