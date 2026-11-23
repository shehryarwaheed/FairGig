package com.fairgig.ui;

import com.fairgig.ui.advocate.AdvocatePages;
import com.fairgig.ui.verifier.VerifierPages;
import com.fairgig.ui.worker.WorkerPages;
import java.util.List;

/** The three roles as the prototype UI sees them (colors, sample user, tabs). */
public enum UiRole {

    WORKER("Worker", "role-worker", "pill-worker", "person", "Ali Raza", "AR",
        WorkerPages.nav(), WorkerPages.tiles()),
    REPRESENTATIVE("Representative", "role-rep", "pill-rep", "check", "Hina Malik", "HM",
        VerifierPages.nav(), VerifierPages.tiles()),
    ADVOCATE("Advocate", "role-advocate", "pill-advocate", "shield", "Maryam Tariq", "MT",
        AdvocatePages.nav(), AdvocatePages.tiles());

    private final String label;
    private final String cssClass;
    private final String pillClass;
    private final String icon;
    private final String userName;
    private final String initials;
    private final List<NavItem> nav;
    private final List<TileSpec> tiles;

    UiRole(String label, String cssClass, String pillClass, String icon, String userName,
           String initials, List<NavItem> nav, List<TileSpec> tiles) {
        this.label = label;
        this.cssClass = cssClass;
        this.pillClass = pillClass;
        this.icon = icon;
        this.userName = userName;
        this.initials = initials;
        this.nav = nav;
        this.tiles = tiles;
    }

    public String label() { return label; }
    public String cssClass() { return cssClass; }
    public String pillClass() { return pillClass; }
    public String icon() { return icon; }
    public String userName() { return userName; }
    public String initials() { return initials; }
    public List<NavItem> nav() { return nav; }
    public List<TileSpec> tiles() { return tiles; }
}
