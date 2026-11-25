package com.hcl.Order_Management_System.DTO;

public class SetupStatusResponse {

    private boolean setupRequired;
    private long totalUsers;

    public SetupStatusResponse(boolean setupRequired, long totalUsers) {
        this.setupRequired = setupRequired;
        this.totalUsers = totalUsers;
    }

    public boolean isSetupRequired() {
        return setupRequired;
    }

    public void setSetupRequired(boolean setupRequired) {
        this.setupRequired = setupRequired;
    }

    public long getTotalUsers() {
        return totalUsers;
    }

    public void setTotalUsers(long totalUsers) {
        this.totalUsers = totalUsers;
    }
}
