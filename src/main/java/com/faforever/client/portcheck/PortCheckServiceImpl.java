package com.faforever.client.portcheck;

import com.faforever.client.fx.HostService;
import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.Action;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.PersistentNotification;
import com.faforever.client.notification.Severity;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.task.TaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;

import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Opens the game port and connects to the FAF relay server in order the see whether data on the game port is received.
 */
public class PortCheckServiceImpl implements PortCheckService {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private static final String PORT_UNREACHABLE_NOTIFICATION_ID = "portUnreachable";

  @Autowired
  TaskService taskService;
  @Autowired
  Environment environment;
  @Autowired
  NotificationService notificationService;
  @Autowired
  PreferencesService preferencesService;
  @Autowired
  HostService hostService;
  @Autowired
  I18n i18n;
  @Autowired
  ApplicationContext applicationContext;

  @Override
  public CompletableFuture<Boolean> checkGamePortInBackground() {
    int port = preferencesService.getPreferences().getForgedAlliance().getPort();

    PortCheckTask task = applicationContext.getBean(PortCheckTask.class);


    return taskService.submitTask(task).thenApply(result -> {
      if (!result) {
        notifyPortUnreachable(port);
      }
      return result;
    }).exceptionally(throwable -> {
      logger.info("Port check failed", throwable);
      notificationService.addNotification(
          new PersistentNotification(i18n.get("portCheckTask.serverUnreachable"), Severity.WARN,
              Collections.singletonList(
                  new Action(
                      i18n.get("portCheckTask.retry"),
                      event -> checkGamePortInBackground()
                  ))
          ));
      return null;
    });
  }

  /**
   * Notifies the user about port unreachability.
   */
  private void notifyPortUnreachable(int port) {
    List<Action> actions = Arrays.asList(
        new Action(
            i18n.get("portCheckTask.help"),
            event -> hostService.showDocument(environment.getProperty("portCheck.helpUrl"))
        ),
        new Action(
            i18n.get("portCheckTask.neverShowAgain"),
            event -> preferencesService.getPreferences().getIgnoredNotifications().add(PORT_UNREACHABLE_NOTIFICATION_ID)
        ),
        new Action(
            i18n.get("portCheckTask.retry"),
            event -> checkGamePortInBackground()
        )
    );

    notificationService.addNotification(
        new PersistentNotification(i18n.get("portCheckTask.unreachableNotification", port), Severity.WARN, actions)
    );
  }
}
