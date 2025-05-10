package spaceWar;

import java.util.Random;

import javax.swing.ImageIcon;

public class fastBoss extends Rocket {
	private Random random;

	public fastBoss() {
		super(new Heath(50, 50)); // Boss có 50 HP();
		this.setImageR(new ImageIcon(getClass().getResource("gameImage/poop3.png")).getImage());
		this.setSpeed(0.9f); // Tốc độ nhanh hơn Rocket thông thường
		this.random = new Random();
	}

	@Override
	public void update() {
		// Di chuyển ngẫu nhiên
//		setAngle(getAngle() + (random.nextFloat() - 0.4f) * 10); // Thay đổi góc ngẫu nhiên trong khoảng [-5, 5]
		if (getAngle() < 0)
			setAngle(getAngle() + 360); // Giữ góc trong khoảng [0, 360)
		if (getAngle() >= 360)
			setAngle(getAngle() - 360);

		// Tính toán vị trí tiếp theo
		// Tính vận tốc theo trục X và Y dựa trên góc và tốc độ
		double velocityX = Math.cos(Math.toRadians(getAngle())) * getSpeed();
		double velocityY = Math.sin(Math.toRadians(getAngle())) * getSpeed();

		// Tính toán vị trí tiếp theo
		double nextX = getX() + velocityX;
		double nextY = getY() + velocityY;

		// Kiểm tra biên và đảo ngược vận tốc nếu chạm biên
		if (nextX < 0) {
			// Chạm biên trái: đảo ngược vận tốc theo trục X
			velocityX = -velocityX;
			nextX = 0; // Đặt lại vị trí vào biên trái
			this.setAngle((getAngle() + random.nextFloat() - 0.8f) * 10);
		} else if (nextX > 800 - ROCKET_SIZE) {
			// Chạm biên phải: đảo ngược vận tốc theo trục X
			velocityX = -velocityX;
			nextX = 800 - ROCKET_SIZE; // Đặt lại vị trí vào biên phải
			this.setAngle((getAngle() + random.nextFloat() - 0.8f) * 10);

		}

		if (nextY < 0) {
			// Chạm biên trên: đảo ngược vận tốc theo trục Y
			velocityY = -velocityY;
			nextY = 0; // Đặt lại vị trí vào biên trên
			this.setAngle((getAngle() + random.nextFloat() - 0.8f) * 10);

		} else if (nextY > 600 - ROCKET_SIZE) {
			// Chạm biên dưới: đảo ngược vận tốc theo trục Y
			velocityY = -velocityY;
			nextY = 600 - ROCKET_SIZE; // Đặt lại vị trí vào biên dưới
			this.setAngle((getAngle() + random.nextFloat() - 0.8f) * 10);

		}

		// Cập nhật vị trí của boss
		setX(nextX);
		setY(nextY);

	}

}
