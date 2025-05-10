package spaceWar;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.swing.JComponent;

public class panelGame extends JComponent {

	private int witdh = 800, height = 600;
	// game fps
	private final int FPS = 60;
	private final int TARGET_TIME = 1000000000 / FPS; // 16,666,666.666 nanoSec
	private Thread thread;
	private boolean start = true;
	private int score = 0;
	private int maxScore = 0;
	private boolean mus = true;
	private final int BOSS_SCORE_INTERVAL = 10; // Khoảng cách điểm để boss xuất hiện
	private int lastBossScore = 0; // Điểm số tại lần cuối boss xuất hiện
//	private int lastSpeedIncreaseScore = 0; // Theo dõi điểm số lần cuối tăng tốc

	private long startTime = System.currentTimeMillis(); // Gán giá trị mặc định ngay từ đầu;
	private int shotTime = 0;
	private int maxShot = 3; // Giới hạn số lượng đạn tối đa
	private int currentShot = 0; // Số lượng đạn hiện tại
	private boolean isPaused = false; // Trạng thái tạm dừng

	// game object
	private Game_dao gameDao = new Game_dao();
	private Player player;
	private List<bullet> bullets;
	private List<Rocket> rockets;
	private List<Effect> boomEffects;

	private Sound sound;
	private background Background;

	// key
	private key_controller key;

	private Graphics2D g2;
	private BufferedImage image;

	private boolean gameStarted = false; // Trạng thái game: chờ hoặc đang chơi
	// Thêm biến để theo dõi trạng thái mute
	// Biến cho hiệu ứng nhấp nháy
	private double blinkTime = 0.0;
	final boolean[] isMuted = { false }; // Sử dụng mảng để thay đổi giá trị trong lambda
	final float defaultVolume = -5.0f; // Âm lượng mặc định khi unmute (có thể điều chỉnh)

	public void start() {
		witdh = this.getWidth();
		height = this.getHeight();

		image = new BufferedImage(witdh, height, BufferedImage.TYPE_INT_ARGB);
		g2 = image.createGraphics();
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);// khu rang cua, ...
		g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

		thread = new Thread(new Runnable() {

			@Override
			public void run() {
				while (start) {
					long startTime = System.nanoTime();
					// Cập nhật blinkTime cho hiệu ứng nhấp nháy
					blinkTime += 0.1;

					if (!isPaused) { // Chỉ cập nhật game nếu không tạm dừng
						drawBackGround();
						drawGame();
						render();
					}
					long time = System.nanoTime() - startTime;
					if (time < TARGET_TIME) { // delay the loop
						long sleep = (TARGET_TIME - time) / 1000000;// nano -> mili sec
						sleep(sleep);
//						System.out.println(sleep);
					}
//					sleep(TARGET_TIME);// test
				}

			}
		});

//		initObject();
		initKeyboard();
//		initBullet();

