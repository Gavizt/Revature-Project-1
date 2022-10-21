package com.revature.util;

import com.revature.model.ReimbursementTicket;
import com.revature.model.User;
import com.revature.repository.DriverRepository;
import io.javalin.Javalin;
import io.javalin.http.Context;
import jakarta.servlet.http.HttpSession;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.Properties;

/**
 * Utility class to be used with the Driver class running the app.
 * 
 * @author johnainsworth
 */
public class DriverUtils {

    // Setting up properties & connections:
    public static Properties loadProperties(Properties props, String propertiesFile) {
        try(FileReader reader = new FileReader(propertiesFile)) {
            props.load(reader);
        } catch (FileNotFoundException e) {
            System.out.println(propertiesFile + " not found.");
            e.printStackTrace();
            return null;
        } catch (IOException e) {
            System.out.println("An IOException occurred");
            e.printStackTrace();
            return null;
        }
        return props;
    }
    public static Javalin startJavalinApp(Properties props) {
        try {
            return Javalin.create().start(
                    Integer.parseInt(props.getProperty("port"))
            );
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    public static Connection connectToPostgresDb(Properties props) {
        try {
            return DriverManager.getConnection(
                    props.getProperty("url"),
                    props.getProperty("username"),
                    props.getProperty("password")
            );
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    // Session handling:
    public static void invalidateSession(Context ctx) {
        HttpSession session = ctx.req().getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }
    public static boolean isSessionRole(Context ctx, String role) {
        /**
         * Return the role of a Session
         */
        HttpSession session = ctx.req().getSession(false);

        if (session != null) {
            if (session.getAttribute("role") != null) {
                return session.getAttribute("role")
                        .toString()
                        .equalsIgnoreCase(role);
            }
        }

        return false;
    }
    public static void updateSessionRole(Context ctx, String newRole) {
        /**
         * Change the role of the current session
         */
        HttpSession session = ctx.req().getSession(false);

        if (session != null) {
            if (session.getAttribute("role") != null) {
                session.setAttribute("role", newRole.toLowerCase());
            }
        }
    }
    public static long getSessionUserId(Context ctx) {
        HttpSession session = ctx.req().getSession(false);
        long sessionUserId = -1;

        if (session != null) {
            if (session.getAttribute("id") != null) {
                sessionUserId = Long.parseLong(session.getAttribute("id").toString());
            }
        }

        return sessionUserId;
    }
    public static String getSessionUsername(Context ctx) {
        HttpSession session = ctx.req().getSession(false);
        String username = null;

        if (session != null) {
            if (session.getAttribute("id") != null) {
                username = session.getAttribute("username").toString();
            }
        }

        return username;
    }

    // Printing:
    public static void mirrorMessage(Context ctx, String message) {
        ctx.json(message);
        System.out.println(message);
    }
    public static String formatReceived(String...strings) {
        /**
         * Returns a formatted string for returning input Strings.
         */
        StringBuilder received = new StringBuilder(" Received");

        for (String s:
                strings) {
            received.append(" '" + s + "'");
        }

        return received.toString();
    }
    public static String buildReimbursementTicketsString(
            List<ReimbursementTicket> tickets, DriverRepository driverRepository) {
        StringBuilder ticketsSting = new StringBuilder();
        long currentUserId = 0;

        for (ReimbursementTicket t:
             tickets) {
            if (currentUserId != t.getAssociatedUserId()) {
                currentUserId = t.getAssociatedUserId();
                ticketsSting.append(" " +
                        driverRepository.getUserRecord(currentUserId).getUsername() +
                        ":\n"
                );
            }

            ticketsSting.append("\t" + t + "\n");
        }

        return (ticketsSting.length() == 0) ? "Empty List" : String.valueOf(ticketsSting);
    }
    public static String buildUsersString(List<User> users) {
        StringBuilder usersString = new StringBuilder();

        for (User u:
             users) {
            usersString.append("\t" + u.toStringNoPassword() + "\n");
        }

        return (usersString.length() == 0) ? "Empty List" : String.valueOf(usersString);
    }

    // Other:
    public static boolean stringsExist(String...strings) {
        /**
         * Returns false if any string is empty, blank, or null.
         */
        try {
            for (String s:
                    strings) {
                if (s.isEmpty() || s.isBlank()) {
                    return false;
                }
            }
            return true;
        } catch (NullPointerException e) {
            return false;
        }
    }
    public static String assignStringIfExists(String string) {
        if (stringsExist(string)) {
            return string;
        }

        return null;
    }
}