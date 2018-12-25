package org.xujin.sc.controller;

import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.xujin.sc.feign.HelloFeignService;

import java.util.Date;

/**
 * @author: xujin
 **/
@CommonsLog
@RequestMapping("/hello")
@RestController
public class HelloController {
    private static final Log log = LogFactory.getLog(HelloController.class);

    @Autowired
    HelloFeignService helloRemote;

    @GetMapping("/{name}")
    public String index(@PathVariable("name") String name)  {
        log.info("the name is " + name);
        return helloRemote.hello(name) + "\n" + new Date().toString();
    }


    @GetMapping("/testCustomFilter/{name}")
    public String customFilter(@PathVariable("name") String name)  {
        log.info("the name is " + name);
        return helloRemote.customFilter(name) + "\n" + new Date().toString();
    }

    @PostMapping("/testLogFilter")
    public String testLogFilter(@RequestBody Object name)  {
        System.out.println(name);
        log.info("the name is " + name);
        return helloRemote.customFilter(name.toString()) + "\n" + new Date().toString();
    }

}
