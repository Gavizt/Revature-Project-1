package com.revature;

import com.revature.model.ReimbursementTicket;
import com.revature.model.User;
import io.javalin.Javalin;
import io.javalin.http.HttpStatus;
import jakarta.servlet.http.HttpSession;

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

        // TODO Use SQL database for Users and Tickets
        // Collections for Users and Tickets
        List<User> users = new LinkedList<>();
        List<ReimbursementTicket> reimbursementTickets = new LinkedList<>();
//        TODO Remove these ID variables when SQL db is integrated.
        AtomicLong nextUserId = new AtomicLong();
        AtomicLong nextTicketId = new AtomicLong();
        nextUserId.set(1);
        nextTicketId.set(1);

        // TODO Add HTTP statuses
        // TODO Integrate HTTP Sessions
        // TODO use ctx.json() to output to Postman where appropriate

        // Register
        app.post("/register", ctx -> {
            User receivedUser = ctx.bodyAsClass(User.class);

            System.out.println("\nREGISTER NEW ACCOUNT");
            System.out.println(formatReceived(receivedUser.toString()));

            // Enforce a username and password to be present
            if (receivedUser.getUsername() == null &&
                    receivedUser.getPassword() == null) {
                System.out.println(" No username or password for\n "
                        + receivedUser);

                ctx.status(HttpStatus.NOT_ACCEPTABLE);
            } else {
                if (getUser(receivedUser, users) != null) {
                    System.out.println(" Username already exists\n "
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

                    System.out.println("\tAdded\n\t "
                            + receivedUser);

                    ctx.status(HttpStatus.CREATED);
                }
            }
        });

        // Login
        app.post("/login", ctx -> {
            /*
             *  1) User enters username & password
             *  2) Their credentials are checked
             *      + check if either is present
             *      + search for the User in the db
             *  3) If they enter correctly
             *      + a session is created with their role attribute.
             */
            System.out.println("\nLOGIN");

            // 1) User enters username & password
            String receivedUsername = ctx.queryParam("username");
            String receivedPassword = ctx.queryParam("password");

            // 2) Their credentials are checked
            //      + check if either is present
            boolean usernameExists = stringsExist(receivedUsername);
            boolean passwordExists = stringsExist(receivedPassword);
            boolean paramsExist = usernameExists && passwordExists;
            if (!usernameExists) {
                System.out.println(" A username has not been entered.");
            }
            if (!passwordExists) {
                System.out.println(" A password has not been entered.");
            }

            //      + search for the User in the db
            User foundUser = null;
            boolean isCorrectUsernamePassword = false;
            if (paramsExist) {
                foundUser = getUser(receivedUsername, users);
                if (foundUser != null) {
                    isCorrectUsernamePassword = true;
                }
            }

            // 3) If they enter correctly
            if (!isCorrectUsernamePassword) {
                System.out.println(" Incorrect username or password.");
            } else {
                //      + a session is created with their role attribute.
                // Create a session if one does not exist
                HttpSession session = ctx.req().getSession();
                // Set username, id, and role attributes accordingly
                session.setAttribute("username", foundUser.getUsername());
                session.setAttribute("id", foundUser.getId());
                switch (foundUser.getRole().toLowerCase()) {
                    case "employee":
                        session.setAttribute("role", "employee");
                        break;
                    case "manager":
                        session.setAttribute("role", "manager");
                        break;
                }
                System.out.println(" User " + foundUser.getUsername() +
                        " has logged in as " + session.getAttribute("role") + ".");
            }
        });

        // Logout
        app.get("/logout", ctx -> {
            System.out.println("\nLOGOUT");
            // Avoid creating a new session if one does not exist
            HttpSession session = ctx.req().getSession(false);
            if (session != null) {
                String u = session.getAttribute("username").toString();
                System.out.println(" User " + u + " has logged out.");
                // Invalidate the current session if one exists
                session.invalidate();
            } else {
                System.out.println(" No user is logged in.");
            }
        });

        // EMPLOYEE ONLY:
        // Create a ticket (new ReimbursementTicket instance)
        // TODO Test me!
        app.post("/reimbursement/submit", ctx -> {
            /*
             * 1) Check for a Session and User's role in the Session
             *      without creating a new session
             * 2) Submit ReimbursementTicket
             *      * check if it can be created
             */
            System.out.println("\nREIMBURSEMENT TICKET SUBMIT");

            // 1) Check for a Session and User's role in the Session
            //      without creating a new session
            boolean isEmployee = isSessionRole(ctx, "employee");

            if (!isEmployee) {
                System.out.println(" Only logged in employees can submit reimbursement requests.");
            } else {
                // 2) Submit ReimbursementTicket
                ReimbursementTicket receivedTicket = ctx.bodyAsClass(ReimbursementTicket.class);
                System.out.println(formatReceived(receivedTicket.toString()));

                // * check if it can be created
                boolean isValidAssociatedUserId =
                        getUser(receivedTicket.getAssociatedUserId(), users) != null;
                boolean isValidAmount = receivedTicket.getAmount() >= 0;
                boolean isValidDescription = stringsExist(receivedTicket.getDescription());
                boolean isValidTicket =
                            isValidAssociatedUserId &&
                            isValidAmount &&
                            isValidDescription;

                if (!isValidAssociatedUserId) {
                    System.out.println("\n User id " +
                            receivedTicket.getAssociatedUserId() +
                            " not found.");
                }

                if (!isValidAmount) {
                    System.out.println("\n Amount $" + receivedTicket.getAmount() +
                            " not greater than $0.00.");
                }

                if (!isValidDescription) {
                    System.out.println("\n Description cannot be empty or blank.");
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
            }
        });

        // MANAGERS AND EMPLOYEES:
        // List Tickets
        // TODO Adjust for roles:
        //  Employees can view their tickets of any status
        //  Managers can view all tickets
        // TODO Test me!
        app.get("/tickets", ctx -> {
            /*
             * 1) Check for a Session and User's role in the Session
             *      without creating a new session
             * 2) List tickets according to query params
             *      + employees can only view their own tickets
             */
            System.out.println("\nLIST TICKETS");

            // 1) Check for a Session and User's role in the Session
            //      without creating a new session
            boolean isManager = isSessionRole(ctx, "manager");
            boolean isEmployee = isSessionRole(ctx, "employee");

            if (!isEmployee && !isManager) {
                System.out.println(" Only logged in users can view reimbursement tickets.");
            } else {
                HttpSession session = ctx.req().getSession();
                // 2) List tickets according to query params
                String receivedStatus = ctx.queryParam("status");
                String receivedUsername = null;
                if (isManager) {
                    receivedUsername = ctx.queryParam("username");
                }
                System.out.println(formatReceived(receivedStatus, receivedUsername));

                /*
                 *  Four cases with the two inputs (status|username):
                 *  (status & username): print tickets of status by username.
                 *  (status & !username): print tickets of status.
                 *  (!status & username): print tickets by username.
                 *  (!status & !username): print tickets.
                 */
                boolean isValidStatus = true;
                boolean isValidUsername = false;
                User foundUser = null;
                if (stringsExist(receivedStatus)) {
                    try {
                        isValidStatus =
                                receivedStatus != null &&
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

                if (isManager) {
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
                            System.out.println("\t" + t);
                        }
                    }
                }

                if (isEmployee) {
                    System.out.println(" Listing tickets for " + session.getAttribute("username"));
                    if (isValidStatus) {
                        System.out.println("  " + receivedStatus + " tickets:");
                        // print employee's ticket of status.
                        for (ReimbursementTicket t:
                                reimbursementTickets) {
                            if (t.getStatus().equalsIgnoreCase(receivedStatus) &&
                                t.getAssociatedUserId() ==
                                        Long.parseLong(session.getAttribute("id").toString())) {
                                System.out.println("\t" + t);
                            }
                        }
                    } else {
                        // print employee's tickets
                        for (ReimbursementTicket t:
                                reimbursementTickets) {
                            if (t.getAssociatedUserId() ==
                                    Long.parseLong(session.getAttribute("id").toString())) {
                                System.out.println("\t" + t);
                            }
                        }
                    }

                }

            }
        });

        // MANAGER ONLY:
        // Process a ticket
        // TODO Test me!
        app.post("/reimbursement/process", ctx -> {
            /*
             * 1) Check for a Session and User's role in the Session
             *      without creating a new session
             * 2) Process ReimbursementTicket
             *      * check if it can be processed
             */
            System.out.println("\nREIMBURSEMENT TICKET PROCESSING");

            // 1) Check for a Session and User's role in the Session
            //      without creating a new session
            boolean isManager = isSessionRole(ctx, "manager");

            if (!isManager) {
                System.out.println(" Only logged in managers can process tickets.");
            } else {
                // 2) Process ReimbursementTicket
                String receivedTicketId = ctx.queryParam("id");
                String receivedManagerChoice = ctx.queryParam("managerChoice");

                System.out.println(formatReceived(
                        receivedTicketId,
                        receivedManagerChoice
                ));

                boolean receivedTicketIdExists = stringsExist(receivedTicketId);
                boolean receivedManagerChoiceExists = stringsExist(receivedManagerChoice);
                boolean queryParamsExist = receivedTicketIdExists && receivedManagerChoiceExists;

                if (!receivedTicketIdExists) {
                    System.out.println(" No ticket ID has been entered.");
                }

                if (!receivedManagerChoiceExists) {
                    System.out.println(" No manager choice for the ticket has been entered.");
                }

                //      * check if it can be processed
                if (queryParamsExist) {
                    long parsedTicketId;
                    ReimbursementTicket foundTicket = null;
                    boolean isValidTicketId = true;
                    boolean isValidManagerChoice;
                    try {
                        isValidManagerChoice =
                                receivedManagerChoice.equalsIgnoreCase("approve") ||
                                        receivedManagerChoice.equalsIgnoreCase("deny");
                    } catch (NullPointerException e) {
                        isValidManagerChoice = false;
                    }
                    boolean isTicketProcessable = true;

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

                    if (isValidTicketId && isValidManagerChoice && isTicketProcessable) {
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
            }
        });

        // Change role of a User
        // TODO Only allow managers to change User roles (with sessions)
        app.post("/role", ctx -> {
            /*
             * 1) Check for a Session and User's role in the Session
             *      without creating a new session
             * 2) Change role accordingly
             *      * check if the User exists
             */
            System.out.println("\nCHANGE ROLE");

            // 1) Check for a Session and User's role in the Session
            //      without creating a new session
            boolean isManager = isSessionRole(ctx, "manager");

            if (!isManager) {
                System.out.println(" Only logged in managers can change roles of other users.");
            } else {
                // 2) Change role accordingly
                String receivedUsername = ctx.queryParam("username");
                String receivedNewRole = ctx.queryParam("newRole");
                System.out.println(formatReceived(
                        receivedUsername,
                        receivedNewRole
                ));

                //      * check if the User exists
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
                    System.out.println(" Setting " + receivedUsername + "'s role from\n");
                    System.out.println("  " + foundUser.getRole() + "\n\tto");

                    switch (receivedNewRole.toLowerCase()) {
                        case "employee":
                            foundUser.setRole("Employee");
                            break;
                        case "manager":
                            foundUser.setRole("Manager");
                            break;
                    }

                    System.out.println("  " + foundUser.getRole());

                    ctx.status(HttpStatus.ACCEPTED);
                } else {
                    ctx.status(HttpStatus.NOT_MODIFIED);
                }
            }
        });

        // TESTING:
        // List Users
        // TODO Remove this expression! Testing purposes only.
        // TODO If kept:
        //  Only allow managers to use
        //  Don't show passwords
        app.get("/users", ctx -> {
            /*
             * 1) Check for a Session and User's role in the Session
             *      without creating a new session
             * 2) List Users
             */
            System.out.println("\nLIST USERS");

            // 1) Check for a Session and User's role in the Session
            //      without creating a new session
            boolean isManager = isSessionRole(ctx, "manager");

            if (!isManager) {
                System.out.println(" Manager is not logged in.");
            } else {
                if (users.isEmpty()) {
                    System.out.println(" There are no Users");
                } else {
                    System.out.println(" " + users.size() + " Users:");
                    System.out.println("  Managers:");
                    for (User u:
                            users) {
                        if (u.getRole().equalsIgnoreCase("manager")) {
                            System.out.println("\t" + u);
                        }
                    }
                    System.out.println("  Employees:");
                    for (User u:
                            users) {
                        if (u.getRole().equalsIgnoreCase("employee")) {
                            System.out.println("\t" + u);
                        }
                    }
                }
            }
        });

        // Force role of a User
        // TODO Remove this expression! Testing purposes only.
        app.post("/role/force", ctx -> {
            System.out.println("\nFORCE CHANGE ROLE");
            String receivedUsername = ctx.queryParam("username");
            String receivedNewRole = ctx.queryParam("newRole");
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
                System.out.println(" Setting " + receivedUsername + "'s role from\n");
                System.out.println("  " + foundUser.getRole() + "\n\tto");

                switch (receivedNewRole.toLowerCase()) {
                    case "employee":
                        foundUser.setRole("Employee");
                        break;
                    case "manager":
                        foundUser.setRole("Manager");
                        break;
                }

                System.out.println("  " + foundUser.getRole());

                ctx.status(HttpStatus.ACCEPTED);
            } else {
                ctx.status(HttpStatus.NOT_MODIFIED);
            }
        });

        // Stop the app
        // TODO TODO Remove this expression! Testing purposes only.
        app.post("/close", ctx -> {
            /*
             * 1) Close all Sessions
             * 2) Close the app.
             */
            invalidateSession(ctx);

            System.out.println("\nClosing the app...");
            app.close();
        });

    }
}
