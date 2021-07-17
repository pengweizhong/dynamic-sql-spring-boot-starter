package com.pengwz.dynamic.processor;

import com.pengwz.dynamic.config.DataSourceConfig;
import com.pengwz.dynamic.exception.BraveException;
import com.pengwz.dynamic.model.DataSourceInfo;
import com.pengwz.dynamic.sql.ContextApplication;
import com.pengwz.dynamic.util.DataSourceUtil;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class AfterApplicationListener implements ApplicationListener<ContextRefreshedEvent> {
    private static final Log log = LogFactory.getLog(AfterApplicationListener.class);

    @Override
    public void onApplicationEvent(ContextRefreshedEvent webServerInitializedEvent) {
        ApplicationContext application = webServerInitializedEvent.getApplicationContext();
        Map<String, DataSourceConfig> beansOfType = application.getBeansOfType(DataSourceConfig.class);
        if (MapUtils.isNotEmpty(beansOfType)) {
            //扫描未被捕获的注解，比如@Configuration
            beansOfType.forEach((beanName, dataSourceConfig) -> {
                Class<?> userClass = ClassUtils.getUserClass(dataSourceConfig);
                String classPath = userClass.getName();
                if (!ContextApplication.existsDataSouce(classPath)) {
                    DataSourceInfo info = new DataSourceInfo();
                    info.setClassPath(classPath);
                    info.setClassBeanName(beanName);
                    info.setDefault(dataSourceConfig.defaultDataSource());
                    info.setDataSource(dataSourceConfig.getDataSource());
                    info.setDataSourceBeanName(DataSourceUtil.getDataSourceBeanName(userClass));
                    DataSource dataSource = application.getBean(info.getDataSourceBeanName(), DataSource.class);
                    info.setDataSource(dataSource);
                    ContextApplication.putDataSource(info);
                }
            });
        }
        checkDataSource();
    }

    public void checkDataSource() {
        List<DataSourceInfo> sourceInfos = ContextApplication.getAllDataSourceInfo();
        if (sourceInfos.isEmpty()) {
            log.error("未检查到数据源！请在配置中定义数据源！");
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
