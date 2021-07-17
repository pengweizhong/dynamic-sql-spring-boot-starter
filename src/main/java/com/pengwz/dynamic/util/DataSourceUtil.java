package com.pengwz.dynamic.util;

import com.pengwz.dynamic.exception.BraveException;
import com.pengwz.dynamic.model.DataSourceInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.annotation.Bean;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class DataSourceUtil {

    private static final Log log = LogFactory.getLog(DataSourceUtil.class);

    private static final List<DataSourceInfo> DATA_SOURCE_INFO_LIST = new ArrayList<>();

    public static void putDataSource(DataSourceInfo info) {
        DATA_SOURCE_INFO_LIST.add(info);
    }

    public static List<DataSourceInfo> getAll() {
        return DATA_SOURCE_INFO_LIST;
    }

    public static String getDataSourceBeanName(Object bean) {
        return getDataSourceBeanName(bean.getClass());
    }

    public static String getDataSourceBeanName(Class<?> beanClass) {
        try {
            Method method = beanClass.getDeclaredMethod("getDataSource");
            Bean beanAnno = method.getAnnotation(Bean.class);
            if (beanAnno == null) {
                throw new BraveException("数据源未纳入spring管理，请加入@Bean注解");
            }
            if (beanAnno.value().length > 0) {
                return beanAnno.value()[0];
            } else if (beanAnno.name().length > 0) {
                return beanAnno.name()[0];
            } else {
                //bean属性不声明bean名称时，默认以方法名
                return "getDataSource";
            }
        } catch (NoSuchMethodException e) {
            log.error(e.getMessage(), e);
            throw new BraveException("Failed to get data source ." + e.getMessage());
        }
    }
}
