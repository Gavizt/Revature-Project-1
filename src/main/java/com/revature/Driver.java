package com.revature;

import com.revature.model.ReimbursementTicket;
import com.revature.model.User;
import io.javalin.Javalin;
import io.javalin.http.HttpStatus;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Driver class for running the app.
 */
public class Driver {

    public static void main(String[] args) {

        Javalin app = Javalin.create().start(8002);

        /*
        Collections for Users and Tickets
        TODO Use SQL database for Users and Tickets
         */
        List<User> users = new LinkedList<>();
        List<ReimbursementTicket> pendingTickets = new LinkedList<>();
        List<ReimbursementTicket> processedTickets = new LinkedList<>();
//        TODO Assign a unique id to each User and Ticket
        AtomicLong nextUserId = new AtomicLong();
        AtomicLong nextTicketId = new AtomicLong();
        nextUserId.set(1);
        nextTicketId.set(1);

        // Create an account (new User instance)
        app.post("/register", ctx -> {
            User receivedUser = ctx.bodyAsClass(User.class);
            // Enforce a username and password to be present
            if (receivedUser.getUsername() == null &&
            receivedUser.getPassword() == null) {
                System.out.println("\nNo username or password for\n "
                        + receivedUser);

                ctx.status(HttpStatus.NOT_ACCEPTABLE);
            } else {
                if (getUser(receivedUser, users) != null) {
                    System.out.println("\nUsername already exists\n "
                            + receivedUser);

                    ctx.status(HttpStatus.NOT_ACCEPTABLE);
                } else {
                    /*
                    User is valid, enforce default Employee role
                                   assign unique id
                     */
                    receivedUser.setRole("Employee");
                    receivedUser.setId(nextUserId.getAndIncrement());
                    users.add(receivedUser);

                    System.out.println("\nAdded\n "
                            + receivedUser);

                    ctx.status(HttpStatus.CREATED);
                }
            }
        });

        // Create a ticket (new ReimbursementTicket instance)
        app.post("/reimbursement", ctx -> {
            ReimbursementTicket receivedTicket = ctx.bodyAsClass(ReimbursementTicket.class);
            // Enforce an associated User id
            if (receivedTicket.getAssociatedUserId() < 1) {
                System.out.println("\nNo associated user id for\n "
                        + receivedTicket);

                ctx.status(HttpStatus.NOT_ACCEPTABLE);
            } else {
                /*
                ReimbursementTicket is valid, enforce "Pending" status
                                              assign unique id
                 */
                receivedTicket.setStatus("Pending");
                receivedTicket.setId(nextTicketId.getAndIncrement());
                pendingTickets.add(receivedTicket);

                System.out.println("\nAdded\n "
                        + receivedTicket);

                ctx.status(HttpStatus.CREATED);
            }
        });

        // Change role of a User
        // TODO Only allow managers to change User roles (with sessions)
        app.post("/role/{username}/{role}", ctx -> {
            String receivedUsername = ctx.pathParam("username");
            String receivedRole = ctx.pathParam("role");

            System.out.println("\nReceived " + receivedUsername +
                    ", " + receivedRole);

            // Check if the User exists
            User foundUser = getUser(receivedUsername, users);
            if (foundUser == null) {
                System.out.println("\nRole of " + receivedUsername +
                        " could not be changed.");

                ctx.status(HttpStatus.BAD_REQUEST);
            } else {
                // Change the role
                boolean isValidRole = true;

                System.out.println("Changing role\n " + foundUser);
                switch (receivedRole.toLowerCase()) {
                    case "manager":
                        foundUser.setRole("Manager");
                        break;
                    case "employee":
                        foundUser.setRole("Employee");
                        break;
                    default:
                        // The input role is wrong, do nothing
                        isValidRole = false;
                }

                if (isValidRole) {
                    System.out.println("\tto\n " + foundUser);

                    ctx.status(HttpStatus.ACCEPTED);
                } else {
                    System.out.println("\tInvalid role " + receivedRole
                            + ".\n\t" + receivedUsername + "'s role not changed.");

                    ctx.status(HttpStatus.NOT_MODIFIED);
                }
            }
        });

        // Change status of a ReimbursementTicket (process it)
        app.post("/status/{ticketId}/{status}", ctx -> {
            String receivedTicketId = ctx.pathParam("ticketId");
            String receivedStatus = ctx.pathParam("status");

            System.out.println("\nReceived " + receivedTicketId +
                    ", " + receivedStatus);

            // Try parsing a long from the ticketId param
            long parsedTicketId = 0;
            boolean isValidTicketId = true;
            try {
                parsedTicketId = Long.parseLong(receivedTicketId);
            } catch (NumberFormatException e) {
                System.out.println(" Could not parse a numeric from " + receivedTicketId);
                isValidTicketId = false;

                ctx.status(HttpStatus.NOT_ACCEPTABLE);
            }


            if (isValidTicketId) {
                // Check if the ReimbursementTicket exists
                ReimbursementTicket foundTicket = getReimbursementTicket(parsedTicketId, pendingTickets);
                if (foundTicket != null) {
                    boolean isValidStatus = true;
                    switch (receivedStatus.toLowerCase()) {
                        case "approved":
                            foundTicket.setStatus("Approved");
                            break;
                        case "denied":
                            foundTicket.setStatus("Denied");
                            break;
                        default:
                            // The input status is wrong, do nothing
                            isValidStatus = false;
                            break;
                    }

                    if (isValidStatus) {
                        processedTickets.add(foundTicket);
                        pendingTickets.remove(foundTicket);
                    }
                }
            }

        });

        // List Users (Employees and Managers)
        // TODO Remove this expression! Testing purposes only.
        app.get("/users", ctx -> {
            System.out.println("\nUsers:");
            if (users.size() == 0) {
                System.out.println(" There are no registered users.");
            } else {
                for (User u:
                        users) {
                    System.out.println("\t" + u);
                }
            }

            ctx.status(HttpStatus.OK);
        });





        // Stop the app
        // TODO TODO Remove this expression! Testing purposes only.
        app.post("/close", ctx -> {
            System.out.println("\nClosing the app...");
            app.close();
        });

    }

    private static User getUser(User user, List<User> users) {
        return getUser(user.getUsername(), users);
    }

    private static User getUser(String username, List<User> users) {
        for (User u:
                users) {
            if (u.getUsername().equals(username)) {
                return u;
            }
        }

        return null;
    }

    private static User getUser(long id, List<User> users) {
        for (User u:
             users) {
            if (u.getId() == id) {
                return u;
            }
        }

        return null;
    }

    private static ReimbursementTicket getReimbursementTicket(ReimbursementTicket ticket, List<ReimbursementTicket> tickets) {
        for (ReimbursementTicket t:
             tickets) {
            if (t.equals(ticket)) {
                return t;
            }
        }

        return null;
    }

    private static ReimbursementTicket getReimbursementTicket(long ticketId, List<ReimbursementTicket> tickets) {
        for (ReimbursementTicket t:
             tickets) {
            if (t.getId() == ticketId) {
                return t;
            }
        }

        return null;
    }
}
