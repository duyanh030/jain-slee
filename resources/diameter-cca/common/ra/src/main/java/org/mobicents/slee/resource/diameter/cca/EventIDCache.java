package org.mobicents.slee.resource.diameter.cca;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.slee.EventTypeID;
import javax.slee.facilities.EventLookupFacility;
import javax.slee.resource.FireableEventType;

import net.java.slee.resource.diameter.base.events.AbortSessionRequest;
import net.java.slee.resource.diameter.base.events.AccountingRequest;
import net.java.slee.resource.diameter.base.events.CapabilitiesExchangeRequest;
import net.java.slee.resource.diameter.base.events.DeviceWatchdogRequest;
import net.java.slee.resource.diameter.base.events.DisconnectPeerRequest;
import net.java.slee.resource.diameter.base.events.ReAuthRequest;
import net.java.slee.resource.diameter.base.events.SessionTerminationRequest;
import net.java.slee.resource.diameter.cca.events.CreditControlMessage;

import org.jdiameter.api.Message;

/**
 * 
 * Caches event IDs for the Diameter CCA RAs.
 *
 * @author <a href="mailto:brainslog@gmail.com"> Alexandre Mendonca </a>
 * @author <a href="mailto:baranowb@gmail.com"> Bartosz Baranowski </a>
 */
public class EventIDCache {

  private static final String BASE_PACKAGE_PREFIX = "net.java.slee.resource.diameter.base.events.";

  private static final String CCA_PACKAGE_PREFIX = "net.java.slee.resource.diameter.cca.events.";

  public static Map<Integer, String> eventNames = new ConcurrentHashMap<Integer, String>();

  static {
    Map<Integer, String> eventsTemp = new HashMap<Integer, String>();

    eventsTemp.put(CreditControlMessage.commandCode, CCA_PACKAGE_PREFIX + "CreditControl");

    // We support Base messages too, just in case...
    eventsTemp.put(AbortSessionRequest.commandCode, BASE_PACKAGE_PREFIX + "AbortSession");
    eventsTemp.put(AccountingRequest.commandCode, BASE_PACKAGE_PREFIX + "Accounting");
    eventsTemp.put(CapabilitiesExchangeRequest.commandCode, BASE_PACKAGE_PREFIX + "CapabilitiesExchange");
    eventsTemp.put(DeviceWatchdogRequest.commandCode, BASE_PACKAGE_PREFIX + "DeviceWatchdog");
    eventsTemp.put(DisconnectPeerRequest.commandCode, BASE_PACKAGE_PREFIX + "DisconnectPeer");
    eventsTemp.put(ReAuthRequest.commandCode, BASE_PACKAGE_PREFIX + "ReAuth");
    eventsTemp.put(SessionTerminationRequest.commandCode, BASE_PACKAGE_PREFIX + "SessionTermination");

    eventNames = Collections.unmodifiableMap(eventsTemp);
  }

  public static final String ERROR_ANSWER                = BASE_PACKAGE_PREFIX + "ErrorAnswer";
  public static final String EXTENSION_DIAMETER_MESSAGE  = BASE_PACKAGE_PREFIX + "ExtensionDiameterMessage";

  private static final String VENDOR  = "java.net";
  private static final String VERSION = "0.8";

  private ConcurrentHashMap<String, FireableEventType> eventIds = new ConcurrentHashMap<String, FireableEventType>();

  public EventIDCache() {
  }

  /**
   * 
   * @param eventLookupFacility
   * @param message
   * @return
   */
  public FireableEventType getEventId(EventLookupFacility eventLookupFacility, Message message) {

    FireableEventType eventID = null;

    // Error is always the same.
    if (message.isError()) {
      eventID = getEventId(eventLookupFacility, ERROR_ANSWER);
    }
    else {
      final int commandCode = message.getCommandCode();
      final boolean isRequest = message.isRequest();

      String eventName = eventNames.get(commandCode);

      if(eventName != null) {
        eventID = getEventId(eventLookupFacility, eventName + (isRequest ? "Request" : "Answer"));
      }
      else {
        eventID = getEventId(eventLookupFacility, EXTENSION_DIAMETER_MESSAGE);
      }
    }

    return eventID;
  }

  /**
   * 
   * @param eventLookupFacility
   * @param eventName
   * @return
   */
  private FireableEventType getEventId(EventLookupFacility eventLookupFacility, String eventName) {

    FireableEventType eventType = eventIds.get(eventName);
    if (eventType == null) {
      try {
        eventType = eventLookupFacility.getFireableEventType(new EventTypeID(eventName,VENDOR,VERSION));
      }
      catch (Throwable e) {
        e.printStackTrace();
      }
      if (eventType != null) {
        eventIds.put(eventName, eventType);
      }
    }
    return eventType;
  }

}
