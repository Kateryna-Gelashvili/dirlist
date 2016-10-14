package org.k;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.k.config.SpringConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = SpringConfig.class)
@WebAppConfiguration
public class DirlistTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    public void firstTest() throws Exception {
        System.out.println("Application Context: " + applicationContext);
    }
}
