package com.revature.repository;

import com.revature.model.*;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static com.revature.util.DriverUtils.stringsExist;

/**
 * Methods for Postgres JDBC.
 *
 * @author johnainsworth
 */
public class DriverRepository {
    final private Properties props;

    // A properties file must be provided
    public DriverRepository(Properties props) {
        this.props = props;
    }

// GENERAL PURPOSE
    // Getters (not the typical class kind)
    public Connection getPostgresConnection() {
        Connection conn = null;

        try {
            conn = DriverManager.getConnection(
                    props.getProperty("url"),
                    props.getProperty("username"),
                    props.getProperty("password")
            );
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return conn;
    }
    public User getUserRecord(User user) {
        return getUserRecord(user.getId());
    }
    public User getUserRecord(long id) {
        ResultSet set = null;
        Connection conn = null;
        PreparedStatement stmt = null;
        String sqlQuery =
                "SELECT * FROM users " +
                "WHERE user_id = ?";
        User user = null;

        try {
            conn = getPostgresConnection();
            stmt = conn.prepareStatement(sqlQuery);
            stmt.setLong(1, id);
            set = stmt.executeQuery();
            if (set.next()) {
                user = new User(
                        set.getLong(1),
                        set.getString(2),
                        set.getString(3),
                        set.getString(4)
                );
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            closeDbObjects(set, stmt, conn);
        }

        return user;
    }
    public User getUserRecord(String username) {
        ResultSet set = null;
        Connection conn = null;
        PreparedStatement stmt = null;
        String sqlQuery =
                "SELECT * FROM users " +
                "WHERE user_username = ?";
        User user = null;

        try {
            conn = getPostgresConnection();
            stmt = conn.prepareStatement(sqlQuery);
            stmt.setString(1, username);
            set = stmt.executeQuery();
            if (set.next()) {
                user = new User(
                        set.getLong(1),
                        set.getString(2),
                        set.getString(3),
                        set.getString(4)
                );
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            closeDbObjects(set, stmt, conn);
        }

        return user;
    }
    public List<User> getUserList() {
        return getUserList(null);
    }
    public List<User> getUserList(String role) {
        ResultSet set = null;
        PreparedStatement stmt = null;
        Connection conn = null;
        List<User> users = new ArrayList<>();

        try {
            conn = getPostgresConnection();
            if (stringsExist(role)) {
                stmt = conn.prepareStatement(
                        "SELECT * FROM users " +
                        "WHERE user_role = ? " +
                        "ORDER BY user_role DESC, user_id");
                stmt.setString(1, role);
            } else {
                stmt = conn.prepareStatement(
                        "SELECT * FROM users " +
                        "ORDER BY user_role DESC, user_id");
            }
            set = stmt.executeQuery();
            while (set.next()) {
                users.add(
                    new User(
                        set.getLong(1),
                        set.getString(2),
                        set.getString(3),
                        set.getString(4)
                    )
                );
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            closeDbObjects(set, stmt, conn);
        }

        return users;
    }
    // SQL Updates
    public void updateRole(long id, String status) {
        PreparedStatement stmt = null;
        Connection conn = null;
        ReimbursementTicket ticket = null;
        String sqlUpdate =
                "UPDATE users " +
                "SET user_role = ? " +
                "WHERE user_id = ?";

        try {
            conn = getPostgresConnection();
            stmt = conn.prepareStatement(sqlUpdate);
            if (status.equalsIgnoreCase("manager") ||
                    status.equalsIgnoreCase("employee")) {
                switch (status.toLowerCase()) {
                    case "manager":
                        stmt.setString(1, "Manager");
                        break;
                    case "employee":
                        stmt.setString(1, "Employee");
                        break;
                }
                stmt.setLong(2, id);
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            closeDbObjects(stmt, conn);
        }
    }
    public void updateReimbursementTicket(ReimbursementTicket ticket) {}
    public void updateUser(User user) {}
    // SQL Deletes
    public void deleteReimbursementTicket(ReimbursementTicket ticket) {
        deleteReimbursementTicket(ticket.getId());
    }
    public void deleteReimbursementTicket(long id) {
        PreparedStatement stmt = null;
        Connection conn = null;
        String sqlDelete =
                "DELETE * FROM reimbursement_tickets " +
                "WHERE ticket_id = ?";

        try {
            conn = getPostgresConnection();
            stmt = conn.prepareStatement(sqlDelete);
            stmt.setLong(1, id);
            stmt.executeQuery();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            closeDbObjects(stmt, conn);
        }
    }
    public void deleteUser(User user, boolean deleteTickets) {
        deleteUser(user.getId(), deleteTickets);
    }
    public void deleteUser(long id, boolean deleteTickets) {
        PreparedStatement stmt = null;
        Connection conn = null;
        ReimbursementTicket ticket = null;
        String sqlDeleteUser =
                "DELETE * FROM users " +
                "WHERE user_id = ?";
        String sqlDeleteUserTickets =
                "DELETE * FROM reimbursement_tickets " +
                "WHERE ticket_user_id = ?";

        try {
            conn = getPostgresConnection();
            if (deleteTickets) {
                stmt = conn.prepareStatement(sqlDeleteUserTickets);
                stmt.setLong(1, id);
                stmt.executeUpdate();
            }
            stmt = conn.prepareStatement(sqlDeleteUser);
            stmt.setLong(1, id);
            stmt.executeQuery();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            closeDbObjects(stmt, conn);
        }
    }
    // Close Objects
    public void closeDbObjects(PreparedStatement stmt, Connection conn) {
        closeDbObjects(null, stmt, conn);
    }
    public void closeDbObjects(ResultSet set, PreparedStatement stmt, Connection conn) {
        try {
            if (conn != null) {
                conn.close();
            }
            if (set != null) {
                set.close();
            }
            if (stmt != null) {
                stmt.close();
            }
        } catch (SQLException | NullPointerException e) {
            e.printStackTrace();
        }
    }

// CONCERNED WITH LOGIN/REGISTER FEATURE
    public long insertUser(User user) {
        PreparedStatement stmt = null;
        Connection conn = null;
        long idSerial = -1;
        User foundUser;
        String sqlInsert =
                "INSERT INTO users " +
                "VALUES (DEFAULT, DEFAULT, ?, ?)";

        try {
            conn = getPostgresConnection();
            foundUser = getUserRecord(user);
            if (foundUser == null) {
                stmt = conn.prepareStatement(sqlInsert);
                stmt.setString(1, user.getUsername());
                stmt.setString(2, user.getPassword());
                stmt.executeUpdate();
            }
            idSerial = getUserRecord(user.getUsername()).getId();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            closeDbObjects(stmt, conn);
        }

        return idSerial;
    }

// CONCERNED WITH SUBMIT TICKET FEATURE
    public void insertReimbursementTicket(ReimbursementTicket ticket, User user) {
        insertReimbursementTicket(ticket, user.getId());
    }
    public void insertReimbursementTicket(ReimbursementTicket ticket, long userId) {
        insertReimbursementTicket(userId, ticket.getAmount(), ticket.getDescription());
    }
    public void insertReimbursementTicket(long userId, double amount, String description) {
        PreparedStatement stmt = null;
        Connection conn = null;
        User foundUser;
        String sqlInsert =
                "INSERT INTO reimbursement_tickets " +
                "VALUES (DEFAULT, ?, DEFAULT, ?, ?)";

        try {
            conn = getPostgresConnection();
            foundUser = getUserRecord(userId);
            if (foundUser != null) {
                stmt = conn.prepareStatement(sqlInsert);
                stmt.setLong(1, foundUser.getId());
                stmt.setDouble(2, amount);
                stmt.setString(3, description);
                stmt.execute();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            closeDbObjects(stmt, conn);
        }
    }

// CONCERNED WITH TICKETING SYSTEM FEATURE
    public void processReimbursementTicket(ReimbursementTicket ticket, String status) {
        long ticketId = ticket.getId();
        PreparedStatement stmt = null;
        Connection conn = null;
        String sqlUpdate =
                "UPDATE reimbursement_tickets " +
                "SET ticket_status = ? " +
                "WHERE ticket_id = ?";

        try {
            conn = getPostgresConnection();
            if (getReimbursementTicketRecord(ticketId)
                    .getStatus()
                    .equalsIgnoreCase("pending")) {
                stmt = conn.prepareStatement(sqlUpdate);
                switch (status.toLowerCase()) {
                    case "approve":
                        stmt.setString(1, "Approved");
                        break;
                    case "deny":
                        stmt.setString(1, "Denied");
                        break;
                }
                stmt.setLong(2, ticket.getId());
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            closeDbObjects(stmt, conn);
        }
    }
    public ReimbursementTicket getReimbursementTicketRecord(ReimbursementTicket ticket) {
        return getReimbursementTicketRecord(ticket.getId());
    }
    public ReimbursementTicket getReimbursementTicketRecord(long ticketId) {
        ResultSet set = null;
        PreparedStatement stmt = null;
        Connection conn = null;
        ReimbursementTicket ticket = null;
        String sqlQuery =
                "SELECT * FROM reimbursement_tickets " +
                "WHERE ticket_id = ?";

        try {
            conn = getPostgresConnection();
            stmt = conn.prepareStatement(sqlQuery);
            stmt.setLong(1, ticketId);
            set = stmt.executeQuery();
            if (set.next()) {
                ticket = new ReimbursementTicket(
                        set.getInt(1),
                        set.getLong(2),
                        set.getString(3),
                        set.getDouble(4),
                        set.getString(5)
                );
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            closeDbObjects(set, stmt, conn);
        }

        return ticket;
    }

// CONCERNED WITH VIEW PREVIOUS TICKETS FEATURE
    public List<ReimbursementTicket> getReimbursementTicketList(String status) {
        ResultSet set = null;
        PreparedStatement stmt = null;
        Connection conn = null;
        List<ReimbursementTicket> tickets = new ArrayList<>();

        try {
            conn = getPostgresConnection();
            if (status == null) {
                stmt = conn.prepareStatement(
                        "SELECT * FROM reimbursement_tickets " +
                        "ORDER BY ticket_user_id, ticket_id");
            } else {
                stmt = conn.prepareStatement(
                        "SELECT * FROM reimbursement_tickets " +
                        "WHERE ticket_status = ? " +
                        "ORDER BY ticket_user_id, ticket_id");
                stmt.setString(1, status);
            }
            set = stmt.executeQuery();
            while (set.next()) {
                tickets.add(
                        new ReimbursementTicket(
                                set.getInt(1),
                                set.getLong(2),
                                set.getString(3),
                                set.getDouble(4),
                                set.getString(5)
                        )
                );
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            closeDbObjects(set, stmt, conn);
        }

        return tickets;
    }
    public List<ReimbursementTicket> getReimbursementTicketList(String status, long userId) {
        /*
         * List a User's tickets
         */
        ResultSet set = null;
        PreparedStatement stmt = null;
        Connection conn = null;
        List<ReimbursementTicket> tickets = new ArrayList<>();

        try {
            conn = getPostgresConnection();
            if (status == null) {
                stmt = conn.prepareStatement(
                        "SELECT * FROM reimbursement_tickets " +
                        "WHERE ticket_user_id = ? " +
                        "ORDER BY ticket_id");
            } else {
                stmt = conn.prepareStatement(
                        "SELECT * FROM reimbursement_tickets " +
                        "WHERE ticket_user_id = ? " +
                        "AND ticket_status = ? " +
                        "ORDER BY ticket_id");
                stmt.setString(2, status);
            }
            stmt.setLong(1, userId);
            set = stmt.executeQuery();
            while (set.next()) {
                tickets.add(
                        new ReimbursementTicket(
                                set.getInt(1),
                                set.getLong(2),
                                set.getString(3),
                                set.getDouble(4),
                                set.getString(5)
                        )
                );
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            closeDbObjects(set, stmt, conn);
        }

        return tickets;
    }
}