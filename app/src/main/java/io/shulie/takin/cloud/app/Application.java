package io.shulie.takin.cloud.app;

import lombok.extern.slf4j.Slf4j;

import springfox.documentation.oas.annotations.EnableOpenApi;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.system.ApplicationPid;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * SpringBoot启动类
 *
 * @author <a href="mailto:472546172@qq.com">张天赐</a>
 */
@Slf4j
@EnableOpenApi
@SpringBootApplication
@MapperScan("io.shulie.takin.cloud.app.mapper")
public class Application {
    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(Application.class);
        ConfigurableApplicationContext context = application.run(args);
        ApplicationPid pid = new ApplicationPid();
        log.info("PID({}:{})", context.getId(), pid);
    }
}