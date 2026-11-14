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
		if (!hasExplicitActiveProfile(args)
				&& !canConnectToMySql()) {
			log.warn("Không kết nối được MySQL tại localhost:3306 -> Tự động kích hoạt profile 'h2' (CSDL nhúng chế độ MySQL kèm dữ liệu mẫu data.sql).");
			app.setAdditionalProfiles("h2");
		}
		app.run(args);
	}

	private static boolean hasExplicitActiveProfile(String[] args) {
		if (hasText(System.getProperty("spring.profiles.active"))
				|| hasText(System.getenv("SPRING_PROFILES_ACTIVE"))) {
			return true;
		}
		for (String arg : args) {
			if (arg.startsWith("--spring.profiles.active=")
					&& hasText(arg.substring("--spring.profiles.active=".length()))) {
				return true;
			}
		}
		return false;
	}

	private static boolean hasText(String value) {
		return value != null && !value.isBlank();
	}

	private static boolean canConnectToMySql() {
		String url = firstNonBlank(
				System.getProperty("spring.datasource.url"),
				System.getenv("SPRING_DATASOURCE_URL"),
				String.format(
						"jdbc:mysql://%s:%s/%s?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Ho_Chi_Minh&characterEncoding=UTF-8&connectTimeout=1500",
						System.getenv().getOrDefault("DB_HOST", "localhost"),
						System.getenv().getOrDefault("DB_PORT", "3306"),
						System.getenv().getOrDefault("DB_NAME", "student_job_marketplace")
				)
		);
		if (!url.startsWith("jdbc:mysql:")) {
			return true;
		}
		String user = firstNonBlank(System.getProperty("spring.datasource.username"),
				System.getenv("SPRING_DATASOURCE_USERNAME"), System.getenv("DB_USERNAME"), "root");
		String password = firstNonBlank(System.getProperty("spring.datasource.password"),
				System.getenv("SPRING_DATASOURCE_PASSWORD"), System.getenv("DB_PASSWORD"), "");

		try (Connection conn = DriverManager.getConnection(withConnectTimeout(url), user, password)) {
			return conn.isValid(2);
		} catch (Exception ignored) {
			return false;
		}
	}

	private static String withConnectTimeout(String url) {
		return url + (url.contains("?") ? "&" : "?") + "connectTimeout=1500";
	}

	private static String firstNonBlank(String... values) {
		for (String value : values) {
			if (hasText(value)) {
				return value;
			}
		}
		return "";
	}

}
