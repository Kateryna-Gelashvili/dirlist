package org.k.controller;

import org.springframework.stereotype.Controller;

/**
 * The controller that is responsible of the web resource resolution and the file downloads.
 */
//@Controller
public class WebController {
//    private static final Logger logger = LoggerFactory.getLogger(WebController.class);
//    private final ResourceLoader resourceLoader;
//    private final DirService dirService;
//    private final ResponseEntity<String> pageSourceResponseEntity;
//
//    private final Map<String, File> dirDownloadTokenDirPathMap = new ConcurrentHashMap<>();
//
//    @Autowired
//    @SuppressWarnings("CdiInjectionPointsInspection")
//    public WebController(ResourceLoader resourceLoader, DirService dirService, ServletContext servletContext)
//            throws IOException, TemplateException {
//        this.resourceLoader = resourceLoader;
//        this.dirService = dirService;
//        this.pageSourceResponseEntity = initializePageSource(servletContext.getContextPath());
//    }
//
//    private ResponseEntity<String> initializePageSource(String contextPath) throws IOException, TemplateException {
//        Configuration configuration = new Configuration(Configuration.VERSION_2_3_23);
//        configuration.setClassLoaderForTemplateLoading(WebController.class.getClassLoader(), "template");
//        configuration.setDefaultEncoding(StandardCharsets.UTF_8.name());
//        configuration.setOutputEncoding(StandardCharsets.UTF_8.name());
//        configuration.setWhitespaceStripping(true);
//        configuration.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
//        Template template = configuration.getTemplate("dirList.html.ftl", StandardCharsets.UTF_8.name());
//        StringWriter sw = new StringWriter();
//        template.process(ImmutableMap.of("contextPath", contextPath), sw);
//        HttpHeaders headers = new HttpHeaders();
//        headers.setContentType(MediaType.TEXT_HTML);
//        return new ResponseEntity<>(sw.toString(), headers, HttpStatus.OK);
//    }
//
//    @GetMapping("/**")
//    @SuppressWarnings("unused")
//    public ResponseEntity<?> handle(
//            @RequestParam(value = "zipped", defaultValue = "false") boolean zipped)
//            throws IOException, TemplateException {
//        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder
//                .currentRequestAttributes()).getRequest();
//        String requestUri = request.getRequestURI().substring(request.getContextPath().length());
//
//        if ("".equals(requestUri) || "/".equals(requestUri)) {
//            return pageSourceResponseEntity;
//        }
//
//        String resourcePathToLoad = resolveResourcePathToLoad(requestUri);
//
//        Resource resource = resourceLoader.getResource("classpath:/static/" + resourcePathToLoad);
//        if (resource.exists()) {
//            return new ResponseEntity<>(resource, HttpStatus.OK);
//        }
//
//        Optional<File> fileOrDirectoryOptional = dirService.resolveFileOrDirectory(resourcePathToLoad);
//        if (fileOrDirectoryOptional.isPresent()) {
//
//            if (zipped) {
//                dirDownloadTokenDirPathMap.put(UUID.randomUUID().toString(),
//                        fileOrDirectoryOptional.get());
//                // todo redirect the request to filenameWithoutExtension + "_" + .zip
//            }
//
//
//            return handleRequestToValidResourcePath(fileOrDirectoryOptional.get(), requestUri, zipped);
//        }
//
//        Optional<Resource> resourceForSubPageOptional = handleResourcePathForSubPages(resourcePathToLoad);
//        if (resourceForSubPageOptional.isPresent()) {
//            return new ResponseEntity<>(resourceForSubPageOptional.get(), HttpStatus.OK);
//        }
//
//        return new ResponseEntity<>("No such file/directory!", HttpStatus.NOT_FOUND);
//    }
//
//    private Optional<Resource> handleResourcePathForSubPages(String resourcePathToLoad) {
//        String possibleDirPath = StringUtils.substringBeforeLast(resourcePathToLoad, "/");
//        Optional<File> dirOptional = dirService.resolveFileOrDirectory(possibleDirPath);
//        if (dirOptional.isPresent()) {
//            File dir = dirOptional.get();
//            if (dir.isDirectory()) {
//                Resource additionalResource =
//                        resourceLoader.getResource(
//                                "classpath:/static/" + StringUtils.substringAfterLast(resourcePathToLoad, "/")
//                        );
//                if (additionalResource.exists()) {
//                    return Optional.of(additionalResource);
//                }
//            }
//        }
//        return Optional.empty();
//    }
//
//    private ResponseEntity<?> handleRequestToValidResourcePath(File file, String requestUri,
//                                                               boolean zipped)
//            throws IOException, TemplateException {
//        if (file.isFile()) {
//            return handleFileDownload(file, zipped);
//        }
//
//        // file is a directory
//        if (!requestUri.endsWith("/")) {
//            HttpHeaders headers = new HttpHeaders();
//            headers.setLocation(URI.create(requestUri + "/"));
//            return new ResponseEntity<>(headers, HttpStatus.MOVED_PERMANENTLY);
//        }
//
//        return pageSourceResponseEntity;
//    }
//
//    private String resolveResourcePathToLoad(String requestUri) {
//        String decodedPath;
//        try {
//            decodedPath = URLDecoder.decode(requestUri, StandardCharsets.UTF_8.name());
//        } catch (UnsupportedEncodingException e) {
//            logger.debug("Unsupported encoding exception while loading " + requestUri);
//            throw new RuntimeException(e);
//        }
//
//        if (decodedPath.startsWith("/")) {
//            decodedPath = decodedPath.substring(1);
//        }
//        return decodedPath;
//    }
//
//    private ResponseEntity<?> handleFileDownload(File file, boolean zipped) {
//        HttpHeaders headers = new HttpHeaders();
//        String contentType = extractContentTypeForFile(file);
//        headers.set(HttpHeaders.CONTENT_TYPE, contentType);
//        headers.set(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=" + file.getName());
//
//        Optional<ByteRangeSpec> byteRangeSpecOptional = extractByteRangeSpec(file.length());
//
//        if (byteRangeSpecOptional.isPresent()) {
//            ByteRangeSpec byteRangeSpec = byteRangeSpecOptional.get();
//            headers.setContentLength(byteRangeSpec.buildContentLengthHeader());
//            headers.set(HttpHeaders.CONTENT_RANGE, byteRangeSpec.buildContentRangeHeader());
//
//            try {
//                RandomAccessFile randomAccessFile = new RandomAccessFile(file, "r");
//                long numberOfBytes = byteRangeSpec.buildContentLengthHeader();
//                FileChannel channel = randomAccessFile.getChannel()
//                        .position(byteRangeSpec.getStart());
//                InputStream partialInputStream =
//                        ByteStreams.limit(Channels.newInputStream(channel), numberOfBytes);
//                return new ResponseEntity<>(
//                        new InputStreamResource(new BufferedInputStream(partialInputStream)),
//                        headers,
//                        HttpStatus.PARTIAL_CONTENT);
//            } catch (Exception e) {
//                throw new UnknownException("Should never happen!");
//            }
//        }
//
//        headers.setContentLength(file.length());
//
//        if (zipped) {
//            HttpServletResponse response = ((ServletRequestAttributes) RequestContextHolder
//                    .currentRequestAttributes()).getResponse();
//            try (InputStream input = new BufferedInputStream(new FileInputStream(file));
//                 ZipOutputStream output = new ZipOutputStream(response.getOutputStream())) {
//                response.setHeader("test1", "test2");
//                output.putNextEntry(new ZipEntry(file.getName()));
//                ByteStreams.copy(input, output);
//                output.closeEntry();
//                output.finish();
//                response.setHeader("test11", "test22");
//                return new ResponseEntity<>(headers, HttpStatus.OK);
//            } catch (IOException e) {
//                throw new UnknownException("Should never happen!");
//            }
//        } else {
//            return new ResponseEntity<>(new FileSystemResource(file), headers, HttpStatus.OK);
//        }
//    }
}