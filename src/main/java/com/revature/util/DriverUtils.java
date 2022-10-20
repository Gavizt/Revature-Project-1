package com.revature.util;

import com.revature.model.ReimbursementTicket;
import com.revature.model.User;
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
    public static Properties getProperties(Properties props, String propertiesFile) {
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
            return null;
        }
    }
    public static void mirrorMessage(Context ctx, String message) {
        ctx.json(message);
        System.out.println(message);
    }

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

    public static void printReimbursementTickets(String status, List<ReimbursementTicket> tickets) {
        /**
         * List ReimbursementTickets by given status.
         */
        System.out.println(" " + status + ":");
        for (ReimbursementTicket t:
                tickets) {
            if (t.getStatus().equalsIgnoreCase(status)) {
                System.out.println("\t" + t);
            }
        }
    }

    public static void printReimbursementTickets(User user, List<ReimbursementTicket> tickets) {
        /**
         * List ReimbursementTickets submitted by user
         */
        System.out.println(" " + user.getUsername() +
                "'s tickets:");
        for (ReimbursementTicket t:
                tickets) {
            if (t.getAssociatedUserId() == user.getId()) {
                System.out.println("\t" + t);
            }
        }
    }

    public static void printReimbursementTickets(
            User user, String status, List<ReimbursementTicket> tickets) {
        /**
         * List ReimbursementTickets submitted by user of given status.
         */
        System.out.println(" " + user.getUsername() +
                "'s " + status + " tickets:");
        for (ReimbursementTicket t:
                tickets) {
            if (t.getId() == user.getId() &&
                    t.getStatus().equalsIgnoreCase(status)) {
                System.out.println("\t" + t);
            }
        }
    }

    public static boolean stringsExist(String...strings) {
        /**
         * Returns false if any string is empty, blank, or null.
         */
        try {
            for (String s:
                    strings) {
                if (s.isEmpty() || s.isBlank() || s == null) {
                    return false;
                }
            }
            return true;
        } catch (NullPointerException e) {
            return false;
        }
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

    public static User getUser(User user, List<User> users) {
        /**
         * Returns a User from a list of Users.
         */
        return getUser(user.getUsername(), users);
    }

    public static User getUser(long id, List<User> users) {
        /**
         * Returns a User from a list of Users by id.
         */
        for (User u:
                users) {
            if (u.getId() == id) {
                return u;
            }
        }

        return null;
    }

    public static User getUser(String username, List<User> users) {
        /**
         * Returns a User from a list of Users by username.
         */
        for (User u:
                users) {
            if (u.getUsername().equals(username)) {
                return u;
            }
        }

        return null;
    }

    public static ReimbursementTicket getReimbursementTicket(long ticketId, List<ReimbursementTicket> tickets) {
        /**
         * Returns a ReimbursementTicket from a list of ReimbursementTickets by id.
         */
        for (ReimbursementTicket t:
                tickets) {
            if (t.getId() == ticketId) {
                return t;
            }
        }

        return null;
    }
}
