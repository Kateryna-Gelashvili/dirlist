package org.k.service;

import org.k.config.SecurityConfig;
import org.k.user.UserInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.Properties;

import javax.annotation.PostConstruct;

@Service
public class PropertiesService {
    public static final String ROOT_DIRECTORY = Optional.ofNullable(System.getenv("LIST_DIR"))
            .orElse("/dirlist");

    private static final Logger logger = LoggerFactory.getLogger(PropertiesService.class);

    private static final String SHOW_HIDDEN_FILES = "show.hidden.files";
    private static final String MAX_DIRECTORY_DOWNLOAD_SIZE_BYTES
            = "max.directory.download.size.bytes";

    private static final long DEFAULT_MAX_DIRECTORY_DOWNLOAD_SIZE_BYTES = 1024L * 1024L * 1024L;

    private static final String CONFIG_FILE_PATH = Optional
            .ofNullable(System.getenv("CONFIG_FILE"))
            .orElse("/etc/dirlist/config.properties");

    private final Properties configProperties;

    public PropertiesService() throws IOException {
        Path configFile = Paths.get(CONFIG_FILE_PATH);
        if (!Files.exists(configFile)) {
            Files.createDirectories(configFile.getParent());
            Files.createFile(configFile);
        }
        Properties properties = new Properties();
        try (InputStream input = new FileInputStream(new File(CONFIG_FILE_PATH))) {
            properties.load(input);
            this.configProperties = properties;
        } catch (IOException e) {
            logger.error("IO Exception, while loading " + CONFIG_FILE_PATH);
            throw new RuntimeException(e);
        }

        Files.createDirectories(Paths.get(ROOT_DIRECTORY));
    }

    @PostConstruct
    protected void postConstruct() throws IOException {

    }

    public Optional<UserInfo> getUserInfo(String username) {
        return SecurityConfig.ROLE_NAME_PREFIX_MAP.entrySet().stream()
                .map(entry -> Optional.ofNullable(configProperties
                        .getProperty(entry.getValue() + username))
                        .map(passwordHash -> new UserInfo(username, passwordHash, entry.getKey())))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .findFirst();
    }

    public boolean showHiddenFiles() {
        return Boolean.valueOf(configProperties.getProperty(SHOW_HIDDEN_FILES));
    }

    public long maxAllowedDirectoryDownloadSize() {
        return Optional.ofNullable(configProperties
                .getProperty(MAX_DIRECTORY_DOWNLOAD_SIZE_BYTES))
                .map(Long::valueOf)
                .orElse(DEFAULT_MAX_DIRECTORY_DOWNLOAD_SIZE_BYTES);
    }
}
