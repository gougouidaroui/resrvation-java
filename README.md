Conference Room Reservation App
   A Java Swing application for reserving conference rooms, built with Maven and SQLite. Features include user authentication, room reservations, PDF generation, admin management, and room search.
Features

User and admin login/registration
Reserve rooms with date/time and equipment options
Prevent overlapping reservations
Search rooms by name, price, capacity
Download reservation PDFs
Admin: manage users and delete reservations
Logout functionality

Prerequisites

Java 17
Maven 3.6+
SQLite

Setup

Clone the repository:git clone https://github.com/your-username/conference-room-app.git
cd conference-room-app


Build the project:mvn clean install


Run the app:mvn clean package exec:java -Dexec.mainClass="com.conferenceroom.App"



Login with default admin: admin/admin

Database

Uses SQLite (conference_room.db, auto-generated).
Schema: rooms, users, reservations.

Dependencies

sqlite-jdbc: SQLite database
jbcrypt: Password hashing
jdatepicker: Date picker UI
itext7-core: PDF generation

   See pom.xml for details.
Usage

Users: Browse rooms, search, reserve, view/cancel reservations, download PDFs.
Admins: Manage users, delete any reservation.

License
   MIT License (or your preferred license)

