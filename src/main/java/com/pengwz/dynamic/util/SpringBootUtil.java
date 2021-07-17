package com.pengwz.dynamic.util;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Spring上下文容器
 */
@Component
public class SpringBootUtil implements ApplicationContextAware {

    private static Class<?> mainApplicationClass;

    private static ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        if (SpringBootUtil.applicationContext == null) {
            SpringBootUtil.applicationContext = applicationContext;
        }
    }

    // 获取applicationContext
    public static ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    // 通过name获取 Bean.
    public static Object getBean(String name) {
        return getApplicationContext().getBean(name);
    }

    // 通过class获取Bean.
    public static <T> T getBean(Class<T> clazz) {
        return getApplicationContext().getBean(clazz);
    }

    // 通过name,以及Clazz返回指定的Bean
    public static <T> T getBean(String name, Class<T> clazz) {
        return getApplicationContext().getBean(name, clazz);
    }

    /**
     * 获取项目存放根目录
     * @return
     */

/*	public static String getWebPath() {
		return getApplicationPath(mainApplicationClass);
	}

	public static String getApplicationPath(final Class<?> sourceClass) {
		ApplicationHome home = new ApplicationHome(sourceClass);
		String path = (home.getSource() == null ? "" : home.getSource().getAbsolutePath());
		return path;
	}

	public static String getUserDir() {
		String userDir = System.getProperty("user.dir");
		return userDir;
	}

	public static Class<?> getMainApplicationClass() {
		return mainApplicationClass;
	}

	public static void setMainApplicationClass(Class<?> mainApplicationClass) {
		SpringBootUtilForAuth.mainApplicationClass = mainApplicationClass;
	}

*/
}
