/*
 * Copyright (c) 1997, 2018 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.sun.xml.ws.rx.rm.runtime.sequence;

import com.sun.istack.NotNull;
import com.sun.xml.ws.rx.rm.runtime.ApplicationMessage;
import com.sun.xml.ws.rx.rm.runtime.delivery.DeliveryQueue;
import com.sun.xml.ws.rx.rm.runtime.delivery.DeliveryQueueBuilder;
import com.sun.xml.ws.rx.util.TimeSynchronizer;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Provides abstract sequence implementation common to both - inbound and outbound
 * sequence
 * 
 * @author Marek Potociar (marek.potociar at sun.com)
 */
public abstract class AbstractSequence implements Sequence {

    protected final SequenceData data;
    private final DeliveryQueue deliveryQueue;
    private final TimeSynchronizer timeSynchronizer;

    /**
     * Initializes instance fields.
     * 
     * @param id sequence identifier
     * 
     * @param securityContextTokenId security context token identifier bound to this sequence
     * 
     * @param expirationTime sequence expiration time
     */
    @SuppressWarnings("LeakingThisInConstructor")
    AbstractSequence(@NotNull SequenceData data, @NotNull DeliveryQueueBuilder deliveryQueueBuilder, @NotNull TimeSynchronizer timeSynchronizer) {
        assert data != null;
        assert deliveryQueueBuilder != null;
        assert timeSynchronizer != null;


        this.data = data;
        this.timeSynchronizer = timeSynchronizer;

        deliveryQueueBuilder.sequence(this);
        this.deliveryQueue = deliveryQueueBuilder.build();
    }

    public String getId() {
        return data.getSequenceId();
    }

    public String getBoundSecurityTokenReferenceId() {
        return data.getBoundSecurityTokenReferenceId();
    }

    public long getLastMessageNumber() {
        return data.getLastMessageNumber();
    }

    public List<AckRange> getAcknowledgedMessageNumbers() {
        List<Long> values = data.getLastMessageNumberWithUnackedMessageNumbers();

        final long lastMessageNumber = values.remove(0);
        final List<Long> unackedMessageNumbers = values;

        if (lastMessageNumber == Sequence.UNSPECIFIED_MESSAGE_ID) {
            // no message associated with the sequence yet
            return Collections.emptyList();
        } else if (unackedMessageNumbers.isEmpty()) {
            // no unacked indexes - we have a single acked range
            return Arrays.asList(new AckRange(Sequence.MIN_MESSAGE_ID, lastMessageNumber));
        } else {
            // need to calculate ranges from the unacked indexes
            List<AckRange> result = new LinkedList<Sequence.AckRange>();

            long lastBottomAckRange = Sequence.MIN_MESSAGE_ID;
            for (long lastUnacked : unackedMessageNumbers) {
                if (lastBottomAckRange < lastUnacked) {
                    result.add(new AckRange(lastBottomAckRange, lastUnacked - 1));
                }
                lastBottomAckRange = lastUnacked + 1;
            }
            if (lastBottomAckRange <= lastMessageNumber) {
                result.add(new AckRange(lastBottomAckRange, lastMessageNumber));
            }

            return result;
        }
    }

    public boolean isAcknowledged(long messageNumber) {
        boolean result = false;
        List<AckRange> listOfAckRange = getAcknowledgedMessageNumbers();
        for (AckRange ackRange : listOfAckRange) {
            if (messageNumber <= ackRange.upper && messageNumber >= ackRange.lower) {
                result = true;
                break;
            }
        }
        return result;
    }

    public boolean hasUnacknowledgedMessages() {
        return !data.getUnackedMessageNumbers().isEmpty();
    }

    public State getState() {
        return data.getState();
    }

    public void setAckRequestedFlag() {
        data.setAckRequestedFlag(true);
    }

    public void clearAckRequestedFlag() {
        data.setAckRequestedFlag(false);
    }

    public boolean isAckRequested() {
        return data.getAckRequestedFlag();
    }
    
    public boolean isFailedOver(long messageNumber) {
        return data.isFailedOver(messageNumber);
    }

    public void updateLastAcknowledgementRequestTime() {
        data.setLastAcknowledgementRequestTime(timeSynchronizer.currentTimeInMillis());
    }

    public long getLastActivityTime() {
        return data.getLastActivityTime();
    }

    public boolean isStandaloneAcknowledgementRequestSchedulable(long delayPeriod) {
        return timeSynchronizer.currentTimeInMillis() - data.getLastAcknowledgementRequestTime() > delayPeriod && hasUnacknowledgedMessages();
    }

    public void close() {
        data.setState(State.CLOSED);
        deliveryQueue.close();
    }

    public boolean isClosed() {
        State currentStatus = data.getState();
        return currentStatus == State.CLOSING || currentStatus == State.CLOSED || currentStatus == State.TERMINATING;
    }

    public boolean isExpired() {
        return (data.getExpirationTime() == Sequence.NO_EXPIRY) ? false : timeSynchronizer.currentTimeInMillis() > data.getExpirationTime();
    }

    public void preDestroy() {
        data.setState(State.TERMINATING);

        // nothing else to do...
    }

    public SequenceData getData() {
        return data;
    }
    
    public ApplicationMessage retrieveMessage(String correlationId) {
        return data.retrieveMessage(correlationId);
    }

    public DeliveryQueue getDeliveryQueue() {
        return deliveryQueue;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        if (this.getClass() != obj.getClass()) {
            return false;
        }
        final AbstractSequence other = (AbstractSequence) obj;
        if ((this.data.getSequenceId() == null) ? (other.data.getSequenceId() != null) : !this.data.getSequenceId().equals(other.data.getSequenceId())) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 41 * hash + (this.data.getSequenceId() != null ? this.data.getSequenceId().hashCode() : 0);
        return hash;
    }
}
