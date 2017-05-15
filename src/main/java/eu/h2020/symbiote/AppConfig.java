package eu.h2020.symbiote;

import com.mongodb.Mongo;
import com.mongodb.MongoClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.AbstractMongoConfiguration;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@Configuration
@EnableMongoRepositories
class AppConfig extends AbstractMongoConfiguration {

    @Value("${symbiote.mongo.dbname.registry}")
    private String databaseName;

    @Value("${symbiote.mongo.host}")
    private String mongoHost;

    @Override
    protected String getDatabaseName() {
        return databaseName;
    }

    @Override
    public Mongo mongo() throws Exception {
        return new Mongo();
    }

    //TODO change 'localhost' in MongoClient to sth read from configuration
    @Bean
    public MongoTemplate mongoTemplate() throws Exception {
        return new MongoTemplate(new MongoClient(mongoHost), getDatabaseName());
    }
}