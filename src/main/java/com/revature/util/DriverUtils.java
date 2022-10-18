package com.revature.util;

import com.revature.model.ReimbursementTicket;
import com.revature.model.User;

import java.util.List;

/**
 * Utility class to be used with the Driver class running the app.
 * 
 * @author johnainsworth
 */
public class DriverUtils {

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
        for (String s:
                strings) {
            if (s.isEmpty() || s.isBlank() || s == null) {
                return false;
            }
        }

        return true;
    }
    public static String formatReceived(String...strings) {
        /**
         * Returns a formatted string for returning input Strings.
         */
        StringBuilder received = new StringBuilder("Received");

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
