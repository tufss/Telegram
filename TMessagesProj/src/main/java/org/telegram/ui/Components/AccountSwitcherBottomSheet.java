/*
 * This is the source code of Telegram for Android v. 5.x.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2018.
 */

package org.telegram.ui.Components;

import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ContactsController;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.SimpleTextView;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.AccountSelectCell;

/**
 * Account switcher bottom sheet for Phase 2.
 * Integrates with existing UserConfig and AccountInstance architecture.
 * Supports up to 30 accounts using client-side account slots.
 * Reuses AccountSelectCell for consistent UI with existing account selector.
 */
public class AccountSwitcherBottomSheet {

    private BottomSheet bottomSheet;
    private LinearLayout accountsList;
    private BaseFragment fragment;
    private OnAccountSelectedListener listener;

    public interface OnAccountSelectedListener {
        void onAccountSelected(int accountNumber, boolean needSwitch);
    }

    public AccountSwitcherBottomSheet(Context context, BaseFragment fragment, OnAccountSelectedListener listener) {
        this.fragment = fragment;
        this.listener = listener;
        createBottomSheet(context);
    }

    private void createBottomSheet(Context context) {
        bottomSheet = new BottomSheet(context, false);
        bottomSheet.setApplyBottomPadding(false);

        FrameLayout containerView = new FrameLayout(context);

        // Title
        SimpleTextView titleView = new SimpleTextView(context);
        titleView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        titleView.setTextSize(20);
        titleView.setTypeface(AndroidUtilities.bold());
        titleView.setText(LocaleController.getString(R.string.SelectAccount));
        titleView.setPadding(AndroidUtilities.dp(24), AndroidUtilities.dp(16), AndroidUtilities.dp(24), AndroidUtilities.dp(16));
        containerView.addView(titleView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP));

        // Accounts list with scroll
        ScrollView scrollView = new ScrollView(context);
        scrollView.setVerticalScrollBarEnabled(true);

        accountsList = new LinearLayout(context);
        accountsList.setOrientation(LinearLayout.VERTICAL);

        scrollView.addView(accountsList, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        containerView.addView(scrollView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP, 0, 56, 0, 0));

        // Populate accounts
        populateAccountsList(context);

        // Add Account button
        if (canAddAccount()) {
            FrameLayout addAccountButton = createAddAccountButton(context);
            containerView.addView(addAccountButton);
        }

        bottomSheet.setCustomView(containerView);
    }

    private void populateAccountsList(Context context) {
        accountsList.removeAllViews();

        // Show current and other active accounts
        for (int i = 0; i < UserConfig.MAX_ACCOUNT_COUNT; i++) {
            UserConfig userConfig = UserConfig.getInstance(i);
            if (userConfig.isClientActivated()) {
                TLRPC.User user = userConfig.getCurrentUser();
                if (user != null) {
                    AccountSelectCell accountCell = new AccountSelectCell(context, false);
                    accountCell.setAccount(i, i == UserConfig.selectedAccount);

                    final int accountNum = i;
                    accountCell.setOnClickListener(v -> {
                        if (listener != null) {
                            listener.onAccountSelected(accountNum, accountNum != UserConfig.selectedAccount);
                        }
                        bottomSheet.dismiss();
                    });

                    accountsList.addView(accountCell, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 56));
                }
            }
        }
    }

    private FrameLayout createAddAccountButton(Context context) {
        FrameLayout buttonContainer = new FrameLayout(context);
        buttonContainer.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));

        // Divider
        View divider = new View(context);
        divider.setBackgroundColor(Theme.getColor(Theme.key_divider));
        buttonContainer.addView(divider, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 1, Gravity.TOP));

        // Button
        FrameLayout button = new FrameLayout(context);
        button.setBackground(Theme.createSimpleSelectorRoundRectDrawable(AndroidUtilities.dp(6),
                Theme.getColor(Theme.key_featuredStickers_addButton),
                Theme.getColor(Theme.key_featuredStickers_addButtonPressed)));

        TextView buttonText = new TextView(context);
        buttonText.setTextColor(Theme.getColor(Theme.key_featuredStickers_buttonText));
        buttonText.setTypeface(AndroidUtilities.bold());
        buttonText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        buttonText.setText(LocaleController.getString(R.string.AddAccount));
        buttonText.setGravity(Gravity.CENTER);
        button.addView(buttonText, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        button.setOnClickListener(v -> {
            bottomSheet.dismiss();
            // Trigger login flow in existing Telegram architecture
            if (fragment != null && fragment.getParentActivity() != null) {
                // The existing LoginActivity/IntroActivity handles multi-account login
                fragment.getParentActivity().startActivity(
                    fragment.getParentActivity().getIntent()
                );
            }
        });

        buttonContainer.addView(button, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 48, Gravity.CENTER, 16, 56, 16, 16));

        return buttonContainer;
    }

    private boolean canAddAccount() {
        int activeCount = 0;
        for (int i = 0; i < UserConfig.MAX_ACCOUNT_COUNT; i++) {
            if (UserConfig.getInstance(i).isClientActivated()) {
                activeCount++;
            }
        }
        return activeCount < UserConfig.MAX_ACCOUNT_COUNT;
    }

    public void show() {
        bottomSheet.show();
    }

    public void dismiss() {
        bottomSheet.dismiss();
    }
}
