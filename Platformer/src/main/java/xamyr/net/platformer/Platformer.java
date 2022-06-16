package xamyr.net.platformer;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.java.JavaPlugin;
import xamyr.net.platformer.commands.*;
import xamyr.net.platformer.platform.Platform;

import java.sql.*;
import java.util.*;
import java.util.logging.Level;

public final class Platformer extends JavaPlugin {
    private Connection platformerDatabase;
    public Map<String, Platform> platforms = new HashMap<>();

    @Override
    public void onEnable() {
        this.saveDefaultConfig();
        this.connectDatabase();
        Bukkit.getPluginManager().registerEvents(new PlatformerEvents(this), this);
        getCommand("platformcreate").setExecutor(new PlatformCreate(this));
        getCommand("platformshowname").setExecutor(new PlatformShowName(this));
        getCommand("platformreload").setExecutor(new PlatformReload(this));
        getCommand("platformdelete").setExecutor(new PlatformDelete(this));
        getCommand("platformedit").setExecutor(new PlatformEdit(this));
        //getCommand("platformmove").setExecutor(new PlatformMoveCommand());

        try {
            this.loadPlatforms();
        } catch (SQLException e) {
            Bukkit.getLogger().info("Error loading platforms: "+e);
        }

    }

    @Override
    public void onDisable() {
        Bukkit.getWorlds().forEach((world -> {
            world.getEntities().forEach((entity -> {
                if(platforms.get(entity.getCustomName()) != null){
                    entity.remove();
                }
            }));
        }));
    }

    public void connectDatabase() {
        try {
            if (platformerDatabase != null && !platformerDatabase.isClosed()) {
                platformerDatabase.close();
            }
        } catch (SQLException ex) {
            this.getLogger().log(Level.SEVERE, "Failed to close existing connection to database", ex);
        }
        try {
            this.platformerDatabase = DriverManager.getConnection(String.format("jdbc:mysql://%s:%d/%s",
                            this.getConfig().getString("mysql.host"), this.getConfig().getInt("mysql.port"), this.getConfig().getString("mysql.database")),
                    this.getConfig().getString("mysql.username"), this.getConfig().getString("mysql.password"));
            try (Statement initStatement = this.platformerDatabase.createStatement()) {
                initStatement.executeUpdate("CREATE TABLE IF NOT EXISTS platforms (`name` VARCHAR(25) NOT NULL, `world` text NOT NULL, `newversion` BOOLEAN NOT NULL, `blocks` text NOT NULL, `direction` VARCHAR(6), `movenoblocks` double NOT NULL, `wait` int NOT NULL, `speed` double NOT NULL,PRIMARY KEY (`name`))");
            }
        } catch (SQLException ex) {
            this.getLogger().log(Level.SEVERE, "Failed to load course database", ex);
        }
    }

    public void loadPlatforms() throws SQLException {

        try (PreparedStatement statement = getDatabase().prepareStatement("SELECT * FROM platforms")){
            try(ResultSet result = statement.executeQuery()){
                while(result.next()) {
                    String name = result.getString("name");
                    World world = Bukkit.getWorld(result.getString("world"));
                    Boolean newVersion = result.getBoolean("newversion");
                    String direction = result.getString("direction");
                    double moveNoBlocks = result.getDouble("movenoblocks");
                    int wait = result.getInt("wait");
                    double speed = result.getDouble("speed");
                    List<Block> blocks = new ArrayList<>();
                    for (String s : result.getString("blocks").split(",")) {

                        String[] block = s.split("/");
                        Material material = Material.valueOf(block[0]);
                        int x = (int) Double.parseDouble(block[1]);
                        int y = (int) Double.parseDouble(block[2]);
                        int z = (int) Double.parseDouble(block[3]);
                        Location location = world.getBlockAt(x, y, z).getLocation();
                        world.setType(location, material);
                        Block b = world.getBlockAt(location);
                        blocks.add(b);
                    }

                    removeOldPlatform(world, name);
                    platforms.put(name, new Platform(this, blocks, newVersion,direction,moveNoBlocks,wait,speed,name));
                    platforms.get(name).movePlatform();
                }
            }
        }
    }

    public void loadPlatform(String name){
        try (PreparedStatement stmt = getDatabase().prepareStatement("SELECT * from `platforms` WHERE `name`=?")){
            stmt.setString(1, name);
            ResultSet result = stmt.executeQuery();

            if(result.next()){
                World world = Bukkit.getWorld(result.getString("world"));
                Boolean newVersion = result.getBoolean("newversion");
                String direction = result.getString("direction");
                double moveNoBlocks = result.getDouble("movenoblocks");
                int wait = result.getInt("wait");
                double speed = result.getDouble("speed");
                List<Block> blocks = new ArrayList<>();
                for (String s : result.getString("blocks").split(",")) {

                    String[] block = s.split("/");
                    Material material = Material.valueOf(block[0]);
                    int x = (int) Double.parseDouble(block[1]);
                    int y = (int) Double.parseDouble(block[2]);
                    int z = (int) Double.parseDouble(block[3]);
                    Location location = world.getBlockAt(x, y, z).getLocation();
                    world.setType(location, material);
                    Block b = world.getBlockAt(location);
                    blocks.add(b);
                }
                removeOldPlatform(world, name);
                platforms.put(name, new Platform(this, blocks, newVersion,direction,moveNoBlocks,wait,speed,name));
                platforms.get(name).movePlatform();
            }
        } catch (SQLException ex) {
            Bukkit.getLogger().info(ex.toString());
        }

    }

    public void deletePlatform(String name){
        try (PreparedStatement stmt = getDatabase().prepareStatement("DELETE FROM `platforms` WHERE name = ?")) {
            stmt.setString(1, name);
            stmt.executeUpdate();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

    public boolean removeOldPlatform(World world, String name){
        boolean deleted = false;
        for(Entity e : world.getEntities() ){
            if(Objects.equals(e.getCustomName(), name)){
                e.remove();
                deleted = true;
            }
        }
        return deleted;
    }

    public Connection getDatabase(){
        try {
            if (!platformerDatabase.isValid(1)) {
                this.connectDatabase();
            }
        } catch (SQLException ex) {
            this.connectDatabase();
        }
        return platformerDatabase;
    }
}
