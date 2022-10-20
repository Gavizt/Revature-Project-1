package com.revature;

import com.revature.model.ReimbursementTicket;
import com.revature.model.User;
import io.javalin.Javalin;
import io.javalin.http.HttpStatus;
import jakarta.servlet.http.HttpSession;

import java.sql.Connection;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicLong;

import static com.revature.util.DriverUtils.*;

/**
 * Driver class for running the app.
 */
public class Driver {

    public static void main(String[] args) {
        // TODO remove these
        // Collections for Users and Tickets
        List<User> users = new LinkedList<>();
        List<ReimbursementTicket> reimbursementTickets = new LinkedList<>();
//        TODO Remove these ID variables when SQL db is integrated.
        AtomicLong nextUserId = new AtomicLong();
        AtomicLong nextTicketId = new AtomicLong();
        nextUserId.set(1);
        nextTicketId.set(1);

        /*
         * Import the properties:
         */
        Properties props = new Properties();
        props = getProperties(props, "src/main/resources/application.properties");
        boolean isValidProperties = props != null;

        /*
         * Set up connection to Javalin app:
         */
        Javalin app = startJavalinApp(props);
        boolean isJavalinRunning = app != null;

        /*
         * TODO Use SQL database for Users and Tickets
         * Set up connection to SQL database:
         */
        Connection connSQL = connectToPostgresDb(props);
        boolean isPostgresDbConnected = connSQL != null;

        if (!isJavalinRunning) {
            System.out.println("Could not start Javalin app.");
        } else {
            System.out.println("Started Javalin app on port: " + props.getProperty("port"));
        }

        if (!isPostgresDbConnected) {
            System.out.println("Could not connect to Postgres DB.");
        } else {
            System.out.println("Connected to Postgres DB as user: " + props.getProperty("username"));
        }

        if (isJavalinRunning && isPostgresDbConnected) {
            // App is properly set up and running
            // TODO Add HTTP statuses
            // Register
            app.post("/register", ctx -> {
                User receivedUser = ctx.bodyAsClass(User.class);

                mirrorMessage(ctx, "\nREGISTER NEW ACCOUNT");
                mirrorMessage(ctx, formatReceived(receivedUser.toString()));

                // Enforce a username and password to be present
                if (receivedUser.getUsername() == null &&
                        receivedUser.getPassword() == null) {
                    mirrorMessage(ctx, " No username or password for\n "
                            + receivedUser);

                    ctx.status(HttpStatus.NOT_ACCEPTABLE);
                } else {
                    if (getUser(receivedUser, users) != null) {
                        mirrorMessage(ctx, " Username already exists\n "
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

                        mirrorMessage(ctx, "\tAdded\n\t "
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
                mirrorMessage(ctx, "\nLOGIN");

                // 1) User enters username & password
                String receivedUsername = ctx.queryParam("username");
                String receivedPassword = ctx.queryParam("password");

                // 2) Their credentials are checked
                //      + check if either is present
                boolean usernameExists = stringsExist(receivedUsername);
                boolean passwordExists = stringsExist(receivedPassword);
                boolean paramsExist = usernameExists && passwordExists;
                if (!usernameExists) {
                    mirrorMessage(ctx, " A username has not been entered.");
                }
                if (!passwordExists) {
                    mirrorMessage(ctx, " A password has not been entered.");
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
                    mirrorMessage(ctx, " Incorrect username or password.");
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
                    mirrorMessage(ctx, " User " + foundUser.getUsername() +
                            " has logged in as " + session.getAttribute("role") + ".");
                }
            });

            // Logout
            app.get("/logout", ctx -> {
                mirrorMessage(ctx, "\nLOGOUT");
                // Avoid creating a new session if one does not exist
                HttpSession session = ctx.req().getSession(false);
                if (session != null) {
                    String u = session.getAttribute("username").toString();
                    mirrorMessage(ctx, " User " + u + " has logged out.");
                    // Invalidate the current session if one exists
                    session.invalidate();
                } else {
                    mirrorMessage(ctx, " No user is logged in.");
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
                mirrorMessage(ctx, "\nREIMBURSEMENT TICKET SUBMIT");

                // 1) Check for a Session and User's role in the Session
                //      without creating a new session
                boolean isEmployee = isSessionRole(ctx, "employee");

                if (!isEmployee) {
                    mirrorMessage(ctx, " Only logged in employees can submit reimbursement requests.");
                } else {
                    // 2) Submit ReimbursementTicket
                    ReimbursementTicket receivedTicket = ctx.bodyAsClass(ReimbursementTicket.class);
                    mirrorMessage(ctx, formatReceived(receivedTicket.toString()));

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
                        mirrorMessage(ctx, "\n User id " +
                                receivedTicket.getAssociatedUserId() +
                                " not found.");
                    }

                    if (!isValidAmount) {
                        mirrorMessage(ctx, "\n Amount $" + receivedTicket.getAmount() +
                                " not greater than $0.00.");
                    }

                    if (!isValidDescription) {
                        mirrorMessage(ctx, "\n Description cannot be empty or blank.");
                    }

                    if (isValidTicket) {
                        receivedTicket.setId(nextTicketId.getAndIncrement());
                        receivedTicket.setStatus("Pending");
                        reimbursementTickets.add(receivedTicket);

                        mirrorMessage(ctx, "\n\tSubmitted\n\t "
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
                mirrorMessage(ctx, "\nLIST TICKETS");

                // 1) Check for a Session and User's role in the Session
                //      without creating a new session
                boolean isManager = isSessionRole(ctx, "manager");
                boolean isEmployee = isSessionRole(ctx, "employee");

                if (!isEmployee && !isManager) {
                    mirrorMessage(ctx, " Only logged in users can view reimbursement tickets.");
                } else {
                    HttpSession session = ctx.req().getSession();
                    // 2) List tickets according to query params
                    String receivedStatus = ctx.queryParam("status");
                    String receivedUsername = null;
                    if (isManager) {
                        receivedUsername = ctx.queryParam("username");
                    }
                    mirrorMessage(ctx, formatReceived(receivedStatus, receivedUsername));

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
                                mirrorMessage(ctx, "\t" + t);
                            }
                        }
                    }

                    if (isEmployee) {
                        mirrorMessage(ctx, " Listing tickets for " + session.getAttribute("username"));
                        if (isValidStatus) {
                            mirrorMessage(ctx, "  " + receivedStatus + " tickets:");
                            // print employee's ticket of status.
                            for (ReimbursementTicket t:
                                    reimbursementTickets) {
                                if (t.getStatus().equalsIgnoreCase(receivedStatus) &&
                                        t.getAssociatedUserId() ==
                                                Long.parseLong(session.getAttribute("id").toString())) {
                                    mirrorMessage(ctx, "\t" + t);
                                }
                            }
                        } else {
                            // print employee's tickets
                            for (ReimbursementTicket t:
                                    reimbursementTickets) {
                                if (t.getAssociatedUserId() ==
                                        Long.parseLong(session.getAttribute("id").toString())) {
                                    mirrorMessage(ctx, "\t" + t);
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
                mirrorMessage(ctx, "\nREIMBURSEMENT TICKET PROCESSING");

                // 1) Check for a Session and User's role in the Session
                //      without creating a new session
                boolean isManager = isSessionRole(ctx, "manager");

                if (!isManager) {
                    mirrorMessage(ctx, " Only logged in managers can process tickets.");
                } else {
                    // 2) Process ReimbursementTicket
                    String receivedTicketId = ctx.queryParam("id");
                    String receivedManagerChoice = ctx.queryParam("managerChoice");

                    mirrorMessage(ctx, formatReceived(
                            receivedTicketId,
                            receivedManagerChoice
                    ));

                    boolean receivedTicketIdExists = stringsExist(receivedTicketId);
                    boolean receivedManagerChoiceExists = stringsExist(receivedManagerChoice);
                    boolean queryParamsExist = receivedTicketIdExists && receivedManagerChoiceExists;

                    if (!receivedTicketIdExists) {
                        mirrorMessage(ctx, " No ticket ID has been entered.");
                    }

                    if (!receivedManagerChoiceExists) {
                        mirrorMessage(ctx, " No manager choice for the ticket has been entered.");
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
                                mirrorMessage(ctx, " Ticket " + foundTicket.getId() +
                                        " is already '" + foundTicket.getStatus() + "'");
                            }
                        } catch (NumberFormatException e) {
                            isValidTicketId = false;
                            mirrorMessage(ctx, " Ticket ID " + receivedTicketId +
                                    " is not valid.");
                        } catch (NullPointerException e) {
                            isTicketProcessable = false;
                            mirrorMessage(ctx, " Could not find a ticket with ID " +
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

                            mirrorMessage(ctx, "\t" + foundTicket.getStatus() +
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
                mirrorMessage(ctx, "\nCHANGE ROLE");

                // 1) Check for a Session and User's role in the Session
                //      without creating a new session
                boolean isManager = isSessionRole(ctx, "manager");

                if (!isManager) {
                    mirrorMessage(ctx, " Only logged in managers can change roles of users.");
                } else {
                    // 2) Change role accordingly
                    String receivedUsername = ctx.queryParam("username");
                    String receivedNewRole = ctx.queryParam("newRole");
                    mirrorMessage(ctx, formatReceived(
                            receivedUsername,
                            receivedNewRole
                    ));

                    //      * check if the User exists
                    User foundUser = getUser(receivedUsername, users);
                    boolean userExists = foundUser != null;
                    boolean newRoleExists = receivedNewRole != null;
                    boolean isValidNewRole = true;

                    if (!userExists) {
                        mirrorMessage(ctx, "\n User " + receivedUsername + " not found.");
                    }

                    if (!newRoleExists) {
                        mirrorMessage(ctx, "\n New role not entered.");
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
                                mirrorMessage(ctx, "\n\t" + receivedNewRole +
                                        " is not a valid role");
                            }
                        }
                    }

                    if (userExists && isValidNewRole) {
                        // Good to go, change the role
                        mirrorMessage(ctx, " Setting " + receivedUsername + "'s role from\n");
                        mirrorMessage(ctx, "  " + foundUser.getRole() + "\n\tto");

                        switch (receivedNewRole.toLowerCase()) {
                            case "employee":
                                foundUser.setRole("Employee");
                                break;
                            case "manager":
                                foundUser.setRole("Manager");
                                break;
                        }

                        mirrorMessage(ctx, "  " + foundUser.getRole());

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
                mirrorMessage(ctx, "\nLIST USERS");

                // 1) Check for a Session and User's role in the Session
                //      without creating a new session
                boolean isManager = isSessionRole(ctx, "manager");

                if (!isManager) {
                    mirrorMessage(ctx, " Manager is not logged in.");
                } else {
                    if (users.isEmpty()) {
                        mirrorMessage(ctx, " There are no Users");
                    } else {
                        mirrorMessage(ctx, " " + users.size() + " Users:");
                        mirrorMessage(ctx, "  Managers:");
                        for (User u:
                                users) {
                            if (u.getRole().equalsIgnoreCase("manager")) {
                                mirrorMessage(ctx, "\t" + u);
                            }
                        }
                        mirrorMessage(ctx, "  Employees:");
                        for (User u:
                                users) {
                            if (u.getRole().equalsIgnoreCase("employee")) {
                                mirrorMessage(ctx, "\t" + u);
                            }
                        }
                    }
                }
            });

            // Force role of a User
            // TODO Remove this expression! Testing purposes only.
            app.post("/role/force", ctx -> {
                mirrorMessage(ctx, "\nFORCE CHANGE ROLE");
                String receivedUsername = ctx.queryParam("username");
                String receivedNewRole = ctx.queryParam("newRole");
                mirrorMessage(ctx, formatReceived(
                        receivedUsername,
                        receivedNewRole
                ));

                User foundUser = getUser(receivedUsername, users);
                boolean userExists = foundUser != null;
                boolean newRoleExists = receivedNewRole != null;
                boolean isValidNewRole = true;

                if (!userExists) {
                    mirrorMessage(ctx, "\n User " + receivedUsername + " not found.");
                }

                if (!newRoleExists) {
                    mirrorMessage(ctx, "\n New role not entered.");
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
                            mirrorMessage(ctx, "\n\t" + receivedNewRole +
                                    " is not a valid role");
                        }
                    }
                }

                if (userExists && isValidNewRole) {
                    // Good to go, change the role
                    mirrorMessage(ctx, " Setting " + receivedUsername + "'s role from\n");
                    mirrorMessage(ctx, "  " + foundUser.getRole() + "\n\tto");

                    switch (receivedNewRole.toLowerCase()) {
                        case "employee":
                            foundUser.setRole("Employee");
                            break;
                        case "manager":
                            foundUser.setRole("Manager");
                            break;
                    }

                    mirrorMessage(ctx, "  " + foundUser.getRole());

                    ctx.status(HttpStatus.ACCEPTED);
                } else {
                    ctx.status(HttpStatus.NOT_MODIFIED);
                }
            });

            // Stop the app
            // TODO Remove this expression! Testing purposes only.
            // TODO Close JDBC related objects.
            app.get("/close", ctx -> {
                /*
                 * 1) Close all Sessions
                 * 2) Close the app.
                 */
                invalidateSession(ctx);

                mirrorMessage(ctx, "\nClosing the app...");
                app.close();
            });

            app.get("/testMessage", ctx -> {
                mirrorMessage(ctx, "test");
            });
        }
    }
}
