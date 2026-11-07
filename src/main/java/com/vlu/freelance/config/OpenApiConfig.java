package com.vlu.freelance.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("API Nền tảng Marketplace Việc làm Part-time cho Sinh viên")
                        .version("1.0.0")
                        .description("Tài liệu API Phân hệ Tin tuyển dụng (Job Module) - Sprint 1 (Buổi 6)\n" +
                                "SVTH: Đoàn Xuân Tiến (2374802010493)\n" +
                                "Nhóm: Viên Thành Đạt · Đoàn Xuân Tiến · Bùi Đăng Khoa\n" +
                                "GVHD: ThS. Hà Đồng Hưng")
                        .contact(new Contact()
                                .name("Đoàn Xuân Tiến")
                                .email("tien.2374802010493@vanlanguni.vn")));
    }
}

