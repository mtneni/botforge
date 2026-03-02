package org.legendstack.basebot;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

import static org.springframework.context.annotation.FilterType.ASSIGNABLE_TYPE;

/**
 * Test boot class that mirrors {@link BotForgeApplication} but excludes
 * the main application and security packages so tests can provide their own
 * configuration.
 */
@SpringBootConfiguration
@EnableAutoConfiguration(exclude = {
        org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration.class,
        org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration.class,
        org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration.class,
        org.springframework.boot.autoconfigure.neo4j.Neo4jAutoConfiguration.class
})
// @EnableDrivine -- Disabled for base integration tests
// @EnableDrivinePropertiesConfig -- Disabled for base integration tests
@org.springframework.context.annotation.Import(BotForgePackageScanConfiguration.class)
@ComponentScan(basePackages = "org.legendstack.basebot", excludeFilters = {
        @ComponentScan.Filter(type = org.springframework.context.annotation.FilterType.REGEX, pattern = "org\\.legendstack\\.basebot\\.rag\\..*"),
        @ComponentScan.Filter(type = org.springframework.context.annotation.FilterType.REGEX, pattern = "org\\.legendstack\\.basebot\\.proposition\\..*"),
        @ComponentScan.Filter(type = org.springframework.context.annotation.FilterType.REGEX, pattern = "org\\.legendstack\\.basebot\\.api\\..*"),
        @ComponentScan.Filter(type = ASSIGNABLE_TYPE, classes = { BotForgeApplication.class,
                McpToolsConfiguration.class, ChatConfiguration.class })
})
class TestBotForgeApplication {
}
