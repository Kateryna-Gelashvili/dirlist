package org.k.service;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableSet;
import org.k.config.SecurityConfig;
import org.k.exception.ConfigException;
import org.k.user.UserInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class PropertiesService {
    private static final Logger logger = LoggerFactory.getLogger(PropertiesService.class);
    private static final String USER_PREFIX = "user.";
    private static final String ADMIN_PREFIX = "admin.";

    private final String configFilePath;

    private final Supplier<Properties> propertiesSupplier
            = Suppliers.memoizeWithExpiration(new Supplier<Properties>() {
        @Override
        public Properties get() {
            Path configFile = validateAndGetConfigFilePath();
            Properties properties = new Properties();
            try (InputStream input = new FileInputStream(configFile.toFile())) {
                properties.load(input);
                return properties;
            } catch (IOException e) {
                logger.error("IO Exception, while loading " + configFilePath);
                throw new RuntimeException(e);
            }
        }
    }, 1, TimeUnit.MINUTES);

    @Inject
    public PropertiesService(@Value("#{systemProperties['config.file.path']}") String configFilePath) {
        this.configFilePath = configFilePath;
    }

    public String getRootDirectory() {
        return Optional.ofNullable(propertiesSupplier.get().getProperty("root.directory"))
                .orElseThrow(() -> new ConfigException("Root directory value is not found in configuration!"));
    }

    public Optional<UserInfo> getUserInfo(String username) {
        return SecurityConfig.ROLE_NAME_PREFIX_MAP.entrySet().stream()
                .map(entry -> Optional.ofNullable(propertiesSupplier.get().getProperty(entry.getValue() + username))
                        .map(passwordHash -> new UserInfo(username, passwordHash, entry.getKey())))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .findFirst();
    }

    private Path validateAndGetConfigFilePath() {
        if (configFilePath == null) {
            throw new ConfigException("Config file path not supplied as JVM property!");
        }
        Path configFile = Paths.get(configFilePath);

        if (!Files.exists(configFile)) {
            throw new ConfigException("Config file not found under path: " + configFile);
        }

        if (Files.isDirectory(configFile)) {
            throw new ConfigException("Config path is not a file but a directory: " + configFile);
        }

        if (!Files.isReadable(configFile)) {
            throw new ConfigException("Config file under " + configFilePath + " seems to be not readable!");
        }
        return configFile;
    }
}
