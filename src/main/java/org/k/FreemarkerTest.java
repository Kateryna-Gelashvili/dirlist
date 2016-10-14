package org.k;

import com.google.common.collect.ImmutableMap;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateExceptionHandler;
import freemarker.template.Version;

import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;

public class FreemarkerTest {
    public static void main(String[] args) throws Exception {
        Configuration configuration = new Configuration(Configuration.VERSION_2_3_23);
        configuration.setClassLoaderForTemplateLoading(FreemarkerTest.class.getClassLoader(), "template");
        configuration.setDefaultEncoding(StandardCharsets.UTF_8.name());
        configuration.setLocale(Locale.ENGLISH);
        configuration.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);

        Template template = configuration.getTemplate("dirList.html.ftl");

        Map<String, String> map = ImmutableMap.of("contextPath", "katya");

        StringWriter sw = new StringWriter();
        template.process(map, sw);
        System.out.println(sw.toString());
    }
}
