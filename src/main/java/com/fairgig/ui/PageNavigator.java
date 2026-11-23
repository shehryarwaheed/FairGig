package com.fairgig.ui;

import com.fairgig.model.User;

/** What a page may ask the app shell to do. */
public interface PageNavigator {

    /** Opens another page (from the sidebar list, or Notifications / Profile). */
    void open(NavItem item);

    /** Finds one of the current role's tabs by its title, or null. */
    NavItem find(String title);

    /** Re-reads the unread count shown on the bell. */
    void refreshNotifications();

    /** Tells the shell that the user's profile changed (name shown in the sidebar and avatar). */
    void userUpdated(User user);
}
