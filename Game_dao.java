package spaceWar;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class Game_dao {
	private static final String URL = "jdbc:mysql://localhost:3306/game?useSSL=false&serverTimezone=UTC";
	private static final String USER = "root";
	private static final String PASSWORD = "";

	private Connection getConnection() throws SQLException {
		return DriverManager.getConnection(URL, USER, PASSWORD);
	}

	// Lưu thông tin phiên chơi
	public void saveGame(int score, int maxScore, long startTime, int duration) throws SQLException {
		String sql = "INSERT INTO panelgame (score, maxScore, startTime, Duration) VALUES (?, ?, ?, ?)";
		try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
			stmt.setInt(1, score);
			stmt.setInt(2, maxScore);
			stmt.setTimestamp(3, new Timestamp(startTime)); // Lưu startTime dưới dạng DATETIME
			stmt.setInt(4, duration);
			stmt.executeUpdate();
		}
	}

// Lấy danh sách 5 game điểm cao nhất từ cơ sở dữ liệu
	public List<String> getRecentGames() throws SQLException {
		List<String> games = new ArrayList<>();
		String sql = "SELECT score, maxScore, startTime, Duration FROM panelgame ORDER BY score DESC LIMIT 5";
		try (Connection conn = getConnection();
				PreparedStatement stmt = conn.prepareStatement(sql);
				ResultSet rs = stmt.executeQuery()) {
			while (rs.next()) {
				String startTimeStr = rs.getTimestamp("startTime") != null ? rs.getTimestamp("startTime").toString()
						: "N/A";
				games.add("Score: " + rs.getInt("score") + ", Max Score: " + rs.getInt("maxScore") + ", Start Time: "
						+ startTimeStr + ", Duration: " + rs.getInt("Duration") + "s");
			}
		}
		return games;
	}

}