package de.hhu.bsinfo.neutrino.api.util;

import lombok.Getter;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;

@Getter
public abstract class BaseService<T> implements InitializingBean, DisposableBean {

    private final T config;

    protected BaseService(T config) {
        this.config = config;
    }

    protected abstract void onStart() throws Exception;

    protected abstract void onDestroy();

    @Override
    public void afterPropertiesSet() throws Exception {
        onStart();
    }

    @Override
    public void destroy() {
        onDestroy();
    }
}
