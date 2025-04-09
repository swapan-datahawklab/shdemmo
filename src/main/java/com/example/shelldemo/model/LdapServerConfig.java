package com.example.shelldemo.model;

/**
 * LDAP-specific configuration that extends the base ConnectionConfig.
 * Adds LDAP-specific properties like baseDn, bindDn, and bindPassword.
 */
public class LdapServerConfig extends ConnectionConfig {
    private String baseDn;
    private String bindDn;
    private String bindPassword;

    public LdapServerConfig() {
        super();
        setDatabaseType("ldap");
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

    @Override
    public String getConnectionUrl() {
        return String.format("jdbc:oracle:thin:@ldap://%s:%d/%s,cn=OracleContext,dc=oracle,dc=com", 
            getHost(), getPort(), getBaseDn());
    }
} 