package com.revature;

import com.revature.model.ReimbursementTicket;
import com.revature.model.User;
import com.revature.repository.DriverRepository;
import io.javalin.Javalin;
import io.javalin.http.HttpStatus;
import jakarta.servlet.http.HttpSession;

import java.sql.Connection;
import java.util.List;
import java.util.Properties;

import static com.revature.util.DriverUtils.*;

/**
 * Driver class for running the app.
 */
public class Driver {

    public static void main(String[] args) {
        /*
         * Import the properties,
         * Start Javalin app,
         * Set up connection to SQL database:
         */
        Properties props = new Properties();
        props = loadProperties(props, "src/main/resources/application.properties");
        Javalin app = startJavalinApp(props);
        Connection connSQL = connectToPostgresDb(props);
        boolean isJavalinRunning = app != null;
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

        // TODO Add HTTP statuses.
        // App is properly set up and running
        if (isJavalinRunning && isPostgresDbConnected) {
            DriverRepository driverRepository = new DriverRepository(props);

        // GENERAL PURPOSE
            // Manager changes role of a User
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
                    String userIdString = ctx.queryParam("userId");
                    String newRole = ctx.queryParam("newRole");
                    mirrorMessage(ctx, formatReceived(
                            userIdString,
                            newRole
                    ));

                    //      * check if the User exists
                    User foundUser = null;
                    long userId = 0;
                    if (userIdString != null) {
                        userId = Long.parseLong(userIdString);
                        foundUser = driverRepository.getUserRecord(userId);
                    }
                    boolean userExists = foundUser != null;
                    boolean newRoleExists = stringsExist(newRole);
                    boolean isValidNewRole = false;

                    if (!userExists) {
                        mirrorMessage(ctx, "\n User with ID " + userIdString + " not found.");
                    }

                    if (!newRoleExists) {
                        mirrorMessage(ctx, "\n New role not entered.");
                    }

                    if (userExists && newRoleExists) {
                        try {
                            isValidNewRole =
                                    newRole.equalsIgnoreCase("employee") ||
                                    newRole.equalsIgnoreCase("manager");
                        } catch (NullPointerException e) {
                            e.printStackTrace();
                        } finally {
                            if (!isValidNewRole) {
                                mirrorMessage(ctx, "\n\t" + newRole +
                                        " is not a valid role");
                            }
                        }
                    }

                    if (userExists && isValidNewRole) {
                        // Good to go, change the role
                        mirrorMessage(ctx, " Setting " + userIdString + "'s role from");
                        mirrorMessage(ctx, "  " + foundUser.getRole() + " to ");

                        driverRepository.updateRole(userId, newRole);

                        if (getSessionUserId(ctx) == userId) {
                            updateSessionRole(ctx, newRole);
                        }

                        mirrorMessage(ctx, "  " +
                                driverRepository.getUserRecord(userId).getUsername() +
                                "'s role is now " +
                                driverRepository.getUserRecord(userId).getRole());

                        ctx.status(HttpStatus.ACCEPTED);
                    } else {
                        ctx.status(HttpStatus.NOT_MODIFIED);
                    }
                }
            });
            // List Users
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
                    String role = ctx.queryParam("role");

                    if (stringsExist(role)) {
                        // List a role
                        List<User> users = driverRepository.getUserList(role);
                        if (users.isEmpty()) {
                            mirrorMessage(ctx,
                                    " There are no users registered as " +
                                            role);
                        } else {
                            mirrorMessage(ctx,
                                     " " + role + "s:\n" +
                                    buildUsersString(users));
                        }
                    } else {
                        // List all
                        List<User> users = driverRepository.getUserList();
                        if (users.isEmpty()) {
                            mirrorMessage(ctx, " There are no registered users.");
                        } else {
                            mirrorMessage(ctx, buildUsersString(users));
                        }
                    }
                }
            });
            // User updates their account
            // TODO Write me!
            //  Will allow User to change their password
            app.post("/account/edit", ctx -> {});
            // Employee updates a pending ticket of theirs
            // TODO Write me!
            //  Will allow User to change description, amount of pending ticket.
            app.post("/ticket/edit", ctx -> {});

        // CONCERNED WITH LOGIN/REGISTER FEATURE
            // Register new User
            app.post("/account/register", ctx -> {
                User user = ctx.bodyAsClass(User.class);

                mirrorMessage(ctx, "\nREGISTER NEW ACCOUNT");
                mirrorMessage(ctx, formatReceived(
                        String.valueOf(user.getId()),
                        user.getRole(),
                        user.getUsername()
                ));

                // Enforce a username and password to be present
                if (user.getUsername() == null && user.getPassword() == null) {
                    mirrorMessage(ctx, " No username or password for\n "
                            + user);

                    ctx.status(HttpStatus.NOT_ACCEPTABLE);
                } else {
                    if (driverRepository.getUserRecord(user.getUsername()) != null) {
                        mirrorMessage(ctx, " Username already exists\n "
                                + user);

                        ctx.status(HttpStatus.NOT_ACCEPTABLE);
                    } else {
                    /*
                    User is valid, enforce default Employee role
                                   assign unique id
                     */
                        driverRepository.insertUser(user);

                        User newUser = driverRepository.getUserRecord(user.getUsername());

                        mirrorMessage(ctx,
                                "\tRegistered User '" +
                                newUser.getId() + "' '" +
                                newUser.getRole() + "' '" +
                                newUser.getUsername() + "'"
                        );

                        ctx.status(HttpStatus.CREATED);
                    }
                }
            });
            // Login
            app.post("/account/login", ctx -> {
                /*
                 *  1) User enters username & password
                 *  2) Their credentials are checked
                 *      + check if either is present
                 *      + correct username & password?
                 *  3) If they enter correctly
                 *      + a session is created with their role attribute.
                 */
                mirrorMessage(ctx, "\nLOGIN");

                // 1) User enters username & password
                String username = ctx.queryParam("username");
                String password = ctx.queryParam("password");

                // 2) Their credentials are checked
                //      + check if either is present
                boolean usernameExists = stringsExist(username);
                boolean passwordExists = stringsExist(password);
                boolean paramsExist = usernameExists && passwordExists;
                if (!usernameExists) {
                    mirrorMessage(ctx, " A username has not been entered.");
                }
                if (!passwordExists) {
                    mirrorMessage(ctx, " A password has not been entered.");
                }

                //      + correct username & password?
                User foundUser = null;
                boolean isCorrectUsernamePassword = false;
                if (paramsExist) {
                    foundUser = driverRepository.getUserRecord(username);
                    if (foundUser != null) {
                        if (foundUser.getUsername().equals(username) &&
                            foundUser.getPassword().equals(password)) {
                            isCorrectUsernamePassword = true;
                        }
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
            app.get("/account/logout", ctx -> {
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

        // CONCERNED WITH SUBMIT TICKET FEATURE
            // Create a ticket (new ReimbursementTicket instance)
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
                    String amountString = ctx.queryParam("amount");
                    String description = ctx.queryParam("description");
                    mirrorMessage(ctx, formatReceived(
                            amountString,
                            description
                    ));

                    // * check if it can be created
                    boolean isValidAmount = false;
                    double amount = -1;

                    if (stringsExist(amountString)) {
                        try {
                            amount = Double.parseDouble(amountString);
                            isValidAmount = amount >= 0;
                        } catch (NumberFormatException e) {
                            e.printStackTrace();
                            isValidAmount = false;
                        }
                    }
                    boolean isValidDescription = stringsExist(description);
                    boolean isValidTicket = isValidAmount && isValidDescription;

                    StringBuilder errorOutput = new StringBuilder();
                    if (!isValidAmount) {
                        errorOutput.append(" Amount $" + amount + " not positive.");
                    }
                    if (!isValidDescription) {
                        errorOutput.append("\n Description cannot be empty or blank.");
                    }
                    mirrorMessage(ctx, errorOutput.toString());

                    if (isValidTicket) {
                        long userId = getSessionUserId(ctx);
                        driverRepository.insertReimbursementTicket(userId, amount, description);

                        mirrorMessage(ctx,
                                "\tSubmitted " +
                                "$" + amount +
                                " reimbursement request for " +
                                getSessionUsername(ctx));

                        ctx.status(HttpStatus.CREATED);
                    } else {
                        ctx.status(HttpStatus.NOT_ACCEPTABLE);
                    }
                }
            });

        // CONCERNED WITH TICKETING SYSTEM FEATURE
            // Process a ticket
            app.post("/reimbursement/process", ctx -> {
                /*
                 * 1) Check if a Manager is logged in
                 * 2) Process ReimbursementTicket
                 *      * check if params exist
                 *      * check if it can be processed
                 */
                mirrorMessage(ctx, "\nREIMBURSEMENT TICKET PROCESSING");

                // 1) Check if a Manager is logged in
                boolean isManager = isSessionRole(ctx, "manager");
                if (!isManager) {
                    mirrorMessage(ctx, " Only logged in managers can process tickets.");
                } else {
                    // 2) Process ReimbursementTicket
                    String ticketIdParam = ctx.queryParam("ticketId");
                    String managerChoiceParam = ctx.queryParam("managerChoice");

                    mirrorMessage(ctx, formatReceived(ticketIdParam, managerChoiceParam));

                    //      * check if params exist
                    StringBuilder errorOutput = new StringBuilder();
                    if (!stringsExist(ticketIdParam)) {
                        errorOutput.append(" No ticket ID has been entered.");
                    }
                    if (!stringsExist(managerChoiceParam)) {
                        errorOutput.append("\n No manager choice for the ticket has been entered.");
                    }
                    mirrorMessage(ctx, errorOutput.toString());

                    //      * check if it can be processed
                    if (stringsExist(ticketIdParam) && stringsExist(managerChoiceParam)) {

                        long ticketId = -1;
                        try {
                            ticketId = Long.parseLong(ticketIdParam);
                        } catch (NumberFormatException e) {
                            e.printStackTrace();
                        }

                        ReimbursementTicket ticket = driverRepository
                                .getReimbursementTicketRecord(ticketId);

                        if (ticket.getStatus().equalsIgnoreCase("pending")) {
                            driverRepository.processReimbursementTicket(ticket, managerChoiceParam);
                            mirrorMessage(ctx,
                                    driverRepository
                                    .getReimbursementTicketRecord(ticket)
                                    .getStatus() +
                                    " ticket " + ticketId);
                        } else {
                            mirrorMessage(ctx,
                                    "Ticket " + ticketId +
                                    " not processed.");
                        }
                    }
                }
            });

        // CONCERNED WITH VIEW PREVIOUS TICKETS FEATURE
            // List Tickets
            app.get("/ticket/list", ctx -> {
                /*
                 * 1) Check if a User is logged in
                 * 2a) List all specified if Manager
                 * 2b) List Employee's if Employee
                 */
                mirrorMessage(ctx, "\nLIST TICKETS");

                // 1) Check if a User is logged in
                if (!isSessionRole(ctx, "manager") && !isSessionRole(ctx, "employee")
                ) {
                    mirrorMessage(ctx, " Only logged in users can view reimbursement tickets.");
                }

                String status = assignStringIfExists(ctx.queryParam("status"));
                String username = assignStringIfExists(ctx.queryParam("username"));

                // 2a) List all specified if Manager
                if (isSessionRole(ctx,"manager")) {
                    List<ReimbursementTicket> tickets;
                    if (username == null) {
                        tickets = driverRepository.getReimbursementTicketList(status);
                    } else {
                        tickets = driverRepository.getReimbursementTicketList(
                                        status,
                                        driverRepository.getUserRecord(username).getId()
                                );
                    }

                    mirrorMessage(ctx, buildReimbursementTicketsString(
                            tickets, driverRepository));
                }

                // 2b) List Employee's if Employee
                if (isSessionRole(ctx,"employee")) {
                    List<ReimbursementTicket> tickets =
                        driverRepository.getReimbursementTicketList(
                            status,
                            getSessionUserId(ctx)
                    );

                    mirrorMessage(ctx, buildReimbursementTicketsString(
                            tickets, driverRepository));
                }
            });

        // TESTING
            // Stop the app
            // TODO Remove this expression! Testing purposes only.
            app.get("/close", ctx -> {
                /*
                 * 1) Close all Sessions
                 * 2) Close the app.
                 */
                mirrorMessage(ctx, "\nClosing the app...");
                invalidateSession(ctx);
                connSQL.close();
                app.close();
            });
        }
    }
}
