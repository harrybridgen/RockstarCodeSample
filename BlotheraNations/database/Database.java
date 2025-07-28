package com.blothera.database;

import com.blothera.NationPlugin;
import com.blothera.database.DiplomacyDAOs.DiplomacyDAO;
import com.blothera.database.DiplomacyDAOs.DiplomacyLecternDAO;
import com.blothera.database.DiplomacyDAOs.DiplomacyRequestsDAO;
import com.blothera.database.NationDAOs.NationDAO;
import com.blothera.database.NationDAOs.NationLecternDAO;
import com.blothera.database.NationDAOs.NationMemberDAO;
import com.blothera.database.TownDAOs.*;
import com.blothera.database.WarDAOs.WarAlliesDAO;
import com.blothera.database.WarDAOs.WarBattleDAO;
import com.blothera.database.WarDAOs.WarDAO;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class Database {

    private final NationPlugin plugin;
    private Connection connection;

    private NationDAO nationDAO;
    private NationLecternDAO nationLecternDAO;
    private NationMemberDAO nationMemberDAO;
    private TownLecternDAO townLecternDAO;
    private TownClaimDAO townClaimDAO;
    private TownDAO townDAO;
    private TownJoinRequestDAO townJoinRequestDAO;
    private TownMemberDAO townMemberDAO;
    private DiplomacyLecternDAO diplomacyLecternDAO;
    private DiplomacyDAO diplomacyDAO;
    private DiplomacyRequestsDAO diplomacyRequestsDAO;
    private WarDAO warDAO;
    private WarBattleDAO warBattleDAO;
    private WarAlliesDAO warAlliesDAO;


    public Database(NationPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Connects to the SQLite database and initializes the DAOs.
     * This method creates the database file if it doesn't exist and sets up the necessary tables.
     */
    public void connect() {
        try {
            File dbFile = new File(plugin.getDataFolder(), "blothera.db");
            if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();

            connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
            try (Statement pragma = connection.createStatement()) {
                pragma.execute("PRAGMA foreign_keys = ON;");
            }
            plugin.getLogger().info("Connected to SQLite database.");

            setupTables();
            this.townJoinRequestDAO = new TownJoinRequestDAO(plugin, connection);
            this.nationLecternDAO = new NationLecternDAO(plugin, connection);
            this.nationMemberDAO = new NationMemberDAO(plugin, connection);
            this.townLecternDAO = new TownLecternDAO(plugin, connection);
            this.nationDAO = new NationDAO(plugin, connection);
            this.townClaimDAO = new TownClaimDAO(plugin, connection);
            this.townDAO = new TownDAO(plugin, connection);
            this.townMemberDAO = new TownMemberDAO(plugin, connection);
            this.diplomacyLecternDAO = new DiplomacyLecternDAO(plugin, connection);
            this.diplomacyDAO = new DiplomacyDAO(plugin, connection);
            this.diplomacyRequestsDAO = new DiplomacyRequestsDAO(plugin, connection);
            this.warDAO = new WarDAO(plugin, connection);
            this.warBattleDAO = new WarBattleDAO(plugin, connection);
            this.warAlliesDAO = new WarAlliesDAO(plugin, connection);


        } catch (SQLException e) {
            plugin.getLogger().severe("Could not connect to database: " + e.getMessage());
        }
    }

    /**
     * Sets up the necessary tables in the SQLite database.
     * This method is called during the initial connection to ensure all required tables are created.
     */
    private void setupTables() {
        try (Statement stmt = connection.createStatement()) {
            setupDiplomacyLecternTable(stmt);
            setupNationLecternTable(stmt);
            setupTownLecternTable(stmt);
            setupNationsTable(stmt);
            setupTownsTable(stmt);
            setupNationMembersTable(stmt);
            setupTownClaimsTable(stmt);
            setupJoinRequestsTable(stmt);
            setupTownMembersTable(stmt);
            setupDiplomacyRequestsTable(stmt);
            setupDiplomaticRelationsTable(stmt);
            setupWarTable(stmt);
            setupWarBattlesTable(stmt);
            setupWarAlliesTable(stmt);
        } catch (SQLException e) {
            plugin.getLogger().severe("Could not create tables: " + e.getMessage());
        }
    }

    private static void setupDiplomaticRelationsTable(Statement stmt) throws SQLException {
        stmt.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS diplomatic_relations (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        nation_a TEXT NOT NULL,
                        nation_b TEXT NOT NULL,
                        relation_type TEXT NOT NULL,
                        established_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        UNIQUE(nation_a, nation_b),
                        FOREIGN KEY (nation_a) REFERENCES nations(uuid) ON DELETE CASCADE,
                        FOREIGN KEY (nation_b) REFERENCES nations(uuid) ON DELETE CASCADE
                    );
                """);
    }

    private static void setupDiplomacyRequestsTable(Statement stmt) throws SQLException {
        stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS diplomacy_requests (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    from_nation_uuid TEXT NOT NULL,
                    to_nation_uuid TEXT NOT NULL,
                    relation_type TEXT NOT NULL,
                    status TEXT DEFAULT 'PENDING',
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    UNIQUE (from_nation_uuid, to_nation_uuid, relation_type),
                    FOREIGN KEY (from_nation_uuid) REFERENCES nations(uuid) ON DELETE CASCADE,
                    FOREIGN KEY (to_nation_uuid) REFERENCES nations(uuid) ON DELETE CASCADE
                );
                """);
    }

    private static void setupTownMembersTable(Statement stmt) throws SQLException {
        stmt.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS town_members (
                        player_uuid TEXT NOT NULL,
                        town_uuid TEXT NOT NULL,
                        joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        PRIMARY KEY (player_uuid, town_uuid),
                        FOREIGN KEY (town_uuid) REFERENCES towns(uuid) ON DELETE CASCADE
                    );
                """);
    }

    private static void setupJoinRequestsTable(Statement stmt) throws SQLException {
        stmt.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS join_requests (
                        player_uuid TEXT NOT NULL,
                        town_uuid TEXT NOT NULL,
                        requested_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        PRIMARY KEY (player_uuid, town_uuid),
                        FOREIGN KEY (town_uuid) REFERENCES towns(uuid) ON DELETE CASCADE
                    );
                """);
    }

    private static void setupTownClaimsTable(Statement stmt) throws SQLException {
        stmt.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS town_claims (
                        town_uuid TEXT NOT NULL,
                        world TEXT NOT NULL,
                        chunk_x INTEGER NOT NULL,
                        chunk_z INTEGER NOT NULL,
                        PRIMARY KEY (world, chunk_x, chunk_z),
                        FOREIGN KEY (town_uuid) REFERENCES towns(uuid) ON DELETE CASCADE
                    );
                """);
    }

    private static void setupNationMembersTable(Statement stmt) throws SQLException {
        stmt.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS nation_members (
                        uuid TEXT PRIMARY KEY,
                        nation_uuid TEXT NOT NULL,
                        joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        FOREIGN KEY (nation_uuid) REFERENCES nations(uuid) ON DELETE CASCADE
                    );
                """);
    }

    private static void setupTownsTable(Statement stmt) throws SQLException {
        stmt.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS towns (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        uuid TEXT UNIQUE NOT NULL,
                        nation_uuid TEXT NOT NULL,
                        name TEXT NOT NULL,
                        is_capital INTEGER DEFAULT 0,
                        founded_at DATE DEFAULT (DATE('now')),
                        leader_uuid TEXT NOT NULL,
                        tax_paid_until DATE DEFAULT (DATE('now', '+7 day')),
                        is_dormant INTEGER DEFAULT 0,
                        FOREIGN KEY (nation_uuid) REFERENCES nations(uuid) ON DELETE SET NULL
                    );
                """);
    }

    private static void setupNationsTable(Statement stmt) throws SQLException {
        stmt.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS nations (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        uuid TEXT UNIQUE NOT NULL,
                        name TEXT NOT NULL,
                        leader_uuid TEXT NOT NULL,
                        crown_used INTEGER DEFAULT 0,
                        founded_at DATE DEFAULT (DATE('now'))
                    );
                """);
    }

    private static void setupTownLecternTable(Statement stmt) throws SQLException {
        stmt.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS town_lecterns (
                        world TEXT,
                        x INTEGER,
                        y INTEGER,
                        z INTEGER,
                        PRIMARY KEY (world, x, y, z)
                    );
                """);
    }

    private static void setupNationLecternTable(Statement stmt) throws SQLException {
        stmt.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS nation_lecterns (
                        world TEXT,
                        x INTEGER,
                        y INTEGER,
                        z INTEGER,
                        PRIMARY KEY (world, x, y, z)
                    );
                """);
    }

    private static void setupDiplomacyLecternTable(Statement stmt) throws SQLException {
        stmt.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS diplomacy_lecterns (
                        world TEXT,
                        x INTEGER,
                        y INTEGER,
                        z INTEGER,
                        PRIMARY KEY (world, x, y, z)
                    );
                """);
    }

    private static void setupWarTable(Statement stmt) throws SQLException {
        stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS wars (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                relation_id INTEGER NOT NULL,
                attacker_nation_uuid TEXT NOT NULL,
                defender_nation_uuid TEXT NOT NULL,
                casus_belli TEXT NOT NULL,
                start_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                total_battles INTEGER NOT NULL,
                is_completed INTEGER DEFAULT 0,
                winner_uuid TEXT,
                FOREIGN KEY (relation_id) REFERENCES diplomatic_relations(id) ON DELETE CASCADE,
                FOREIGN KEY (attacker_nation_uuid) REFERENCES nations(uuid) ON DELETE CASCADE,
                FOREIGN KEY (defender_nation_uuid) REFERENCES nations(uuid) ON DELETE CASCADE
                );
                """);
    }

    private static void setupWarBattlesTable(Statement stmt) throws SQLException {
        stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS war_battles (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                war_id INTEGER NOT NULL,
                scheduled_date DATE NOT NULL,
                slot TEXT NOT NULL,
                location TEXT NOT NULL,
                is_completed INTEGER DEFAULT 0,
                winner_uuid TEXT,
                FOREIGN KEY (war_id) REFERENCES wars(id) ON DELETE CASCADE
                );
                """);
    }

    private static void setupWarAlliesTable(Statement stmt) throws SQLException {
        stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS war_allies (
                war_id INTEGER NOT NULL,
                ally_nation_uuid TEXT NOT NULL,
                side TEXT CHECK(side IN ('attacker', 'defender')) NOT NULL,
                accepted INTEGER DEFAULT 0,
                PRIMARY KEY (war_id, ally_nation_uuid),
                FOREIGN KEY (war_id) REFERENCES wars(id) ON DELETE CASCADE,
                FOREIGN KEY (ally_nation_uuid) REFERENCES nations(uuid) ON DELETE CASCADE
                );
                """);
    }


    public Connection getConnection() {
        return connection;
    }

    public NationDAO getNationDAO() {
        return nationDAO;
    }

    public NationLecternDAO getNationLecternDAO() {
        return nationLecternDAO;
    }

    public NationMemberDAO getNationMemberDAO() {
        return nationMemberDAO;
    }

    public TownLecternDAO getTownLecternDAO() {
        return townLecternDAO;
    }

    public TownClaimDAO getTownClaimDAO() {
        return townClaimDAO;
    }

    public TownDAO getTownDAO() {
        return townDAO;
    }

    public TownJoinRequestDAO getJoinRequestDAO() {
        return townJoinRequestDAO;
    }

    public TownMemberDAO getTownMemberDAO() {
        return townMemberDAO;
    }

    public DiplomacyLecternDAO getDiplomacyLecternDAO() {
        return diplomacyLecternDAO;
    }

    public DiplomacyDAO getDiplomacyDAO() {
        return diplomacyDAO;
    }

    public DiplomacyRequestsDAO getDiplomacyRequestsDAO() {
        return diplomacyRequestsDAO;
    }

    public WarDAO getWarDAO() {
        return warDAO;
    }

    public WarBattleDAO getWarBattleDAO() {
        return warBattleDAO;
    }

    public WarAlliesDAO getWarAlliesDAO() {
        return warAlliesDAO;
    }

    public void disconnect() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                plugin.getLogger().info("Disconnected from SQLite database.");
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Error closing database: " + e.getMessage());
        }
    }
}
