package com.fairgig.ui;

import com.fairgig.controller.AppContext;
import com.fairgig.model.User;

/**
 * Everything a page needs: who is logged in, their role, the controllers and a way to navigate.
 * Pages talk to the backend ONLY through {@code app()} (controllers), never to repositories.
 */
public record PageContext(User user, UiRole role, AppContext app, PageNavigator nav) {

    public int userId() {
        return user.getUserId();
    }
}
