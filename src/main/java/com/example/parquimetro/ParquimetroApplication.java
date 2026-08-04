package com.example.parquimetro;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling; // IMPORTANTE: Importación añadida

@SpringBootApplication
@EnableScheduling // 🔥 ESTO ENCIENDE EL MOTOR DEL TIEMPO
public class ParquimetroApplication {

	public static void main(String[] args) {
		SpringApplication.run(ParquimetroApplication.class, args);
	}

}