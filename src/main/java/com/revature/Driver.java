package com.revature;

import com.revature.model.ReimbursementTicket;
import com.revature.model.User;
import io.javalin.Javalin;
import io.javalin.http.HttpStatus;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static com.revature.util.DriverUtils.*;

/**
 * Driver class for running the app.
 */
public class Driver {

    public static void main(String[] args) {

        Javalin app = Javalin.create().start(8001);

        /*
        Collections for Users and Tickets
        TODO Use SQL database for Users and Tickets
         */
        List<User> users = new LinkedList<>();
        List<ReimbursementTicket> reimbursementTickets = new LinkedList<>();
//        TODO Assign a unique id to each User and Ticket
        AtomicLong nextUserId = new AtomicLong();
        AtomicLong nextTicketId = new AtomicLong();
        nextUserId.set(1);
        nextTicketId.set(1);

        // TODO Add HTTP statuses

        // Create an account (new User instance as Employee)
        app.post("/register", ctx -> {
            User receivedUser = ctx.bodyAsClass(User.class);

            System.out.println("\nREGISTER NEW ACCOUNT");
            System.out.println(formatReceived(receivedUser.toString()));

            // Enforce a username and password to be present
            if (receivedUser.getUsername() == null &&
            receivedUser.getPassword() == null) {
                System.out.println("\n No username or password for\n "
                        + receivedUser);

                ctx.status(HttpStatus.NOT_ACCEPTABLE);
            } else {
                if (getUser(receivedUser, users) != null) {
                    System.out.println("\n Username already exists\n "
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

                    System.out.println("\n\tAdded\n\t "
                            + receivedUser);

                    ctx.status(HttpStatus.CREATED);
                }
            }
        });

        // Create a ticket (new ReimbursementTicket instance)
        // TODO Test me!
        app.post("/reimbursement/submit", ctx -> {
            ReimbursementTicket receivedTicket = ctx.bodyAsClass(ReimbursementTicket.class);

            System.out.println("\nREIMBURSEMENT TICKET SUBMIT");
            System.out.println(formatReceived(receivedTicket.toString()));

            boolean isValidAssociatedUserId =
                    receivedTicket.getAssociatedUserId() > 0 &&
                    getUser(receivedTicket.getAssociatedUserId(), users) != null;
            boolean isValidAmount = receivedTicket.getAmount() <= 0;
            boolean isValidDescription = receivedTicket.getDescription().isBlank();
            boolean isValidTicket =
                    isValidAssociatedUserId &&
                    isValidAmount &&
                    isValidDescription;

            if (isValidAssociatedUserId) {
                System.out.println("\n User id " +
                        receivedTicket.getAssociatedUserId() +
                        " not found.");
            }

            if (isValidAmount) {
                System.out.println("\n Amount $" + receivedTicket +
                        " not greater than $0.00.");
            }

            if (isValidDescription) {
                System.out.println("\n Description cannot be blank.");
            }

            if (isValidTicket) {
                receivedTicket.setId(nextTicketId.getAndIncrement());
                receivedTicket.setStatus("Pending");
                reimbursementTickets.add(receivedTicket);

                System.out.println("\n\tSubmitted\n\t "
                        + receivedTicket);

                ctx.status(HttpStatus.CREATED);
            } else {
                ctx.status(HttpStatus.NOT_ACCEPTABLE);
            }
        });

        // Process a ticket (change status to "Approved" or "Denied")
        // TODO Test me!
        app.post("/reimbursement/process", ctx -> {
            String receivedTicketId = ctx.queryParam("id");
            String receivedManagerChoice = ctx.queryParam("managerChoice");

            System.out.println("\nREIMBURSEMENT TICKET PROCESSING");
            System.out.println(formatReceived(
                    receivedTicketId,
                    receivedManagerChoice
            ));

            boolean receivedTicketIdExists = receivedTicketId != null;
            boolean receivedManagerChoiceExists = receivedManagerChoice != null;
            boolean queryParamsExist = receivedTicketIdExists && receivedManagerChoiceExists;

            if (!receivedTicketIdExists) {
                System.out.println(" No ticket ID has been entered.");
            }

            if (!receivedManagerChoiceExists) {
                System.out.println(" No manager choice for the ticket has been entered.");
            }

            if (queryParamsExist) {
                long parsedTicketId = -1;
                ReimbursementTicket foundTicket = null;
                boolean isValidTicketId = true;
                boolean isValidManagerChoice =
                        receivedManagerChoice.equalsIgnoreCase("approve") ||
                        receivedManagerChoice.equalsIgnoreCase("deny");
                boolean isTicketProcessable = true;
                boolean allIsGood = false;

                try {
                    parsedTicketId = Long.parseLong(receivedTicketId);
                    foundTicket = getReimbursementTicket(parsedTicketId, reimbursementTickets);

                    isTicketProcessable = foundTicket.getStatus().equalsIgnoreCase("pending");
                    if (!isTicketProcessable) {
                        System.out.println(" Ticket " + foundTicket.getId() +
                                " is already '" + foundTicket.getStatus() + "'");
                    }
                } catch (NumberFormatException e) {
                    isValidTicketId = false;
                    System.out.println(" Ticket ID " + receivedTicketId +
                            " is not valid.");
                } catch (NullPointerException e) {
                    isTicketProcessable = false;
                    System.out.println(" Could not find a ticket with ID " +
                            receivedTicketId + ".");
                }

                allIsGood = isValidTicketId && isValidManagerChoice && isTicketProcessable;

                if (allIsGood) {
                    switch (receivedManagerChoice.toLowerCase()) {
                        case "approve":
                            foundTicket.setStatus("Approved");
                            break;
                        case "deny":
                            foundTicket.setStatus("Denied");
                            break;
                    }

                    System.out.println("\t" + foundTicket.getStatus() +
                            " ticket with ID " + foundTicket.getId());

                    ctx.status(HttpStatus.ACCEPTED);
                } else {
                    ctx.status(HttpStatus.NOT_ACCEPTABLE);
                }

            }
        });

        // Change role of a User
        // TODO Only allow managers to change User roles (with sessions)
        app.post("/role", ctx -> {
            String receivedUsername = ctx.queryParam("username");
            String receivedNewRole = ctx.queryParam("newRole");

            System.out.println("\nCHANGE ROLE");
            System.out.println(formatReceived(
                    receivedUsername,
                    receivedNewRole
            ));

            User foundUser = getUser(receivedUsername, users);
            boolean userExists = foundUser != null;
            boolean newRoleExists = receivedNewRole != null;
            boolean isValidNewRole = true;

            if (!userExists) {
                System.out.println("\n User " + receivedUsername + " not found.");
            }

            if (!newRoleExists) {
                System.out.println("\n New role not entered.");
            }

            if (userExists && newRoleExists) {
                try {
                    isValidNewRole =
                            receivedNewRole.equalsIgnoreCase("employee") ||
                            receivedNewRole.equalsIgnoreCase("manager");
                } catch (NullPointerException e) {
                    isValidNewRole = false;
                } finally {
                    if (!isValidNewRole) {
                        System.out.println("\n\t" + receivedNewRole +
                                " is not a valid role");
                    }
                }
            }

            if (userExists && isValidNewRole) {
                // Good to go, change the role
                System.out.println(" Setting " + receivedUsername + "'s role from " +
                        foundUser.getRole() + "\n\tto");

                switch (receivedNewRole.toLowerCase()) {
                    case "employee":
                        foundUser.setRole("Employee");
                        break;
                    case "manager":
                        foundUser.setRole("Manager");
                        break;
                }

                System.out.println(" " + foundUser.getRole());

                ctx.status(HttpStatus.ACCEPTED);
            } else {
                ctx.status(HttpStatus.NOT_MODIFIED);
            }
        });

        // List Users (Employees and Managers)
        // TODO Remove this expression! Testing purposes only.
        app.get("/users", ctx -> {

            System.out.println("\nLIST USERS");

            if (users.isEmpty()) {
                System.out.println(" There are no Users");
            } else {
                System.out.println(" " + users.size() + " Users");
                System.out.println(" Managers:");
                for (User u:
                     users) {
                    System.out.println("\t" + u);
                }
                System.out.println(" Employees:");
                for (User u:
                     users) {
                    System.out.println("\t" + u);
                }
            }
        });

        // List Tickets (ReimbursementTickets)
        // TODO Remove this expression! Testing purposes only.
        // TODO Test me!
        app.get("/tickets", ctx -> {
            String receivedStatus = ctx.queryParam("status");
            String receivedUsername = ctx.queryParam("username");

            System.out.println("\nLIST TICKETS");
            System.out.println(formatReceived(receivedStatus, receivedUsername));

            /*
             * Four cases with the two inputs (status|username):
             *  (status & username): print tickets of status by username.
             *  (status & !username): print tickets of status.
             *  (!status & username): print tickets by username.
             *  (!status & !username): print tickets.
             */

            // Determine which case to execute:
            boolean isValidStatus = true;
            boolean isValidUsername = false;
            User foundUser = null;
            if (stringsExist(receivedStatus)) {
                try {
                    isValidStatus =
                            receivedStatus.equalsIgnoreCase("pending") ||
                            receivedStatus.equalsIgnoreCase("approved") ||
                            receivedStatus.equalsIgnoreCase("denied");
                } catch (NullPointerException e) {
                    isValidStatus = false;
                }
            }
            if (stringsExist(receivedUsername)) {
                foundUser = getUser(receivedUsername, users);
                isValidUsername = foundUser != null;
            }

            if (isValidStatus && !isValidUsername) {
                // (status & !username): print tickets of status.
                printReimbursementTickets(receivedStatus, reimbursementTickets);
            } else if (!isValidStatus && isValidUsername) {
                // (!status & username): print tickets by username.
                printReimbursementTickets(foundUser, reimbursementTickets);
            } else if (isValidStatus && isValidUsername) {
                // (status & username): print tickets of status by username.
                printReimbursementTickets(foundUser, receivedStatus, reimbursementTickets);
            } else {
                // (!status & !username): print tickets.
                for (ReimbursementTicket t:
                     reimbursementTickets) {
                    System.out.println("\tt");
                }
            }

        });

        // Stop the app
        // TODO TODO Remove this expression! Testing purposes only.
        app.post("/close", ctx -> {
            System.out.println("\nClosing the app...");
            app.close();
        });

    }
}