		thread.start();

	}

	private void addRocket() {
		Random ran = new Random();
		int locationY = ran.nextInt(height - 50) + 25;
		Rocket rocket = new Rocket();
		rocket.changeLocation(0, locationY);
		rocket.changeAngle(0);
		rockets.add(rocket);
		int locationY2 = ran.nextInt(height - 50) + 25;
		Rocket rocket2 = new Rocket();
		rocket2.changeLocation(witdh, locationY2);
		rocket2.changeAngle(180);
		rockets.add(rocket2);
		if (score % BOSS_SCORE_INTERVAL == 0 && score > lastBossScore) {

			Boss boss = new Boss(
					new Heath(100 + 5 * (score / BOSS_SCORE_INTERVAL), 100 + 5 * (score / BOSS_SCORE_INTERVAL)));
			boss.changeLocation(0, locationY);
			boss.changeAngle(0);
			rockets.add(boss);
			lastBossScore = score; // Cập nhật điểm số cuối cùng mà boss xuất hiện
		}
		if (score % BOSS_SCORE_INTERVAL == 5 && score > lastBossScore) {
			fastBoss boss2 = new fastBoss();
//					new Heath(100 + 5 * (score / BOSS_SCORE_INTERVAL), 100 + 5 * (score / BOSS_SCORE_INTERVAL)));
			boss2.changeLocation(0, locationY);
			boss2.changeAngle(0);
			rockets.add(boss2);
			lastBossScore = score; // Cập nhật điểm số cuối cùng mà boss xuất hiện
		}

	}

	private void initObject() {

		sound = new Sound();
		sound.playBackgroundMusic();
		// Khởi tạo background một lần duy nhất
		Background = new background("gameImage/summerlandscape.png");

		player = new Player();
		player.changeLocation(150, 150);
		rockets = new ArrayList<>();
		boomEffects = new ArrayList<>();
//		// Lấy maxScore cao nhất từ cơ sở dữ liệu
//		try {
//			maxScore = gameDao.getOverallMaxScore();
//		} catch (SQLException e) {
//			e.printStackTrace();
//		}

		new Thread(new Runnable() {

			@Override
			public void run() {
				while (start) {
					if (gameStarted && !isPaused) {
						addRocket();
					}
					sleep(3000);
				}

			}
		}).start();
		initBullet();
	}

	private void initBullet() {
		bullets = new ArrayList<>();
		new Thread(new Runnable() {

			@Override
			public void run() {
				while (start) {
					if (gameStarted && !isPaused) {
						for (int i = 0; i < bullets.size(); i++) {
							bullet BL = bullets.get(i);
							if (BL != null) {
								BL.update();
								checkBullet(BL);
								if (!BL.check(witdh, height)) {
									bullets.remove(BL);
									currentShot--;
								}
							} else {
								bullets.remove(BL);
							}
							System.out.println("bullet size: " + bullets.size());
							System.out.println("currentShot: " + currentShot);

						}
						// effect
						for (int i = 0; i < boomEffects.size(); i++) {
							Effect boomeffect = boomEffects.get(i);
							if (boomeffect != null) {
								boomeffect.update();
								if (!boomeffect.check()) {
									boomEffects.remove(boomeffect);

								}
							} else {
								boomEffects.remove(boomeffect);
							}
						}
					}
					sleep(5);
				}

			}
		}).start();
	}

	private void initKeyboard() {
		key = new key_controller();
		requestFocus();
		addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_A) {
					key.setKey_left(true);
				} else if (e.getKeyCode() == KeyEvent.VK_D) {
					key.setKey_right(true);
				} else if (e.getKeyCode() == KeyEvent.VK_SPACE) {
					key.setKey_space(true);
				} else if (e.getKeyCode() == KeyEvent.VK_J) {
					key.setKey_j(true);
				} else if (e.getKeyCode() == KeyEvent.VK_K) {
					key.setKey_k(true);

				} else if (e.getKeyCode() == KeyEvent.VK_ENTER) {
					key.setKey_enter(true);
//					if (firstRun)
//						firstRun = false;

				} else if (e.getKeyCode() == KeyEvent.VK_M) {
					key.setKey_m(true);
				} else if (e.getKeyCode() == KeyEvent.VK_L) {
					key.setKey_l(true);
				} else if (e.getKeyCode() == KeyEvent.VK_P) {
					isPaused = !isPaused; // Đảo ngược trạng thái tạm dừng
					sleep(200); // Tránh nhấn P liên tục
				}

			}

			@Override
			public void keyReleased(KeyEvent e) {

				if (e.getKeyCode() == KeyEvent.VK_A) {
					key.setKey_left(false);
				} else if (e.getKeyCode() == KeyEvent.VK_D) {
					key.setKey_right(false);
				} else if (e.getKeyCode() == KeyEvent.VK_SPACE) {
					key.setKey_space(false);
				} else if (e.getKeyCode() == KeyEvent.VK_J) {
					key.setKey_j(false);
				} else if (e.getKeyCode() == KeyEvent.VK_K) {
					key.setKey_k(false);
				} else if (e.getKeyCode() == KeyEvent.VK_ENTER) {
					key.setKey_enter(false);
				} else if (e.getKeyCode() == KeyEvent.VK_M) {
					key.setKey_m(false);
				} else if (e.getKeyCode() == KeyEvent.VK_L) {
					key.setKey_l(false);
				}

			}
		});

		new Thread(new Runnable() {

			@Override
			public void run() {
				float s = 0.5f;

				while (start) {
					if (key.isKey_m()) {
						if (!isMuted[0]) {
							// Mute: Đặt âm lượng về -80.0f (tắt tiếng)
							sound.setBackgroundVolume(-80.0f);
							isMuted[0] = true;
						} else {
							// Unmute: Đặt lại âm lượng mặc định
							sound.setBackgroundVolume(defaultVolume);
							isMuted[0] = false;
						}
					}
					if (key.isKey_enter() && !gameStarted) {
						gameStarted = true;
						initObject(); // Khởi tạo đối tượng game khi bắt đầu

					}

					if (gameStarted && !isPaused && player != null) {// Chỉ cập nhật nếu game đã bắt đầu và không tạm
																		// dừng
						if (player.isAlive()) {
							float ang = player.getAngle();

							if (key.isKey_left()) {
								ang -= s;
							}
							if (key.isKey_right()) {
								ang += s;
							}
							if (key.isKey_space()) {
								player.speedUp();

							} else {
								player.speedDown();
							}
							player.update();
							//
							if (key.isKey_l() && !player.isShieldActive() && System.currentTimeMillis()
									- player.getLastShieldActivation() > player.getShieldCooldown()) {
								player.activateShield();
							}
							//
							if (key.isKey_j() || key.isKey_k()) {
								if (shotTime == 0 && currentShot < maxShot) {// Kiểm tra giới hạn đạn
									if (key.isKey_j()) {
										bullets.add(0,
												new bullet(player.getX(), player.getY(), player.getAngle(), 5, 2f));
									} else {
										bullets.add(0,
												new bullet(player.getX(), player.getY(), player.getAngle(), 15, 1.5f));
									}
									sound.SoundShoot();
									currentShot++;
								}
								shotTime++;

								if (shotTime >= 15) {
									shotTime = 0;// Reset để cho phép bắn tiếp
								}
							} else {
								shotTime = 0;// Reset khi không nhấn phím
							}
							player.changeAngle(ang);
						} else {
							if (key.isKey_enter()) {
								resetGame();
							}
						}
						for (int i = 0; i < rockets.size(); i++) {
							Rocket rocket = rockets.get(i);
							if (rocket != null) {
								rocket.update();
								if (!rocket.check(witdh, height)) {
									rockets.remove(rocket);

								} else {
									if (player.isAlive()) {
										checkPlayer(rocket);

									}
								}
							}
						}
					}
					sleep(5);
				}
			}
		}).start();

	}

	private void drawBackGround() {
//		g2.setBackground(new Color(30, 30, 30));
//		g2.setColor(new Color(30, 30, 30));
//
//		g2.fillRect(0, 0, witdh, height);
		if (Background != null) {
//		Background = new background("gameImage/summerlandscape.png");
			Background.draw(g2, 800, 600);
		} else {
			g2.setColor(new Color(30, 30, 35));
			g2.fillRect(0, 0, 800, 600); // Vẽ nền mặc định nếu Background chưa khởi tạo
			// 2. Vẽ một vài ngôi sao trắng ngẫu nhiên
			g2.setColor(Color.WHITE);
			Random random = new Random();

			int x = random.nextInt(800); // Vị trí x ngẫu nhiên
			int y = random.nextInt(600); // Vị trí y ngẫu nhiên
			g2.fillOval(x, y, 3, 3); // Vẽ sao là hình tròn nhỏ (2x2 pixel)

		}

	}

	private void drawGame() {
		if (!gameStarted) {
			GAMEhint(g2); // Hiển thị hướng dẫn trước khi game bắt đầu
		} else if (player != null) {// Chỉ vẽ nếu player đã được khởi tạo
			if (player.isAlive()) {
				player.draw(g2);

				// bullet
				for (int i = 0; i < bullets.size(); i++) {
					bullet BL = bullets.get(i);
					if (BL != null) {
						BL.draw(g2);
						// BL.update();
					}
				}
			}

			// enemy
			for (int i = 0; i < rockets.size(); i++) {
				Rocket rocket = rockets.get(i);
				if (rocket != null) {
					rocket.draw(g2);
				}
			}
			// effect
			for (int i = 0; i < boomEffects.size(); i++) {
				Effect boom = boomEffects.get(i);
				if (boom != null) {
					boom.draw(g2);
				}
			}

			g2.setColor(Color.white);
			g2.setFont(getFont().deriveFont(Font.BOLD, 15f));
			g2.drawString("Score : " + score, 10, 20);
			g2.drawString("max_Score : " + maxScore, 90, 20);

			if (isPaused) {
				String text = "Paused";
				String textKey = "Press P to Resume";
				g2.setFont(getFont().deriveFont(Font.BOLD, 50f));
				FontMetrics fm = g2.getFontMetrics();
				Rectangle2D r2 = fm.getStringBounds(text, g2);
				double textWidth = r2.getWidth();
				double textHeight = r2.getHeight();
				double x = (800 - textWidth) / 2;
				double y = (600 - textHeight) / 2;
				g2.drawString(text, (int) x, (int) y + fm.getAscent());
				g2.setFont(getFont().deriveFont(Font.BOLD, 15f));
				fm = g2.getFontMetrics();
				r2 = fm.getStringBounds(textKey, g2);
				textWidth = r2.getWidth();
				textHeight = r2.getHeight();
				x = (800 - textWidth) / 2;
				y = (600 - textHeight) / 2;
				g2.drawString(textKey, (int) x, (int) y + fm.getAscent() + 50);
			} else if (!player.isAlive()) {
				String text = "GAME OVER";
				String textKey = "Press key enter to Continue...";
				g2.setFont(getFont().deriveFont(Font.BOLD, 50f));
				FontMetrics fm = g2.getFontMetrics();
				Rectangle2D r2 = fm.getStringBounds(text, g2);
				double textWidth = r2.getWidth();
				double textHeight = r2.getHeight();
				double x = (witdh - textWidth) / 2;
				double y = (height - textHeight) / 2;
				g2.drawString(text, (int) x, (int) y + fm.getAscent());
				g2.setFont(getFont().deriveFont(Font.BOLD, 15f));
				fm = g2.getFontMetrics();
				r2 = fm.getStringBounds(textKey, g2);
				textWidth = r2.getWidth();
				textHeight = r2.getHeight();
				x = (witdh - textWidth) / 2;
				y = (height - textHeight) / 2;
				g2.drawString(textKey, (int) x, (int) y + fm.getAscent() + 50);

			}
			checkBossBullets();
		}

	}

	private void render() {
		Graphics g = getGraphics();
		g.drawImage(image, 0, 0, null);
		g.dispose();
	}

	private void sleep(long speed) {
		try {
			Thread.sleep(speed);
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	private void checkBullet(bullet BL) {
		for (int i = 0; i < rockets.size(); i++) {
			Rocket rocket = rockets.get(i);
			if (rocket != null) {
				Area area = new Area(BL.getShape());
				area.intersect(rocket.getShape());

				if (!area.isEmpty()) {
					boomEffects.add(
							new Effect(BL.getCenterX(), BL.getCenterY(), 3, 5, 60, 0.5f, new Color(250, 207, 105)));
					if (!rocket.updateHP(BL.getSize())) {

						score++;
						updateScore();
						if (rocket instanceof Boss || rocket instanceof fastBoss) {
							this.player.addHP();
						}

						rockets.remove(rocket);
						sound.SoundDestroy();
						double x = rocket.getX() + rocket.ROCKET_SIZE / 2;
						double y = rocket.getY() + rocket.ROCKET_SIZE / 2;
						boomEffects.add(new Effect(x, y, 5, 5, 75, 0.3f, new Color(32, 170, 169)));
						boomEffects.add(new Effect(x, y, 7, 7, 85, 0.25f, new Color(30, 150, 139)));
						boomEffects.add(new Effect(x, y, 5, 5, 75, 0.2f, new Color(240, 107, 159)));

					} else {
						sound.SoundHit();
					}
					bullets.remove(BL);
					currentShot--;
				}
			}
		}

	}

	private void checkBossBullets() {
		for (int i = 0; i < rockets.size(); i++) {
			Rocket rocket = rockets.get(i);
			if (rocket instanceof Boss) { // Kiểm tra nếu là Boss
				Boss boss = (Boss) rocket;
				List<bullet> bossBullets = boss.getBossBullets();
				for (int j = 0; j < bossBullets.size(); j++) {
					bullet bullet = bossBullets.get(j);
					if (bullet != null) {
						// Kiểm tra va chạm với Player
						Area area = new Area(bullet.getShape());
						area.intersect(player.getShape());
						if (!area.isEmpty()) { // Nếu có va chạm
//							player.updateHP(bullet.getSize());// Giảm máu Player
							if (!player.isShieldActive()) {
								if (!player.updateHP(bullet.getSize())) {
									sound.SoundDie();
									player.setAlive(false);
									double x = player.getX() + player.PLAYER_SIZE / 2;
									double y = player.getY() + player.PLAYER_SIZE / 2;
									boomEffects.add(new Effect(x, y, 5, 5, 75, 0.3f, new Color(32, 170, 169)));
									boomEffects.add(new Effect(x, y, 7, 7, 85, 0.25f, new Color(30, 150, 139)));
									boomEffects.add(new Effect(x, y, 5, 5, 75, 0.2f, new Color(240, 107, 159)));
								}
							}
							bossBullets.remove(j); // Xóa đạn
							j--; // Giảm chỉ số để không bỏ sót phần tử

						}
//						else if (!bullet.check(WIDTH, HEIGHT)) { // Giả sử check() kiểm tra biên
//							bossBullets.remove(j);
//							j--;
//						}
					}
				}
			}
		}
	}

	private void checkPlayer(Rocket rocket) {

		if (rocket != null) {
			Area area = new Area(player.getShape());
			area.intersect(rocket.getShape());
			if (!area.isEmpty()) {
				double rocketHP = rocket.getHP();
				if (!rocket.updateHP(player.getHP())) {
					rockets.remove(rocket);
					sound.SoundDestroy();
					double x = rocket.getX() + rocket.ROCKET_SIZE / 2;
					double y = rocket.getY() + rocket.ROCKET_SIZE / 2;
					boomEffects.add(new Effect(x, y, 5, 5, 75, 0.3f, new Color(32, 170, 169)));
					boomEffects.add(new Effect(x, y, 7, 7, 85, 0.25f, new Color(30, 150, 139)));
					boomEffects.add(new Effect(x, y, 5, 5, 75, 0.2f, new Color(240, 107, 159)));
				}
				if (!player.isShieldActive()) {
					if (!player.updateHP(rocketHP)) {
						sound.SoundDie();
						player.setAlive(false);
						double x = player.getX() + player.PLAYER_SIZE / 2;
						double y = player.getY() + player.PLAYER_SIZE / 2;
						boomEffects.add(new Effect(x, y, 5, 5, 75, 0.3f, new Color(32, 170, 169)));
						boomEffects.add(new Effect(x, y, 7, 7, 85, 0.25f, new Color(30, 150, 139)));
						boomEffects.add(new Effect(x, y, 5, 5, 75, 0.2f, new Color(240, 107, 159)));

						try {
							int duration = (int) ((System.currentTimeMillis() - startTime) / 1000);
							gameDao.saveGame(score, maxScore, startTime, duration);
//							gameDao.updateMaxScore(maxScore);
						} catch (SQLException e) {
							e.printStackTrace();
						}
					}
				}
			}
		}

	}

	private void updateScore() {
		if (score > maxScore) {
			maxScore = score;
		}
//		 Kiểm tra mốc điểm để tăng tốc rocket
//		if (score >= lastSpeedIncreaseScore + 2) {
//			Rocket.increaseSpeed();
//			lastSpeedIncreaseScore = score;
//		}
	}

	private void GAMEhint(Graphics2D g2) {
		// Thiết lập độ trong suốt cho toàn bộ khu vực
		g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.9f));

		// --- Khu vực Hướng dẫn ---
		int guideBoxX = 50;
		int guideBoxY = 50;
		int guideBoxWidth = 300;
		int guideBoxHeight = 250;

		// Vẽ nền cho khu vực Hướng dẫn
		g2.setColor(new Color(0, 0, 0, 180));
		g2.fillRect(guideBoxX, guideBoxY, guideBoxWidth, guideBoxHeight);

		// Vẽ viền cho khu vực Hướng dẫn
		g2.setColor(Color.CYAN);
		g2.drawRect(guideBoxX, guideBoxY, guideBoxWidth, guideBoxHeight);

		// Vẽ tiêu đề "Hướng dẫn"
		g2.setColor(Color.YELLOW);
		g2.setFont(new Font("Arial", Font.BOLD, 24));
		g2.drawString("Hướng dẫn", guideBoxX + 20, guideBoxY + 30);

		// Vẽ nội dung hướng dẫn
		g2.setColor(Color.WHITE);
		g2.setFont(new Font("Arial", Font.PLAIN, 16));
		g2.drawString("A/D: Xoay tàu", guideBoxX + 20, guideBoxY + 60);
		g2.drawString("Space: Tăng tốc độ", guideBoxX + 20, guideBoxY + 90);
		g2.drawString("J/K: Bắn đạn", guideBoxX + 20, guideBoxY + 120);
		// g2.drawString("L: Kích hoạt khiên", guideBoxX + 20, guideBoxY + 150);
		g2.drawString("P: Tạm dừng", guideBoxX + 20, guideBoxY + 150);

		// --- Khu vực Top 5 ---
		int tableBoxX = 800 - 350;
		int tableBoxY = 50;
		int tableBoxWidth = 300;
		int tableBoxHeight = 250;

		// Vẽ nền cho khu vực Top 5
		g2.setColor(new Color(0, 0, 0, 180));
		g2.fillRect(tableBoxX, tableBoxY, tableBoxWidth, tableBoxHeight);

		// Vẽ viền cho khu vực Top 5
		g2.setColor(Color.CYAN);
		g2.drawRect(tableBoxX, tableBoxY, tableBoxWidth, tableBoxHeight);

		// Vẽ tiêu đề "Top 5 Games"
		g2.setColor(Color.YELLOW);
		g2.setFont(new Font("Arial", Font.BOLD, 24));
		g2.drawString("Top 5 siuVipPro", tableBoxX + 20, tableBoxY + 30);

		// Vẽ bảng Top 5
		try {
			List<String> recentGames = gameDao.getRecentGames();
			g2.setFont(new Font("Arial", Font.PLAIN, 14));

			// Vẽ tiêu đề cột
			g2.setColor(Color.ORANGE);
			g2.drawString("Score   Max   Start Time            Duration", tableBoxX + 20, tableBoxY + 60);

			// Vẽ đường kẻ ngang dưới tiêu đề cột
			g2.setColor(Color.GRAY);
			g2.drawLine(tableBoxX + 20, tableBoxY + 65, tableBoxX + tableBoxWidth - 20, tableBoxY + 65);

			// Vẽ danh sách top 5
			g2.setColor(Color.WHITE);
			int yOffset = tableBoxY + 85;
			for (int i = 0; i < recentGames.size(); i++) {
				String game = recentGames.get(i);
				String[] parts = game.split(", ");
				String score = parts[0].split(": ")[1];
				String maxScore = parts[1].split(": ")[1];
				String startTime = parts[2].split(": ")[1];
				String duration = parts[3].split(": ")[1].replace("s", "");

				String formattedLine = String.format("%-7s %-5s %-21s %-5s", score, maxScore, startTime, duration);
				g2.drawString(formattedLine, tableBoxX + 20, yOffset);
				yOffset += 30;

				// Vẽ đường kẻ ngang giữa các dòng
				if (i < recentGames.size() - 1) {
					g2.setColor(Color.GRAY);
					g2.drawLine(tableBoxX + 20, yOffset - 10, tableBoxX + tableBoxWidth - 20, yOffset - 10);
					g2.setColor(Color.WHITE);
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
			g2.setColor(Color.RED);
			g2.drawString("Error loading top games", tableBoxX + 20, tableBoxY + 60);
		}

		// --- Hiệu ứng nhấp nháy cho "Nhấn Enter để bắt đầu" ---
		String startText = "Nhấn Enter để bắt đầu";
		g2.setFont(new Font("Arial", Font.BOLD, 20));
		FontMetrics fm = g2.getFontMetrics();
		Rectangle2D startTextBounds = fm.getStringBounds(startText, g2);
		int startTextX = (int) (800 - startTextBounds.getWidth()) / 2;
		int startTextY = 600 - 50;

		// Tính toán độ trong suốt dựa trên Math.sin
		float alpha = (float) (0.5 + 0.5 * Math.sin(blinkTime)); // Giá trị từ 0.0 đến 1.0
		g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
		g2.setColor(Color.GREEN);
		g2.drawString(startText, startTextX, startTextY);

		// Đặt lại độ trong suốt về mặc định
		g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));

	}

	private void resetGame() {

		score = 0;
		rockets.clear();
		bullets.clear();
		player.changeLocation(150, 150);
		player.resetLive();
		lastBossScore = 0;
		currentShot = 0;
//		lastSpeedIncreaseScore = 0;
//		Rocket.resetSpeed(); // Đặt lại tốc độ rocket
		startTime = System.currentTimeMillis();

	}
}
