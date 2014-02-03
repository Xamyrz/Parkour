/*
 * Copyright (C) 2014 Maciej Mionskowski
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package me.cmastudios.mcparkour.data;

import com.google.common.collect.ImmutableList;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

public class CustomCourse {

    private ParkourCourse course;
    private ArrayList<PotionEffect> effects;

    //Template EFFECT:LEVEL;EFFECT:LEVEL
    public static CustomCourse loadCourse(Connection conn, int id) throws SQLException {
        ParkourCourse pk = ParkourCourse.loadCourse(conn, id);
        try (PreparedStatement stmt = conn.prepareStatement("SELECT * FROM custom WHERE id=?")) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    ArrayList<PotionEffect> effect = new ArrayList<>();
                    String[] effs = rs.getString("effects").split(";");
                    for (String eff : effs) {
                        String[] potionEff = eff.split(":");
                        effect.add(new PotionEffect(PotionEffectType.getByName(potionEff[0]), Integer.MAX_VALUE, Integer.parseInt(potionEff[1])));
                    }
                    return new CustomCourse(pk, effect);
                }
            }
        }
        return null;
    }

    public CustomCourse(ParkourCourse course, ArrayList<PotionEffect> effects) {
        this.course = course;
        this.effects = effects;
    }

    public ImmutableList<PotionEffect> getEffects() {
        return ImmutableList.copyOf(effects);
    }

    //TODO: Add commands for adding effects
    public void addEffect(PotionEffect effect) {
        for (PotionEffect eff : effects) {
            if (eff.getType() == effect.getType()) {
                effects.remove(eff);
                break;
            }
        }
        effects.add(effect);
    }

    public boolean removeEffect(PotionEffectType type) {
        for (PotionEffect eff : effects) {
            if (eff.getType() == type) {
                effects.remove(eff);
                return true;
            }
        }
        return false;
    }

    public void clearEffects() {
        effects.clear();
    }

    public void setEffects(ArrayList<PotionEffect> effects) {
        this.effects = effects;
    }

    public void save(Connection conn) {
        try (PreparedStatement stmt = conn.prepareStatement("INSERT INTO custom (`id`,`effects`) VALUES (?,?) ON DUPLICATE KEY UPDATE effects=VALUES(effects)")) {
            stmt.setInt(1, course.getId());
            StringBuilder builder = new StringBuilder();
            for (PotionEffect effect : effects) {
                builder.append(effect.getType().getName()).append(":").append(effect.getAmplifier()).append(";");
            }
            stmt.setString(2, builder.toString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
