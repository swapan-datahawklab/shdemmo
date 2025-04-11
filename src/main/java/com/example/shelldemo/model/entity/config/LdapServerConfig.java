package com.example.shelldemo.model.entity.config;

/**
 * LDAP-specific configuration that extends the base ConnectionConfig.
 * Adds LDAP-specific properties like baseDn, bindDn, and bindPassword.
 */
public class LdapServerConfig extends ConnectionConfig {
    private String baseDn;
    private String bindDn;
    private String bindPassword;
    private boolean useSsl;
    private int connectionTimeout = 5000;
    private int readTimeout = 5000;

    public LdapServerConfig() {
        super();
        setDbType("ldap");
    }

    public String getBaseDn() {
        return baseDn;
    }

    public void setBaseDn(String baseDn) {
        this.baseDn = baseDn;
    }

    public String getBindDn() {
        return bindDn;
    }

    public void setBindDn(String bindDn) {
        this.bindDn = bindDn;
    }

    public String getBindPassword() {
        return bindPassword;
    }

    public void setBindPassword(String bindPassword) {
        this.bindPassword = bindPassword;
    }

    public boolean isUseSsl() {
        return useSsl;
    }

    public void setUseSsl(boolean useSsl) {
        this.useSsl = useSsl;
    }

    public int getConnectionTimeout() {
        return connectionTimeout;
    }

    public void setConnectionTimeout(int connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    public int getReadTimeout() {
        return readTimeout;
    }

    public void setReadTimeout(int readTimeout) {
        this.readTimeout = readTimeout;
    }

    @Override
    public String getConnectionUrl() {
        return String.format("jdbc:oracle:thin:@ldap://%s:%d/%s,cn=OracleContext,dc=oracle,dc=com", 
            getHost(), getPort(), getBaseDn());
    }
} 