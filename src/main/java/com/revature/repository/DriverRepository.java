package com.revature.repository;

import com.revature.model.*;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Statements for Postgres db io.
 *
 * @author johnainsworth
 */
public class DriverRepository {
    private final Properties props;

    // A properties file must be provided
    public DriverRepository(Properties props) {
        this.props = props;
    }

    // General
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
    public void closeDbObjects(ResultSet set, PreparedStatement stmt, Connection conn) {
        try {
            conn.close();
            set.close();
            stmt.close();
        } catch (SQLException | NullPointerException e) {
            e.printStackTrace();
        }
    }
    public void closeDbObjects(PreparedStatement stmt, Connection conn) {
        closeDbObjects(null, stmt, conn);
    }
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
    public User getUserRecord(User user) {
        return getUserRecord(user.getId());
    }
    public User getUserRecord(long id) {
        ResultSet set = null;
        Connection conn = null;
        PreparedStatement stmt = null;
        final String sqlQuery =
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

    // Concerned with Login/Register Feature
    public void insertUser(User user) {
        PreparedStatement stmt = null;
        Connection conn = null;
        User foundUser;
        final String sqlInsert =
                "INSERT INTO users " +
                "VALUES (DEFAULT, DEFAULT, ?, ?)";

        try {
            conn = getPostgresConnection();
            foundUser = getUserRecord(user);
            if (foundUser == null) {
                stmt = conn.prepareStatement(sqlInsert);
                stmt.setString(3, user.getUsername());
                stmt.setString(4, user.getPassword());
                stmt.execute(sqlInsert);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            closeDbObjects(stmt, conn);
        }
    }

    // Concerned with Submit Ticket Feature
    public void insertReimbursementTicket(ReimbursementTicket ticket, User user) {
        insertReimbursementTicket(ticket, user.getId());
    }
    public void insertReimbursementTicket(ReimbursementTicket ticket, long userId) {
        PreparedStatement stmt = null;
        Connection conn = null;
        User foundUser;
        final String sqlInsert =
                "INSERT INTO reimbursement_tickets " +
                "VALUES (DEFAULT, ?, DEFAULT, ?, ?)";

        try {
            conn = getPostgresConnection();
            foundUser = getUserRecord(userId);
            if (foundUser != null) {
                stmt = conn.prepareStatement(sqlInsert);
                stmt.setLong(2, foundUser.getId());
                stmt.setDouble(4, ticket.getAmount());
                stmt.setString(5, ticket.getDescription());
                stmt.execute(sqlInsert);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            closeDbObjects(stmt, conn);
        }
    }

    // Concerned with Ticketing System Feature
    public void processReimbursementTicket(ReimbursementTicket ticket, String status) {
        long ticketId = ticket.getId();
        PreparedStatement stmt = null;
        Connection conn = null;
        final String sqlUpdate =
                "UPDATE reimbursement_tickets " +
                "SET ticket_status = ? " +
                "WHERE ticket_id = ?";

        try {
            conn = getPostgresConnection();
            if (getReimbursementTicketRecord(ticketId)
                    .getStatus()
                    .equalsIgnoreCase("pending")) {
                stmt = conn.prepareStatement(sqlUpdate);
                if (status.equalsIgnoreCase("approve") ||
                    status.equalsIgnoreCase("deny")) {
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

    // Concerned with View Previous Tickets Feature
    public List<ReimbursementTicket> getReimbursementTicketList() {
        return getReimbursementTicketList(null);
    }
    public List<ReimbursementTicket> getReimbursementTicketList(String status) {
        ResultSet set = null;
        PreparedStatement stmt = null;
        Connection conn = null;
        List<ReimbursementTicket> tickets = new ArrayList<>();

        try {
            conn = getPostgresConnection();
            if (status == null) {
                stmt = conn.prepareStatement("SELECT * FROM reimbursement_tickets" +
                        "ORDER BY ticket_id DESC");
            } else {
                stmt = conn.prepareStatement("SELECT * FROM reimbursement_tickets " +
                        "WHERE ticket_status != ? " +
                        "ORDER BY ticket_id DESC");
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

}
