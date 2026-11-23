package com.fairgig.ui;

import com.fairgig.controller.Outcome;
import com.fairgig.model.Platform;
import com.fairgig.model.PlatformRepresentative;
import com.fairgig.model.User;
import com.fairgig.model.Worker;
import java.util.List;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/** View and update your own profile (FR-04) and change your password. Works for every role. */
public class ProfilePage {

    private final PageContext ctx;
    private User user;

    private final FormField name = new FormField("Full name", "person", "Your full name", false);
    private final FormField email = new FormField("Email address", "mail", "you@example.com", false);
    private final FormField phone = new FormField("Phone (optional)", "person", "0300-1234567", false);
    private final FormField city = new FormField("City", "layers", "Lahore", false);
    private final FormField zone = new FormField("City zone", "layers", "Gulberg", false);
    private final Banner profileBanner = new Banner();

    private final FormField currentPassword = new FormField("Current password", "lock", "Your current password", true);
    private final FormField newPassword = new FormField("New password", "lock", "At least 8 characters, letters and numbers", true);
    private final FormField confirmPassword = new FormField("Repeat the new password", "lock", "Type it again", true);
    private final Banner passwordBanner = new Banner();

    public ProfilePage(PageContext ctx) {
        this.ctx = ctx;
        this.user = ctx.user();
    }

    public Node build() {
        VBox page = Pages.page();
        Outcome<User> fresh = ctx.app().auth().viewProfile(ctx.userId());
        if (fresh.isOk()) {
            user = fresh.value();
        } else {
            profileBanner.showError(fresh.message());
        }
        fill();

        Button save = Widgets.primaryButton("Save changes");
        save.setOnAction(e -> saveProfile());
        Button change = Widgets.primaryButton("Change password");
        change.setOnAction(e -> changePassword());

        VBox info = new VBox(14);
        info.getChildren().add(infoLine("Account type", ctx.role().label()));
        if (user instanceof PlatformRepresentative rep) {
            info.getChildren().add(infoLine("Assigned platform", platformName(rep.getPlatformId())));
        }
        info.getChildren().addAll(name, email);
        if (user instanceof Worker) {
            info.getChildren().addAll(phone, city, zone);
        }
        info.getChildren().addAll(profileBanner, save);
        VBox profileCard = Pages.section("Your details", info);
        profileCard.setMaxWidth(520);

        VBox passwordCard = Pages.section("Change password", currentPassword, newPassword, confirmPassword, passwordBanner, change);
        passwordCard.setMaxWidth(520);

        HBox columns = new HBox(20, profileCard, passwordCard);
        columns.setAlignment(Pos.TOP_LEFT);
        HBox.setHgrow(profileCard, Priority.ALWAYS);
        HBox.setHgrow(passwordCard, Priority.ALWAYS);

        page.getChildren().addAll(Pages.header(NavItem.PROFILE.title(), NavItem.PROFILE.subtitle()), columns);
        return Pages.scroll(page);
    }

    private void fill() {
        name.setText(user.getName());
        email.setText(user.getEmail());
        if (user instanceof Worker w) {
            phone.setText(w.getPhone() == null ? "" : w.getPhone());
            city.setText(w.getCity());
            zone.setText(w.getCityZone());
        }
    }

    private void saveProfile() {
        profileBanner.hide();
        for (FormField f : List.of(name, email, phone, city, zone)) {
            f.setError(null);
        }
        Outcome<User> result = ctx.app().auth().updateProfile(ctx.userId(), name.text(), email.text(),
            phone.text(), city.text(), zone.text());
        if (result.isOk()) {
            user = result.value();
            profileBanner.showSuccess("Your profile was saved.");
            ctx.nav().userUpdated(user);
        } else {
            profileBanner.showError(result.problems());
        }
    }

    private void changePassword() {
        passwordBanner.hide();
        if (!newPassword.text().equals(confirmPassword.text())) {
            passwordBanner.showError("The two new passwords are not the same. Please type them again.");
            return;
        }
        Outcome<Void> result = ctx.app().auth().changePassword(ctx.userId(), currentPassword.text(), newPassword.text());
        if (result.isOk()) {
            currentPassword.setText("");
            newPassword.setText("");
            confirmPassword.setText("");
            passwordBanner.showSuccess("Your password was changed.");
        } else {
            passwordBanner.showError(result.problems());
        }
    }

    private String platformName(Integer platformId) {
        Outcome<List<Platform>> platforms = ctx.app().auth().platformsForRegistration();
        if (platforms.isOk() && platformId != null) {
            return platforms.value().stream().filter(p -> p.getPlatformId().equals(platformId))
                .map(Platform::getName).findFirst().orElse("Platform #" + platformId);
        }
        return "Platform #" + platformId;
    }

    private static Node infoLine(String label, String value) {
        HBox line = new HBox(12, Widgets.caps(label), Widgets.spacer(), Widgets.label(value, "h-card"));
        line.setAlignment(Pos.CENTER_LEFT);
        return line;
    }
}
