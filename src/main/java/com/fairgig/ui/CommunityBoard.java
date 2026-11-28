package com.fairgig.ui;

import com.fairgig.controller.Outcome;
import com.fairgig.service.community.CommentView;
import com.fairgig.service.community.PostView;
import java.util.List;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/** The list of posts, shared by the worker's Community tab and the advocate's Moderation tab. */
public class CommunityBoard extends VBox {

    private final PageContext ctx;
    private final boolean moderator;
    private final Banner banner = new Banner();
    private final VBox list = new VBox(14);

    public CommunityBoard(PageContext ctx, boolean moderator) {
        super(12);
        this.ctx = ctx;
        this.moderator = moderator;
        getChildren().addAll(banner, list);
        reload();
    }

    public void reload() {
        list.getChildren().clear();
        Outcome<List<PostView>> posts = ctx.app().community().board(ctx.userId());
        if (!posts.isOk()) {
            banner.showError(posts.problems());
            return;
        }
        if (posts.value().isEmpty()) {
            list.getChildren().add(Pages.empty("chat", "No posts yet", "Anonymous posts will appear here."));
            return;
        }
        for (PostView post : posts.value()) {
            list.getChildren().add(card(post));
        }
    }

    private VBox card(PostView post) {
        String meta = post.alias() + (post.mine() ? " (you)" : "") + "  -  " + post.category().label()
            + (post.platformName() == null ? "" : "  -  " + post.platformName()) + "  -  " + Fmt.when(post.createdAt());
        VBox card = new VBox(8, Widgets.label(post.title(), "h-card"), Widgets.label(meta, "hint-text"),
            wrapped(post.content()));
        card.getStyleClass().add("clay-card");
        card.setPadding(new Insets(16));

        VBox thread = new VBox(8);
        thread.setVisible(false);
        thread.setManaged(false);

        Button comments = small("Comments");
        comments.setOnAction(e -> {
            boolean open = !thread.isVisible();
            thread.setVisible(open);
            thread.setManaged(open);
            if (open) {
                loadThread(post, thread);
            }
        });
        HBox actions = new HBox(10, Widgets.label(post.likes() + (post.likes() == 1 ? " like" : " likes"), "muted"));
        actions.setAlignment(Pos.CENTER_LEFT);
        if (!moderator) {
            Button like = small(post.likedByMe() ? "Liked" : "Like");
            like.setDisable(post.likedByMe());
            like.setOnAction(e -> {
                Outcome<Void> result = ctx.app().community().like(ctx.userId(), post.postId());
                if (result.isOk()) {
                    reload();
                } else {
                    banner.showError(result.problems());
                }
            });
            actions.getChildren().add(like);
        }
        actions.getChildren().add(comments);
        card.getChildren().addAll(actions, thread);

        if (moderator) {
            FormField reason = new FormField("Reason for removal", "shield", "Why is this post inappropriate?", false);
            Button remove = small("Remove post");
            remove.setOnAction(e -> {
                Outcome<Void> result = ctx.app().community().removePost(ctx.userId(), post.postId(), reason.text());
                if (result.isOk()) {
                    reload();
                    banner.showSuccess("The post was removed and your reason was recorded.");
                } else {
                    reason.setError(result.message());
                }
            });
            card.getChildren().addAll(reason, remove);
        }
        return card;
    }

    private void loadThread(PostView post, VBox thread) {
        thread.getChildren().clear();
        Outcome<List<CommentView>> result = ctx.app().community().comments(ctx.userId(), post.postId());
        if (!result.isOk()) {
            banner.showError(result.problems());
            return;
        }
        FormField input = new FormField("Your comment", "chat", "Write something helpful", false);
        Integer[] replyTo = {null};
        Label replying = Widgets.label("", "hint-text");
        for (CommentView c : result.value()) {
            VBox row = new VBox(2, Widgets.label(c.alias() + (c.mine() ? " (you)" : "") + "  -  "
                + Fmt.when(c.createdAt()), "hint-text"), wrapped(c.content()));
            // replies are indented under the thread
            VBox.setMargin(row, new Insets(0, 0, 0, c.parentCommentId() == null ? 0 : 28));
            if (!moderator) {
                Button reply = small("Reply");
                reply.setOnAction(e -> {
                    replyTo[0] = c.commentId();
                    replying.setText("Replying to " + c.alias());
                });
                row.getChildren().add(reply);
            }
            thread.getChildren().add(row);
        }
        if (result.value().isEmpty()) {
            thread.getChildren().add(Widgets.label("No comments yet.", "muted"));
        }
        if (!moderator) {
            Button send = small("Post comment");
            send.setOnAction(e -> {
                Outcome<CommentView> added = ctx.app().community()
                    .addComment(ctx.userId(), post.postId(), replyTo[0], input.text());
                if (added.isOk()) {
                    loadThread(post, thread);
                } else {
                    input.setError(added.message());
                }
            });
            thread.getChildren().addAll(replying, input, send);
        }
    }

    private static Label wrapped(String text) {
        Label l = Widgets.label(text, "label");
        l.setWrapText(true);
        return l;
    }

    private static Button small(String text) {
        Button b = Widgets.secondaryButton(text);
        b.setMaxWidth(Region.USE_PREF_SIZE);
        b.setStyle("-fx-padding: 6 14 6 14; -fx-font-size: 12px;");
        return b;
    }
}
