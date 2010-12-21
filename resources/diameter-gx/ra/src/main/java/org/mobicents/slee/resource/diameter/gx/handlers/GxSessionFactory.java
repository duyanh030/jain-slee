/*
 * JBoss, Home of Professional Open Source
 *
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a full listing
 * of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU General Public License, v. 2.0.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License,
 * v. 2.0 along with this distribution; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301, USA.
 */
package org.mobicents.slee.resource.diameter.gx.handlers;

import org.jdiameter.api.InternalException;
import org.jdiameter.api.SessionFactory;
import org.jdiameter.api.app.AppAnswerEvent;
import org.jdiameter.api.app.AppRequestEvent;
import org.jdiameter.api.app.AppSession;
import org.jdiameter.api.auth.events.ReAuthAnswer;
import org.jdiameter.api.auth.events.ReAuthRequest;
import org.jdiameter.api.gx.ClientGxSession;
import org.jdiameter.api.gx.ServerGxSession;
import org.jdiameter.api.gx.events.GxCreditControlAnswer;
import org.jdiameter.api.gx.events.GxCreditControlRequest;
import org.jdiameter.common.impl.app.gx.GxSessionFactoryImpl;
import org.mobicents.slee.resource.diameter.base.handlers.DiameterRAInterface;

/**
 * @author <a href="mailto:baranowb@gmail.com"> Bartosz Baranowski </a>
 * @author <a href="mailto:brainslog@gmail.com"> Alexandre Mendonca </a>
 * @author <a href="mailto:carl-magnus.bjorkell@emblacom.com"> Carl-Magnus Björkell </a>
 */
public class GxSessionFactory extends GxSessionFactoryImpl {

    public DiameterRAInterface ra;

    /**
     * @param sessionFactory
     */
    public GxSessionFactory(DiameterRAInterface ra, SessionFactory sessionFactory, int defaultDirectDebitingFailureHandling,
                            int defaultCreditControlFailureHandling, long defaultValidityTime, long defaultTxTimerValue) {
        super(sessionFactory);

        super.defaultDirectDebitingFailureHandling = defaultDirectDebitingFailureHandling;
        super.defaultCreditControlFailureHandling = defaultCreditControlFailureHandling;
        super.defaultValidityTime = defaultValidityTime;
        super.defaultTxTimerValue = defaultTxTimerValue;

        this.ra = ra;
    }

    @Override
    public void doCreditControlAnswer(ClientGxSession session, GxCreditControlRequest request, GxCreditControlAnswer answer) throws InternalException {
        ra.fireEvent(session.getSessionId(), answer.getMessage());
    }

    @Override
    public void doCreditControlRequest(ServerGxSession session, GxCreditControlRequest request) throws InternalException {
        ra.fireEvent(session.getSessionId(), request.getMessage());
    }

    @Override
    public void doOtherEvent(AppSession session, AppRequestEvent request, AppAnswerEvent answer) throws InternalException {
        if (request != null) {
            ra.fireEvent(session.getSessionId(), request.getMessage());
        } else {
            ra.fireEvent(session.getSessionId(), answer.getMessage());
        }
    }

    @Override
    public void doReAuthAnswer(ServerGxSession session, ReAuthRequest request, ReAuthAnswer answer) throws InternalException {
        ra.fireEvent(session.getSessionId(), answer.getMessage());
    }

    @Override
    public void doReAuthRequest(ClientGxSession session, ReAuthRequest request) throws InternalException {
        ra.fireEvent(session.getSessionId(), request.getMessage());
    }
}