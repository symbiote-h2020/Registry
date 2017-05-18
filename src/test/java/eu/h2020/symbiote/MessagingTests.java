package eu.h2020.symbiote;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import eu.h2020.symbiote.core.model.Platform;
import eu.h2020.symbiote.messaging.RabbitManager;
import eu.h2020.symbiote.model.PlatformResponse;
import eu.h2020.symbiote.model.RegistryPlatform;
import eu.h2020.symbiote.repository.RepositoryManager;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static eu.h2020.symbiote.TestSetupConfig.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Created by mateuszl on 16.02.2017.
 */
@RunWith(MockitoJUnitRunner.class)
public class MessagingTests {

    private static Logger log = LoggerFactory.getLogger(MessagingTests.class);

    private Random rand;
    RepositoryManager mockedRepository;

    @InjectMocks
    private RabbitManager rabbitManager;

    @Before
    public void setup() throws IOException, TimeoutException {
        ReflectionTestUtils.setField(rabbitManager, "rabbitHost", "localhost");
        ReflectionTestUtils.setField(rabbitManager, "rabbitUsername", "guest");
        ReflectionTestUtils.setField(rabbitManager, "rabbitPassword", "guest");

        ReflectionTestUtils.setField(rabbitManager, "platformExchangeName", PLATFORM_EXCHANGE_NAME);
        ReflectionTestUtils.setField(rabbitManager, "platformExchangeType", "topic");
        ReflectionTestUtils.setField(rabbitManager, "plaftormExchangeDurable", true);
        ReflectionTestUtils.setField(rabbitManager, "platformExchangeAutodelete", false);
        ReflectionTestUtils.setField(rabbitManager, "platformExchangeInternal", false);

        ReflectionTestUtils.setField(rabbitManager, "resourceExchangeName", RESOURCE_EXCHANGE_NAME);
        ReflectionTestUtils.setField(rabbitManager, "resourceExchangeType", "topic");
        ReflectionTestUtils.setField(rabbitManager, "resourceExchangeDurable", true);
        ReflectionTestUtils.setField(rabbitManager, "resourceExchangeAutodelete", false);
        ReflectionTestUtils.setField(rabbitManager, "resourceExchangeInternal", false);

        ReflectionTestUtils.setField(rabbitManager, "platformCreationRequestedRoutingKey", PLATFORM_CREATION_REQUESTED);
        ReflectionTestUtils.setField(rabbitManager, "platformModificationRequestedRoutingKey", PLATFORM_MODIFICATION_REQUESTED);
        ReflectionTestUtils.setField(rabbitManager, "platformRemovalRequestedRoutingKey", PLATFORM_REMOVAL_REQUESTED);
        ReflectionTestUtils.setField(rabbitManager, "resourceCreationRequestedRoutingKey", RESOURCE_CREATION_REQUESTED);
        ReflectionTestUtils.setField(rabbitManager, "resourceModificationRequestedRoutingKey", RESOURCE_MODIFICATION_REQUESTED);
        ReflectionTestUtils.setField(rabbitManager, "resourceRemovalRequestedRoutingKey", RESOURCE_REMOVAL_REQUESTED);


        ReflectionTestUtils.setField(rabbitManager, "platformCreatedRoutingKey", PLATFORM_CREATED_ROUTING_KEY);

        ReflectionTestUtils.invokeMethod(rabbitManager, "init");

        mockedRepository = mock(RepositoryManager.class);
        rand = new Random();
    }

    @After
    public void teardown() {
        log.info("Rabbit cleaned!");
        try {
            Connection connection = rabbitManager.getConnection();
            Channel channel;
            if (connection != null && connection.isOpen()) {
                channel = connection.createChannel();
                channel.queueDelete(PLATFORM_CREATION_REQUESTED);
                channel.queueDelete(PLATFORM_MODIFICATION_REQUESTED);
                channel.queueDelete(PLATFORM_REMOVAL_REQUESTED);
                channel.queueDelete(RESOURCE_CREATION_REQUESTED);
                channel.queueDelete(RESOURCE_MODIFICATION_REQUESTED);
                channel.queueDelete(RESOURCE_REMOVAL_REQUESTED);
                channel.queueDelete(RESOURCE_CREATION_REQUESTED_QUEUE);
                channel.queueDelete(RESOURCE_MODIFICATION_REQUESTED_QUEUE);
                channel.queueDelete(RESOURCE_REMOVAL_REQUESTED_QUEUE);
                channel.queueDelete(PLATFORM_CREATION_REQUESTED_QUEUE);
                channel.queueDelete(PLATFORM_MODIFICATION_REQUESTED_QUEUE);
                channel.queueDelete(PLATFORM_REMOVAL_REQUESTED_QUEUE);
                channel.close();
                connection.close();
            }
        } catch (TimeoutException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void ResourceCreationRequestConsumerTest(){
    }

    @Test
    public void ResourceModificationRequestConsumerTest(){
    }

    @Test
    public void ResourceRemovalRequestConsumerTest(){
    }

    @Test
    public void PlatformCreationRequestConsumerTest() throws Exception {
        rabbitManager.startConsumerOfPlatformCreationMessages(mockedRepository);

        Platform requestPlatform = generatePlatformA();

        ObjectMapper mapper = new ObjectMapper();
        String message = mapper.writeValueAsString(requestPlatform);

        PlatformResponse platformResponse = new PlatformResponse();
        platformResponse.setStatus(200);
        platformResponse.setMessage("ok");
        platformResponse.setPlatform(requestPlatform);

        when(mockedRepository.savePlatform(any())).thenReturn(platformResponse);

        rabbitManager.sendCustomMessage(PLATFORM_EXCHANGE_NAME, PLATFORM_CREATION_REQUESTED, message, RegistryPlatform.class.getCanonicalName());

        // Sleep to make sure that the platform has been saved to the repo before querying
        TimeUnit.MILLISECONDS.sleep(200);

        Channel channel = rabbitManager.getConnection().createChannel();

        ArgumentCaptor<RegistryPlatform> argument = ArgumentCaptor.forClass(RegistryPlatform.class);
        verify(mockedRepository).savePlatform(argument.capture());

        Assert.assertTrue(argument.getValue().getId().equals(requestPlatform.getPlatformId()));
        Assert.assertTrue(argument.getValue().getComments().get(0).equals(requestPlatform.getDescription()));
        Assert.assertTrue(argument.getValue().getLabels().get(0).equals(requestPlatform.getName()));
    }

    @Test
    public void PlatformModificationRequestConsumerTest(){

    }

    @Test
    public void PlatformRemovalRequestConsumerTest(){

    }


}
