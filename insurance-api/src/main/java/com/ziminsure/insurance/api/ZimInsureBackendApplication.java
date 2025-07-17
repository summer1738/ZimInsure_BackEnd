package com.ziminsure.insurance.api;

import com.ziminsure.insurance.domain.User;
import com.ziminsure.insurance.service.UserService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.boot.autoconfigure.domain.EntityScan;

@SpringBootApplication(scanBasePackages = "com.ziminsure.insurance")
@EnableJpaRepositories(basePackages = "com.ziminsure.insurance.repository")
@EntityScan(basePackages = "com.ziminsure.insurance.domain")
public class ZimInsureBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(ZimInsureBackendApplication.class, args);
	}

	@Bean
	public CommandLineRunner createDefaultSuperAdmin(UserService userService) { 
		return args -> {
			String email = "ziminsure@gmail.com";
			if (userService.findByEmail(email).isEmpty()) {
				User admin = new User();
				admin.setEmail(email);
				admin.setPassword("virus1738");
				admin.setFullName("Super Admin");
				admin.setIdNumber("SUPERADMIN001");
				admin.setAddress("Harare, Zimbabwe");
				admin.setPhone("+263781296767");
				admin.setRole(User.Role.SUPER_ADMIN);
				userService.registerUser(admin);
				System.out.println("Default SUPER_ADMIN created: " + email);
			}
		};
	}
}
