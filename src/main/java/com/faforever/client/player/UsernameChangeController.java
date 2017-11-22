package com.faforever.client.player;

import com.faforever.client.fx.Controller;
import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.ImmediateNotification;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.ReportAction;
import com.faforever.client.notification.Severity;
import com.faforever.client.reporting.ReportingService;
import com.faforever.client.user.UserService;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.Collections;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class UsernameChangeController implements Controller<Node> {
  private final UserService userService;
  private final NotificationService notificationService;
  private final I18n i18n;
  private final ReportingService reportingService;
  public TextField usernameField;
  public VBox root;
  public Button changeButton;
  private Runnable callback;
  private ReadOnlyStringProperty displayPlayer;

  @Inject
  public UsernameChangeController(UserService userService, NotificationService notificationService, I18n i18n, ReportingService reportingService) {
    this.userService = userService;
    this.notificationService = notificationService;
    this.i18n = i18n;
    this.reportingService = reportingService;
  }

  @Override
  public void initialize() {
    usernameField.textProperty().addListener((observable, oldValue, newValue) -> changeButton.setDisable(displayPlayer == null || newValue.equals(displayPlayer.get())));
  }

  public void setDisplayPlayer(ReadOnlyStringProperty displayPlayer) {
    this.displayPlayer = displayPlayer;
    ChangeListener<String> changeListener = (observable, oldValue, newValue) -> {
      if (!userService.getUsername().equals(newValue)) {
        root.setVisible(false);
        return;
      }
      usernameField.setText(newValue);
    };
    changeListener.changed(displayPlayer, null, displayPlayer.get());
    displayPlayer.addListener(changeListener);
  }

  @Override
  public Node getRoot() {
    return root;
  }


  public void onUsernameChangeRequested(ActionEvent actionEvent) {
    userService.changeUsername(usernameField.getText())
        .thenAccept(aVoid -> callback.run())
        .exceptionally(throwable -> {
              notificationService.addNotification(new ImmediateNotification(i18n.get("errorTitle"),
                  i18n.get("settings.account.username.error", throwable.getMessage()), Severity.ERROR, Collections.singletonList(new ReportAction(i18n, reportingService, throwable))));

              return null;
            }
        );
  }

  public void registerOnNameChangedCallback(Runnable callback) {
    this.callback = callback;
  }
}
