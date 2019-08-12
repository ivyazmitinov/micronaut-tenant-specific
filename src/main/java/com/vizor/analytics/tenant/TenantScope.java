package com.vizor.analytics.tenant;

import io.micronaut.context.BeanResolutionContext;
import io.micronaut.context.annotation.Context;
import io.micronaut.context.scope.CustomScope;
import io.micronaut.inject.BeanDefinition;
import io.micronaut.inject.BeanIdentifier;
import io.micronaut.multitenancy.tenantresolver.TenantResolver;

import javax.inject.Inject;
import javax.inject.Provider;
import java.io.Serializable;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Context
public class TenantScope implements CustomScope<TenantSpecific>
{
    private Map<String, Map<String, Object>> scopesByTenant = new ConcurrentHashMap<>();

    @Inject
    private TenantResolver tenantResolver;

    @Override
    public Class<TenantSpecific> annotationType()
    {
        return TenantSpecific.class;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(BeanResolutionContext resolutionContext,
                     BeanDefinition<T> beanDefinition,
                     BeanIdentifier identifier,
                     Provider<T> provider)
    {
        Map<String, Object> tenantScope = getTenantScope();
        String beanId = identifier.toString();

        T bean = (T) tenantScope.get(beanId);
        if (bean == null)
        {
            bean = provider.get();
            tenantScope.put(beanId, bean);
        }

        return bean;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Optional<T> remove(BeanIdentifier identifier)
    {
        Map<String, Object> tenantScope = getTenantScope();
        String beanId = identifier.toString();
        return Optional.ofNullable((T)tenantScope.remove(beanId));
    }

    private Map<String, Object> getTenantScope()
    {
        Serializable resolvedTenant = tenantResolver.resolveTenantIdentifier();
        if (resolvedTenant == null)
        {
            throw new IllegalStateException("Unable to resolve tenant ID while trying to manage tenant-specific bean context");
        }

        String tenantId = resolvedTenant.toString();
        return scopesByTenant.computeIfAbsent(tenantId, key -> new ConcurrentHashMap<>());
    }
}
