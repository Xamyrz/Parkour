package me.cmastudios.mcparkour.menu;

import me.cmastudios.mcparkour.Parkour;
import me.cmastudios.mcparkour.data.ParkourChooseMenu;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;

public class Menu {
    private Player player;
    private Parkour plugin;
    private Inventory chooseMenu;
    private Inventory settingsMenu;

    public Menu(Player player, Parkour plugin){
        this.player = player;
        this.plugin = plugin;
        this.chooseMenu = Bukkit.createInventory(player,54, Parkour.getString("choosemenu.title"));
        this.settingsMenu = Bukkit.createInventory(player, 9, Parkour.getString("settings.inventory.name"));
    }

    public Inventory getChooseMenu(){
        return chooseMenu;
    }

    public void renderChooseMenu(){
        getChooseMenuData().render(chooseMenu,player, plugin);
    }

    public Inventory getSettingsMenu(){
        return settingsMenu;
    }
    public ParkourChooseMenu getChooseMenuData() {
        if(player.hasMetadata("choosemenu")) {
            for(MetadataValue value : player.getMetadata("choosemenu")) {
                if(value.getOwningPlugin()==plugin) {
                    return (ParkourChooseMenu) value.value();
                }
            }
        }
        ParkourChooseMenu menu = new ParkourChooseMenu();
        player.setMetadata("choosemenu",new FixedMetadataValue(plugin,menu));
        return menu;
    }



}
