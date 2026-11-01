package com.marketplace;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.net.InetSocketAddress;
import java.net.Socket;

@SpringBootApplication
public class ParttimeMarketplaceApplication {

	private static final Logger log = LoggerFactory.getLogger(ParttimeMarketplaceApplication.class);

	public static void main(String[] args) {
		SpringApplication app = new SpringApplication(ParttimeMarketplaceApplication.class);
		if (System.getProperty("spring.profiles.active") == null
				&& System.getenv("SPRING_PROFILES_ACTIVE") == null
				&& !isMySqlPortOpen()) {
			log.warn("Không phát hiện dịch vụ MySQL tại localhost:3306 -> Tự động kích hoạt profile 'h2' (CSDL nhúng chế độ MySQL kèm dữ liệu mẫu data.sql).");
			app.setAdditionalProfiles("h2");
		}
		app.run(args);
	}

	private static boolean isMySqlPortOpen() {
		String host = System.getenv().getOrDefault("DB_HOST", "localhost");
		int port = Integer.parseInt(System.getenv().getOrDefault("DB_PORT", "3306"));
		try (Socket socket = new Socket()) {
			socket.connect(new InetSocketAddress(host, port), 500);
			return true;
		} catch (Exception ex) {
			return false;
		}
	}

}
