package com.quizgenix.dto;

public class SystemConfigDTO {

    private boolean maintenanceMode;
    private boolean allowRegistration;
    private boolean emailNotifications;
    private String defaultPlan;

    public SystemConfigDTO() {
    }

    public SystemConfigDTO(boolean maintenanceMode, boolean allowRegistration, boolean emailNotifications,
            String defaultPlan) {
        this.maintenanceMode = maintenanceMode;
        this.allowRegistration = allowRegistration;
        this.emailNotifications = emailNotifications;
        this.defaultPlan = defaultPlan;
    }

    public boolean isMaintenanceMode() {
        return maintenanceMode;
    }

    public void setMaintenanceMode(boolean maintenanceMode) {
        this.maintenanceMode = maintenanceMode;
    }

    public boolean isAllowRegistration() {
        return allowRegistration;
    }

    public void setAllowRegistration(boolean allowRegistration) {
        this.allowRegistration = allowRegistration;
    }

    public boolean isEmailNotifications() {
        return emailNotifications;
    }

    public void setEmailNotifications(boolean emailNotifications) {
        this.emailNotifications = emailNotifications;
    }

    public String getDefaultPlan() {
        return defaultPlan;
    }

    public void setDefaultPlan(String defaultPlan) {
        this.defaultPlan = defaultPlan;
    }
}