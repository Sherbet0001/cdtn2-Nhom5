package com.marketplace;

import com.marketplace.repositories.CategoryRepository;
import com.marketplace.repositories.LocationRepository;
import com.marketplace.repositories.ProfileRepository;
import com.marketplace.repositories.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Kiểm thử xác nhận cấu hình MySQL 8.0 thật (Profile "mysql"):
 * Kiểm tra kết nối MySQL, khởi tạo 8 bảng từ schema.sql và tính lũy đẳng (idempotent) của data.sql khi chạy nhiều lần.
 */
@SpringBootTest
@ActiveProfiles("mysql")
@EnabledIf("isMySqlReachable")
@SuppressWarnings("null")
class MySqlConfigurationIntegrationTests {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private LocationRepository locationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProfileRepository profileRepository;

    static boolean isMySqlReachable() {
        String host = System.getenv().getOrDefault("DB_HOST", "localhost");
        String port = System.getenv().getOrDefault("DB_PORT", "3306");
        String dbName = System.getenv().getOrDefault("DB_NAME", "parttime_db");
        String user = System.getenv().getOrDefault("DB_USERNAME", "root");
        String password = System.getenv().getOrDefault("DB_PASSWORD", "");
        String url = String.format(
                "jdbc:mysql://%s:%s/%s?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Ho_Chi_Minh&connectTimeout=2000",
                host, port, dbName
        );
        try (Connection conn = DriverManager.getConnection(url, user, password)) {
            return conn.isValid(2);
        } catch (Exception ex) {
            return false;
        }
    }

    @Test
    @DisplayName("MySQL 8.0 Config & Idempotent SQL: Xác nhận kết nối MySQL thật, đủ 8 bảng chuẩn và chạy lại data.sql nhiều lần không lỗi")
    void verifyMySqlSchemaAndIdempotentSeedData() throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            String productName = connection.getMetaData().getDatabaseProductName();
            assertThat(productName).containsIgnoringCase("MySQL");

            // Kiểm tra đủ 8 bảng chuẩn trong schema.sql
            Set<String> tables = new HashSet<>();
            try (ResultSet rs = connection.getMetaData().getTables(connection.getCatalog(), null, "tbl_%", new String[]{"TABLE"})) {
                while (rs.next()) {
                    tables.add(rs.getString("TABLE_NAME").toLowerCase());
                }
            }
            assertThat(tables).contains(
                    "tbl_categories",
                    "tbl_locations",
                    "tbl_users",
                    "tbl_profiles",
                    "tbl_jobs",
                    "tbl_applications",
                    "tbl_reviews",
                    "tbl_notifications"
            );

            // Chạy lại schema.sql và data.sql lần thứ 2 trên cùng kết nối để xác nhận INSERT IGNORE không gây lỗi trùng khóa
            ScriptUtils.executeSqlScript(connection, new ClassPathResource("schema.sql"));
            ScriptUtils.executeSqlScript(connection, new ClassPathResource("data.sql"));
        }

        assertThat(categoryRepository.count()).isGreaterThanOrEqualTo(8);
        assertThat(locationRepository.count()).isGreaterThanOrEqualTo(10);
        assertThat(userRepository.count()).isGreaterThanOrEqualTo(6);
        assertThat(profileRepository.count()).isGreaterThanOrEqualTo(6);
    }
}

