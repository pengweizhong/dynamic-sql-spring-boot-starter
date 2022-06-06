package com.pengwz.dynamic.processor;

import com.pengwz.dynamic.config.DataSourceConfig;
import com.pengwz.dynamic.config.DataSourceManagement;
import com.pengwz.dynamic.exception.BraveException;
import com.pengwz.dynamic.interceptor.SQLInterceptor;
import com.pengwz.dynamic.model.DataSourceInfo;
import com.pengwz.dynamic.model.DbType;
import com.pengwz.dynamic.sql.ContextApplication;
import com.pengwz.dynamic.util.DataSourceUtil;
import com.pengwz.dynamic.util.ProxyUtils;
import com.pengwz.dynamic.utils.ConverterUtils;
import com.pengwz.dynamic.utils.InterceptorHelper;
import com.pengwz.dynamic.utils.convert.ConverterAdapter;
import com.pengwz.dynamic.utils.convert.LocalDateConverterAdapter;
import com.pengwz.dynamic.utils.convert.LocalDateTimeConverterAdapter;
import com.pengwz.dynamic.utils.convert.LocalTimeConverterAdapter;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;

import javax.sql.DataSource;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class DataSourceApplicationListener implements ApplicationListener<ContextRefreshedEvent> {
    private static final Log log = LogFactory.getLog(DataSourceApplicationListener.class);

    @Override
    public void onApplicationEvent(ContextRefreshedEvent refreshedEvent) {
        ApplicationContext application = refreshedEvent.getApplicationContext();
        Map<String, DataSourceConfig> dataSourceConfigType = application.getBeansOfType(DataSourceConfig.class);
        if (MapUtils.isNotEmpty(dataSourceConfigType)) {
            dataSourceConfigType.forEach((beanName, dataSourceConfig) -> {
                Class<?> userClass = ClassUtils.getUserClass(dataSourceConfig);
                String classPath = userClass.getName();
                if (!ContextApplication.existsDataSouce(classPath)) {
                    DataSourceInfo info = new DataSourceInfo();
                    info.setClassPath(classPath);
                    info.setClassBeanName(beanName);
                    info.setDefault(dataSourceConfig.defaultDataSource());
                    info.setDataSourceBeanName(DataSourceUtil.getDataSourceBeanName(userClass));
                    DataSource dataSource = application.getBean(info.getDataSourceBeanName(), DataSource.class);
                    DataSource targetDataSource = (DataSource) ProxyUtils.getTarget(dataSource);
                    info.setDataSource(targetDataSource);
                    DbType dbType = DataSourceManagement.getDbType(targetDataSource);
                    info.setDbType(dbType);
                    ContextApplication.putDataSource(info);
                }
            });
        }
        checkDataSource();
        //更新ConverterAdapter
        final Map<String, ConverterAdapter> converterAdapterMap = application.getBeansOfType(ConverterAdapter.class);
        if (MapUtils.isNotEmpty(converterAdapterMap)) {
            converterAdapterMap.forEach((beanName, converterAdapter) -> {
                try {
                    final Class<?> userClass = ClassUtils.getUserClass(converterAdapter);
                    final Method declaredMethod = userClass.getDeclaredMethod("converter", Class.class, Class.class, Object.class);
                    final Class<?> returnType = declaredMethod.getReturnType();
                    final Map<Class<?>, ConverterAdapter<?>> cacheConverterAdapterMap = ConverterUtils.getConverterAdapterMap();
                    final ConverterAdapter<?> cacheAdapter = cacheConverterAdapterMap.get(returnType);
                    if (cacheAdapter != null
                            && !LocalDateConverterAdapter.class.isAssignableFrom(cacheAdapter.getClass())
                            && !LocalDateTimeConverterAdapter.class.isAssignableFrom(cacheAdapter.getClass())
                            && !LocalTimeConverterAdapter.class.isAssignableFrom(cacheAdapter.getClass())
                    ) {
                        final String cacheGeneric = cacheAdapter.getClass().toGenericString();
                        final String generic = userClass.toGenericString();
                        if (!cacheGeneric.equals(generic)) {
                            log.error("发现了[" + returnType + "]类型的多个转换器，已存在的转换器类路径：" + cacheGeneric + "，检索到新的转换器：" + generic);
                            log.warn("请检查冲突的类型转换器，即将执行默认操作：" + generic + "将覆盖" + cacheGeneric);
                        }
                    }
                    ConverterUtils.putConverterAdapter(returnType, (ConverterAdapter<?>) ProxyUtils.getTarget(converterAdapter));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }
        //加载拦截器
        final Map<String, SQLInterceptor> sqlInterceptorMap = application.getBeansOfType(SQLInterceptor.class);
        if (MapUtils.isNotEmpty(sqlInterceptorMap)) {
            sqlInterceptorMap.values().forEach(InterceptorHelper::initSQLInterceptor);
        }

    }

    public void checkDataSource() {
        List<DataSourceInfo> sourceInfos = ContextApplication.getAllDataSourceInfo();
        if (sourceInfos.isEmpty()) {
            return;
        }
        if (sourceInfos.size() == 1) {
            boolean aDefault = sourceInfos.get(0).isDefault();
            if (!aDefault) {
                log.info("当数据源只有一个时，应当将该数据源设置为默认数据源，以减少在实体类中频繁指定数据源");
            }
        } else {
            Map<Boolean, Long> booleanLongMap = sourceInfos.stream().map(DataSourceInfo::isDefault).collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
            Long aLong = booleanLongMap.get(Boolean.TRUE);
            if (aLong > 1) {
                log.error("仅支持一个默认数据源，请检查数据源配置");
                throw new BraveException("仅支持一个默认数据源，请检查数据源配置");
            }
        }
    }
}
