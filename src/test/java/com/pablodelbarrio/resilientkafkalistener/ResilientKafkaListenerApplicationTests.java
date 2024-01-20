package com.pablodelbarrio.resilientkafkalistener;

import com.pablodelbarrio.resilientkafkalistener.config.KafkaConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class ResilientKafkaListenerApplicationTests {

	@Autowired
	private KafkaConfig kafkaConfig;

	@Test
	void sentEventOK() {
		kafkaConfig.kafkaTemplate().send("origin-topic", "OK").join();
	}

	@Test
	void sentEventKO() {
		kafkaConfig.kafkaTemplate().send("origin-topic", "KO").join();
	}

}
