package org.k.config;

import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.client.config.ClientNetworkConfig;
import com.hazelcast.config.GroupConfig;
import com.hazelcast.core.HazelcastInstance;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class HazelcastConfig {

    @Bean
    public HazelcastInstance hazelcastInstance(@Value("${hazelcast.addresses}") String[] addresses,
                                               @Value("${hazelcast.username}") String username,
                                               @Value("${hazelcast.password}") String password) {
        ClientConfig clientConfig = new ClientConfig();
        clientConfig.setGroupConfig(new GroupConfig(username, password));
        ClientNetworkConfig networkConfig = clientConfig.getNetworkConfig();
        networkConfig.addAddress(addresses);

        return HazelcastClient.newHazelcastClient(clientConfig);
    }
}
