/*
 * Copyright (C) 2014 Connor Monahan
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

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.Objects;

public class GameResult {
	public enum GameType {
		DUEL,
		GUILDWAR
	}

	private Timestamp time;
	private String type;
	private String uuidWin;
	private String winner;
	private String uuidLoss;
	private String loser;

	private GameResult() {
		this.time = new Timestamp(new Date().getTime());
	}

	public GameResult(OfflinePlayer winner, OfflinePlayer loser) {
		this();
		this.type = GameType.DUEL.name();
		this.uuidWin = winner.getUniqueId().toString();
		this.winner = winner.getName();
		this.uuidLoss = loser.getUniqueId().toString();
		this.loser = loser.getName();
	}

	public GameResult(Guild winner, Guild loser) {
		this();
		this.type = GameType.GUILDWAR.name();
		this.winner = winner.getTag();
		this.loser = loser.getTag();
	}

	public void save(Connection conn) throws SQLException {
		try (PreparedStatement stmt = conn.prepareStatement("INSERT INTO gameresults (time, type, uuidwin, uuidloss) VALUES (?, ?, ?, ?)")) {
			if(Objects.equals(type, "GUILDWAR")) {
				stmt.setTimestamp(1, time);
				stmt.setString(2, type);
				stmt.setString(3, winner);
				stmt.setString(4, loser);
				stmt.setString(5, winner);
				stmt.setString(6, loser);
			}else if(Objects.equals(type, "DUEL")){
				stmt.setTimestamp(1, time);
				stmt.setString(2, type);
				stmt.setString(3, uuidWin);
				stmt.setString(4, uuidLoss);
			}
			stmt.executeUpdate();
		}
	}
}
