package bibliotheca;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BibliothecaApplication {

    public static void main(String[] args) {
        SpringApplication.run(BibliothecaApplication.class, args);
    }
}
