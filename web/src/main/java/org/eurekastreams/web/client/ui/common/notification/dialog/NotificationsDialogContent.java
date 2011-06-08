/*
 * Copyright (c) 2010-2011 Lockheed Martin Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.eurekastreams.web.client.ui.common.notification.dialog;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.eurekastreams.server.domain.InAppNotificationDTO;
import org.eurekastreams.web.client.events.DialogLinkClickedEvent;
import org.eurekastreams.web.client.events.EventBus;
import org.eurekastreams.web.client.events.Observer;
import org.eurekastreams.web.client.events.UnreadNotificationClearedEvent;
import org.eurekastreams.web.client.events.data.GotNotificationListResponseEvent;
import org.eurekastreams.web.client.model.NotificationListModel;
import org.eurekastreams.web.client.ui.Session;
import org.eurekastreams.web.client.ui.common.dialog.BaseDialogContent;
import org.eurekastreams.web.client.ui.pages.master.CoreCss;
import org.eurekastreams.web.client.ui.pages.master.StaticResourceBundle;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Style.Display;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * Dialog content (i.e. main panel) for showing notifications.
 */
public class NotificationsDialogContent extends BaseDialogContent
{
    /** Main content widget. */
    private final Widget main;

    /** To unwire the observer when done with dialog. */
    private Observer<DialogLinkClickedEvent> linkClickedObserver;

    /** Binder for building UI. */
    private static LocalUiBinder binder = GWT.create(LocalUiBinder.class);

    /** Local styles. */
    @UiField
    LocalStyle style;

    /** Global CSS. */
    @UiField(provided = true)
    CoreCss coreCss;

    /** The list of sources. */
    @UiField
    FlowPanel sourceFiltersPanel;

    /** Scroll panel holding the notification list. */
    @UiField
    ScrollPanel notificationListScrollPanel;

    /** The displayed list of notifications. */
    @UiField
    FlowPanel notificationListPanel;

    /** Element to indicate no notifications. */
    @UiField
    DivElement noNotificationsUi;

    /** Notifications. */
    private List<InAppNotificationDTO> allNotifications;

    /** The IDs of the notifications currently being displayed. */
    private final Collection<Long> idsShowing = new ArrayList<Long>();

    /** Source representing all notifications. */
    private Source rootSource;

    /** Index of actual sources. */
    private Map<String, Source> sourceIndex;

    /** Currently-selected source. */
    private Source currentSource;

    /** Currently selected show read option. */
    private final boolean currentShowRead = false;

    /** Observer (allow unlinking). */
    private final Observer<UnreadNotificationClearedEvent> unreadNotificationClearedObserver = // \n
    new Observer<UnreadNotificationClearedEvent>()
    {
        public void update(final UnreadNotificationClearedEvent ev)
        {
            reduceUnreadCount(ev.getNotification());
        }
    };

    /**
     * Constructor.
     */
    public NotificationsDialogContent()
    {
        coreCss = StaticResourceBundle.INSTANCE.coreCss();
        main = binder.createAndBindUi(this);

        // // -- build UI --
        // main.addStyleName(StaticResourceBundle.INSTANCE.coreCss().notifDialogMain());
        //
        // Hyperlink editSettings =
        // new Hyperlink("edit settings", Session.getInstance().generateUrl(
        // new CreateUrlRequest(Page.SETTINGS, null, "tab", "Notifications")));
        // editSettings.addStyleName(StaticResourceBundle.INSTANCE.coreCss().notifEditSettingsLink());
        // main.add(editSettings);
        //
        // scrollPanel.addStyleName(StaticResourceBundle.INSTANCE.coreCss().notifScrollList());
        //
        // scrollPanel.add(listPanel);
        //
        // main.add(scrollPanel);
        // listPanel.addStyleName(StaticResourceBundle.INSTANCE.coreCss().notifWait());

        // -- setup events --
        final EventBus eventBus = Session.getInstance().getEventBus();

        eventBus.addObserver(GotNotificationListResponseEvent.class, new Observer<GotNotificationListResponseEvent>()
        {
            public void update(final GotNotificationListResponseEvent ev)
            {
                eventBus.removeObserver(ev, this);
                storeReceivedNotifications(ev.getResponse());
                selectSource(currentSource);
            }
        });

        eventBus.addObserver(UnreadNotificationClearedEvent.class, unreadNotificationClearedObserver);

        // Since none of the links cause a full page load (which would annihilate the dialog), we must explicitly close
        // the dialog. We cannot count on a history change event (or any of the related events) because the user may
        // already be on the exact page to which the link would send them. (If clicking a link would cause no change to
        // the URL, the GWT does not raise the event.) So we close the dialog on a link being clicked. We directly
        // listen on the "edit settings" link, and have the links in notifications raise an event we listen to.

        // editSettings.addClickHandler(new ClickHandler()
        // {
        // public void onClick(final ClickEvent inArg0)
        // {
        // close();
        // }
        // });
        //
        // linkClickedObserver = new Observer<DialogLinkClickedEvent>()
        // {
        // public void update(final DialogLinkClickedEvent inArg1)
        // {
        // close();
        // }
        // };
        // Session.getInstance().getEventBus().addObserver(DialogLinkClickedEvent.class, linkClickedObserver);

        // -- request data --
        NotificationListModel.getInstance().fetch(null, false);
    }

