package com.marketplace;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.sql.Connection;
import java.sql.DriverManager;

@SpringBootApplication
public class ParttimeMarketplaceApplication {

	private static final Logger log = LoggerFactory.getLogger(ParttimeMarketplaceApplication.class);

	public static void main(String[] args) {
		SpringApplication app = new SpringApplication(ParttimeMarketplaceApplication.class);
		if (System.getProperty("spring.profiles.active") == null
				&& System.getenv("SPRING_PROFILES_ACTIVE") == null
				&& !canConnectToMySql()) {
			log.warn("Không kết nối được MySQL tại localhost:3306 -> Tự động kích hoạt profile 'h2' (CSDL nhúng chế độ MySQL kèm dữ liệu mẫu data.sql).");
			app.setAdditionalProfiles("h2");
		}
		app.run(args);
	}

	private static boolean canConnectToMySql() {
		String host = System.getenv().getOrDefault("DB_HOST", "localhost");
		String port = System.getenv().getOrDefault("DB_PORT", "3306");
		String dbName = System.getenv().getOrDefault("DB_NAME", "parttime_db");
		String user = System.getenv().getOrDefault("DB_USERNAME", "root");
		String envPass = System.getenv("DB_PASSWORD");

		String url = String.format(
				"jdbc:mysql://%s:%s/%s?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Ho_Chi_Minh&characterEncoding=UTF-8&connectTimeout=1500",
				host, port, dbName
		);

		String[] candidatePasswords = (envPass != null)
				? new String[]{envPass}
				: new String[]{"123456", "", "root", "admin"};

		for (String password : candidatePasswords) {
			try (Connection conn = DriverManager.getConnection(url, user, password)) {
				if (conn.isValid(2)) {
					System.setProperty("spring.datasource.url", url);
					System.setProperty("spring.datasource.username", user);
					System.setProperty("spring.datasource.password", password);
					return true;
				}
			} catch (Exception ignored) {
				// Thử mật khẩu tiếp theo hoặc chuyển sang profile h2
			}
		}
		return false;
	}

}
