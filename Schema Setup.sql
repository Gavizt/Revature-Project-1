/*
 * Testing for Project 1 features.
 * 
 * Schemas for User and ReimbursementTicket.
 * There are 4 tables:
 * 	user_roles
 * 	users
 * 	reimbursement_ticket_statuses
 * 	reimbursement_tickets
 * 
 * 		When a User registers, a record in the "users" table
 * 		is created with the default role "Employee". 
 * 
 * 		An Employee submits a Reimbursement ticket. This
 * 		Ticket is assigned the default status "Pending". A
 * 		Manager processes Tickets by approving or denying
 * 		them. This assigns a Ticket's status to "Approved"
 * 		or "Denied", depending on what the Manager chooses.
 */


/*
 * user_roles table:
 * 	Stores all of the User roles.
 * 		'name' is the primary key
 * 
 * 		Adding more is easy and the "users" table always
 * 		requires reference to this table which enforces
 * 		the roles on the Users. Everyone with a registered
 * 		account in the system must at least be considered
 * 		an "Employee".
 */
DROP TABLE IF EXISTS user_roles;
CREATE TABLE IF NOT EXISTS user_roles (
	role_name VARCHAR PRIMARY KEY
);

-- Current roles:
INSERT INTO user_roles VALUES
('Employee'),
('Manager');

/*
 * users table:
 * 	Stores basic attributes for registered Users:
 * 		'id' as the primary key
 * 		'role' as a foreign key to "user_roles"
 * 		'username' as the User's username (must be unique)
 * 		'password' as the User's password
 * 	All of these columns must have a value (NOT NULL)
 * 
 * 		This table works closely with the "user_roles" table
 * 		to enforce that each User is at least registered
 * 		as an "Employee" and can only be registered as the
 * 		roles present on that table.
 */
DROP TABLE IF EXISTS users;
CREATE TABLE IF NOT EXISTS users (
	user_id serial PRIMARY KEY,
	
	user_role VARCHAR NOT NULL
	REFERENCES user_roles(role_name)
	CONSTRAINT user_roles_default DEFAULT 'Employee',
	
	user_username VARCHAR UNIQUE NOT NULL,
	
	user_password VARCHAR NOT NULL
);

/*
 * reimbursement_ticket_statuses table:
 * 	Stores all of the ReimbursementTicket statuses.
 * 		'name' as the primary key
 * 
 * 		The "reimbursement_tickets" table refers to this table for
 * 		its Ticket statuses. The default status "Pending" is
 * 		enforced by that table.
 */
DROP TABLE IF EXISTS reimbursement_ticket_statuses;
CREATE TABLE IF NOT EXISTS reimbursement_ticket_statuses (
	status_name VARCHAR(32) PRIMARY KEY
);

-- Current statuses:
INSERT INTO reimbursement_ticket_statuses VALUES
('Pending'),
('Approved'),
('Denied');

/*
 * reimbursement_tickets table:
 * 	Stores all of the ReimbursementTickets.
 * 		'id' as the primary key
 * 		'user_id' as the foreign key to "users"
 * 		'status' as the Ticket's current status (default is "Pending")
 * 		'amount' as the dollar amount (cannot be negative)
 * 		'description' as the description of the request
 * 
 * 		This is where all ReimbursementTickets are submitted by
 * 		Employees and reviewed and processed by Managers. When a Ticket
 * 		is submitted by an Employee, it should follow the default status
 * 		of "Pending".
 * 
 * 		I was storing submitted Tickets ("Pending") and processed Tickets
 * 		("Approved" or "Denied") in separate tables, but I found using a
 * 		single table a bit simpler.
 */
DROP TABLE IF EXISTS reimbursement_tickets;
CREATE TABLE IF NOT EXISTS reimbursement_tickets (
	ticket_id serial PRIMARY KEY,
	
	ticket_user_id INTEGER NOT NULL
	REFERENCES users(user_id),
	
	ticket_status VARCHAR(32) NOT NULL
	REFERENCES reimbursement_ticket_statuses(status_name)
	CONSTRAINT ticket_status_default DEFAULT 'Pending',
	
	ticket_amount NUMERIC NOT NULL
	CONSTRAINT check_ticket_amount
	CHECK (ticket_amount >= 0),
	
	ticket_description TEXT NOT NULL
	CONSTRAINT default_ticket_description
	DEFAULT 'No description.'
);