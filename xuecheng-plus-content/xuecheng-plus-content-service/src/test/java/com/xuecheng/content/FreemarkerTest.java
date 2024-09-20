package com.xuecheng.content;

import com.xuecheng.content.model.dto.CoursePreviewDto;
import com.xuecheng.content.service.CoursePublishService;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

@SpringBootTest
public class FreemarkerTest {
    @Autowired
    CoursePublishService coursePublishService;
    @Test
    public void testGenerateHtmlByTemplate() throws IOException, TemplateException {
        Configuration configuration = new Configuration(Configuration.getVersion());
        String classpath = this.getClass().getResource("/").getPath();
        configuration.setDirectoryForTemplateLoading(new File(classpath+"/templates/"));
        configuration.setDefaultEncoding("utf-8");
        Template template = configuration.getTemplate("course_template.ftl");
        Map<String, Object> data = new HashMap<>();
        CoursePreviewDto coursePreviewInfo = coursePublishService.getCoursePreviewInfo(120L);
        data.put("model", coursePreviewInfo);
        String content  = FreeMarkerTemplateUtils.processTemplateIntoString(template, data);
        System.out.println(content);
        InputStream inputStream = IOUtils.toInputStream(content, "utf-8");
        FileOutputStream fileOutputStream = new FileOutputStream(new File("D:\\static-html\\120.html"));
        IOUtils.copy(inputStream, fileOutputStream);
    }
}
