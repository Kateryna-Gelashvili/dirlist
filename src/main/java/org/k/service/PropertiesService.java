package org.k.service;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;

import org.k.config.SecurityConfig;
import org.k.exception.ConfigException;
import org.k.user.UserInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

@Service
public class PropertiesService {
    private static final Logger logger = LoggerFactory.getLogger(PropertiesService.class);

    private static final String ROOT_DIRECTORY = "root.directory";
    private static final String SHOW_HIDDEN_FILES = "show.hidden.files";

    private final String configFilePath;

    @SuppressWarnings("Guava")
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

    @Autowired
    public PropertiesService(@Value("#{systemProperties['config.file.path']}") String configFilePath) {
        this.configFilePath = configFilePath;
    }

    public String getRootDirectory() {
        return Optional.ofNullable(propertiesSupplier.get().getProperty(ROOT_DIRECTORY))
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

    public boolean showHiddenFiles() {
        return Boolean.valueOf(propertiesSupplier.get().getProperty(SHOW_HIDDEN_FILES));
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
