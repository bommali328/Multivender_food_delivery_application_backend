package com.foodie;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = FoodieApplication.class) // 🔴 ఇక్కడ మెయిన్ క్లాస్ ని explicitly బైండ్ చేయండి
class FoodieApplicationTests {

	@Test
	void contextLoads() {
	}

}