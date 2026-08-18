/*
 * This is the source code of Telegram for Android v. 5.x.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2018.
 */

package org.telegram.messenger;

import android.content.SharedPreferences;

/**
 * Multi-account configuration for Phase 2.
 * Extends account support to up to 30 accounts with client-side management.
 */
public class MultiAccountConfig {

    // Maximum number of accounts (expanded from 4 to 30)
    public static final int MAX_ACCOUNT_COUNT = 30;
    public static final int DEFAULT_ACCOUNT_COUNT = 3;

    private static volatile MultiAccountConfig[] instance = new MultiAccountConfig[MAX_ACCOUNT_COUNT];

    public static synchronized MultiAccountConfig getInstance(int accountNum) {
        if (accountNum < 0 || accountNum >= MAX_ACCOUNT_COUNT) {
            return getInstance(0);
        }
        if (instance[accountNum] == null) {
            instance[accountNum] = new MultiAccountConfig(accountNum);
        }
        return instance[accountNum];
    }

    private final int accountNumber;
    private String accountLabel;
    private boolean isActive;

    public MultiAccountConfig(int accountNumber) {
        this.accountNumber = accountNumber;
        this.isActive = false;
        this.accountLabel = null;
        loadConfig();
    }

    public void loadConfig() {
        try {
            SharedPreferences prefs = getPreferences();
            this.accountLabel = prefs.getString("accountLabel_" + accountNumber, null);
            this.isActive = prefs.getBoolean("accountActive_" + accountNumber, false);
        } catch (Exception e) {
            FileLog.e(e);
        }
    }

    public void saveConfig() {
        try {
            SharedPreferences prefs = getPreferences();
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString("accountLabel_" + accountNumber, accountLabel);
            editor.putBoolean("accountActive_" + accountNumber, isActive);
            editor.apply();
        } catch (Exception e) {
            FileLog.e(e);
        }
    }

    public void setAccountLabel(String label) {
        this.accountLabel = label;
        saveConfig();
    }

    public String getAccountLabel() {
        return accountLabel;
    }

    public void setActive(boolean active) {
        this.isActive = active;
        saveConfig();
    }

    public boolean isActive() {
        return isActive;
    }

    public int getAccountNumber() {
        return accountNumber;
    }

    public static int getActiveAccountCount() {
        int count = 0;
        for (int i = 0; i < MAX_ACCOUNT_COUNT; i++) {
            if (UserConfig.getInstance(i).isClientActivated()) {
                count++;
            }
        }
        return count;
    }

    public static boolean canAddAccount() {
        return getActiveAccountCount() < MAX_ACCOUNT_COUNT;
    }

    private SharedPreferences getPreferences() {
        if (accountNumber == 0) {
            return ApplicationLoader.applicationContext.getSharedPreferences("multiaccountconfig", android.content.Context.MODE_PRIVATE);
        } else {
            return ApplicationLoader.applicationContext.getSharedPreferences("multiaccountconfig_" + accountNumber, android.content.Context.MODE_PRIVATE);
        }
    }

    public static void clearAllConfigs() {
        for (int i = 0; i < MAX_ACCOUNT_COUNT; i++) {
            try {
                getInstance(i).getPreferences().edit().clear().apply();
            } catch (Exception e) {
                FileLog.e(e);
            }
        }
    }
}