    /**
     * Invoked on closing before the dialog is removed from screen.
     */
    @Override
    public void beforeHide()
    {
        if (linkClickedObserver != null)
        {
            Session.getInstance().getEventBus().removeObserver(DialogLinkClickedEvent.class, linkClickedObserver);
            linkClickedObserver = null;
        }
        EventBus.getInstance().removeObserver(UnreadNotificationClearedEvent.class, unreadNotificationClearedObserver);
    }

    /**
     * {@inheritDoc}
     */
    public Widget getBody()
    {
        return main;
    }

    /**
     * {@inheritDoc}
     */
    public String getTitle()
    {
        return "Notifications";
    }

    /**
     * Reduces the unread count for all applicable sources.
     *
     * @param item
     *            Notification read / deleted.
     */
    private void reduceUnreadCount(final InAppNotificationDTO item)
    {
        // Source source = rootSource;
        //
        // if (item.getSourceType() != null && item.getSourceUniqueId() != null)
        // {
        // source = sourceIndex.get(item.getSourceType() + item.getSourceUniqueId());
        // }

        Source source = sourceIndex.get(item.getSourceType() + item.getSourceUniqueId());
        if (source == null)
        {
            source = rootSource;
        }

        // work from the specific source up, reducing the unread count
        while (source != null)
        {
            int count = source.getUnreadCount() - 1;
            source.setUnreadCount(count);
            String text = count > 0 ? source.getDisplayName() + " (" + count + ")" : source.getDisplayName();
            source.getWidget().setText(text);
            source = source.getParent();
        }
    }

    /**
     * Handles the received list of notifications.
     *
     * @param list
     *            List of notifications.
     */
    private void storeReceivedNotifications(final List<InAppNotificationDTO> list)
    {
        allNotifications = list;

        SourceListBuilder builder = new SourceListBuilder(list, Session.getInstance().getCurrentPerson()
                .getAccountId());
        rootSource = builder.getRootSource();
        sourceIndex = builder.getSourceIndex();

        for (Source source : builder.getSourceList())
        {
            addSourceFilter(source, source.getParent() != null && source.getParent() != rootSource);
        }

        currentSource = rootSource;
    }

    /**
     * Creates and adds the widget for a source filter.
     *
     * @param source
     *            Source data.
     * @param indent
     *            If the label should be indented.
     */
    private void addSourceFilter(final Source source, final boolean indent)
    {
        int count = source.getUnreadCount();
        String text = count > 0 ? source.getDisplayName() + " (" + count + ")" : source.getDisplayName();

        final Label label = new Label(text);
        label.addStyleName(style.sourceFilter());
        label.addStyleName(StaticResourceBundle.INSTANCE.coreCss().buttonLabel());
        if (indent)
        {
            label.addStyleName(style.sourceFilterIndented());
        }
        label.addClickHandler(new ClickHandler()
        {
            public void onClick(final ClickEvent inEvent)
            {
                selectSource(source);
            }
        });

        sourceFiltersPanel.add(label);

        source.setWidget(label);
    }

    /**
     * Updates the display to show a new source.
     *
     * @param newSource
     *            New source.
     */
    private void selectSource(final Source newSource)
    {
        currentSource.getWidget().removeStyleName(style.sourceFilterSelected());

        currentSource = newSource;
        currentSource.getWidget().addStyleName(style.sourceFilterSelected());
        displayNotifications(currentSource.getFilter(), currentShowRead);
    }

    /**
     * Displays the appropriate subset of notifications.
     *
     * @param filter
     *            Filter for notifications.
     * @param showRead
     *            If read notifications should be displayed (unread are always displayed).
     */
    private void displayNotifications(final Source.Filter filter, final boolean showRead)
    {
        noNotificationsUi.getStyle().setDisplay(Display.NONE);
        notificationListScrollPanel.setVisible(false);

        notificationListPanel.clear();
        idsShowing.clear();

        for (InAppNotificationDTO item : allNotifications)
        {
            if (filter.shouldDisplay(item) && (showRead || !item.isRead()))
            {
                idsShowing.add(item.getId());
                notificationListPanel.add(new NotificationWidget(item));

            }
        }
        if (idsShowing.isEmpty())
        {
            noNotificationsUi.getStyle().clearDisplay();
        }
        else
        {
            notificationListScrollPanel.scrollToTop();
            notificationListScrollPanel.setVisible(true);
        }
    }

    /**
     * Local styles.
     */
    interface LocalStyle extends CssResource
    {
        /** @return Style for sources. */
        @ClassName("source-filter")
        String sourceFilter();

        /** @return Added style for the selected source. */
        @ClassName("source-filter-selected")
        String sourceFilterSelected();

        /** @return Added style for indented sources. */
        @ClassName("source-filter-indented")
        String sourceFilterIndented();
    }

    /**
     * Binder for building UI.
     */
    interface LocalUiBinder extends UiBinder<Widget, NotificationsDialogContent>
    {
    }
}
